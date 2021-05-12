

import javassist.ByteArrayClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import org.openjdk.jmh.annotations.Mode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.openjdk.jmh.runner.CompilerHints;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.generators.core.BenchmarkGenerator;
import org.openjdk.jmh.generators.core.FileSystemDestination;
import org.openjdk.jmh.runner.BenchmarkList;
import org.openjdk.jmh.generators.core.GeneratorSource;
import org.openjdk.jmh.generators.core.ClassInfo;
import org.openjdk.jmh.util.HashMultimap;
import org.openjdk.jmh.util.Multimap;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.instrument.ClassDefinition;

import org.openjdk.jmh.generators.core.MethodInfo;
import org.openjdk.jmh.generators.reflection.MyClassInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.io.File;
import java.io.InputStream;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;


public class BenchmarkTest {

    private static final String WORK_DIR = "prod";
    private static final String TEST_DIR = "build" +File.separator+ "classes" +File.separator+ "java" +File.separator+ "test";
    private static final String FORKED_PROCESS_MARKER = "jmh.forked";
    private static final String COMPILE_SCRIPT = "./compileGenerated.bat";
    private static final String JMH_CORE_JAR = "/lib/jmh-core-1.28.jar";
    private static final String MY_BENCHMARK_LIST = WORK_DIR + "/META-INF/BenchmarkList";
    private static final String MY_COMPILER_HINTS = WORK_DIR + "/META-INF/CompilerHints";

    private static final int NUMBER_OF_FORKS = 1;
    private static final int NUMBER_OF_WARMUPS = 0;
    private static final int NUMBER_OF_MEASUREMENTS = 1;
    private static final Mode BENCHMARK_MODE = Mode.All;
    private static final Class BENCHMARK_ANNOTATION = Test.class;
    private static final Class BENCHMARK_ANNOTATION2 = org.junit.Test.class;

    private static String BENCHMARK_GENERATOR_CLASS = "org.openjdk.jmh.generators.core.BenchmarkGenerator";
    private static String BENCHMARK_LIST_CLASS = "org.openjdk.jmh.runner.BenchmarkList";
    private static String COMPILER_HINTS_CLASS = "org.openjdk.jmh.runner.CompilerHints";

    // The code to put into the JMH methods - call ME and then return MY replacements
    private static String TAKE_FAKE_ANNOTATIONS = "Object value=BenchmarkTest.buildFakeAnnotatedSet();return value;";
    private static String TAKE_FAKE_BENCHMARK_LIST = "Object value=BenchmarkTest.getMyBenchmarkList();return value;";
    private static String TAKE_FAKE_COMPILER_HINTS = "Object value=BenchmarkTest.getMyCompilerHints();return value;";


    public static void main(String[] args) throws Exception {
        BenchmarkTest benchmarkTest = new BenchmarkTest();
        benchmarkTest.init();
    }

    private void init() throws Exception {
        // http://javadox.com/org.openjdk.jmh/jmh-core/1.12/org/openjdk/jmh/runner/options/OptionsBuilder.html
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
        runProcess(COMPILE_SCRIPT);
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
            Collection<File> includeClassFiles = getUTClasses(new File(TEST_DIR));
            for (File classFile : includeClassFiles) {
                Class clazz = null;
                try {
                    String path = classFile.getAbsolutePath();
                    int index = path.indexOf(TEST_DIR);
                    String className = path.replace(File.separator, ".").substring(index + TEST_DIR.length() +1, path.length() - ".class".length());
                    clazz = Class.forName(className);
                    log("Class: " + clazz);
                } catch (Throwable t) {
                    log("ERROR: Cant get class: " + t);
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
        Set<File> fileTree = new HashSet<File>();
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

    private void printLines(String cmd, InputStream ins) throws Exception {
        String line = null;
        BufferedReader in = new BufferedReader(
                new InputStreamReader(ins));
        while ((line = in.readLine()) != null) {
            log(cmd + " " + line);
        }
    }

    private void runProcess(String command) throws Exception {
        Process pro = Runtime.getRuntime().exec(command);
        printLines(command + " stdout:", pro.getInputStream());
        printLines(command + " stderr:", pro.getErrorStream());
        pro.waitFor();
        log(command + " exitValue() " + pro.exitValue());
    }

    /*
     **************** BenchmarkTestAgent
     *
     */


    private static MyGeneratorSource myGeneratorSource;

    public static Multimap<ClassInfo, MethodInfo> buildFakeAnnotatedSet() {
        Multimap<ClassInfo, MethodInfo> result = new HashMultimap<>();
        for (ClassInfo currentClass : myGeneratorSource.getClasses()) {
            if (currentClass.isAbstract()) continue;
            for (MethodInfo mi : currentClass.getMethods()) {
                Object ann = mi.getAnnotation(BENCHMARK_ANNOTATION);
                Object ann2 = mi.getAnnotation(BENCHMARK_ANNOTATION2);
                if (ann != null || ann2 != null) {
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
                if (System.getProperty(FORKED_PROCESS_MARKER) != null) {
                    return;
                }
                instrumentation = inst;
                log("Agent Premain called...");
                JarFile jarFile = new JarFile(WORK_DIR + BenchmarkTest.JMH_CORE_JAR);
                byte[] benchmarkListBytes = getJMHBytesForClass(jarFile, BENCHMARK_LIST_CLASS.replace(".", "/") + ".class");
                byte[] compilerHintsBytes = getJMHBytesForClass(jarFile, COMPILER_HINTS_CLASS.replace(".", "/") + ".class");
                byte[] benchmarkGeneratorBytes = getJMHBytesForClass(jarFile, BENCHMARK_GENERATOR_CLASS.replace(".", "/") + ".class");
                // Put in code to basically call MY replacement methods and return MY values - instead of JMH
                replaceCode(BENCHMARK_GENERATOR_CLASS, "buildAnnotatedSet", TAKE_FAKE_ANNOTATIONS, benchmarkGeneratorBytes);
                replaceCode(BENCHMARK_LIST_CLASS, "defaultList", TAKE_FAKE_BENCHMARK_LIST, benchmarkListBytes);
                replaceCode(COMPILER_HINTS_CLASS, "defaultList", TAKE_FAKE_COMPILER_HINTS, compilerHintsBytes);
            } catch (Exception e) {
                e.printStackTrace();
                log("Error: " + e);
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
                log("Modified " + className + "." + methodName);
            } catch (Throwable t) {
                log("Could not create the class creation: " + t);
            }
        }

        static byte[] getJMHBytesForClass(JarFile jarFile, String className) throws IOException {
            JarEntry entry = jarFile.getJarEntry(className);
            return streamToByteArray(jarFile.getInputStream(entry));
        }

        static byte[] streamToByteArray(InputStream stream) throws IOException {
            byte[] buffer = new byte[1024];
            ByteArrayOutputStream os = new ByteArrayOutputStream();

            int line = 0;
            // read bytes from stream, and store them in buffer
            while ((line = stream.read(buffer)) != -1) {
                // Writes bytes from byte array (buffer) into output stream.
                os.write(buffer, 0, line);
            }
            stream.close();
            os.flush();
            os.close();
            return os.toByteArray();
        }
    }

    static private void log(String msg)
    {
        System.out.println(msg);
    }

}
