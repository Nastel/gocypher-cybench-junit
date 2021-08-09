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
import org.openjdk.jmh.generators.reflection.MyClassInfo;
import org.openjdk.jmh.runner.BenchmarkList;
import org.openjdk.jmh.runner.CompilerHints;
import org.openjdk.jmh.util.HashMultimap;
import org.openjdk.jmh.util.Multimap;

public class Test2Benchmark {

    static final String WORK_DIR_ARG = System.getProperty("t2b.buildDir");
    static final String TEST_DIR_ARG = System.getProperty("t2b.testDir");
    static final String BENCH_DIR_ARG = System.getProperty("t2b.benchDir");

    static String WORK_DIR;
    static String TEST_DIR;
    static String BENCH_DIR;

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
            ANN_COND_NG //
            , ANN_COND_JU //
            , ANN_COND_JU5 //
    };

    static Collection<String> t2bClassPath = new ArrayList<>(3);
    // The code to put into the JMH methods - call ME and then return MY replacements
    private static MyGeneratorSource myGeneratorSource;
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
                err("failed to delete benchmarks dir, reason: " + exc.getLocalizedMessage());
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
            err("failure occurred while running Test2Benchmark transformer app, exc: " + t.getLocalizedMessage());
            t.printStackTrace();
        }
    }

    private static void addClassPath(File classDir) {
        try {
            T2BUtils.addClassPath(classDir);
            t2bClassPath.add(classDir.getCanonicalPath());
        } catch (Exception exc) {
            err("failed to add classpath entry: " + classDir.getAbsolutePath() + ", reason: "
                    + exc.getLocalizedMessage());
        }
    }

    public static Multimap<ClassInfo, MethodInfo> buildFakeAnnotatedSet() {
        Multimap<ClassInfo, MethodInfo> result = new HashMultimap<>();
        Collection<ClassInfo> testClasses = myGeneratorSource.getClasses();
        log("Starting Test Classes Analysis: >>>>>>>>>>>>>>>>>>>>");
        for (ClassInfo classInfo : testClasses) {
            if (classInfo.isAbstract()) {
                continue;
            }

            T2BClassTransformer clsTransform = new T2BClassTransformer(classInfo);

            if (clsTransform.hasNonStaticFields()) {
                clsTransform.annotateClassState();
            }

            Collection<MethodInfo> mil = new ArrayList<>();
            for (MethodInfo methodInfo : clsTransform.getMethods()) {
                AnnotationCondition.MethodState testValid = isValidTest(methodInfo, BENCHMARK_ANNOTATIONS);
                if (testValid == AnnotationCondition.MethodState.VALID) {
                    clsTransform.annotateMethodTag(methodInfo, BENCHMARK_ANNOTATIONS);
                    mil.add(methodInfo);
                } else if (testValid != AnnotationCondition.MethodState.NOT_TEST) {
                    log(String.format("%-20.20s: %s", "Skipping Test Method",
                            methodInfo.getQualifiedName() + ", reason: " + testValid.name()));
                }
            }

            if (clsTransform.isClassAltered()) {
                try {
                    clsTransform.storeClass(BENCH_DIR);
                    clsTransform.toClass();
                } catch (Exception exc) {
                    err("failed to use altered class: " + clsTransform.getAlteredClassName() + ", reason: "
                            + exc.getLocalizedMessage());
                    exc.printStackTrace();
                }

                classInfo = clsTransform.getClassInfo();
                Collection<MethodInfo> amil = classInfo.getMethods();
                for (MethodInfo mi : mil) {
                    MethodInfo ami = getAlteredMethod(mi, amil);
                    if (ami != null) {
                        result.put(classInfo, ami);
                    }
                }
            } else {
                for (MethodInfo mi : mil) {
                    result.put(clsTransform.getClassInfo(), mi);
                }
            }
        }
        log("Completed Test Classes Analysis: <<<<<<<<<<<<<<<<<<<");

        return result;
    }

    private static MethodInfo getAlteredMethod(MethodInfo mi, Collection<MethodInfo> amil) {
        for (MethodInfo ami : amil) {
            if (ami.getName().equals(mi.getName())) {
                return ami;
            }
        }

        return null;
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
        return BenchmarkList.fromFile(BENCH_DIR + "/META-INF/BenchmarkList");
    }

    /*
     **************** Test2BenchmarkAgent
     *
     */

    public static CompilerHints getMyCompilerHints() {
        return CompilerHints.fromFile(BENCH_DIR + "/META-INF/CompilerHints");
    }

    static void log(String msg) {
        System.out.println(msg);
    }

    static void err(String msg) {
        System.out.println("ERROR: " + msg);
    }

    private void buildBenchmarks() throws Exception {
        generateBenchmarkList();
        CompileProcess.WindowsCompileProcess compileProcess = new CompileProcess.WindowsCompileProcess();
        compileProcess.compile();
        // String cp = compileProcess.getCompileClassPath();
        String cp = getT2BClassPath();
        writePropsToFile(BENCH_DIR, cp);
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
        myGeneratorSource = new MyGeneratorSource();
        gen.generate(myGeneratorSource, dst);
        gen.complete(myGeneratorSource, dst);

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
            err("failed to write benchmark run configuration properties, reason: " + exc.getLocalizedMessage());
            exc.printStackTrace();
        }
    }

    private static String escapePath(String path) {
        return path == null ? path : path.replace("\\", "/");
    }

    class MyGeneratorSource implements GeneratorSource {

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
                        benchmarkClassList.add(new MyClassInfo(clazz));
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
