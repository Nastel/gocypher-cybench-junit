package com.gocypher.cybench;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.generators.core.*;
import org.openjdk.jmh.generators.reflection.MyClassInfo;
import org.openjdk.jmh.runner.BenchmarkList;
import org.openjdk.jmh.runner.CompilerHints;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.util.HashMultimap;
import org.openjdk.jmh.util.Multimap;
import org.testng.annotations.Test;

import javassist.ByteArrayClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;

public class BenchmarkTest {

    private static final String WORK_DIR = "prod";
    private static final String TEST_DIR = "build" + File.separator + "classes" + File.separator + "java"
            + File.separator + "test";
    private static final String FORKED_PROCESS_MARKER = "jmh.forked";
    private static final String JMH_CORE_JAR = "/lib/jmh-core-1.31.jar";
    private static final String MY_BENCHMARK_LIST = WORK_DIR + "/META-INF/BenchmarkList";
    private static final String MY_COMPILER_HINTS = WORK_DIR + "/META-INF/CompilerHints";

    private static final int NUMBER_OF_FORKS = 1;
    private static final int NUMBER_OF_WARMUPS = 0;
    private static final int NUMBER_OF_MEASUREMENTS = 1;
    private static final Mode BENCHMARK_MODE = Mode.All;
    private static final Class<? extends Annotation> BENCHMARK_ANNOTATION = Test.class;
    private static final Class<? extends Annotation> BENCHMARK_ANNOTATION2 = org.junit.Test.class;
    private static final Class<? extends Annotation> BENCHMARK_ANNOTATION3 = org.junit.jupiter.api.Test.class;

    private static String BENCHMARK_GENERATOR_CLASS = "org.openjdk.jmh.generators.core.BenchmarkGenerator";
    private static String BENCHMARK_LIST_CLASS = "org.openjdk.jmh.runner.BenchmarkList";
    private static String COMPILER_HINTS_CLASS = "org.openjdk.jmh.runner.CompilerHints";

    // The code to put into the JMH methods - call ME and then return MY replacements
    private static String TAKE_FAKE_ANNOTATIONS = "Object value=com.gocypher.cybench.BenchmarkTest.buildFakeAnnotatedSet();return value;";
    private static String TAKE_FAKE_BENCHMARK_LIST = "Object value=com.gocypher.cybench.BenchmarkTest.getMyBenchmarkList();return value;";
    private static String TAKE_FAKE_COMPILER_HINTS = "Object value=com.gocypher.cybench.BenchmarkTest.getMyCompilerHints();return value;";

    public static void main(String[] args) throws Exception {
        BenchmarkTest benchmarkTest = new BenchmarkTest();
        benchmarkTest.init();
    }

    private void init() throws Exception {
        // http://javadox.com/org.openjdk.jmh/jmh-core/1.31/org/openjdk/jmh/runner/options/OptionsBuilder.html
        Options opt = new OptionsBuilder()
                .include(".*")
                .jvmArgsPrepend("-D" + FORKED_PROCESS_MARKER + "=true")
                .warmupIterations(NUMBER_OF_WARMUPS)
                .mode(BENCHMARK_MODE)
                .forks(NUMBER_OF_FORKS)
                .forks(NUMBER_OF_FORKS)
                .measurementIterations(NUMBER_OF_MEASUREMENTS)
                .build();
        generateBenchmarkList();
        new com.gocypher.cybench.CompileProcess.WindowsCompileProcess();
        new Runner(opt).run();
    }

    private void generateBenchmarkList() throws Exception {
        File prodF = new File(WORK_DIR);
        FileSystemDestination dst = new FileSystemDestination(prodF, prodF);
        BenchmarkGenerator gen = new BenchmarkGenerator();
        myGeneratorSource = new MyGeneratorSource();
        gen.generate(myGeneratorSource, dst);
        gen.complete(myGeneratorSource, dst);
    }

    Collection<ClassInfo> benchmarkClassList;

    class MyGeneratorSource implements GeneratorSource {

