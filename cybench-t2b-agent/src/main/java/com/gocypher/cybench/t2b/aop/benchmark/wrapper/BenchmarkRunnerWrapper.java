package com.gocypher.cybench.t2b.aop.benchmark.wrapper;

import org.aspectj.lang.ProceedingJoinPoint;

public interface BenchmarkRunnerWrapper {
    void run(ProceedingJoinPoint testPoint) throws Throwable;

    void cleanup();
}
