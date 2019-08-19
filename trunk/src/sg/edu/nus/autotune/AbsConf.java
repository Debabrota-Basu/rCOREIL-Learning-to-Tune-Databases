/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sg.edu.nus.autotune;

/**
 *
 * @author Chen Weidong
 */
import java.sql.SQLException;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jblas.DoubleMatrix;

import edu.ucsc.dbtune.metadata.Index;

public class AbsConf {

    public static DoubleMatrix toEtaVector(AbsConf s, BitSet bs1, BitSet bs2) throws SQLException {
        
//        BitSet bs1 = DB2DATA.getAddCandidate(sql);
//        BitSet bs2 = DB2DATA.getDropCandidate(sql);
        
        int n = DB2DATA.getNumOfIndexes();
        DoubleMatrix vector = new DoubleMatrix(2*n-1);
        
        for (int i = bs1.nextSetBit(0); i >= 0; i = bs1.nextSetBit(i + 1)) {
            if (s.contains(i)){
                vector.put(i, 1);
            }else{
                vector.put(i, -1);
            }
        }
        
        for (int i = bs2.nextSetBit(1); i >= 0; i = bs1.nextSetBit(i + 1)) {
            if (s.contains(i)){
                vector.put(n+i-1, 1);
            }else{
                vector.put(n+i-1, -1);
            }
        }
        
        vector.put(0, 1);
        return vector;
    }
    
    public static DoubleMatrix toZetaVector(AbsConf s0, AbsConf s1) throws SQLException {
        int n = DB2DATA.getNumOfIndexes();
        DoubleMatrix vector = new DoubleMatrix(2*n-1);

        if (s0.contains(s1)) {
            return vector;
        }

        BitSet bs0 = (BitSet) s0.toBitSet().clone();
        BitSet bs1 = (BitSet) s1.toBitSet().clone();

        BitSet toBeCreated = (BitSet) bs1.clone();
        toBeCreated.andNot(bs0);

        for (int i = toBeCreated.nextSetBit(0); i >= 0; i = toBeCreated.nextSetBit(i + 1)) {
            BitSet bs = DB2DATA.getCoveredBy(i);
            for(int j=bs.nextSetBit(0);j>=0;j=bs.nextSetBit(j+1)){
                if(s0.contains(j)){
                    vector.put(j,1);
                }else{
                    vector.put(j,-1);
                }
            }
        }

        vector.put(0, 1);

        return vector;
    }

    private static DoubleMatrix toZetaVector(int id) throws SQLException {
        DoubleMatrix vector = new DoubleMatrix(DB2DATA.getNumOfIndexes());

        BitSet bs1 = DB2DATA.getCoveredBy(id);
        for (int i = bs1.nextSetBit(0); i >= 0; i = bs1.nextSetBit(i + 1)) {
            vector.put(i, PARTIAL_BENEFIT);
        }

        vector.put(id, 1);

        vector.put(0, 1);

        return vector;
    }

//	private Set<Integer> _ints;
    private BitSet _bs;

    private static final double PARTIAL_BENEFIT = 1.0;

    public AbsConf(BitSet bs) {
        _bs = (BitSet) bs.clone();
    }

    public AbsConf(Set<Integer> setInt) {

        _bs = new BitSet();

        for (int i : setInt) {
            _bs.set(i);
        }
    }

    public AbsConf() {
        this(new HashSet<Integer>());
    }

    public void add(int i) throws SQLException {
        _bs.set(i);
    }

    public double changeToCost(AbsConf s) throws SQLException {
        double cost = 0;

        if (this.equals(s)) {
            return cost;
        }

        BitSet bs0 = (BitSet) _bs.clone();
        BitSet bs1 = (BitSet) s.toBitSet().clone();

        BitSet toBeCreated = (BitSet) bs1.clone();
        BitSet toBeDropped = (BitSet) bs0.clone();

        toBeCreated.andNot(bs0);
        toBeDropped.andNot(bs1);

        for (int i = toBeCreated.nextSetBit(0); i >= 0; i = toBeCreated.nextSetBit(i + 1)) {
            AbsIndex aIdx = new AbsIndex(i);
            cost += aIdx.create();
        }

        for (int i = toBeDropped.nextSetBit(0); i >= 0; i = toBeDropped.nextSetBit(i + 1)) {
            AbsIndex aIdx = new AbsIndex(i);
            cost += aIdx.drop();
        }

        return cost;
    }

    public double whatif_changeToCost(AbsConf s) throws SQLException {
        double cost = 0;

        if (this.contains(s)) {
            return cost;
        }

        BitSet bs0 = (BitSet) _bs.clone();
        BitSet bs1 = (BitSet) s.toBitSet().clone();

        BitSet toBeCreated = (BitSet) bs1.clone();
        BitSet toBeDropped = (BitSet) bs0.clone();

        toBeCreated.andNot(bs0);
        toBeDropped.andNot(bs1);

        for (int i = toBeCreated.nextSetBit(0); i >= 0; i = toBeCreated.nextSetBit(i + 1)) {
            cost += this.whatif_getAddIndexCost(i);
        }

        for (int i = toBeDropped.nextSetBit(0); i >= 0; i = toBeDropped.nextSetBit(i + 1)) {
            cost += this.whatif_getDropIndexCost(i);
        }

        return cost;
    }

    public AbsConf clone() {
        BitSet bs = (BitSet) _bs.clone();
        return new AbsConf(bs);
    }

