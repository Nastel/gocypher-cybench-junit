package com.gocypher.cybench;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;

import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.generators.core.*;
import org.openjdk.jmh.generators.reflection.MyClassInfo;
import org.openjdk.jmh.runner.BenchmarkList;
import org.openjdk.jmh.runner.CompilerHints;
import org.openjdk.jmh.util.HashMultimap;
import org.openjdk.jmh.util.Multimap;

public class BenchmarkTest {

    static final String WORK_DIR_ARG = System.getProperty("buildDir");
    static final String TEST_DIR_ARG = System.getProperty("testDir");
    static final String BENCH_DIR_ARG = System.getProperty("benchDir");

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

    // The code to put into the JMH methods - call ME and then return MY replacements
    private static MyGeneratorSource myGeneratorSource;
    Collection<ClassInfo> benchmarkClassList;

    BenchmarkTest() {
        TEST_DIR = initTestDir();
        BENCH_DIR = initBenchDir();
    }

    private String initTestDir() {
        String testDirPath;
        if (TEST_DIR_ARG == null || TEST_DIR_ARG.isEmpty()) {
            // Maven layout
            File testDirMvn = new File(WORK_DIR_ARG + "/test-classes");
            if (testDirMvn.exists()) {
                testDirPath = testDirMvn.getAbsolutePath();
                addClassPath(new File(WORK_DIR_ARG + "/classes"));
            } else {
                // Gradle layout
                File testDirGrd = new File(WORK_DIR_ARG + "/classes/java/test");
                if (testDirMvn.exists()) {
                    testDirPath = testDirGrd.getAbsolutePath();
                    addClassPath(new File(WORK_DIR_ARG + "/classes/java/main"));
                } else {
                    // Use build dir
                    testDirPath = WORK_DIR_ARG;
                }
            }
        } else {
            testDirPath = TEST_DIR_ARG;
        }

        File testDir = new File(testDirPath);
        log("*** Setting Test Classes dir to use: " + testDir.getAbsolutePath());
        addClassPath(testDir);

        return testDirPath;
    }

    private String initBenchDir() {
        String benchDirPath;
        if (BENCH_DIR_ARG == null || BENCH_DIR_ARG.isEmpty()) {
            benchDirPath = TEST_DIR + "/../t2b";
        } else {
            benchDirPath = BENCH_DIR_ARG;
        }
        File benchDir = new File(benchDirPath);
        log("*** Setting Benchmarks dir to use: " + benchDir.getAbsolutePath());
        if (benchDir.exists()) {
            try {
                log("*** Removing existing benchmarks dir: " + benchDir.getCanonicalPath());
                // Files.walk(benchDir.toPath()).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
            } catch (Exception exc) {
                err("failed to delete benchmarks dir: " + exc.getLocalizedMessage());
            }
        }
        addClassPath(benchDir);

        return benchDirPath;
    }

    public static void main(String... args) throws Exception {
        log("Starting Test2Benchmark transformer app...");
        BenchmarkTest benchmarkTest = new BenchmarkTest();
        benchmarkTest.init();
    }

