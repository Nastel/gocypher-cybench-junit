package com.gocypher.cybench;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.generators.core.*;
import org.openjdk.jmh.generators.reflection.MyClassInfo;
import org.openjdk.jmh.runner.BenchmarkList;
import org.openjdk.jmh.runner.CompilerHints;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.util.HashMultimap;
import org.openjdk.jmh.util.Multimap;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ClassFile;
import javassist.bytecode.ConstPool;
import javassist.bytecode.annotation.EnumMemberValue;

public class BenchmarkTest {

    static final String WORK_DIR_ARG = System.getProperty("buildDir");
    static final String TEST_DIR_ARG = System.getProperty("testDir");
    static final String BENCH_DIR_ARG = System.getProperty("benchDir");
    static String TEST_DIR;
    static {
        if (TEST_DIR_ARG == null || TEST_DIR_ARG.isEmpty()) {
            // Maven layout
            File testDirMvn = new File(WORK_DIR_ARG + "/test-classes");
            if (testDirMvn.exists()) {
                TEST_DIR = testDirMvn.getAbsolutePath();
            } else {
                // Gradle layout
                File testDirGrd = new File(WORK_DIR_ARG + "/classes/java/test");
                if (testDirMvn.exists()) {
                    TEST_DIR = testDirGrd.getAbsolutePath();
                } else {
                    // Use build dir
                    TEST_DIR = WORK_DIR_ARG;
                }
            }
        } else {
            TEST_DIR = TEST_DIR_ARG;
        }

        log("*** Setting Test Classes dir to use: " + new File(TEST_DIR).getAbsolutePath());
    }
    static String BENCH_DIR;
    static {
        if (BENCH_DIR_ARG == null || BENCH_DIR_ARG.isEmpty()) {
            BENCH_DIR = TEST_DIR + "/../t2b";
        } else {
            BENCH_DIR = BENCH_DIR_ARG;
        }
        File benchDir = new File(BENCH_DIR);
        log("*** Setting Benchmarks dir to use: " + benchDir.getAbsolutePath());
        if (benchDir.exists()) {
            try {
                log("*** Removing existing benchmarks dir: " + benchDir.getCanonicalPath());
                Files.walk(benchDir.toPath()).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
            } catch (Exception exc) {
                err("failed to delete benchmarks dir: " + exc.getLocalizedMessage());
            }
        }
    }

    static final String NEW_CLASS_NAME_SUFIX = "_JMH_State";
    static final String FORKED_PROCESS_MARKER = "jmh.forked";
    static final String MY_BENCHMARK_LIST = BENCH_DIR + "/META-INF/BenchmarkList";
    static final String MY_COMPILER_HINTS = BENCH_DIR + "/META-INF/CompilerHints";

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
        log("Starting Test2Benchmark transformer app...");
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
        Collection<ClassInfo> testClasses = myGeneratorSource.getClasses();
        log("Starting Test Classes Analysis: >>>>>>>>>>>>>>>>>>>>");
        for (ClassInfo classInfo : testClasses) {
            if (classInfo.isAbstract()) {
                continue;
            }

            boolean hasNonStaticFields = false;
            for (FieldInfo fieldInfo : getAllFields(classInfo)) {
                if (!fieldInfo.isStatic()) {
                    hasNonStaticFields = true;
                    break;
                }
            }

            if (hasNonStaticFields) {
                Annotation stateAnnotation = classInfo.getAnnotation(State.class);
                if (stateAnnotation == null) {
                    String clsName = getClassName(classInfo);
                    String statedClassName = getStatedClassName(clsName);
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

    public static Collection<FieldInfo> getAllFields(ClassInfo ci) {
        Collection<FieldInfo> ls = new ArrayList<>();
        do {
            ls.addAll(ci.getFields());
        } while ((ci = ci.getSuperClass()) != null);
        return ls;
    }

    public static String getClassName(ClassInfo classInfo) {
        try {
            Field f = classInfo.getClass().getSuperclass().getDeclaredField("klass");
            f.setAccessible(true);
            Class<?> cls = (Class<?>) f.get(classInfo);
            return cls.getName();
        } catch (Throwable exc) {
            return classInfo.getQualifiedName();
        }
    }

    public static String getStatedClassName(String className) {
        if (className.contains("$")) {
            String[] cnt = className.split("\\$");
            cnt[0] = cnt[0] + NEW_CLASS_NAME_SUFIX;
            return String.join("$", cnt);
        } else {
            return className + NEW_CLASS_NAME_SUFIX;
        }
    }

    public static Class<?> annotateClass(String clsName) {
        try {
            Class<?> annotatedClass = addAnnotation(clsName, State.class.getName(), Scope.class.getName(),
                    Scope.Benchmark.name());
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

    private static Class<?> addAnnotation(String className, String annotationName, String typeName, String valueName)
            throws Exception {
        ClassPool pool = ClassPool.getDefault();
        CtClass ctClass = pool.getAndRename(className, getStatedClassName(className));

        ClassFile classFile = ctClass.getClassFile();
        ConstPool constpool = classFile.getConstPool();

        AnnotationsAttribute annotationsAttribute = new AnnotationsAttribute(constpool,
                AnnotationsAttribute.visibleTag);
        javassist.bytecode.annotation.Annotation annotation = new javassist.bytecode.annotation.Annotation(
                annotationName, constpool);
        EnumMemberValue emv = new EnumMemberValue(constpool);
        emv.setType(typeName);
        emv.setValue(valueName);
        annotation.addMemberValue("value", emv);
        annotationsAttribute.setAnnotation(annotation);

        classFile.addAttribute(annotationsAttribute);
        ctClass.writeFile(new File(BENCH_DIR).getCanonicalPath());
        return ctClass.toClass();
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
        System.out.println("ERROR: " + msg);
    }

    private void init() throws Exception {
        // http://javadox.com/org.openjdk.jmh/jmh-core/1.32/org/openjdk/jmh/runner/options/OptionsBuilder.html
        Options opt = new OptionsBuilder() //
                .include(".*") //
                .jvmArgsPrepend("-D" + FORKED_PROCESS_MARKER + "=true") //
                .warmupIterations(NUMBER_OF_WARMUPS) //
                .mode(BENCHMARK_MODE) //
                .forks(NUMBER_OF_FORKS) //
                .measurementIterations(NUMBER_OF_MEASUREMENTS) //
                .build();
        generateBenchmarkList();
        new com.gocypher.cybench.CompileProcess.WindowsCompileProcess();
        new Runner(opt).run();
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
                Collection<File> includeClassFiles = BenchmarkTest.getUTClasses(testDir);
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
