package com.gocypher.cybench;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;

import org.openjdk.jmh.generators.core.*;
import org.openjdk.jmh.generators.reflection.T2BClassInfo;
import org.openjdk.jmh.runner.BenchmarkList;
import org.openjdk.jmh.runner.CompilerHints;
import org.openjdk.jmh.util.HashMultimap;
import org.openjdk.jmh.util.Multimap;

import com.gocypher.cybench.t2b.transform.T2BClassTransformer;

public class Test2Benchmark {

    static final String WORK_DIR_ARG = System.getProperty("t2b.build.dir");
    static final String TEST_DIR_ARG = System.getProperty("t2b.test.dir");
    static final String BENCH_DIR_ARG = System.getProperty("t2b.bench.dir");

    static String WORK_DIR;
    static String TEST_DIR;
    static String BENCH_DIR;

    private static T2BMapper JUNIT4_MAPPER = new T2BMapper(org.junit.Test.class, org.junit.Ignore.class) {

        @Override
        public MethodState isAnnotationSkippable(Annotation ann) {
            org.junit.Test tAnn = (org.junit.Test) ann;
            if (tAnn.expected() != org.junit.Test.None.class) {
                return MethodState.EXCEPTION_EXPECTED;
            }
            return MethodState.VALID;
        }

        @Override
        public Class<? extends Annotation> getSetupAnnotation() {
            return org.junit.Before.class;
        }

        @Override
        public Class<? extends Annotation> getTearDownAnnotation() {
            return org.junit.After.class;
        }
    };
    private static T2BMapper JUNIT5_MAPPER = new T2BMapper(org.junit.jupiter.api.Test.class,
            org.junit.jupiter.api.Disabled.class) {

        @Override
        public MethodState isAnnotationSkippable(Annotation ann) {
            return MethodState.VALID;
        }

        @Override
        public Class<? extends Annotation> getSetupAnnotation() {
            return org.junit.jupiter.api.BeforeEach.class;
        }

        @Override
        public Class<? extends Annotation> getTearDownAnnotation() {
            return org.junit.jupiter.api.AfterEach.class;
        }
    };
    private static T2BMapper TESTNG_MAPPER = new T2BMapper(org.testng.annotations.Test.class,
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

        @Override
        public Class<? extends Annotation> getSetupAnnotation() {
            return org.testng.annotations.BeforeMethod.class;
        }

        @Override
        public Class<? extends Annotation> getTearDownAnnotation() {
            return org.testng.annotations.AfterMethod.class;
        }
    };

    public static final T2BMapper[] T2B_MAPPERS = new T2BMapper[] { //
            JUNIT4_MAPPER //
            , JUNIT5_MAPPER //
            , TESTNG_MAPPER //
    };

    static Collection<String> t2bClassPath = new ArrayList<>(3);
    // The code to inject into the JMH methods - call ME and then return MY replacements
    private static T2BGeneratorSource t2bGeneratorSource;
    Collection<ClassInfo> benchmarkClassList;

    Test2Benchmark() throws IOException {
        WORK_DIR = initWorkDir();
        TEST_DIR = initTestDir();
        BENCH_DIR = initBenchDir();
    }

    private String initWorkDir() throws IOException {
        String workDirPath;
        if (WORK_DIR_ARG == null || WORK_DIR_ARG.isEmpty()) {
            String udProp = System.getProperty("user.dir");
            if (udProp == null || udProp.isEmpty()) {
                workDirPath = ".";
            } else {
                workDirPath = udProp;
            }
        } else {
            workDirPath = WORK_DIR_ARG;
        }
        File workDir = new File(workDirPath);
        workDirPath = workDir.getCanonicalPath();
        log("*** Setting Work dir to use: " + workDirPath);

        return workDirPath;
    }

