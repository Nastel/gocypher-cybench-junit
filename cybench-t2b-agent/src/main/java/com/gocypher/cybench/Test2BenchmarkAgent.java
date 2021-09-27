package com.gocypher.cybench;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.nio.file.Paths;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javassist.ByteArrayClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;

public class Test2BenchmarkAgent {

    static Instrumentation instrumentation;

    private static String TAKE_FAKE_ANNOTATIONS = "Object value=com.gocypher.cybench.Test2Benchmark.buildFakeAnnotatedSet();return value;";
    private static String TAKE_FAKE_BENCHMARK_LIST = "Object value=com.gocypher.cybench.Test2Benchmark.getMyBenchmarkList();return value;";
    private static String TAKE_FAKE_COMPILER_HINTS = "Object value=com.gocypher.cybench.Test2Benchmark.getMyCompilerHints();return value;";

    private static String BENCHMARK_GENERATOR_CLASS = "org.openjdk.jmh.generators.core.BenchmarkGenerator";
    private static String BENCHMARK_LIST_CLASS = "org.openjdk.jmh.runner.BenchmarkList";
    private static String COMPILER_HINTS_CLASS = "org.openjdk.jmh.runner.CompilerHints";

    public static void premain(String agentArgs, Instrumentation inst) {
        try {
            instrumentation = inst;
            Test2Benchmark.log("Test2Benchmark Agent Premain called...");

            Class<?> klass = org.openjdk.jmh.annotations.Benchmark.class;
            URL location = klass.getResource('/' + klass.getName().replace('.', '/') + ".class");
            // jar:file:/C:/Users/slabs/.m2/repository/org/openjdk/jmh/jmh-core/1.32/jmh-core-1.32.jar!/org/openjdk/jmh/annotations/Benchmark.class
            String[] split = location.toString().replaceFirst("jar:file:/", "").split("!");
            Test2Benchmark.log("JMH:" + split[0]);
            File file = Paths.get(split[0]).toFile();
            Test2Benchmark.log("JMH:" + file.getAbsolutePath());

            JarFile jarFile = new JarFile(file);

            byte[] benchmarkListBytes = getBytes(jarFile, BENCHMARK_LIST_CLASS);
            byte[] compilerHintsBytes = getBytes(jarFile, COMPILER_HINTS_CLASS);
            byte[] benchmarkGeneratorBytes = getBytes(jarFile, BENCHMARK_GENERATOR_CLASS);
            // Put in code to basically call MY replacement methods and return MY values - instead of JMH
            replaceCode(BENCHMARK_GENERATOR_CLASS, "buildAnnotatedSet", TAKE_FAKE_ANNOTATIONS, benchmarkGeneratorBytes);
            replaceCode(BENCHMARK_LIST_CLASS, "defaultList", TAKE_FAKE_BENCHMARK_LIST, benchmarkListBytes);
            replaceCode(COMPILER_HINTS_CLASS, "defaultList", TAKE_FAKE_COMPILER_HINTS, compilerHintsBytes);
        } catch (Exception e) {
            Test2Benchmark.errWithTrace("failed to initialize agent", e);
        }
    }

    private static byte[] getBytes(JarFile jarFile, String className) throws IOException {
        return getJMHBytesForClass(jarFile, className.replace(".", "/") + ".class");
    }

    static void replaceCode(String className, String methodName, String code, byte[] bytes) {
        try {
            ClassPool pool = ClassPool.getDefault(); // Edited to simplify
            pool.insertClassPath(new ByteArrayClassPath(className, bytes));
            CtClass ctClass = pool.get(className);
            CtMethod[] methods = ctClass.getDeclaredMethods(methodName);
            methods[0].insertBefore(code);
            ClassDefinition definition = new ClassDefinition(Class.forName(className), ctClass.toBytecode());
            instrumentation.redefineClasses(definition);
            Test2Benchmark.log("Modified " + className + "." + methodName);
        } catch (Throwable t) {
            Test2Benchmark.log("Could not modify class method: " + className + "." + methodName + ", exc: " + t);
        }
    }

    static byte[] getJMHBytesForClass(JarFile jarFile, String className) throws IOException {
        JarEntry entry = jarFile.getJarEntry(className);
        return streamToByteArray(jarFile.getInputStream(entry));
    }

    static byte[] streamToByteArray(InputStream stream) throws IOException {
        try {
            byte[] buffer = new byte[1024];
            try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
                int line;
                // read bytes from stream, and store them in buffer
                while ((line = stream.read(buffer)) != -1) {
                    // Writes bytes from byte array (buffer) into output stream.
                    os.write(buffer, 0, line);
                }

                os.flush();

                return os.toByteArray();
            }
        } finally {
            stream.close();
        }
    }
}
