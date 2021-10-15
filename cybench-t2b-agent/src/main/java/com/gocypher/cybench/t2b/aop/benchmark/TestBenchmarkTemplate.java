package com.gocypher.cybench.t2b.aop.benchmark;

import org.aspectj.lang.ProceedingJoinPoint;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import com.gocypher.cybench.core.annotation.BenchmarkMetaData;
import com.gocypher.cybench.core.annotation.BenchmarkTag;
import com.gocypher.cybench.t2b.aop.TestAspects;
import com.gocypher.cybench.t2b.aop.benchmark.wrapper.AbstractBenchmarkRunnerWrapper;

@State(Scope.Benchmark)
@BenchmarkMetaData(key = "c", value = "t")
public class TestBenchmarkTemplate {
    private ProceedingJoinPoint testPoint;

    @Benchmark
    @BenchmarkMetaData(key = "a", value = "b")
    @BenchmarkTag(tag = "ddddddd")
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
