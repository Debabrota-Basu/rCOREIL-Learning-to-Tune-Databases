/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sg.edu.nus.autotune;

import static edu.ucsc.dbtune.DatabaseSystem.newDatabaseSystem;
import static edu.ucsc.dbtune.optimizer.DB2Optimizer.SELECT_FROM_EXPLAIN;
import static edu.ucsc.dbtune.optimizer.DB2Optimizer.SELECT_FROM_EXPLAIN_FOR_UPDATE;
import static edu.ucsc.dbtune.util.MetadataUtils.getMaterializationStatement;
import static org.slf4j.LoggerFactory.getLogger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import javax.sql.rowset.CachedRowSet;

import com.sun.rowset.CachedRowSetImpl;

import org.slf4j.Logger;

import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import edu.ucsc.dbtune.DatabaseSystem;
import edu.ucsc.dbtune.metadata.Column;
import edu.ucsc.dbtune.metadata.Index;
import edu.ucsc.dbtune.metadata.Schema;
import edu.ucsc.dbtune.metadata.Table;
import edu.ucsc.dbtune.optimizer.DB2Optimizer;
import edu.ucsc.dbtune.optimizer.Optimizer;
import edu.ucsc.dbtune.util.Environment;
import edu.ucsc.dbtune.workload.SQLCategory;
import edu.ucsc.dbtune.workload.SQLStatement;
import edu.ucsc.dbtune.workload.Workload;

/**
 *
 * @author Chen Weidong
 */
public class DB2DATA implements DataConnectivity {

    public final static String SELECT_FROM_EXPLAIN_NEW = "SELECT TOTAL_COST FROM systools.explain_operator WHERE OPERATOR_TYPE = 'RETURN'";

    protected static DatabaseSystem db;

    protected static Environment en;

    protected static Optimizer io;

    private final static Logger LOG = getLogger("e2s2");

    private final static int _maxIndexLength = 2;

    private static List<Column> _columns;

    private static List<String> _colNames;

    private static List<Set<Integer>> _columnsPerTable;

    private static List<List<Integer>> _allIndexes;

    private static List<BitSet> _coveredBy;

    private static List<BitSet> _covering;

    private static HashMap<Integer, String> _primaryKey;

    private static Set<Integer> _primaryCol;

    private static Set<Index> _systemIndex;
    
    public static boolean bWFIT;
    
    public static String workload;

    public static void dropAllIndexes() throws SQLException {
        String drop_all = "select INDNAME, SYSTEM_REQUIRED from syscat.indexes where tabschema = 'DB2ADMIN'";
        Connection con = db.getConnection();
        Statement stmt = con.createStatement();

        ResultSet rs = stmt.executeQuery(drop_all);
        while (rs.next()) {
            StringBuilder str = new StringBuilder();

            if (rs.getBoolean(2)) {
                continue;
            }

            str.append("DROP INDEX " + rs.getString(1));
            execute(str.toString());
        }

        stmt.close();

    }

    public static double execute(String sql) throws SQLException {
        
        LOG.info(sql + ";");

        SQLStatement sqls = new SQLStatement(sql);
        if (sqls.getSQLCategory().isSame(SQLCategory.NOT_SELECT)) {
            return executeUpdate(sql);
        } else {
            return executeQuery(sql);
        }
    }

    private static double executeQuery(String sql) throws SQLException {
        Connection con = db.getConnection();

        PreparedStatement ps = con.prepareStatement(sql);
        double startTime = System.nanoTime();
        ps.execute();
        double endTime = System.nanoTime();
        double totalTime = endTime - startTime;

        ps.close();

        return totalTime / 1000000.0;
    }

    private static double executeUpdate(String sql) throws SQLException {
        Connection con = db.getConnection();

        PreparedStatement ps = con.prepareStatement(sql);
        ps.addBatch();

        double startTime = System.nanoTime();
        ps.executeBatch();
        double endTime = System.nanoTime();
        double totalTime = endTime - startTime;

        ps.clearBatch();
        ps.close();

        return totalTime / 1000000.0;
    }

    public static void explain(String sql) throws SQLException {
        Connection con = db.getConnection();
        DB2Optimizer.clearAdviseAndExplainTables(con);
        Statement stmt = con.createStatement();

        stmt.execute("SET CURRENT EXPLAIN MODE = EVALUATE INDEXES");
        stmt.execute(sql);
        stmt.execute("SET CURRENT EXPLAIN MODE = NO");

        ResultSet rs_select = stmt.executeQuery(SELECT_FROM_EXPLAIN);
        printResultSet(rs_select);
        ResultSet rs_update = stmt.executeQuery(SELECT_FROM_EXPLAIN_FOR_UPDATE);
        printResultSet(rs_update);

        stmt.close();
//		DB2Optimizer.clearAdviseAndExplainTables(con);
    }

