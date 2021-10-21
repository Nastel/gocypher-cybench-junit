package com.gocypher.cybench.t2b.aop;

import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.weaver.loadtime.Agent;
import org.openjdk.jmh.generators.core.ClassInfo;
import org.openjdk.jmh.generators.reflection.T2BClassInfo;

import com.gocypher.cybench.Test2Benchmark;
import com.gocypher.cybench.t2b.aop.benchmark.T2BTestBenchmark;
import com.gocypher.cybench.t2b.aop.benchmark.runner.BenchmarkRunnerWrapper;
import com.gocypher.cybench.t2b.transform.BenchmarkClassTransformer;

public class DefaultTestJoinPointHandler implements TestJoinPointHandler {

    private final BenchmarkRunnerWrapper benchmarkRunner = AOPConfigHandler.getBenchmarkRunner();

    public void runTestAsBenchmark(Method testMethod, ProceedingJoinPoint testPoint) {
        Thread testRunnerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                alterBenchmarkClass(testMethod);

                try {
                    benchmarkRunner.run(testPoint);
                } catch (Throwable exc) {
                    Test2Benchmark.err("benchmark run failed", exc);
                } finally {
                    benchmarkRunner.cleanup();
                }
            }
        });
        testRunnerThread.start();
        try {
            testRunnerThread.join();
        } catch (Exception exc) {
        }
    }

    protected static void alterBenchmarkClass(Method testMethod) {
        try {
            ClassInfo bClsInfo = new T2BClassInfo(T2BTestBenchmark.class);
            BenchmarkClassTransformer clsTransform = new BenchmarkClassTransformer(bClsInfo);
            clsTransform.doTransform(testMethod);
            byte[] clsBytes = clsTransform.getClassBytes();

            Instrumentation instrumentation = Agent.getInstrumentation();
            ClassDefinition clsDefinition = new ClassDefinition(T2BTestBenchmark.class, clsBytes);
            instrumentation.redefineClasses(clsDefinition);
        } catch (Exception exc) {
            Test2Benchmark.err("failed to redefine benchmark class", exc);
        }
    }
}
