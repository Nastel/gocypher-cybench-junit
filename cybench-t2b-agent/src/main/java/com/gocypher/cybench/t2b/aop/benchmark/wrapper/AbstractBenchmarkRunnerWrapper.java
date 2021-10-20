package com.gocypher.cybench.t2b.aop.benchmark.wrapper;

import java.util.concurrent.atomic.AtomicReference;

import org.aspectj.lang.ProceedingJoinPoint;

public abstract class AbstractBenchmarkRunnerWrapper implements BenchmarkRunnerWrapper {

    private static final String[] EMPTY_ARGS = new String[0];

    private static final AtomicReference<ProceedingJoinPoint> testPoint = new AtomicReference<>();
    protected final String[] args;

    public AbstractBenchmarkRunnerWrapper(String args) {
        this.args = args == null ? EMPTY_ARGS : args.split("\\s");
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
