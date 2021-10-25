/*
 * Copyright (C) 2020-2021, K2N.IO.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 *
 */

package com.gocypher.cybench;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

public abstract class CompileProcess {

    static String makeSourcesList() {
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

    public abstract void compile();

    static class APICompileProcess extends CompileProcess {

        public APICompileProcess() throws Exception {
        }

        @Override
        public void compile() {
            try {
                String s = CompileProcess.makeSourcesList();
                JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
                Test2Benchmark.log("> Compiling T2B generated sources: @" + s);
                int exitValue = compiler.run(System.in, System.out, System.err, "@" + s);
                Test2Benchmark.log("< T2B generated sources compilation completed: exitValue=" + exitValue);
            } catch (Throwable e) {
                Test2Benchmark.errWithTrace("T2B generated sources compilation failed", e);
            }
        }
    }

    static class WindowsCompileProcess extends CompileProcess {
        final String CMD_COMPILE;

        private final String classPath;

        public WindowsCompileProcess() throws Exception {
            classPath = getClassPath();
            CMD_COMPILE = getJavacCmd();
        }

        private String getJavacCmd() {
            String javac = "javac";
            String prop = System.getProperty("t2b.jdk.home");
            if (prop == null || prop.isEmpty()) {
                String libPath = System.getProperty("java.library.path");
                prop = libPath.substring(0, libPath.indexOf(';'));
                if (prop != null && !prop.isEmpty()) {
                    String tJavac = prop + "/javac";
                    String osName = System.getProperty("os.name");
                    if (osName.startsWith("Windows")) {
                        tJavac += ".exe";
                    }

                    File javacFile = new File(tJavac);
                    if (javacFile.exists()) {
                        javac = tJavac;
                    }
                }
            } else {
                javac = prop + "/bin/javac";
            }
            return javac + " -cp <CLASSPATH> @";
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
                String s = CompileProcess.makeSourcesList();
                runProcess(CMD_COMPILE.replace("<CLASSPATH>", "\"" + classPath + "\"") + s);
            } catch (Throwable e) {
                Test2Benchmark.errWithTrace("cannot run compile", e);
            }
        }

        static void printLines(String cmd, InputStream ins) throws Exception {
            String line;
            BufferedReader in = new BufferedReader(new InputStreamReader(ins));
            while ((line = in.readLine()) != null) {
                Test2Benchmark.log(cmd + " " + line.replaceAll("[\\r\\n]", ""));
            }
        }

        static void runProcess(String command) throws Exception {
            int cmdId = command.hashCode();
            Test2Benchmark.log(">" + cmdId + "> Running command: " + command);
            Process pro = Runtime.getRuntime().exec(command);
            printLines(">" + cmdId + "> >> stdout:", pro.getInputStream());
            printLines(">" + cmdId + "> >> stderr:", pro.getErrorStream());
            pro.waitFor();
            Test2Benchmark.log("<" + cmdId + "< exitValue=" + pro.exitValue());
        }
    }
}
