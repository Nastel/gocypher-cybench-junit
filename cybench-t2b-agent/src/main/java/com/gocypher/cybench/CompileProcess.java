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

import org.slf4j.Logger;
import org.slf4j.event.Level;

public abstract class CompileProcess {
    private static Logger LOGGER = T2BUtils.getLogger(CompileProcess.class);

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
            LOGGER.info("Created sources file: {}", f.getAbsolutePath());

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
                LOGGER.info("> Compiling T2B generated sources: @{}", s);
                int exitValue = compiler.run(System.in, System.out, System.err, "@" + s);
                LOGGER.info("< T2B generated sources compilation completed: exitValue={}", exitValue);
            } catch (Throwable e) {
                LOGGER.error("T2B generated sources compilation failed", e);
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
            LOGGER.info("Starting Class Path Listing: >>>>>>>>>>>>>>>>>>>>>>>");
            String[] cps = classPath.split(File.pathSeparator);
            for (String cpe : cps) {
                LOGGER.info("Class Path Entry: {}", cpe);
            }
            LOGGER.info("Completed Class Path Listing: <<<<<<<<<<<<<<<<<<<<<<");

            return classPath;
        }

        @Override
        public void compile() {
            try {
                String s = CompileProcess.makeSourcesList();
                runProcess(CMD_COMPILE.replace("<CLASSPATH>", "\"" + classPath + "\"") + s);
            } catch (Throwable e) {
                LOGGER.error("Cannot run compile", e);
            }
        }

        static void printLines(String cmd, InputStream ins, Level lvl) throws Exception {
            String line;
            BufferedReader in = new BufferedReader(new InputStreamReader(ins));
            while ((line = in.readLine()) != null) {
                if (lvl == Level.ERROR) {
                    LOGGER.error("{} {}", cmd, line.replaceAll("[\\r\\n]", ""));
                } else {
                    LOGGER.info("{} {}", cmd, line.replaceAll("[\\r\\n]", ""));
                }
            }
        }

        static void runProcess(String command) throws Exception {
            int cmdId = command.hashCode();
            LOGGER.info(">{}> Running command: {}", cmdId, command);
            Process pro = Runtime.getRuntime().exec(command);
            printLines(">" + cmdId + "> >> stdout:", pro.getInputStream(), Level.INFO);
            printLines(">" + cmdId + "> >> stderr:", pro.getErrorStream(), Level.ERROR);
            pro.waitFor();
            LOGGER.info("<{}< exitValue={}", cmdId, pro.exitValue());
        }
    }
}