        @Override
        public Collection<ClassInfo> getClasses() {
            if (benchmarkClassList != null) {
                return benchmarkClassList;
            }
            benchmarkClassList = new ArrayList<>();
            Collection<File> includeClassFiles = BenchmarkTest.getUTClasses(new File(BenchmarkTest.TEST_DIR));
            for (File classFile : includeClassFiles) {
                Class clazz = null;
                try {
                    String path = classFile.getAbsolutePath();
                    int index = path.indexOf(BenchmarkTest.TEST_DIR);
                    String className = path.replace(File.separator, ".")
                            .substring(index + BenchmarkTest.TEST_DIR.length() + 1, path.length() - ".class".length());
                    clazz = Class.forName(className);
                    BenchmarkTest.log("Class: " + clazz);
                } catch (Throwable t) {
                    BenchmarkTest.log("ERROR: Cant get class: " + t);
                }
                benchmarkClassList.add((ClassInfo) new MyClassInfo(clazz));
            }
            return benchmarkClassList;
        }

        @Override
        public ClassInfo resolveClass(String className) {
            return null;
        }
    }

    private static Collection<File> getUTClasses(File dir) {
        Set<File> fileTree = new HashSet<>();
        if (dir == null || dir.listFiles() == null) {
            return fileTree;
        }
        for (File entry : dir.listFiles()) {
            if (entry.isFile()) {
                if (entry.getName().endsWith(".class")) {
                    fileTree.add(entry);
                }
            } else {
                fileTree.addAll(getUTClasses(entry));
            }
        }
        return fileTree;
    }

    /*
     **************** BenchmarkTestAgent
     *
     */

    private static MyGeneratorSource myGeneratorSource;

    public static Multimap<ClassInfo, MethodInfo> buildFakeAnnotatedSet() {
        Multimap<ClassInfo, MethodInfo> result = new HashMultimap<>();
        for (ClassInfo currentClass : myGeneratorSource.getClasses()) {
            if (currentClass.isAbstract()) {
                continue;
            }
            for (MethodInfo mi : currentClass.getMethods()) {
                Annotation ann = mi.getAnnotation(BENCHMARK_ANNOTATION);
                Annotation ann2 = mi.getAnnotation(BENCHMARK_ANNOTATION2);
                Annotation ann3 = mi.getAnnotation(BENCHMARK_ANNOTATION3);
                if (ann != null || ann2 != null || ann3 != null) {
                    result.put(currentClass, mi);
                }
            }
        }
        return result;
    }

    public static BenchmarkList getMyBenchmarkList() {
        return BenchmarkList.fromFile(MY_BENCHMARK_LIST);
    }

    public static CompilerHints getMyCompilerHints() {
        return CompilerHints.fromFile(MY_COMPILER_HINTS);
    }

    public static class BenchmarkTestAgent {

        static Instrumentation instrumentation;

        public static void premain(String agentArgs, Instrumentation inst) {
            try {
                // It's the Runner...skip the agent
                if (System.getProperty(BenchmarkTest.FORKED_PROCESS_MARKER) != null) {
                    return;
                }
                instrumentation = inst;
                BenchmarkTest.log("Agent Premain called...");
                JarFile jarFile = new JarFile(BenchmarkTest.WORK_DIR + BenchmarkTest.JMH_CORE_JAR);
                byte[] benchmarkListBytes = getJMHBytesForClass(jarFile,
                        BenchmarkTest.BENCHMARK_LIST_CLASS.replace(".", "/") + ".class");
                byte[] compilerHintsBytes = getJMHBytesForClass(jarFile,
                        BenchmarkTest.COMPILER_HINTS_CLASS.replace(".", "/") + ".class");
                byte[] benchmarkGeneratorBytes = getJMHBytesForClass(jarFile,
                        BenchmarkTest.BENCHMARK_GENERATOR_CLASS.replace(".", "/") + ".class");
                // Put in code to basically call MY replacement methods and return MY values - instead of JMH
                replaceCode(BenchmarkTest.BENCHMARK_GENERATOR_CLASS, "buildAnnotatedSet",
                        BenchmarkTest.TAKE_FAKE_ANNOTATIONS, benchmarkGeneratorBytes);
                replaceCode(BenchmarkTest.BENCHMARK_LIST_CLASS, "defaultList", BenchmarkTest.TAKE_FAKE_BENCHMARK_LIST,
                        benchmarkListBytes);
                replaceCode(BenchmarkTest.COMPILER_HINTS_CLASS, "defaultList", BenchmarkTest.TAKE_FAKE_COMPILER_HINTS,
                        compilerHintsBytes);
            } catch (Exception e) {
                e.printStackTrace();
                BenchmarkTest.log("Error: " + e);
            }
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
                BenchmarkTest.log("Modified " + className + "." + methodName);
            } catch (Throwable t) {
                BenchmarkTest.log("Could not create the class creation: " + t);
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
                    int line = 0;
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

    static void log(String msg) {
        System.out.println(msg);
    }

}