    public static BitSet getAddCandidateColumns(String sql) throws SQLException {

        BitSet rec = recommend(sql);
        BitSet cols = new BitSet();
        for (int i = rec.nextSetBit(0); i >= 0; i = rec.nextSetBit(i + 1)) {
            for (int j : DB2DATA.getAllIndexes().get(i)) {
                cols.set(j);
            }
        }
        return cols;
    }

    public static BitSet getAddCandidateBig(String sql, AbsConf s) throws SQLException {

        BitSet cand = new BitSet();
        BitSet candAddCol = DB2DATA.getAddCandidateColumns(sql);
        for (int i = 0; i < DB2DATA.getNumOfIndexes(); i++) {
            if (!s.contains(i) && !DB2DATA.isPrimaryKey(i)) {
                if (candAddCol.get(DB2DATA.getAllIndexes().get(i).get(0))) {
                    cand.set(i);
                }
            }
        }

        return cand;
    }

    public static BitSet getAddCandidateBig(String sql) throws SQLException {

        BitSet cand = new BitSet();
        BitSet candAddCol = DB2DATA.getAddCandidateColumns(sql);
        for (int i = 0; i < DB2DATA.getNumOfIndexes(); i++) {
            if (!DB2DATA.isPrimaryKey(i)) {
                if (candAddCol.get(DB2DATA.getAllIndexes().get(i).get(0))) {
                    cand.set(i);
                }
            }
        }

        return cand;

    }

    public static BitSet getAddCandidate(String sql, AbsConf s) throws SQLException {
        BitSet cand = getAddCandidate(sql);
        BitSet bs = s.toBitSet();

        for (int i = cand.nextSetBit(0); i >= 0; i = cand.nextSetBit(i + 1)) {
            if (bs.get(i)) {
                cand.set(i, false);
            }
        }
        return cand;
    }

    public static BitSet getAddCandidate(String sql) throws SQLException {
        BitSet cand = new BitSet();
        BitSet rec = recommend(sql);
        for (int i = rec.nextSetBit(0); i >= 0; i = rec.nextSetBit(i + 1)) {
            BitSet bs = getCoveredBy(i);
            for (int j = bs.nextSetBit(0); j >= 0; j = bs.nextSetBit(j + 1)) {
                cand.set(j);
            }
        }
        return cand;
    }

    public static List<List<Integer>> getAllIndexes() {
        return _allIndexes;
    }

    public static Column getColumn(int i) {
        return _columns.get(i);
    }

    public static int getColumnID(Column col) {
        return _columns.indexOf(col);
    }

    public static String getColumnName(int i) {
        return _colNames.get(i);
    }

    public static BitSet getCoveredBy(int i) {
        return _coveredBy.get(i);
    }

    public static BitSet getCovering(int i) {
        return _covering.get(i);
    }

    public static CachedRowSet excute(String sql) throws SQLException {
        Connection con = db.getConnection();
        Statement stmt = con.createStatement();
        ResultSet rs = stmt.executeQuery(sql);
        CachedRowSetImpl crs = new CachedRowSetImpl();
        crs.populate(rs);
        stmt.close();
        return crs;
    }

    public static AbsConf getCurrentConfigration() throws SQLException {
        String sql = "select INDNAME, TABNAME, COLNAMES, SYSTEM_REQUIRED from syscat.indexes where tabschema = 'DB2ADMIN'";
        Connection con = db.getConnection();
        Statement stmt = con.createStatement();

        Set<Integer> conf = new HashSet<>();

        _primaryKey = new HashMap<>();
        _primaryCol = new HashSet<>();

        ResultSet rs = stmt.executeQuery(sql);
        while (rs.next()) {
            String idxName = rs.getString(1);
            String tableName = rs.getString(2);
            String colNames = rs.getString(3).substring(1);
            Boolean isRequired = rs.getBoolean(4);

            if (isRequired) {
                continue;
            }

            List<Integer> colIDs = new ArrayList<>();
            String[] colNameArray = colNames.split("\\+");

            for (String str : colNameArray) {
                String colName = tableName + "." + str;
                colIDs.add(_colNames.indexOf(colName));
            }

            int ID = _allIndexes.indexOf(colIDs);

            conf.add(ID);

            if (isRequired) {
                _primaryKey.put(ID, idxName);
                _primaryCol.addAll(colIDs);
                AbsIndex idx = new AbsIndex(ID);
                _systemIndex.add(idx.toIndexSimple());
//                _systemIndex.add(idx.toIndex());
            }
        }

        _primaryKey.put(0, "EmptyIndex");
        conf.add(0);

        stmt.close();
        return new AbsConf(conf);
    }

