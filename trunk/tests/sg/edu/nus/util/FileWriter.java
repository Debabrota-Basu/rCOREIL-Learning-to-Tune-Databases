package sg.edu.nus.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;

import static org.apache.commons.io.FilenameUtils.getFullPath;
import static org.apache.commons.io.FilenameUtils.normalize;
import org.slf4j.Logger;
import static org.slf4j.LoggerFactory.getLogger;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.io.Files.newWriter;

public class FileWriter
{
    private static final int DEFAULT_FLUSH_SIZE = 1;
    private static final Logger LOG = getLogger("e2s2");

    private final String _filename;
    private final int _flushSize;
    private final BufferedWriter _writer;

    private int _count = 0;

    public FileWriter(String filename, int flushSize) throws Exception {
        _filename = normalize(filename);
        _flushSize = flushSize;
        _writer = getWriter();
    }

    public FileWriter(String filename) throws Exception {
        _filename = normalize(filename);
        _flushSize = DEFAULT_FLUSH_SIZE;
        _writer = getWriter();
    }

    private BufferedWriter getWriter() throws Exception {
        if (_filename == null || _filename.isEmpty()) {
            LOG.debug("Dummy file writer is in use");
            return null;
        }

        mkdir(getFullPath(_filename));

        File file = new File(_filename);
        if (!file.createNewFile()) {
            LOG.error("File already exists: " + _filename);
            return null;
        }
        else {
            return newWriter(file, UTF_8);
        }
    }

    private void mkdir(String path) {
        File dir = new File(path);
        dir.mkdirs();
    }

    public void write(String msg) throws IOException {
        if (_writer == null)
            return; // for dummy writer

        _writer.append(msg + "\n");
        ++_count;

        if (_count % _flushSize == 0) {
            _writer.flush();
        }
    }

    public void endOfFile() throws IOException {
        if (_writer == null)
            return; // for dummy writer

        _writer.flush();
    }

    public String getFilename() {
        return _filename;
    }
}