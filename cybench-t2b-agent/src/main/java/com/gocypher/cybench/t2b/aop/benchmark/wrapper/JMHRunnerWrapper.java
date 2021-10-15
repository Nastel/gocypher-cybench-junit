package com.gocypher.cybench.t2b.aop.benchmark.wrapper;

import org.aspectj.lang.ProceedingJoinPoint;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.CommandLineOptions;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import com.gocypher.cybench.Test2Benchmark;

public class JMHRunnerWrapper extends AbstractBenchmarkRunnerWrapper {

    public JMHRunnerWrapper(String args) {
        super(args);
    }

    @Override
    public void run(ProceedingJoinPoint testPoint) throws Throwable {
        setTestPoint(testPoint);
        cleanContext();

        Test2Benchmark.log("Starting JMH Runner...");
        OptionsBuilder options = new OptionsBuilder();
        options.forks(0);
        options.warmupForks(0);

        if (args != null) {
            CommandLineOptions cliOptions = new CommandLineOptions(args);
            options.parent(cliOptions);
        }

        options.build();

        try {
            Runner jmhRunner = new Runner(options);
            jmhRunner.run();
        } finally {
            Test2Benchmark.log("JMH Runner completed...");
        }
    }
}