    private String initTestDir() throws IOException {
        String testDirPath;
        if (TEST_DIR_ARG == null || TEST_DIR_ARG.isEmpty()) {
            // Maven layout
            File testDirMvn = new File(WORK_DIR + "/test-classes");
            if (testDirMvn.exists()) {
                testDirPath = testDirMvn.getAbsolutePath();
                addClassPath(new File(WORK_DIR + "/classes"));
            } else {
                // Gradle layout
                File testDirGrd = new File(WORK_DIR + "/classes/java/test");
                if (testDirGrd.exists()) {
                    testDirPath = testDirGrd.getAbsolutePath();
                    addClassPath(new File(WORK_DIR + "/classes/java/main"));
                } else {
                    // Use build dir
                    testDirPath = WORK_DIR;
                }
            }
        } else {
            testDirPath = TEST_DIR_ARG;
        }

        File testDir = new File(testDirPath);
        testDirPath = testDir.getCanonicalPath();
        log("*** Setting Test Classes dir to use: " + testDirPath);
        addClassPath(testDir.getCanonicalFile());

        return testDirPath;
    }

    private String initBenchDir() throws IOException {
        String benchDirPath;
        if (BENCH_DIR_ARG == null || BENCH_DIR_ARG.isEmpty()) {
            if (WORK_DIR.equals(TEST_DIR)) {
                benchDirPath = TEST_DIR + "/t2b";
            } else {
                benchDirPath = TEST_DIR + "/../t2b";
            }
        } else {
            benchDirPath = BENCH_DIR_ARG;
        }
        File benchDir = new File(benchDirPath);
        benchDirPath = benchDir.getCanonicalPath();
        log("*** Setting Benchmarks dir to use: " + benchDirPath);
        if (benchDir.exists()) {
            try {
                log("*** Removing existing benchmarks dir: " + benchDirPath);
                Files.walk(benchDir.toPath()).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
            } catch (Exception exc) {
                err("failed to delete benchmarks dir", exc);
            }
        }
        addClassPath(benchDir.getCanonicalFile());

        return benchDirPath;
    }

    public static void main(String... args) throws Exception {
        log("Starting Test2Benchmark transformer app...");
        try {
            Test2Benchmark test2Benchmark = new Test2Benchmark();
            test2Benchmark.buildBenchmarks();
        } catch (Throwable t) {
            errWithTrace("failure occurred while running Test2Benchmark transformer app", t);
        }
    }

    private static void addClassPath(File classDir) {
        try {
            T2BUtils.addClassPath(classDir);
            t2bClassPath.add(classDir.getCanonicalPath());
        } catch (Exception exc) {
            err("failed to add classpath entry: " + classDir.getAbsolutePath(), exc);
        }
    }

    public static Multimap<ClassInfo, MethodInfo> buildT2BAnnotatedSet() {
        Multimap<ClassInfo, MethodInfo> result = new HashMultimap<>();
        Collection<ClassInfo> testClasses = t2bGeneratorSource.getClasses();
        log("Starting Test Classes Analysis: >>>>>>>>>>>>>>>>>>>>");
        for (ClassInfo classInfo : testClasses) {
            if (classInfo.isAbstract()) {
                continue;
            }

            T2BClassTransformer clsTransform = new T2BClassTransformer(classInfo);
            clsTransform.doTransform(T2B_MAPPERS);
            clsTransform.storeTransformedClass(BENCH_DIR);

            if (clsTransform.hasBenchmarks()) {
                result.putAll(clsTransform.getClassInfo(), clsTransform.getBenchmarkMethods());
            }
        }
        log("Completed Test Classes Analysis: <<<<<<<<<<<<<<<<<<<");

        return result;
    }

    public static BenchmarkList getBenchmarkList() {
        return BenchmarkList.fromFile(BENCH_DIR + "/META-INF/BenchmarkList");
    }

    /*
     **************** Test2BenchmarkAgent
     *
     */

    public static CompilerHints getCompilerHints() {
        return CompilerHints.fromFile(BENCH_DIR + "/META-INF/CompilerHints");
    }

    public static void log(String msg) {
        System.out.println(msg);
    }

    public static void warn(String msg) {
        System.out.println("WARN: " + msg);
    }

    public static void err(String msg) {
        System.out.println("ERROR: " + msg);
    }

    public static void err(String msg, Throwable t) {
        System.out.println("ERROR: " + msg + ", reason: " + t.getLocalizedMessage());
    }