    public static Set<Index> systemIndex() {
        return _systemIndex;
    }

    public static BitSet getDropCandidate(String sql, AbsConf s) throws SQLException {
        BitSet cand = new BitSet();
        
        SQLStatement sqls = new SQLStatement(sql);
        if (!sqls.getSQLCategory().isSame(SQLCategory.NOT_SELECT)) {
            return cand;
        }
        
        BitSet cols = getUpdatedCols(sql);

        BitSet bs = s.toBitSet();

        for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
            if (isPrimaryKey(i)) {
                continue;
            }
            List<Integer> ints = _allIndexes.get(i);
            for (int j = cols.nextSetBit(0); j >= 0; j = cols.nextSetBit(j + 1)) {
                if (ints.contains(j)) {
                    cand.set(i);
                    break;
                }
            }
        }

        return cand;
    }

    public static BitSet getDropCandidate(String sql) throws SQLException {
        BitSet cand = new BitSet();
        SQLStatement sqls = new SQLStatement(sql);
        
        if (!sqls.getSQLCategory().isSame(SQLCategory.NOT_SELECT)) {
            return cand;
        }
        
        BitSet cols = getUpdatedCols(sql);
        for (int i = 0; i < DB2DATA.getNumOfIndexes(); i++) {

            if (isPrimaryKey(i)) {
                continue;
            }
            List<Integer> ints = _allIndexes.get(i);
            for (int j = cols.nextSetBit(0); j >= 0; j = cols.nextSetBit(j + 1)) {
                if (ints.contains(j)) {
                    cand.set(i);
                    break;
                }
            }
        }

        return cand;
    }

    public static int getMaxIndexLength() {
        return _maxIndexLength;
    }

    public static int getNumOfIndexes() {
        return _allIndexes.size();
    }

    public static String getPrimaryKeyName(int i) {
        if (isPrimaryKey(i)) {
            return _primaryKey.get(i);
        }
        throw new RuntimeException("Index " + i + " is not primary key!");
    }

    public static BitSet getUpdatedCols(String sql) throws SQLException {
        return obtainAffectCols(sql, SELECT_FROM_EXPLAIN_FOR_UPDATE);
    }

    public static boolean isPrimaryKey(int i) {
        return _primaryKey.containsKey(i);
    }

    public static Set<Integer> getPrimaryCol() {
        return _primaryCol;
    }

    public static BitSet obtainAffectCols(String sql, String STATEMENT) throws SQLException {
        //		Set<Integer> list = new HashSet<Integer>();
        BitSet list = new BitSet();

        Connection con = db.getConnection();
        DB2Optimizer.clearAdviseAndExplainTables(con);
        Statement stmt = con.createStatement();

        stmt.execute("SET CURRENT EXPLAIN MODE = EVALUATE INDEXES");
        stmt.execute(sql);
        stmt.execute("SET CURRENT EXPLAIN MODE = NO");

        ResultSet rs = stmt.executeQuery(STATEMENT);
        ResultSetMetaData rsmd = rs.getMetaData();
        int columnsNumber = rsmd.getColumnCount();

        int name_id = -1;
        int col_id = -1;

        for (int i = 1; i <= columnsNumber; i++) {
            switch (rsmd.getColumnName(i)) {
                case "OBJECT_NAME":
                    name_id = i;
                    break;
                case "COLUMN_NAMES":
                    col_id = i;
                    break;
            }
        }

        while (rs.next()) {
            String tableName = rs.getString(name_id);
            String columnValue = rs.getString(col_id);

            if (columnValue == null) {
                continue;
            }

            int l = columnValue.length();
            int fromIndex = 0;
            while (columnValue.indexOf(".", fromIndex) >= 0) {
                int a = columnValue.indexOf(".", fromIndex);
                int b = columnValue.indexOf("+", a);
                String str;
                if (b == -1) {
                    str = columnValue.substring(a + 1, l);
                } else {
                    str = columnValue.substring(a + 1, b);
                }
                if (!str.equals("$RID$")) {
                    str = tableName + "." + str;
                    list.set(_colNames.indexOf(str));
                }
                fromIndex = a + 1;
            }

        }

        stmt.close();
        return list;
    }

    public static void printResultSet(ResultSet rs) throws SQLException {
        ResultSetMetaData rsmd = rs.getMetaData();
        int columnsNumber = rsmd.getColumnCount();

        for (int i = 1; i <= columnsNumber; i++) {
            if (i > 1) {
                System.out.print("\t");
            }
            System.out.print(rsmd.getColumnName(i));
        }
        System.out.println();

        while (rs.next()) {

            for (int i = 1; i <= columnsNumber; i++) {
                if (i > 1) {
                    System.out.print(",  ");
                }
                String columnValue = rs.getString(i);
                System.out.print(columnValue);
            }
            System.out.println();
        }
    }

    //return set of index id
    public static BitSet recommend(String sql) throws SQLException {
        Connection con = db.getConnection();
        DB2Optimizer.clearAdviseAndExplainTables(con);
        Statement stmt = con.createStatement();

//        DB2Optimizer.clearAdviseAndExplainTables(con);
        try {
            stmt.execute("SET CURRENT EXPLAIN MODE = RECOMMEND INDEXES");
            stmt.execute(sql);
            stmt.execute("SET CURRENT EXPLAIN MODE = NO");
            stmt.close();
        } catch (SQLException e) {
            stmt.execute("SET CURRENT EXPLAIN MODE = NO");
            stmt.close();
            throw e;
        }

        Set<Index> rec_indexes = DB2Optimizer.readAdviseIndexTable(con,
                db.getCatalog());

        BitSet bs = new BitSet();
        for (Index idx : rec_indexes) {
            AbsIndex aIdx = new AbsIndex(idx);
            bs.set(aIdx.getID());
        }
        return bs;
    }

    public static double whatif_getAddIndexCost(AbsConf s, int i) throws SQLException {
        if (s.contains(i)) {
            throw new RuntimeException("This Conf already contains index ID "
                    + i + "!");
        }

        if (isPrimaryKey(i)) {
            throw new RuntimeException("Index ID " + i + "is primary key!");
        }

        AbsIndex aIdx = new AbsIndex(i);

        return whatif_getAddIndexCost(s.toSetIndex(), aIdx.toIndex());
    }

    public static double whatif_getAddIndexCost(Set<Index> indexes, Index index) throws SQLException {
//          return io.explain(getMaterializationStatement(index), new HashSet<Index>()).getSelectCost();
        return whatif_getExecCost(getMaterializationStatement(index), indexes);
    }

    public static double whatif_getDropIndexCost(AbsConf s, int i) throws SQLException {
        if (!s.contains(i)) {
            throw new RuntimeException("This Conf does not contains index ID "
                    + i + "!");
        }

        if (isPrimaryKey(i)) {
            throw new RuntimeException("Index ID " + i + "is primary key!");
        }

        AbsIndex aIdx = new AbsIndex(i);

        return whatif_getDropIndexCost(s.toSetIndex(), aIdx.toIndex());
    }

    public static double whatif_getDropIndexCost(Set<Index> indexes, Index index) throws SQLException {
        // if index is created already, it will return empty plan!!
        // else error!
        // return io.explain(dropIndexStatement(index),
        // indexes).getSelectCost();
        return 0;
    }

    public static double whatif_getExecCost(String sql, AbsConf s) throws SQLException {
        return whatif_getExecCost(sql, s.toSetIndex());
    }

    public static double whatif_getExecCost(String sql, Set<Index> conf) throws SQLException {
        //		System.out.println("*** Update cost:\t" + io.explain(sql, conf).getUpdateCost());
//		return io.explain(sql, new HashSet<Index>()).getTotalCost();
        double cost;
        SQLStatement sqls = new SQLStatement(sql);
        if (sqls.getSQLCategory().isSame(SQLCategory.NOT_SELECT)) {
            cost = io.explain(sql, conf).getTotalCost();
        } else {
            cost = whatif_getSelectCost(sql, conf);
        }

        return cost;
    }

    private static double whatif_getSelectCost(String sql, Set<Index> conf) throws SQLException {
        Connection con = db.getConnection();
        DB2Optimizer.clearAdviseAndExplainTables(con);
        DB2Optimizer.insertIntoAdviseIndexTable(con, conf);

        Statement stmt = con.createStatement();

        stmt.execute("SET CURRENT EXPLAIN MODE = EVALUATE INDEXES");
        stmt.execute(sql);
        stmt.execute("SET CURRENT EXPLAIN MODE = NO");

        ResultSet rs = stmt.executeQuery(SELECT_FROM_EXPLAIN_NEW);

        ResultSetMetaData rsmd = rs.getMetaData();

        double cost = 0;

        while (rs.next()) {

            cost += rs.getDouble(1);

        }

        stmt.close();
        return cost;
    }

    public static List<Column> getColumns() {
        return _columns;
    }

    public static List<Set<Integer>> getColumnsPerTable() {
        return _columnsPerTable;
    }
    private Random ran = new Random(1);
    protected boolean isLoadEnvironmentParameter = false;

    public DB2DATA(boolean flag, String str) throws Exception {
        bWFIT = flag;
        workload = str;
        getEnvironmentParameters();
        dropAllIndexes();
        extractInfo();
    }

    private void extractInfo() throws Exception {
        List<Schema> schemas = db.getCatalog().schemas();

        if (schemas.size() > 1) {
            throw new RuntimeException("More than 1 schema!");
        }

        Schema schema = schemas.get(0);

        // list all the tables
        Iterator<Table> iter_tables = schema.tables().iterator();
        List<Table> tables = Lists.newArrayList(iter_tables);
        int numOfTables = tables.size();

        // list all the columns
        _columns = new ArrayList<>();
        _colNames = new ArrayList<>();
        _columnsPerTable = new ArrayList<>();
        int column_index = 0;
        for (int i = 0; i < numOfTables; i++) {
            Table table = tables.get(i);
            _columns.addAll(table.columns());
            Set<Integer> columnsSet = new HashSet<>();
            for (int j = 0; j < table.columns().size(); j++) {
                _colNames.add(table.getName() + "."
                        + table.columns().get(j).getName());
                columnsSet.add(column_index);
                column_index++;
            }
            _columnsPerTable.add(columnsSet);
        }

        // List of all indexes;
        _allIndexes = generateAllIndexes(_columnsPerTable);

//        System.out.print(_allIndexes);
        // generate coveredby and covering list
        _coveredBy = genCoveredBy();
        _covering = genCovering();

        // get the current conf
        _systemIndex = new HashSet<>();
        getCurrentConfigration();
    }

    private List<BitSet> genCoveredBy() {
        // return array that is covered by i
        List<BitSet> coveredBy = new ArrayList<>();
        for (int i = 0; i < _allIndexes.size(); i++) {
            BitSet bs = new BitSet();
            for (int j = 0; j <= i; j++) {
                if (Tools.IsCovered(_allIndexes.get(j), _allIndexes.get(i))) {
                    bs.set(j);
                }
            }
            coveredBy.add(i, bs);
        }
        return coveredBy;
    }

    private List<BitSet> genCovering() {
        // return index that is covering by i
        List<BitSet> covering = new ArrayList<>();
        for (int i = 0; i < _allIndexes.size(); i++) {
            BitSet bs = new BitSet();
            for (int j = i; j < _allIndexes.size(); j++) {
                if (Tools.IsCovering(_allIndexes.get(j), _allIndexes.get(i))) {
                    bs.set(j);
                }
            }
            covering.add(i, bs);
        }
        return covering;
    }

    private List<List<Integer>> generateAllIndexes(List<Set<Integer>> LSI) {
        Set<List<Integer>> SLI = new HashSet<>();
        for (Set<Integer> SI0 : LSI) {
            Set<Set<Integer>> PS = Sets.powerSet(SI0);
            for (Set<Integer> SI1 : PS) {

                if (SI1.size() > _maxIndexLength) {
                    continue;
                }

                List<Integer> LI1 = new ArrayList<>(SI1);

                Collection<List<Integer>> PLI1 = Collections2
                        .permutations(LI1);
                for (List<Integer> LI2 : PLI1) {
                    if (LI2.size() <= _maxIndexLength) {
                        SLI.add(LI2);
                    }
                }
            }
        }
        List<List<Integer>> LLI = new ArrayList<>(SLI);
        Collections.sort(LLI, new CustomComparator());
        return LLI;
    }

    private void getEnvironmentParameters() throws Exception {
        if (isLoadEnvironmentParameter) {
            return;
        }

        en = Environment.getInstance();
        db = newDatabaseSystem(en);
        io = db.getOptimizer();

        LOG.info(io.getClass().toString());
        isLoadEnvironmentParameter = true;
    }

}
