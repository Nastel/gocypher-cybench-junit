package com.gocypher.cybench;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;

import static com.gocypher.cybench.BenchmarkTest.log;

public abstract class CompileProcess {
    static final String COMPILE_SCRIPT = "./compileGenerated.bat";

    public static void main(String[] args) {
        WindowsCompileProcess.makeSourcesList();
    }

    void runProcess(String command) throws Exception {
        log("Running command: " + command);
        Process pro = Runtime.getRuntime().exec(command);
        printLines(command + " stdout:", pro.getInputStream());
        printLines(command + " stderr:", pro.getErrorStream());
        pro.waitFor();
        log(command + " exitValue() " + pro.exitValue());
    }

    static void printLines(String cmd, InputStream ins) throws Exception {
        String line;
        BufferedReader in = new BufferedReader(new InputStreamReader(ins));
        while ((line = in.readLine()) != null) {
            log(cmd + " " + line);
        }
    }

    static class WindowsCompileProcess extends CompileProcess {

        //TODO collect and set actual classpath
        static final String COMPILE = "javac -cp c:\\workspace\\tnt4j-streams2\\build\\tnt4j-streams-1.12.0-SNAPSHOT\\lib\\*;c:\\workspace\\tnt4j-streams2\\build\\tnt4j-streams-1.12.0-SNAPSHOT\\;c:\\workspace\\tnt4j-streams2\\tnt4j-streams-core\\target\\test-classes\\;prod\\lib\\*;build/classes/java/test @";

        public WindowsCompileProcess() {
            try {
                String s = makeSourcesList();
                runProcess(COMPILE + s);
                // runProcess(CLEANUP);
            } catch (Exception e) {
                log("Cannot run compile");
                e.printStackTrace();
            }
        }

        private static String makeSourcesList() {
            try {
                PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:**/*.java");
                File f = File.createTempFile("sourcesList", "");
                //f.deleteOnExit();
                try (FileOutputStream fos = new FileOutputStream(f)) {
                    Files.walk(Paths.get(System.getProperty("buildDir") + "/..")).filter(fw -> matcher.matches(fw))
                            .filter(Files::isRegularFile).forEach(fw -> {
                                try {
                                    fos.write(fw.toAbsolutePath().toString().getBytes(StandardCharsets.UTF_8));
                                    fos.write('\n');
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            });
                    fos.flush();
                }
                log("Created sources file" + f.getAbsolutePath());

                return f.getAbsolutePath();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }
    }
}
