package sg.edu.nus.autotune;

import java.sql.SQLException;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Set;

import org.jblas.DoubleMatrix;
import org.slf4j.Logger;

import edu.ucsc.dbtune.metadata.Index;
import edu.ucsc.dbtune.workload.FileWorkloadReader;
import edu.ucsc.dbtune.workload.SQLStatement;
import static org.slf4j.LoggerFactory.getLogger;
import static sg.edu.nus.autotune.Execution.DEFAULT_RESULT_FILE_PREFIX;

public class NewClass extends Execution {

    private static final String RESULT_FILE_PREFIX = null;
    private static final Logger LOG = getLogger("e2s2");
    private static final boolean DEBUG = true;
    private static final boolean EXPLOR = false;
    private static final int SEED = 1;

    private BitSet _explored = new BitSet();

    private DB2DATA _data;

    public NewClass(DataConnectivity data) throws SQLException {
        super(data);
        _data = (DB2DATA) data;

        DB2DATA.dropAllIndexes();

        if (RESULT_FILE_PREFIX == null || RESULT_FILE_PREFIX.isEmpty()) {
            _resultFilePrefix = DEFAULT_RESULT_FILE_PREFIX;
        } else {
            _resultFilePrefix = RESULT_FILE_PREFIX;
        }
    }

    private AbsConf compute(AbsConf s0, String sql, BitSet bs1, BitSet bs2, DoubleMatrix eta, DoubleMatrix theta) throws SQLException {

        BitSet candAdd = DB2DATA.getAddCandidate(sql, s0);
        BitSet candDrop = s0.getDropCandidate(sql);

        DoubleMatrix eta_vector = AbsConf.toEtaVector(s0, bs1, bs2);
        DoubleMatrix s0_vector = s0.toVector();
        AbsConf s1 = s0.clone();

        DoubleMatrix zeta_v0 = AbsConf.toZetaVector(s0, s0);
        DoubleMatrix eta_v0 = eta_vector;
        DoubleMatrix theta_v0 = s0_vector;
        double minCost = eta.dot(zeta_v0) + eta.dot(eta_v0) + _gamma * theta.dot(theta_v0);

        for (int i = candAdd.previousSetBit(candAdd.length()); i >= 0; i = candAdd.previousSetBit(i - 1)) {
            
            if(s1.covers(i)){
                continue;
            }
            
            AbsConf s = s0.clone();
            s.add(i);

            DoubleMatrix s_vector = s.toVector();
            DoubleMatrix zeta_v = AbsConf.toZetaVector(s0, s);
            DoubleMatrix eta_v = AbsConf.toEtaVector(s, bs1, bs2);
            DoubleMatrix theta_v = s_vector;

            double cost = eta.dot(zeta_v) + eta.dot(eta_v) + _gamma * theta.dot(theta_v);

            if (cost < minCost) {
                s1.add(i);
                _explored.or(DB2DATA.getCoveredBy(i));
            }

        }

        for (int i = candDrop.nextSetBit(0); i >= 0; i = candDrop.nextSetBit(i + 1)) {

            if (DB2DATA.isPrimaryKey(i)) {
                continue;
            }

            AbsConf s = s0.clone();
            s.drop(i);

            DoubleMatrix s_vector = s.toVector();
//                        DoubleMatrix zeta_v = AbsConf.toZetaVector(s0,s);
            DoubleMatrix eta_v = AbsConf.toEtaVector(s, bs1, bs2);
            DoubleMatrix theta_v = s_vector;

            double cost = 0 + eta.dot(eta_v) + _gamma * theta.dot(theta_v);

            if (cost < minCost) {
//		s1.drop(i);
                _explored.set(i);
            }

        }

        return s1;
    }

