package sg.edu.nus.autotune;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jblas.DoubleMatrix;
import org.slf4j.Logger;

import edu.ucsc.dbtune.workload.FileWorkloadReader;
import edu.ucsc.dbtune.workload.SQLStatement;
import static org.slf4j.LoggerFactory.getLogger;

import com.google.common.base.Joiner;

public class Optimizer extends Execution {
    private static Joiner byTab = Joiner.on("\t").skipNulls();

    private static final String RESULT_FILE_PREFIX = null;
    private static final Logger LOG = getLogger("e2s2");
    private static final boolean DEBUG = true;
    private static final boolean EXPLOR = false;
    private static final int SEED = 1;

    private Set<Integer> _explored = new HashSet<>();

    private DB2DATA _data;

    public Optimizer(DataConnectivity data) throws SQLException {
        super(data);
        _data = (DB2DATA) data;
        DB2DATA.dropAllIndexes();

        if (RESULT_FILE_PREFIX == null || RESULT_FILE_PREFIX.isEmpty()) {
            _resultFilePrefix = DEFAULT_RESULT_FILE_PREFIX;
        } else {
            _resultFilePrefix = RESULT_FILE_PREFIX;
        }
    }

    private AbsConf compute(AbsConf s0, String sql, DoubleMatrix theta) throws SQLException {

        BitSet candAdd = DB2DATA.getAddCandidate(sql, s0);
        BitSet candDrop = s0.getDropCandidate(sql);

        AbsConf s1 = s0.clone();

        double minCost = s0.whatif_changeToCost(s1) + s1.whatif_evaluate(sql) + _gamma * theta.dot(s1.toVector());
        
        for (int i = candAdd.previousSetBit(candAdd.length()); i >= 0; i = candAdd.previousSetBit(i - 1)) {
            
            if(s1.covers(i)){
                continue;
            }
            
            AbsConf s = s0.clone();
            s.add(i);
            double cost0 = s0.whatif_getAddIndexCost(i) + s.whatif_evaluate(sql) + _gamma * theta.dot(s.toVector());
            if (cost0 < minCost) {
                s1.add(i);
                _explored.add(i);
            }

        }

        for (int i = candDrop.nextSetBit(0); i >= 0; i = candDrop.nextSetBit(i + 1)) {
            AbsConf s = s0.clone();
            s.drop(i);
            double cost0 = s.whatif_evaluate(sql) + _gamma * theta.dot(s.toVector());
            if (cost0 < minCost) {
                s1.drop(i);
                _explored.add(i);
            }

        }

        return s1;
    }

    @Override
    protected int run() throws Exception {

        AbsConf s0 = DB2DATA.getCurrentConfigration();

        //value function estimation
        LSTD LS_theta = new LSTD(DB2DATA.getNumOfIndexes(), _gamma, 100);
        DoubleMatrix theta = LS_theta.getVector();
        DoubleMatrix s0_vector = s0.toVector();

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
        
        LOG.info("Start execution of Optimizer with WhatIf Estimator with " + DB2DATA.workload + " with " + _gamma);
        _output.write("# Optimizer with WhatIf Estimator with " + DB2DATA.workload + " with " + _gamma);
//        _output.write("# Total_Time" + "\t" + "Alg_Time" + "\t" + "Exec_Time" + "\t" + "Trans_time" + "\t"
//                + "Total_Cost" + "\t" + "Exec_Cost" + "\t" + "Trans_Cost");
        _output.write("# Overhead" + "\t" + "Conf_Change" + "\t" + "Exec_Time" + "\t" + "WI_Conf_Change" + "\t" + "WI_Exec_Time");

        for (SQLStatement sqls : wl) {

            String sql = sqls.getSQL();

            index += 1;

            try {

                double startTime = System.nanoTime();

                double start_alg_time = System.nanoTime();
                AbsConf s1 = compute(s0, sql, theta);
                double s0_change_cost = s0.whatif_changeToCost(s1);
                double s1_evaluate = s1.whatif_evaluate(sql);
                double end_alg_time = System.nanoTime();
                double alg_time = (end_alg_time - start_alg_time) / 1000000.0;
                LOG.info("Overhead: " + alg_time);

                real_s0_change_cost = s0.changeToCost(s1);
                real_s1_evaluate = DB2DATA.execute(sql);
                real_cost = real_s0_change_cost + real_s1_evaluate;

                double hat_theta = s0_change_cost + s1_evaluate;
                DoubleMatrix s1_vector = s1.toVector();
                theta = LS_theta.get(s0_vector, s1_vector, hat_theta);
                
                //copy to old
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

//                LOG.info("SQL " + index + ": " + sql);
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

                  _output.write(alg_time + "\t" + real_s0_change_cost + "\t" + real_s1_evaluate + "\t" + s0_change_cost + "\t" + s1_evaluate);

            } catch (Exception e) {
                LOG.warn("Failed SQL " + index + ": " + sql);
                e.printStackTrace();
            }

        }

        _output.write(s0.toSetInt().toString());
        _output.write(s0.toString());
        _output.write(theta.toString());

        LOG.info("End of execution");

        return 0;
    }

}
