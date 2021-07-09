package com.gocypher.cybench;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;

public abstract class CompileProcess {

    static void printLines(String cmd, InputStream ins) throws Exception {
        String line;
        BufferedReader in = new BufferedReader(new InputStreamReader(ins));
        while ((line = in.readLine()) != null) {
            BenchmarkTest.log(cmd + " " + line);
        }
    }

    static void runProcess(String command) throws Exception {
        int cmdId = command.hashCode();
        BenchmarkTest.log(">" + cmdId + "> Running command: " + command);
        Process pro = Runtime.getRuntime().exec(command);
        printLines(">" + cmdId + "> >> stdout:", pro.getInputStream());
        printLines(">" + cmdId + "> >> stderr:", pro.getErrorStream());
        pro.waitFor();
        BenchmarkTest.log("<" + cmdId + "< exitValue() " + pro.exitValue());
    }

    static class WindowsCompileProcess extends CompileProcess {
        static final String COMPILE = "javac -cp <CLASSPATH> @";

        public WindowsCompileProcess() throws Exception {
            String classPath = T2BUtils.getSysClassPath();
            BenchmarkTest.log("Starting Class Path Listing: >>>>>>>>>>>>>>>>>>>>>>>");
            String[] cps = classPath.split(System.getProperty("path.separator"));
            for (String cpe : cps) {
                BenchmarkTest.log("Class Path Entry: " + cpe);
            }
            BenchmarkTest.log("Completed Class Path Listing: <<<<<<<<<<<<<<<<<<<<<<");

            try {
                String s = makeSourcesList();
                CompileProcess.runProcess(COMPILE.replace("<CLASSPATH>", classPath) + s);
                // runProcess(CLEANUP);
            } catch (Throwable e) {
                BenchmarkTest.err("Cannot run compile");
                e.printStackTrace();
            }
        }

        private static String makeSourcesList() {
            try {
                PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:**/*.java");
                File f = File.createTempFile("sourcesList", "");
                f.deleteOnExit();
                try (FileOutputStream fos = new FileOutputStream(f)) {
                    Files.walk(Paths.get(BenchmarkTest.BENCH_DIR)).filter(fw -> matcher.matches(fw))
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
                BenchmarkTest.log("Created temp sources file: " + f.getAbsolutePath());

                return f.getAbsolutePath();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }
    }
}
