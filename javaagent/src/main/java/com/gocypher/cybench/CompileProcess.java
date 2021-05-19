package com.gocypher.cybench;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import static com.gocypher.cybench.BenchmarkTest.log;

public abstract class CompileProcess {
    static final String COMPILE_SCRIPT = "./compileGenerated.bat";

    void runProcess(String command) throws Exception {
        Process pro = Runtime.getRuntime().exec(command);
        printLines(command + " stdout:", pro.getInputStream());
        printLines(command + " stderr:", pro.getErrorStream());
        pro.waitFor();
        log(command + " exitValue() " + pro.exitValue());
    }

    void printLines(String cmd, InputStream ins) throws Exception {
        String line = null;
        BufferedReader in = new BufferedReader(
                new InputStreamReader(ins));
        while ((line = in.readLine()) != null) {
            log(cmd + " " + line);
        }
    }


    static class WindowsCompileProcess extends CompileProcess {
        static final String MAKE_SOURCES_LIST = "dir /s /B prod\\*.java > sources.txt";
        static final String COMPILE = "javac -cp c:\\workspace\\tnt4j-streams2\\build\\tnt4j-streams-1.12.0-SNAPSHOT\\lib\\*;c:\\workspace\\tnt4j-streams2\\build\\tnt4j-streams-1.12.0-SNAPSHOT\\;c:\\workspace\\tnt4j-streams2\\tnt4j-streams-core\\target\\test-classes\\;prod\\lib\\*;build/classes/java/test @sources.txt";
        static final String CLEANUP = "rm sources.txt";


        public WindowsCompileProcess() {
            try {
                runProcess(MAKE_SOURCES_LIST);
                runProcess(COMPILE);
                runProcess(CLEANUP);
            } catch (Exception e) {
                log("Cannot run compile");
                e.printStackTrace();
            }
        }
    }
}