    public static void errWithTrace(String msg, Throwable t) {
        err(msg, t);
        t.printStackTrace();
    }

    private void buildBenchmarks() throws Exception {
        generateBenchmarkList();
        Test2BenchmarkAgent.restoreJMHCode();
        CompileProcess compileProcess = new CompileProcess.APICompileProcess();
        compileProcess.compile();
        String cp = getT2BClassPath();
        writePropsToFile(BENCH_DIR, cp);
        cleanup();
    }

    private void cleanup() {
        // remove JMH compiler generated empty files
        File tFile = new File("BenchmarkList");
        boolean deleted = tFile.delete();
        tFile = new File("CompilerHints");
        deleted = tFile.delete();
    }

    private static String getT2BClassPath() {
        StringBuilder sb = new StringBuilder();
        for (String cpStr : t2bClassPath) {
            if (sb.length() > 0) {
                sb.append(File.pathSeparator);
            }
            sb.append(cpStr);
        }

        return sb.toString();
    }

    private void generateBenchmarkList() throws Exception {
        File prodF = new File(BENCH_DIR);
        FileSystemDestination dst = new FileSystemDestination(prodF, prodF);
        BenchmarkGenerator gen = new BenchmarkGenerator();
        t2bGeneratorSource = new T2BGeneratorSource();
        gen.generate(t2bGeneratorSource, dst);
        gen.complete(t2bGeneratorSource, dst);

        if (dst.hasErrors()) {
            for (SourceError se : dst.getErrors()) {
                err(se.toString());
            }
        }
        if (dst.hasWarnings()) {
            for (SourceWarning sw : dst.getWarnings()) {
                log("WARNING: " + sw.toString());
            }
        }
    }

    private void writePropsToFile(String benchDir, String classPath) {
        try {
            File f = new File(".benchRunProps");
            if (f.exists()) {
                f.delete();
            }
            try (FileWriter fos = new FileWriter(f)) {
                fos.write("BENCH_DIR=\"" + escapePath(benchDir) + "\"");
                fos.write('\n');
                fos.write("T2B_CLASS_PATH=\"" + escapePath(classPath) + "\"");
                fos.write('\n');
                fos.flush();
            }
        } catch (IOException exc) {
            errWithTrace("failed to write benchmark run configuration properties", exc);
        }
    }

    private static String escapePath(String path) {
        return path == null ? path : path.replace("\\", "/");
    }

    class T2BGeneratorSource implements GeneratorSource {

        @Override
        public Collection<ClassInfo> getClasses() {
            if (benchmarkClassList != null) {
                return benchmarkClassList;
            }
            benchmarkClassList = new ArrayList<>();
            File testDir = new File(Test2Benchmark.TEST_DIR).getAbsoluteFile();
            String testDirPath = testDir.getPath();
            if (!testDirPath.endsWith(File.separator)) {
                testDirPath += File.separator;
            }

            if (!testDir.exists()) {
                Test2Benchmark.err("test dir does not exist: " + testDir);
            } else {
                Test2Benchmark.log("Starting Test Classes Search: >>>>>>>>>>>>>>>>>>>>>>");
                Collection<File> includeClassFiles = T2BUtils.getUTClasses(testDir);
                for (File classFile : includeClassFiles) {
                    try {
                        String path = classFile.getAbsolutePath();
                        int index = path.indexOf(testDirPath);
                        String className = path.replace(File.separator, ".") //
                                .substring(index + testDirPath.length(), path.length() - ".class".length());
                        // TODO far from bulletproof

                        Class<?> clazz = Class.forName(className);
                        Test2Benchmark.log("Found Test Class: " + clazz);
                        benchmarkClassList.add(new T2BClassInfo(clazz));
                    } catch (Throwable t) {
                        Test2Benchmark.err("can't get test class: " + t);
                    }
                }
                Test2Benchmark.log("Completed Test Classes Search: <<<<<<<<<<<<<<<<<<<<<");
            }
            return benchmarkClassList;
        }

        @Override
        public ClassInfo resolveClass(String className) {
            return null;
        }
    }

}
