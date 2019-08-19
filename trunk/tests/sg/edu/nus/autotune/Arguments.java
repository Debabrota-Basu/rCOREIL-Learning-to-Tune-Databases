package sg.edu.nus.autotune;

import java.io.PrintStream;
import java.util.List;

import static org.apache.commons.io.FilenameUtils.normalize;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import static org.slf4j.LoggerFactory.getLogger;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newLinkedList;

public class Arguments
{
    private static final int SCREEN_WIDTH = 80;
    private static final String DEFAULT_RESULT_DIR = "./result";
    private static final int DEFAULT_ALGO = 1;
//    private static final String DEFAULT_WORKLOAD = "/../../workloads/db2/nus/filter_tpcc_1min_1_3k_revise.sql";
//    private static final String DEFAULT_WORKLOAD = "/../../workloads/db2/nus/filter_tpcc_1min_1_3k.sql";
//    private static final String DEFAULT_WORKLOAD = "/../../workloads/db2/nus/workload_shift.sql";
    private static final String DEFAULT_WORKLOAD = "/../../workloads/db2/nus/filter_tpcc_1min_1_3k_select_update.sql";//generate from filter...revise.sql
    private static final Logger LOG = getLogger("e2s2");

    private final String _mainclassName;

    public Arguments(String className) {
        _mainclassName = className;
    }

    @Option(name = "-f", aliases = "--input-file", metaVar = "<path>",
            usage = "input file [def:none]")
    public String inputFile = DEFAULT_WORKLOAD;

    @Option(name = "-d", aliases = "--dir", metaVar = "<path>",
            usage = "output directory [def:" + DEFAULT_RESULT_DIR + "]")
    public String resultDir = DEFAULT_RESULT_DIR;

    @Option(name = "--no-output", usage = "ignore output [def:false]")
    public boolean noOutput = false;
    
    @Option(name = "-a", aliases = "--algo", metaVar = "<num>",
            usage = "choice of algorithm [def:" + DEFAULT_ALGO + "]")
    public int algo = DEFAULT_ALGO;

    @Option(name = "-h", aliases = { "-?", "--help" }, hidden = false,
            usage = "print this help message")
    public boolean help = false;

    public boolean processArgs(String[] args) throws Exception {
        if (!parseArgs(args))
            return false;

        if (help) {
            printHelp(System.out);
        }
        else {
            sanityCheckArgs();
            deriveArgs();
        }

        return true;
    }

    private boolean parseArgs(String[] args) {
        CmdLineParser parser = new CmdLineParser(this);
        parser.setUsageWidth(SCREEN_WIDTH);

        try {
            parser.parseArgument(args);
        }
        catch (CmdLineException e) {
            LOG.error(e.getMessage());
            System.err.println();
            printHelp(System.err);

            return false;
        }

        return true;
    }

    private void sanityCheckArgs() {
//        checkState(numIndex > 0, "Number of indexes must be positive");
//        checkState(numSubQueries > 0, "Max number of sub queries must be positive");
        checkState(algo > 0, "Choice of algorithm must be positive");
    }

    private void deriveArgs() {}

    public void showArgs() {
        for (String msg : getInfo()) {
            System.out.println(msg);
        }
    }

    private List<String> getInfo() {
        List<String> info = newLinkedList();

        info.add("  Result directory: " + resultDir);
//        info.add("  Number of Indexes: " + numIndex);
//        info.add("  Max number of sub queries: " + numSubQueries);
        info.add("  Algorithm: " + algo);
        return info;
    }

    private void printHelp(PrintStream out) {
        String cmd = "java " + _mainclassName;
        String cmdIndent = "  " + cmd;

        out.println("USAGE: ");
        out.println(cmdIndent + " [OPTION]...");
        out.println();
        out.println("OPTIONS: ");
        (new CmdLineParser(this)).printUsage(out);
        out.println();
        out.println("EXAMPLES: ");
        out.println(cmdIndent + " -C 500 -Q 200 -a 2 -i 800");
        out.println();
    }

    public static int main(String[] args) throws Exception {
        Arguments argsAll = new Arguments("Arguments");

        if (!argsAll.processArgs(args))
            return -1;

        if (!argsAll.help)
            argsAll.showArgs();

        return 0;
    }
}
