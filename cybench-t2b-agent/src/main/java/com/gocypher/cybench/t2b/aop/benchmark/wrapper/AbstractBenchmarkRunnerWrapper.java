package com.gocypher.cybench.t2b.aop.benchmark.wrapper;

import java.util.concurrent.atomic.AtomicReference;

import org.aspectj.lang.ProceedingJoinPoint;

public abstract class AbstractBenchmarkRunnerWrapper implements BenchmarkRunnerWrapper {

    private static final AtomicReference<ProceedingJoinPoint> testPoint = new AtomicReference<>();
    protected final String[] args;

    public AbstractBenchmarkRunnerWrapper(String args) {
        this.args = args == null ? new String[0] : args.split("\\s");
    }

    public static ProceedingJoinPoint getTestPoint() {
        return testPoint.get();
    }

    protected static void setTestPoint(ProceedingJoinPoint testPoint) {
        AbstractBenchmarkRunnerWrapper.testPoint.set(testPoint);
    }

    protected void cleanContext() {
        System.runFinalization();
        System.gc();
        System.runFinalization();
        System.gc();
    }

    @Override
    public void cleanup() {
    }
}
