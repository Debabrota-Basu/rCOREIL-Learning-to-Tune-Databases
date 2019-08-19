package sg.edu.nus.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

import static org.apache.commons.io.FilenameUtils.normalize;

import com.google.common.base.Splitter;
import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.base.Preconditions.checkArgument;
import com.google.common.collect.ImmutableList;
import static com.google.common.io.Files.newReader;

public class FileReader
{
    private static final String DEFAULT_SPLIT_PATTERN = "\\s+";

    private final String _filename;
    private final BufferedReader _reader;
    private final Splitter _splitter;

    public FileReader(String filename, String splitPunc) throws Exception {
        checkArgument(filename != null);
        _filename = normalize(filename);

        _reader = newReader(new File(_filename), UTF_8);

        String splitePattern;
        if (splitPunc == null || splitPunc.trim().isEmpty()) {
            splitePattern = DEFAULT_SPLIT_PATTERN;
        }
        else {
            splitePattern = splitPunc.trim() + "\\s+";
        }
        _splitter = Splitter.on(Pattern.compile(splitePattern)).trimResults();
    }

    public String readLine() throws IOException {
        return _reader.readLine();
    }

    public List<String> readLineAndSplit() throws IOException {
        String line = _reader.readLine();

        if (line == null)
            return null;
        else
            return _splitter.splitToList(line);
    }

    public String getFilename() {
        return _filename;
    }
}