    private static void addClassPath(File classDir) {
        try {
            T2BUtils.addClassPath(classDir);
        } catch (Exception exc) {
            err("Failed to add classpath entry: " + classDir.getAbsolutePath() + ", exc: " + exc.getLocalizedMessage());
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

            boolean hasNonStaticFields = false;
            for (FieldInfo fieldInfo : T2BUtils.getAllFields(classInfo)) {
                if (!fieldInfo.isStatic()) {
                    hasNonStaticFields = true;
                    break;
                }
            }

            if (hasNonStaticFields) {
                Annotation stateAnnotation = classInfo.getAnnotation(State.class);
                if (stateAnnotation == null) {
                    String clsName = T2BUtils.getClassName(classInfo);
                    String statedClassName = T2BUtils.getStatedClassName(clsName);
                    Class<?> annotatedClass;
                    try {
                        annotatedClass = Class.forName(statedClassName);
                        classInfo = new MyClassInfo(annotatedClass);
                    } catch (Exception exc) {
                        annotatedClass = annotateClass(clsName);
                        if (annotatedClass != null) {
                            classInfo = new MyClassInfo(annotatedClass);
                        }
                    }
                }
            }

            for (MethodInfo methodInfo : classInfo.getMethods()) {
                AnnotationCondition.MethodState testValid = isValidTest(methodInfo, BENCHMARK_ANNOTATIONS);
                if (testValid == AnnotationCondition.MethodState.VALID) {
                    result.put(classInfo, methodInfo);
                } else if (testValid != AnnotationCondition.MethodState.NOT_TEST) {
                    log(String.format("%-20.20s: %s", "Skipping Test Method",
                            methodInfo.getQualifiedName() + ", reason: " + testValid.name()));
                }
            }
        }
        log("Completed Test Classes Analysis: <<<<<<<<<<<<<<<<<<<");

        return result;
    }

    public static Class<?> annotateClass(String clsName) {
        try {
            Class<?> annotatedClass = T2BUtils.addAnnotation(clsName, State.class.getName(), Scope.class.getName(),
                    Scope.Benchmark.name(), BENCH_DIR);
            Annotation stateAnnotation = annotatedClass.getAnnotation(State.class);
            if (stateAnnotation != null) {
                log(String.format("%-20.20s: %s", "Added",
                        "@State annotation for class " + clsName + " and named it " + annotatedClass.getName()));
                return annotatedClass;
            }
        } catch (Exception exc) {
            err("Failed to add @State annotation for " + clsName + ", reason " + exc.getLocalizedMessage());
            exc.printStackTrace();
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
     **************** BenchmarkTestAgent
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

    private void init() throws Exception {
        generateBenchmarkList();
        CompileProcess.WindowsCompileProcess compileProcess = new CompileProcess.WindowsCompileProcess();
        compileProcess.compile();
        String cp = compileProcess.getCompileClassPath();
        writePropsToFile(BENCH_DIR, cp);
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
                fos.write("BENCH_DIR=" + benchDir);
                fos.write('\n');
                fos.write("RUN_CLASS_PATH=" + classPath);
                fos.write('\n');
                fos.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    class MyGeneratorSource implements GeneratorSource {

        @Override
        public Collection<ClassInfo> getClasses() {
            if (benchmarkClassList != null) {
                return benchmarkClassList;
            }
            benchmarkClassList = new ArrayList<>();
            File testDir = new File(BenchmarkTest.TEST_DIR).getAbsoluteFile();
            String testDirPath = testDir.getPath();
            if (!testDirPath.endsWith(File.separator)) {
                testDirPath += File.separator;
            }

            if (!testDir.exists()) {
                BenchmarkTest.err("Test dir does not exist: " + testDir);
            } else {
                BenchmarkTest.log("Starting Test Classes Search: >>>>>>>>>>>>>>>>>>>>>>");
                Collection<File> includeClassFiles = T2BUtils.getUTClasses(testDir);
                for (File classFile : includeClassFiles) {
                    Class<?> clazz = null;
                    try {
                        String path = classFile.getAbsolutePath();
                        int index = path.indexOf(testDirPath);
                        String className = path.replace(File.separator, ".") //
                                .substring(index + testDirPath.length(), path.length() - ".class".length());
                        // TODO far from bulletproof

                        clazz = Class.forName(className);
                        BenchmarkTest.log("Found Test Class: " + clazz);
                    } catch (Throwable t) {
                        BenchmarkTest.err("Can't get test class: " + t);
                    }
                    benchmarkClassList.add(new MyClassInfo(clazz));
                }
                BenchmarkTest.log("Completed Test Classes Search: <<<<<<<<<<<<<<<<<<<<<");
            }
            return benchmarkClassList;
        }

        @Override
        public ClassInfo resolveClass(String className) {
            return null;
        }
    }

}
