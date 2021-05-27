package com.gocypher.cybench;

import java.io.File;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

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

public class BenchmarkTest {

    static final String WORK_DIR = System.getProperty("buildDir");
    static final String TEST_DIR = System.getProperty("buildDir") + File.separator + ".." + File.separator
            + "test-classes" + File.separator;
    static final String FORKED_PROCESS_MARKER = "jmh.forked";
    static final String MY_BENCHMARK_LIST = WORK_DIR + "/META-INF/BenchmarkList";
    static final String MY_COMPILER_HINTS = WORK_DIR + "/META-INF/CompilerHints";

    static final int NUMBER_OF_FORKS = 1;
    static final int NUMBER_OF_WARMUPS = 0;
    static final int NUMBER_OF_MEASUREMENTS = 1;
    static final Mode BENCHMARK_MODE = Mode.All;
    static final Class<? extends Annotation> BENCHMARK_ANNOTATION = org.testng.annotations.Test.class;
    static final Class<? extends Annotation> BENCHMARK_ANNOTATION2 = org.junit.Test.class;
    static final Class<? extends Annotation> BENCHMARK_ANNOTATION3 = org.junit.jupiter.api.Test.class;

    // The code to put into the JMH methods - call ME and then return MY replacements
    private static MyGeneratorSource myGeneratorSource;
    Collection<ClassInfo> benchmarkClassList;

    public static void main(String[] args) throws Exception {
        log("Main started");
        BenchmarkTest benchmarkTest = new BenchmarkTest();
        benchmarkTest.init();
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

    /*
     **************** BenchmarkTestAgent
     *
     */

    public static CompilerHints getMyCompilerHints() {
        return CompilerHints.fromFile(MY_COMPILER_HINTS);
    }

    static void log(String msg) {
        System.out.println(msg);
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

    class MyGeneratorSource implements GeneratorSource {

        @Override
        public Collection<ClassInfo> getClasses() {
            if (benchmarkClassList != null) {
                return benchmarkClassList;
            }
            benchmarkClassList = new ArrayList<>();
            File testDir = new File(BenchmarkTest.TEST_DIR).getAbsoluteFile();
            Collection<File> includeClassFiles = BenchmarkTest.getUTClasses(testDir);

            if (!testDir.exists()) {
                BenchmarkTest.log("NO TEST DIR" + testDir.getAbsolutePath());
            }
            for (File classFile : includeClassFiles) {
                Class<?> clazz = null;
                try {
                    String path = classFile.getAbsolutePath();
                    int index = path.indexOf(BenchmarkTest.TEST_DIR);
                    String className = path.replace(File.separator, ".")
                            .substring(index + BenchmarkTest.TEST_DIR.length(), path.length() - ".class".length());
                    // TODO far from bulletproof

                    clazz = Class.forName(className);
                    BenchmarkTest.log("Class: " + clazz);
                } catch (Throwable t) {
                    BenchmarkTest.log("ERROR: Cant get class: " + t);
                }
                benchmarkClassList.add(new MyClassInfo(clazz));
            }
            return benchmarkClassList;
        }

        @Override
        public ClassInfo resolveClass(String className) {
            return null;
        }
    }

}
