package org.yesworkflow;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import junit.framework.TestCase;

public abstract class YesWorkflowTestCase extends TestCase {
    
    public static final String EOL = System.getProperty("line.separator");

    protected OutputStream stdoutBuffer;
    protected OutputStream stderrBuffer;
    
    protected PrintStream stdoutStream;
    protected PrintStream stderrStream;
    
    protected static Path getTestDirectory(String testName) throws IOException {
        Path testDirectoryPath = new File("target/tests/" + testName).toPath();
        Files.createDirectories(testDirectoryPath);
        return testDirectoryPath;
    }
    
    
    @Override
    public void setUp() throws Exception {
        
        super.setUp();

        stdoutBuffer = new ByteArrayOutputStream();
        stdoutStream = new PrintStream(stdoutBuffer);
    
        stderrBuffer = new ByteArrayOutputStream();
        stderrStream = new PrintStream(stderrBuffer);
    }
    
    // reads a file from the filesystem, replacing stored EOL with local EOL sequence
    public static String readTextFile(String path) throws IOException {
        InputStream reader =  new FileInputStream(path);
        return readLineEndingNormalizedFileFromReader(new InputStreamReader(reader));
    } 
    
    // reads a file from the classpath, replacing stored EOL with local EOL sequence
    public static String readTextFileOnClasspath(String path) throws IOException {
        InputStream stream = YesWorkflowTestCase.class.getClassLoader().getResourceAsStream(path);
        return readLineEndingNormalizedFileFromReader(new InputStreamReader(stream));
    } 
    
    // reads a file from input stream line by line, replacing stored EOL with local EOL sequence
    public static String readLineEndingNormalizedFileFromReader(InputStreamReader fileReader) throws IOException {
        BufferedReader reader = new BufferedReader(fileReader);
        String line = null;
        StringBuilder stringBuilder = new StringBuilder();
        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line);
            stringBuilder.append(EOL);
        }
        return stringBuilder.toString();
    }
}
