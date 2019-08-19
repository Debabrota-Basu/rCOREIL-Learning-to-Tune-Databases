package sg.edu.nus.autotune;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import edu.ucsc.dbtune.metadata.Column;
import edu.ucsc.dbtune.metadata.Index;

public class AbsIndex {
	private List<Integer> _ints;
	private String _name;
	private int _ID;
	private boolean _isPrimaryKey;
	private int _maxIndexLength = DB2DATA.getMaxIndexLength();
	
	public AbsIndex(int i){
		this(DB2DATA.getAllIndexes().get(i));
	}
		
	public boolean isEmpty(){
		return _ints.isEmpty();
	}
	
	public String toString(){
		StringBuilder str = new StringBuilder();
		str.append("[ ");
		for(int i:_ints){
			str.append(DB2DATA.getColumnName(i) + ", " );
		}
		str.append("]");
		return str.toString();
	}
		
	public AbsIndex(Index idx){
		List<Column> columns = idx.columns();
		if (columns.size() > _maxIndexLength) {
			columns = columns.subList(0, _maxIndexLength);
		}
		
		List<Integer> ints = new ArrayList<>();
		for(Column col:columns){
			ints.add(DB2DATA.getColumnID(col));
		}
		
		_ints = ints;
		
                if (DB2DATA.bWFIT){
                    _name = idx.getName();
                    _ID = idx.getId();
                }else{
                    _name = genName(ints);
                    _ID = DB2DATA.getAllIndexes().indexOf(ints);
                }
	
		_isPrimaryKey = DB2DATA.isPrimaryKey(_ID);
		
		if(_isPrimaryKey){
			_name = DB2DATA.getPrimaryKeyName(_ID);
		}
		
	}
	
	public AbsIndex(List<Integer> ints){
		_ints = ints;
		_name = genName(ints);
		_ID = DB2DATA.getAllIndexes().indexOf(ints);
		_isPrimaryKey = DB2DATA.isPrimaryKey(_ID);
		
		if(_isPrimaryKey){
			_name = DB2DATA.getPrimaryKeyName(_ID);
		}
	}
	
	public Index toIndex() throws SQLException{
		List<Column> columns = new ArrayList<Column>();
		for (int i : toListInt()) {
			columns.add(DB2DATA.getColumn(i));
		}
		Index idx = new Index(getName(), columns);
		idx.setId(getID());
		return idx;
	}
	
	public Index toIndexSimple() throws SQLException{
		List<Column> columns = new ArrayList<Column>();
		for (int i : toListInt()) {
			columns.add(DB2DATA.getColumn(i));
		}
//		Index idx = new Index(columns, (List<Boolean>) null );
		Index idx = new Index(getName(), columns);
		return idx;
	}
	
	public double create() throws SQLException{
		
		if(isPrimaryKey()){
			return 0;
//			throw new RuntimeException("Cannot create primary key!!");
		}
		
		StringBuilder str = new StringBuilder();
		str.append("CREATE INDEX " + getName());
		
		List<Integer> ints = toListInt();
		
		String table = DB2DATA.getColumn(ints.get(0)).getTable().getName();;
		str.append(" ON " + table + " ( ");

		for (int i = 0; i < ints.size(); i++) {
			str.append(DB2DATA.getColumn(ints.get(i)).getName());
			if (i != ints.size() - 1) {
				str.append(", ");
			} else {
				str.append(" ");
			}
		}
		str.append(")");
				
		return DB2DATA.execute(str.toString());
	}
	
	public double drop() throws SQLException{
		
		if(isPrimaryKey()){
			return 0;
//			throw new RuntimeException("Cannot drop primary key!!");
		}
		
		StringBuilder str = new StringBuilder();
		str.append("DROP INDEX " + getName());
		
		return DB2DATA.execute(str.toString());
	}
	
	public String getName(){
		return _name;
	}
	
	public List<Integer> toListInt(){
		return _ints;
	}
	
	public boolean isPrimaryKey(){
		return _isPrimaryKey;
	}
	
	public int getID(){
		return _ID;
	}
	
	private static String genName(List<Integer> ints){
		StringBuilder str = new StringBuilder();
		str.append("MYIDX");
		for (int i : ints) {
			str.append("_" + i);
		}
		return str.toString();
	}
}
