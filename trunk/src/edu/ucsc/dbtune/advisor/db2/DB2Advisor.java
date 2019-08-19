package edu.ucsc.dbtune.advisor.db2;

import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.ucsc.dbtune.DatabaseSystem;
import edu.ucsc.dbtune.advisor.AbstractAdvisor;
import edu.ucsc.dbtune.advisor.RecommendationStatistics;
import edu.ucsc.dbtune.metadata.Index;
import edu.ucsc.dbtune.optimizer.DB2Optimizer;
import edu.ucsc.dbtune.workload.SQLStatement;

import static edu.ucsc.dbtune.util.OptimizerUtils.getBaseOptimizer;

/**
 * Generates a recommendation according to the db2advis program.
 *
 * @author Ivo Jimenez
 */
public class DB2Advisor extends AbstractAdvisor
{
    private DatabaseSystem dbms;
    private int spaceBudget;

    /**
     * constructor.
     *
     * @param dbms
     *      system connected representing a DB2 instance
     * @param spaceBudget
     *      amount of space on disk allowed to build indexes
     * @throws SQLException
     *      if the underlaying DBMS is not a DB2 instance
     */
    public DB2Advisor(DatabaseSystem dbms, int spaceBudget)
        throws SQLException
    {
        if (!(getBaseOptimizer(dbms.getOptimizer()) instanceof DB2Optimizer))
            throw new SQLException("Expecting DB2Optimizer");

        this.dbms = dbms;
        this.spaceBudget = spaceBudget;
    }

    /**
     * {@inheritDoc}
     */
    public void process(List<SQLStatement> workload) throws SQLException
    {
        Statement stmt = dbms.getConnection().createStatement();

        stmt.execute("DELETE FROM systools.advise_index");
        stmt.execute("DELETE FROM systools.advise_workload");

        int i = 0;

        for (SQLStatement sql : workload)
            stmt.execute(
                    "INSERT INTO systools.advise_workload VALUES(" +
                    "   'dbtuneworkload'," +
                    "    " + i++ + ", " +
                    "   '" + sql.getSQL().replace("'", "''") + "'," +
                    "   '',1,0,0,0,0,'')");

        stmt.close();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCandidateSetFixed()
    {
        throw new RuntimeException("Not implemented");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RecommendationStatistics getOptimalRecommendationStatistics()
    {
        throw new RuntimeException("Not implemented");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void processNewStatement(SQLStatement sql) throws SQLException
    {
        throw new SQLException("Can't recommend single statements");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<Index> getRecommendation() throws SQLException
    {
        CallableStatement cstmt =
            dbms.getConnection().prepareCall(
                "CALL SYSPROC.DESIGN_ADVISOR(" +
                "   ?, ?, ?, blob(' " +
                "      <?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "      <plist version=\"1.0\"> " +
                "         <dict> " +
                "            <key>CMD_OPTIONS</key>" +
                "            <string>" +
                "               -workload  dbtuneworkload " +
                "               -disklimit " + spaceBudget +
                "               -type      I " +
                "               -compress  OFF " +
                //"               -qualifier tpcds " +
                "               -drop" +
                "            </string>" +
                "         </dict>" +
                "      </plist>'), " +
                "   NULL, ?, ?)");

        cstmt.setInt(1, 1);
        cstmt.setInt(2, 0);
        cstmt.setString(3, "en_US");
        cstmt.registerOutParameter(4, Types.BLOB);
        cstmt.registerOutParameter(5, Types.BLOB);
        cstmt.execute();

        Set<Index> unique = new HashSet<Index>();

        for (Index i : DB2Optimizer.readAdviseIndexTable(dbms.getConnection(), dbms.getCatalog()))
            unique.add(new Index(i));

        //double space = 0;
        //for (Index i : unique)
            //space += i.getBytes();

        //System.out.println("Count:  " + unique.size());
        //System.out.println("Budget: " + budget);
        //System.out.println("Actual: " + space / 1000000);

        return new HashSet<Index>(unique);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RecommendationStatistics getRecommendationStatistics()
    {
        throw new RuntimeException("Not yet");
    }
}
