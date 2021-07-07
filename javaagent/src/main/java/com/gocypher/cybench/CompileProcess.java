package com.gocypher.cybench;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
            ClassLoader classloader = ClassLoader.getSystemClassLoader();
            if (classloader != null) {
                URL[] urls;
                if (classloader instanceof URLClassLoader) {
                    urls = ((URLClassLoader) classloader).getURLs();
                } else {
                    Class<?> clCls = classloader.getClass();
                    Field ucpField = clCls.getDeclaredField("ucp");
                    ucpField.setAccessible(true);
                    Object ucp = ucpField.get(classloader);
                    Method getUrlsMethod = ucp.getClass().getDeclaredMethod("getURLs");
                    getUrlsMethod.setAccessible(true);
                    urls = (URL[]) getUrlsMethod.invoke(ucp);
                }
                String cp = Stream.of(urls).map(u -> u.getPath()).map(s -> s.substring(1)).peek(System.out::println)
                        .collect(Collectors.joining(System.getProperty("path.separator")));

                try {
                    String s = makeSourcesList();
                    CompileProcess.runProcess(COMPILE.replace("<CLASSPATH>", cp) + s);
                    // runProcess(CLEANUP);
                } catch (Exception e) {
                    BenchmarkTest.err("Cannot run compile");
                    e.printStackTrace();
                }
            }
        }

        private static String makeSourcesList() {
            try {
                PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:**/*.java");
                File f = File.createTempFile("sourcesList", "");
                // f.deleteOnExit();
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
                BenchmarkTest.log("Created temp sources file: " + f.getAbsolutePath());

                return f.getAbsolutePath();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }
    }
}
