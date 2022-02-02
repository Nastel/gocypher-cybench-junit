/*
 * Copyright (C) 2020-2021, K2N.IO.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 *
 */

package com.gocypher.cybench;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.gocypher.cybench.t2b.utils.LogUtils;

import javassist.ByteArrayClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;

public class Test2BenchmarkAgent {
    static {
        initSession();
    }

    private static Logger LOGGER = LogUtils.getLogger(Test2BenchmarkAgent.class);

    static Instrumentation instrumentation;

    private static String PLUG_T2B_ANNOTATIONS = "Object value=com.gocypher.cybench.Test2Benchmark.buildT2BAnnotatedSet();return value;";
    private static String PLUG_T2B_BENCHMARK_LIST = "Object value=com.gocypher.cybench.Test2Benchmark.getBenchmarkList();return value;";
    private static String PLUG_T2B_COMPILER_HINTS = "Object value=com.gocypher.cybench.Test2Benchmark.getCompilerHints();return value;";

    private static String BENCHMARK_GENERATOR_CLASS = "org.openjdk.jmh.generators.core.BenchmarkGenerator";
    private static String BENCHMARK_LIST_CLASS = "org.openjdk.jmh.runner.BenchmarkList";
    private static String COMPILER_HINTS_CLASS = "org.openjdk.jmh.runner.CompilerHints";

    private static byte[] origBenchmarkListBytes;
    private static byte[] origCompilerHintsBytes;
    private static byte[] origBenchmarkGeneratorBytes;

    private static void initSession() {
        synchronized (System.getProperties()) {
            if (System.getProperty("t2b.session.id") == null) {
                System.setProperty("t2b.session.id", UUID.randomUUID().toString());
            }
        }
    }

    public static void premain(String agentArgs, Instrumentation inst) {
        // Handle duplicate agents
        if (instrumentation != null) {
            return;
        }

        instrumentation = inst;

        LOGGER.info("Test2Benchmark Agent Premain called...");

        boolean translateAgent = isTranslateAgent(agentArgs);

        if (translateAgent) {
            translateAgent();
        } else {
            org.aspectj.weaver.loadtime.Agent.premain(agentArgs, inst);
        }
    }

    private static boolean isTranslateAgent(String agentArgs) {
        if (StringUtils.isNotEmpty(agentArgs)) {
            String[] args = agentArgs.split(";");
            for (String arg : args) {
                String[] argPair = arg.split(":");
                if (argPair[0].equalsIgnoreCase("mode")) {
                    return argPair[1].equalsIgnoreCase("translate");
                }
            }
        }

        return false;
    }

    private static void translateAgent() {
        try {
            Class<?> klass = org.openjdk.jmh.annotations.Benchmark.class;
            URL location = klass.getResource('/' + klass.getName().replace('.', '/') + ".class");
            // jar:file:/C:/Users/slabs/.m2/repository/org/openjdk/jmh/jmh-core/1.34/jmh-core-1.34.jar!/org/openjdk/jmh/annotations/Benchmark.class
            String[] split = location.toString().replaceFirst("jar:file:/", "").split("!");
            LOGGER.info("JMH: {}", split[0]);
            File file = Paths.get(split[0]).toFile();
            LOGGER.info("JMH: {}", file.getAbsolutePath());

            JarFile jarFile = new JarFile(file);

            origBenchmarkListBytes = getBytes(jarFile, BENCHMARK_LIST_CLASS);
            origCompilerHintsBytes = getBytes(jarFile, COMPILER_HINTS_CLASS);
            origBenchmarkGeneratorBytes = getBytes(jarFile, BENCHMARK_GENERATOR_CLASS);
            // Put in code to basically call MY replacement methods and return MY values - instead of JMH
            replaceCode(BENCHMARK_GENERATOR_CLASS, "buildAnnotatedSet", PLUG_T2B_ANNOTATIONS,
                    origBenchmarkGeneratorBytes);
            replaceCode(BENCHMARK_LIST_CLASS, "defaultList", PLUG_T2B_BENCHMARK_LIST, origBenchmarkListBytes);
            replaceCode(COMPILER_HINTS_CLASS, "defaultList", PLUG_T2B_COMPILER_HINTS, origCompilerHintsBytes);
        } catch (Exception e) {
            LOGGER.error("Failed to initialize T2B agent", e);
        }
    }

    public static void agentmain(String options, Instrumentation instrumentation) {
        premain(options, instrumentation);
    }

    private static byte[] getBytes(JarFile jarFile, String className) throws IOException {
        return getJMHBytesForClass(jarFile, className.replace('.', '/') + ".class");
    }

    static void replaceCode(String className, String methodName, String plugCode, byte[] classBytes) {
        try {
            ClassPool pool = ClassPool.getDefault(); // Edited to simplify
            pool.insertClassPath(new ByteArrayClassPath(className, classBytes));
            CtClass ctClass = pool.get(className);
            CtMethod[] methods = ctClass.getDeclaredMethods(methodName);
            methods[0].insertBefore(plugCode);
            ClassDefinition clsDefinition = new ClassDefinition(Class.forName(className), ctClass.toBytecode());
            instrumentation.redefineClasses(clsDefinition);
            LOGGER.info("Modified method: {}.{}", className, methodName);
        } catch (Throwable t) {
            LOGGER.warn("Could not modify class method: {}.{}, exc: {}", className, methodName,
                    t.getLocalizedMessage());
        }
    }

    static byte[] getJMHBytesForClass(JarFile jarFile, String className) throws IOException {
        JarEntry entry = jarFile.getJarEntry(className);
        return streamToByteArray(jarFile.getInputStream(entry));
    }

    static void restoreJMHCode() {
        restore(COMPILER_HINTS_CLASS, origCompilerHintsBytes);
        restore(BENCHMARK_LIST_CLASS, origBenchmarkListBytes);
        restore(BENCHMARK_GENERATOR_CLASS, origBenchmarkGeneratorBytes);
    }

    static void restore(String className, byte[] classBytes) {
        if (instrumentation == null || classBytes == null) {
            return;
        }

        try {
            ClassDefinition clsDefinition = new ClassDefinition(Class.forName(className), classBytes);
            instrumentation.redefineClasses(clsDefinition);
            LOGGER.info("Restored class: {}", className);
        } catch (Throwable t) {
            LOGGER.warn("Could not restore class: {}, exc: {}", className, t.getLocalizedMessage());
        }
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

    public static Instrumentation getInstrumentation() {
        if (instrumentation == null) {
            throw new UnsupportedOperationException("CyBench T2B agent was neither started via '-javaagent' (preMain) "
                    + "nor attached via 'VirtualMachine.loadAgent' (agentMain)");
        }
        return instrumentation;
    }
}
