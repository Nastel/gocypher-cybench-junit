package com.gocypher.cybench.t2b.aop.benchmark;

import org.aspectj.lang.ProceedingJoinPoint;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import com.gocypher.cybench.t2b.aop.TestAspects;
import com.gocypher.cybench.t2b.aop.benchmark.wrapper.AbstractBenchmarkRunnerWrapper;

@State(Scope.Benchmark)
public class TestBenchmarkTemplate {
    private ProceedingJoinPoint testPoint;

    @Benchmark
    public void testBenchmark(Blackhole b) throws Throwable {
        if (testPoint != null) {
            testPoint.proceed();
        }
    }

    @Setup(Level.Trial)
    public void setupTrial() {
        testPoint = AbstractBenchmarkRunnerWrapper.getTestPoint();
        TestAspects.log(testPoint);
    }

    @Setup(Level.Iteration)
    public void setupIteration() {
    }

    @Setup(Level.Invocation)
    public void setupInvocation() {
    }

    @TearDown(Level.Trial)
    public void teardownTrial() {
        testPoint = null;
    }

    @TearDown(Level.Iteration)
    public void teardownIteration() {
    }

    @TearDown(Level.Invocation)
    public void teardownInvocation() {
    }
}
