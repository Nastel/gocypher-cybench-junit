package com.gocypher.cybench.t2b.aop.benchmark.wrapper;

import org.aspectj.lang.ProceedingJoinPoint;

import com.gocypher.cybench.Test2Benchmark;
import com.gocypher.cybench.launcher.BenchmarkRunner;

public class CybenchRunnerWrapper extends AbstractBenchmarkRunnerWrapper {

    public CybenchRunnerWrapper(String args) {
        super(args);
    }

    @Override
    public void run(ProceedingJoinPoint testPoint) throws Exception {
        setTestPoint(testPoint);
        cleanContext();

        Test2Benchmark.log("Starting Cybench Runner...");
        try {
            BenchmarkRunner.main(args);
        } finally {
            Test2Benchmark.log("Cybench Runner completed...");
        }
    }
}
