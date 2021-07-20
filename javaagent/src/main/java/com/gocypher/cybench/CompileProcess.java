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
            Test2Benchmark.log(cmd + " " + line);
        }
    }

    static void runProcess(String command) throws Exception {
        int cmdId = command.hashCode();
        Test2Benchmark.log(">" + cmdId + "> Running command: " + command);
        Process pro = Runtime.getRuntime().exec(command);
        printLines(">" + cmdId + "> >> stdout:", pro.getInputStream());
        printLines(">" + cmdId + "> >> stderr:", pro.getErrorStream());
        pro.waitFor();
        Test2Benchmark.log("<" + cmdId + "< exitValue() " + pro.exitValue());
    }

    public abstract void compile();

    static class WindowsCompileProcess extends CompileProcess {
        static final String CMD_COMPILE = "javac -cp <CLASSPATH> @";

        private final String classPath;

        public WindowsCompileProcess() throws Exception {
            classPath = getClassPath();
        }

        private String getClassPath() throws Exception {
            String classPath = T2BUtils.getCurrentClassPath();
            Test2Benchmark.log("Starting Class Path Listing: >>>>>>>>>>>>>>>>>>>>>>>");
            String[] cps = classPath.split(File.pathSeparator);
            for (String cpe : cps) {
                Test2Benchmark.log("Class Path Entry: " + cpe);
            }
            Test2Benchmark.log("Completed Class Path Listing: <<<<<<<<<<<<<<<<<<<<<<");

            return classPath;
        }

        @Override
        public void compile() {
            try {
                String s = makeSourcesList();
                CompileProcess.runProcess(CMD_COMPILE.replace("<CLASSPATH>", "\"" + classPath + "\"") + s);
            } catch (Throwable e) {
                Test2Benchmark.err("Cannot run compile");
                e.printStackTrace();
            }
        }

        private static String makeSourcesList() {
            try {
                PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:**/*.java");
                File f = new File(Test2Benchmark.BENCH_DIR, ".sourceList");
                try (FileOutputStream fos = new FileOutputStream(f)) {
                    Files.walk(Paths.get(Test2Benchmark.BENCH_DIR)) //
                            .filter(fw -> matcher.matches(fw)) //
                            .filter(Files::isRegularFile) //
                            .forEach(fw -> {
                                try {
                                    fos.write(fw.toAbsolutePath().toString().getBytes(StandardCharsets.UTF_8));
                                    fos.write('\n');
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            });
                    fos.flush();
                }
                Test2Benchmark.log("Created sources file: " + f.getAbsolutePath());

                return f.getAbsolutePath();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        public String getCompileClassPath() {
            return classPath;
        }
    }
}