    public boolean contains(AbsConf s) {
        BitSet bs = s.toBitSet();
        for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
            if (!this.contains(i)) {
                return false;
            }
        }
        return true;
    }

    public boolean contains(int i) {
        return _bs.get(i);
    }

    public boolean covers(int i) {
        for (int j = _bs.nextSetBit(0); j >= 0; j = _bs.nextSetBit(j + 1)) {
            if (DB2DATA.getCoveredBy(j).get(i)) {
                return true;
            }
        }
        return false;
    }

    public void drop(int i) throws SQLException {
        _bs.set(i, false);
    }

    public boolean equals(AbsConf s) {
        return _bs.equals(s.toBitSet());
    }

    public BitSet getAddCandidate(String sql) throws SQLException {
        return DB2DATA.getAddCandidate(sql, this);
    }

    public BitSet getAddCandidateBig(String sql) throws SQLException {
        return DB2DATA.getAddCandidateBig(sql, this);
    }

    public BitSet getDropCandidate(String sql) throws SQLException {
        return DB2DATA.getDropCandidate(sql, this);
    }

    public BitSet toBitSet() {
        return _bs;
    }

    public Set<Index> toSetIndex() throws SQLException {
        Set<Index> indexes = new HashSet<>();
        for (int i = _bs.nextSetBit(0); i >= 0; i = _bs.nextSetBit(i + 1)) {
            AbsIndex aIdx = new AbsIndex(i);
            if (!aIdx.isEmpty()) {
                indexes.add(aIdx.toIndex());
            }
        }
        return indexes;
    }

    public Set<Integer> toSetInt() {
        Set<Integer> ints = new HashSet<>();
        for (int i = _bs.nextSetBit(0); i >= 0; i = _bs.nextSetBit(i + 1)) {
            ints.add(i);
        }
        return ints;
    }

    public String toString() {

        StringBuilder str = new StringBuilder();

        str.append("{ ");
        for (int i = _bs.nextSetBit(0); i >= 0; i = _bs.nextSetBit(i + 1)) {
            AbsIndex aIdx = new AbsIndex(i);
            str.append(i + ":" + aIdx.toString() + " ");
        }
        str.append("}");

        return str.toString();
    }

    public String toStringSimple() {

        StringBuilder str = new StringBuilder();

        str.append("{");
        for (int i = 0; i < DB2DATA.getNumOfIndexes(); i++) {
            if (_bs.get(i)) {
                str.append("1");
            } else {
                str.append("0");
            }
        }
        str.append("}");

        return str.toString();
    }

    public BitSet toBitSetVector() {
        BitSet bs = new BitSet();
        for (int i = _bs.nextSetBit(0); i >= 0; i = _bs.nextSetBit(i + 1)) {
            BitSet bs0 = DB2DATA.getCoveredBy(i);
            for (int j = bs0.nextSetBit(0); j >= 0; j = bs0.nextSetBit(j + 1)) {
                bs.set(j);
            }
        }
        return bs;

    }

    public DoubleMatrix toVector() {
        DoubleMatrix vector = new DoubleMatrix(DB2DATA.getNumOfIndexes());

        BitSet bs = new BitSet();
        for (int i = _bs.nextSetBit(0); i >= 0; i = _bs.nextSetBit(i + 1)) {
            BitSet bs0 = DB2DATA.getCoveredBy(i);
            for (int j = bs0.nextSetBit(0); j >= 0; j = bs0.nextSetBit(j + 1)) {
                bs.set(j);
            }
        }

        for (int i = 0; i < DB2DATA.getNumOfIndexes(); i++) {
            if (bs.get(i)) {
                vector.put(i, 1);
            } else {
                vector.put(i, -1);
            }
        }

        vector.put(0, 1);

        return vector;
    }

//	public DoubleMatrix toVectorPos(){
//		DoubleMatrix vector = new DoubleMatrix(DB2DATA.getNumOfIndexes());
//		
//		BitSet bs = new BitSet();
//		for (int i = _bs.nextSetBit(0); i >= 0; i = _bs.nextSetBit(i+1)) {
//			BitSet bs0 = DB2DATA.getCoveredBy(i);
//			for (int j = bs0.nextSetBit(0); j >= 0; j = bs0.nextSetBit(j+1)) {
//				bs.set(j);
//			}
//		}
//		
//		for (int i=0;i<DB2DATA.getNumOfIndexes();i++){
//			if(bs.get(i)){
//				vector.put(i, 1);
//			}
//		}
//		
//		vector.put(0, 1);
//		
//		return vector;
//	}
    
    public double whatif_evaluate(String sql) throws SQLException {
        return DB2DATA.whatif_getExecCost(sql, this);
    }

//    public double whatif_evaluateAddIndex(String sql, int i) throws SQLException {
//        AbsConf s = this.clone();
//        double addCost = s.add(i);
//        return addCost + s.whatif_evaluate(sql);
//    }
//
//    public double whatif_evaluateDropIndex(String sql, int i) throws SQLException {
//        AbsConf s = this.clone();
//        double dropCost = s.drop(i);
//        return dropCost + s.whatif_evaluate(sql);
//    }

    public double whatif_getAddIndexCost(int i) throws SQLException {

        if (this.contains(i)) {
            return 0;
        }

        if (_bs.get(i)) {
            throw new RuntimeException("This Conf already contains index ID " + i + "!");
        }

        if (DB2DATA.isPrimaryKey(i)) {
            throw new RuntimeException("Index ID " + i + "is primary key!");
        }

        return DB2DATA.whatif_getAddIndexCost(this, i);
    }

    public double whatif_getDropIndexCost(int i) throws SQLException {

        if (!this.contains(i)) {
            throw new RuntimeException("This Conf does not contains index ID " + i + "!");
        }

        if (DB2DATA.isPrimaryKey(i)) {
            throw new RuntimeException("Index ID " + i + " is primary key!");
        }

        return DB2DATA.whatif_getDropIndexCost(this, i);
    }
}
