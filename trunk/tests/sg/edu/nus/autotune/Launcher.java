/*
  The launcher code written to run rCOREIL, COREIL, LSTD with WhatIf Cost Estimator, and a Replay option.
  The Arguments are written in Arguments file in this folder.

  Author: Debabrota Basu

*/

package sg.edu.nus.autotune;

import java.util.Date;

import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

import com.google.common.base.Stopwatch;

import sg.edu.nus.util.FileReader;
import sg.edu.nus.util.FileWriter;
import static sg.edu.nus.util.TimeUtils.getTimestamp;

public class Launcher {

    private static final String RESULT_FILE_SUFFIX = "txt";
    private static final Logger LOG = getLogger("e2s2");

    private final Date _date;

    public Launcher(Date date) {
        _date = date;
    }

    public int run(String[] args) throws Exception {
        Arguments argsAll = new Arguments("Launcher");
        if (!argsAll.processArgs(args)) {
            return -1;
        }
        if (argsAll.help) {
            return 0;
        }

        FileReader input;
        if (argsAll.inputFile == null) {
            input = null;
            LOG.info("Input: none");
        }

        //DataConnectivity data = new MockData();
        DataConnectivity data;
        Execution exec;
        switch (argsAll.algo) {

            case 1:
                data = new DB2DATA(false, argsAll.inputFile);
                exec = new rCOREIL(data);
                break;
                
            case 2:
                data = new DB2DATA(false, argsAll.inputFile);
                exec = new COREIL(data);
                break;
            
            case 3:
                data = new DB2DATA(true, argsAll.inputFile);
                exec = new OptimizerWhatIf(Data);
                break;  
                            	
            case 4:
                data = new DB2DATA(true, argsAll.inputFile);
                exec = new Replay(data);
                break;
            
            default:
                data = new DB2DATA(false, argsAll.inputFile);
                exec = new rCOREIL(data);
                break;
        }

        FileWriter output;
        if (!argsAll.noOutput) {
            output = new FileWriter(argsAll.resultDir + "/"
                    + exec.getResultFilePrefix() + "_" + getTimestamp(_date)
                    + "." + RESULT_FILE_SUFFIX);
            LOG.info("Output: " + output.getFilename());
        } else {
            output = new FileWriter(null);
            LOG.info("No output is generating");
        }
        exec.setOutputWriter(output);

        exec.run();

        output.endOfFile();
        return 0;
    }

    public static void main(String[] args) throws Exception {
        Date date = new Date();
        System.out.println("[-Start-] " + date);
        Stopwatch stopwatch = Stopwatch.createStarted();

        int rc = (new Launcher(date)).run(args);

        stopwatch.stop();
        System.out.println("[--End--] Runtime: " + stopwatch + ", RC: " + rc);
    }
}
