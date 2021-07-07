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
    static final String TEST_DIR = System.getProperty("testDir");
    static final String TEST_DIR_MVN = WORK_DIR + File.separator + ".." + File.separator + "test-classes"
            + File.separator;
    static final String TEST_DIR_GRD = WORK_DIR + File.separator + ".." + File.separator + "test" + File.separator;
    static final String FORKED_PROCESS_MARKER = "jmh.forked";
    static final String MY_BENCHMARK_LIST = WORK_DIR + "/META-INF/BenchmarkList";
    static final String MY_COMPILER_HINTS = WORK_DIR + "/META-INF/CompilerHints";

    static final int NUMBER_OF_FORKS = 1;
    static final int NUMBER_OF_WARMUPS = 0;
    static final int NUMBER_OF_MEASUREMENTS = 1;
    static final Mode BENCHMARK_MODE = Mode.All;

    private static AnnotationCondition ANN_COND_JU = new AnnotationCondition(org.junit.Test.class,
            org.junit.Ignore.class) {

        @Override
        public MethodState isAnnotationSkippable(Annotation ann) {
            org.junit.Test tAnn = (org.junit.Test) ann;
            if (tAnn.expected() != org.junit.Test.None.class) {
                return MethodState.EXCEPTION_EXPECTED;
            }
            return MethodState.VALID;
        }
    };
    private static AnnotationCondition ANN_COND_JU5 = new AnnotationCondition(org.junit.jupiter.api.Test.class,
            org.junit.jupiter.api.Disabled.class) {

        @Override
        public MethodState isAnnotationSkippable(Annotation ann) {
            return MethodState.VALID;
        }
    };
    private static AnnotationCondition ANN_COND_NG = new AnnotationCondition(org.testng.annotations.Test.class,
            org.testng.annotations.Ignore.class) {

        @Override
        public MethodState isAnnotationSkippable(Annotation ann) {
            org.testng.annotations.Test tAnn = (org.testng.annotations.Test) ann;
            if (!tAnn.enabled()) {
                return MethodState.DISABLED;
            }
            if (tAnn.expectedExceptions().length > 0) {
                return MethodState.EXCEPTION_EXPECTED;
            }

            return MethodState.VALID;
        }
    };

    public static final AnnotationCondition[] BENCHMARK_ANNOTATIONS = new AnnotationCondition[] { //
            ANN_COND_NG//
            , ANN_COND_JU //
            , ANN_COND_JU5 //
    };

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
        for (ClassInfo classInfo : myGeneratorSource.getClasses()) {
            if (classInfo.isAbstract()) {
                continue;
            }

            for (MethodInfo methodInfo : classInfo.getMethods()) {
                AnnotationCondition.MethodState testValid = isValidTest(methodInfo, BENCHMARK_ANNOTATIONS);
                if (testValid == AnnotationCondition.MethodState.VALID) {
                    result.put(classInfo, methodInfo);
                } else if (testValid != AnnotationCondition.MethodState.NOT_TEST) {
                    log("SKIPPING: " + methodInfo.getQualifiedName() + ", REASON: " + testValid.name());
                }
            }
        }
        return result;
    }

    private static AnnotationCondition.MethodState isValidTest(MethodInfo mi, AnnotationCondition... aConds) {
        if (aConds != null) {
            for (AnnotationCondition aCond : aConds) {
                AnnotationCondition.MethodState ms = aCond.isValid(mi);
                if (ms == AnnotationCondition.MethodState.NOT_TEST) {
                    continue;
                } else {
                    return ms;
                }
            }
        }

        return AnnotationCondition.MethodState.NOT_TEST;
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

    static void err(String msg) {
        System.err.println(msg);
    }

    private void init() throws Exception {
        // http://javadox.com/org.openjdk.jmh/jmh-core/1.31/org/openjdk/jmh/runner/options/OptionsBuilder.html
        Options opt = new OptionsBuilder()
                .include(".*")
                .jvmArgsPrepend("-D" + FORKED_PROCESS_MARKER + "=true")
                .warmupIterations(NUMBER_OF_WARMUPS)
                .mode(BENCHMARK_MODE)
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

        if (dst.hasErrors()) {
            for (SourceError se : dst.getErrors()) {
                err("ERROR: " + se.toString());
            }
        }
        if (dst.hasWarnings()) {
            for (SourceWarning sw : dst.getWarnings()) {
                log("WARNING: " + sw.toString());
            }
        }
    }

    class MyGeneratorSource implements GeneratorSource {

        @Override
        public Collection<ClassInfo> getClasses() {
            if (benchmarkClassList != null) {
                return benchmarkClassList;
            }
            benchmarkClassList = new ArrayList<>();
            String testDirPath = BenchmarkTest.TEST_DIR;
            File testDir;
            if (testDirPath == null || testDirPath.isEmpty()) {
                testDirPath = BenchmarkTest.TEST_DIR_MVN;
                testDir = new File(testDirPath);
                if (!testDir.exists()) {
                    testDirPath = BenchmarkTest.TEST_DIR_GRD;
                    testDir = new File(testDirPath);
                }
            } else {
                testDir = new File(testDirPath);
            }
            testDir = testDir.getAbsoluteFile();

            if (!testDir.exists()) {
                BenchmarkTest.log("NO TEST DIR" + testDir);
            } else {
                Collection<File> includeClassFiles = BenchmarkTest.getUTClasses(testDir);
                for (File classFile : includeClassFiles) {
                    Class<?> clazz = null;
                    try {
                        String path = classFile.getAbsolutePath();
                        int index = path.indexOf(testDirPath);
                        String className = path.replace(File.separator, ".").substring(index + testDirPath.length(),
                                path.length() - ".class".length());
                        // TODO far from bulletproof

                        clazz = Class.forName(className);
                        BenchmarkTest.log("Class: " + clazz);
                    } catch (Throwable t) {
                        BenchmarkTest.log("ERROR: Can't get class: " + t);
                    }
                    benchmarkClassList.add(new MyClassInfo(clazz));
                }
            }
            return benchmarkClassList;
        }

        @Override
        public ClassInfo resolveClass(String className) {
            return null;
        }
    }

}
