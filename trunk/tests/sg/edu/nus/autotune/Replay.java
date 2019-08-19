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

import sg.edu.nus.util.FileReader;

public class Replay extends Execution {

    private static Joiner byTab = Joiner.on("\t").skipNulls();

    private static final String RESULT_FILE_PREFIX = null;
    private static final Logger LOG = getLogger("e2s2");

    public Replay(DataConnectivity data) throws SQLException {
        super(data);
        DB2DATA.dropAllIndexes();

        if (RESULT_FILE_PREFIX == null || RESULT_FILE_PREFIX.isEmpty()) {
            _resultFilePrefix = DEFAULT_RESULT_FILE_PREFIX;
        } else {
            _resultFilePrefix = RESULT_FILE_PREFIX;
        }
    }

    @Override
    protected int run() throws Exception {

        LOG.info("# Replay with " + DB2DATA.workload);
        _output.write("# Replay with " + DB2DATA.workload);
//        _output.write("# Overhead" + "\t" + "Conf_Change" + "\t" + "Exec_Time");
        _output.write("# Conf_Change" + "\t" + "Exec_Time");

        int index = 0;
        double alg_time = 0;
        double real_s0_change_cost = 0;
        double real_s1_evaluate = 0;

        FileReader in = new FileReader(DB2DATA.en.getWorkloadsFoldername() + DB2DATA.workload, null);

        String line = null;
        while ((line = in.readLine()) != null) {
            try {
                if (line.startsWith("Overhead")) {
                    alg_time = Double.parseDouble(line.substring(10));
                } else if (line.startsWith("CREATE") || line.startsWith("DROP")) {
                    real_s0_change_cost += DB2DATA.execute(line.substring(0, line.length() - 1));
                } else {
                    ++index;
                    real_s1_evaluate = DB2DATA.execute(line.substring(0, line.length() - 1));
//                    _output.write(alg_time + "\t" + real_s0_change_cost + "\t" + real_s1_evaluate);
                    _output.write(real_s0_change_cost + "\t" + real_s1_evaluate);
                    LOG.info("SQL " + index + " done");

                    alg_time = 0;
                    real_s0_change_cost = 0;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        LOG.info("End of execution");

        return 0;
    }
}
