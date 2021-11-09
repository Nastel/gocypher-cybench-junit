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

package com.gocypher.cybench.t2b.aop;

import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.weaver.loadtime.Agent;
import org.openjdk.jmh.generators.core.ClassInfo;
import org.openjdk.jmh.generators.reflection.T2BClassInfo;
import org.slf4j.Logger;

import com.gocypher.cybench.T2BUtils;
import com.gocypher.cybench.t2b.aop.benchmark.T2BTestBenchmark;
import com.gocypher.cybench.t2b.aop.benchmark.runner.BenchmarkRunnerWrapper;
import com.gocypher.cybench.t2b.transform.BenchmarkClassTransformer;

public class DefaultTestJoinPointHandler implements TestJoinPointHandler {
    private static Logger LOGGER = T2BUtils.getLogger(DefaultTestJoinPointHandler.class);

    private final BenchmarkRunnerWrapper benchmarkRunner = AOPConfigHandler.getBenchmarkRunner();

    @Override
    public void runTestAsBenchmark(Method testMethod, ProceedingJoinPoint testPoint) {
        Thread testRunnerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                alterBenchmarkClass(testMethod);

                try {
                    benchmarkRunner.run(testPoint);
                } catch (Throwable exc) {
                    LOGGER.error("Benchmark run failed, reason: {}", exc);
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
            LOGGER.error("Failed to redefine benchmark class, reason: {}", exc.getLocalizedMessage());
        }
    }
}