    @Override
    protected int run() throws Exception {

        AbsConf s0 = DB2DATA.getCurrentConfigration();
        DoubleMatrix s0_vector = s0.toVector();

		//change conf estimation
//		LeastSquare LS_zeta = new LeastSquare(DB2DATA.getNumOfIndexes()*2-1);
//		DoubleMatrix zeta = LS_zeta.getVector();
        //query execution estimation
        ridge LS_eta = new ridge(DB2DATA.getNumOfIndexes() * 2 - 1);
        DoubleMatrix eta = LS_eta.getVector();
        //value function estimation
        LSTD LS_theta = new LSTD(DB2DATA.getNumOfIndexes(), _gamma, 100);
        DoubleMatrix theta = LS_theta.getVector();

        int index = 0;
        double avgCost = 0;
        double avgCost_near = 0;
        double real_avgCost = 0;
        double real_avgCost_near = 0;

        double real_s0_change_cost = 0;
        double real_s1_evaluate = 0;
        double real_cost = 0;

        FileWorkloadReader wl = new FileWorkloadReader(
                DB2DATA.en.getWorkloadsFoldername()
                + DB2DATA.workload);
        
        LOG.info("# new algo with " + DB2DATA.workload + " with " + _gamma);
        _output.write("# new algo with " + DB2DATA.workload + " with " + _gamma);
//		_output.write("current cost" + "\t" + "trans cost" + "\t" + "exec cost" + "\t" +  "avg cost" + "\t" 
//				+ "avg cost (near)" + "\t" + "#conf" + "\t" + "Total time" + "\t" + "exec time" + "\t"
//				+ "real cost" + "\t" + "real trans cost" + "\t" + "real exec cost" + "\t" + "avg real cost" + "\t" + "avg real cost (near)");
//        _output.write("# Totla_Time" + "\t" + "Alg_Time" + "\t" + "Exec_Time" + "\t" + "Trans_time" + "\t"
//                + "Total_Cost" + "\t" + "Exec_Cost" + "\t" + "Trans_Cost");
        _output.write("# Overhead" + "\t" + "Conf_Change" + "\t" + "Exec_Time" + "\t" + "WI_Conf_Change" + "\t" 
                + "WI_Exec_Time" + "\t" + "Expected_cost1" + "\t" + "Expected_cost2");

        for (SQLStatement sqls : wl) {

            String sql = sqls.getSQL();
            index += 1;

            try {
                double startTime = System.nanoTime();

                double start_alg_time = System.nanoTime();
                BitSet bs1 = DB2DATA.getAddCandidate(sql);
                BitSet bs2 = DB2DATA.getDropCandidate(sql);
                AbsConf s1 = compute(s0, sql, bs1, bs2, eta, theta);
                DoubleMatrix s1_vector = s1.toVector();

                DoubleMatrix zeta_vector = AbsConf.toZetaVector(s0, s1);
                double s0_change_cost = s0.whatif_changeToCost(s1);

                double zeta_error = eta.dot(zeta_vector) - s0_change_cost;
                eta = LS_eta.get(zeta_vector, zeta_error);
                

                DoubleMatrix eta_vector = AbsConf.toEtaVector(s1, bs1, bs2);
                double estimated_cost1 = eta.dot(zeta_vector);
                double estimated_cost2 = eta.dot(eta_vector);
                double s1_evaluate = s1.whatif_evaluate(sql);

                double eta_error = eta.dot(eta_vector) - s1_evaluate;
                eta = LS_eta.get(eta_vector, eta_error);
//                System.out.println("eta: " + eta_vector);

                double hat_theta = s0_change_cost + s1_evaluate;
                theta = LS_theta.get(s0_vector, s1_vector, hat_theta);

                double end_alg_time = System.nanoTime();
                double alg_time = (end_alg_time - start_alg_time) / 1000000.0;
                LOG.info("Overhead: " + alg_time);

                real_s0_change_cost = s0.changeToCost(s1);
                real_s1_evaluate = DB2DATA.execute(sql);
                        
                BitSet temp_bs0 = s0.toBitSet();
                BitSet temp_bs1 = s1.toBitSet();
                for (int i = 0; i < DB2DATA.getNumOfIndexes(); ++i) {
                    if (temp_bs0.get(i) != temp_bs1.get(i)) {
                        AbsIndex temp_idx = new AbsIndex(i);
                        if (temp_bs0.get(i) == true) {
                           _output.write(index + "\t" + i + "\t" + -1 + "\t" + temp_idx.toString());
                        }else{
                           _output.write(index + "\t" + i + "\t" + 1 + "\t" + temp_idx.toString());
                        }
                    }
                }
                
                s0_vector = s1_vector;
                s0 = s1.clone();

                double endTime = System.nanoTime();
                double totalTime = (endTime - startTime) / 1000000.0;

                double alpha = 1.0 / index;
                avgCost = (1 - alpha) * avgCost + alpha * hat_theta;
                real_avgCost = (1 - alpha) * real_avgCost + alpha * real_cost;

                double beta = Math.max(1.0 / index, 1.0 / DB2DATA.getNumOfIndexes());
                avgCost_near = (1 - beta) * avgCost_near + beta * hat_theta;
                real_avgCost_near = (1 - beta) * real_avgCost_near + beta * real_cost;

                LOG.info("SQL " + index + ": " + sql);
//
//                LOG.info(zeta_vector.toString("%.0f"));
//                LOG.info("Zeta Error:\t" + zeta_error);
//                LOG.info(eta_vector.toString("%.0f"));
//                LOG.info("Eta Error:\t" + eta_error);
//
//                LOG.info("Totla_Time:\t" + totalTime);
//                LOG.info("Alg_Time:\t" + alg_time);
//                LOG.info("Exec_Time:\t" + real_s1_evaluate);
//                LOG.info("Trans_time:\t" + real_s0_change_cost);
//                LOG.info("Total_Cost:\t" + hat_theta);
//                LOG.info("Trans_Cost:\t" + s1_evaluate);
//                LOG.info("Exec_Cost:\t" + s0_change_cost);
//                LOG.info("Average Cost: \t" + avgCost);
//                LOG.info("Average Cost (near): \t" + avgCost_near);
//                LOG.info("Explored " + _explored.size());
//                LOG.info("Current conf is " + s0.toStringSimple());
//                LOG.info("Current conf is " + s0.toSetInt().toString());
//                LOG.info("Current conf is " + s0.toString());
//                LOG.info("Current size of conf is " + s0.toSetInt().size());

//                _output.write(totalTime + "\t" + alg_time + "\t" + real_s1_evaluate + "\t" + real_s0_change_cost + "\t"
//                        + hat_theta + "\t" + s1_evaluate + "\t" + s0_change_cost);
                  _output.write(alg_time + "\t" + real_s0_change_cost + "\t" + real_s1_evaluate + "\t" + s0_change_cost + "\t"
                          + s1_evaluate + "\t" + estimated_cost1 + "\t" + estimated_cost2);

//                if (index > 2000){
//                    break;
//                }
            } catch (Exception e) {
                LOG.warn("Failed SQL " + index + ": " + sql);
//                e.printStackTrace();
            }

        }

//        _output.write(s0.toString());
//        _output.write(eta.toString());
//        _output.write(theta.toString());

        LOG.info("End of execution");

        return 0;
    }

}
