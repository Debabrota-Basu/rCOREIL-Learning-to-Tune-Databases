package sg.edu.nus.autotune;

import static com.google.common.base.Preconditions.checkArgument;
import sg.edu.nus.util.FileWriter;

public abstract class Execution
{
    protected static final String DEFAULT_RESULT_FILE_PREFIX = "autotune";

    protected final double _gamma = 0.9999;

    protected String _resultFilePrefix;
    protected FileWriter _output;

    protected Execution(DataConnectivity data) {
        checkArgument(data != null);
    }

    protected abstract int run() throws Exception;

    public String getResultFilePrefix() {
        return _resultFilePrefix;
    }

    public void setOutputWriter(FileWriter writer) {
        _output = writer;
    }
}