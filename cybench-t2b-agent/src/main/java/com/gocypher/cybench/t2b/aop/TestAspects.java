package com.gocypher.cybench.t2b.aop;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Properties;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.aspectj.lang.reflect.SourceLocation;

import com.gocypher.cybench.T2BMapper;
import com.gocypher.cybench.Test2Benchmark;
import com.gocypher.cybench.t2b.aop.benchmark.wrapper.BenchmarkRunnerWrapper;

public class TestAspects {

    private static final String SYS_PROP_AOP_CONFIG = "t2b.aop.cfg.path"; // TODO: change name
    private static final String DEFAULT_AOP_CONFIG_PATH = "config/t2b.properties";
    private static String configPath = System.getProperty(SYS_PROP_AOP_CONFIG, DEFAULT_AOP_CONFIG_PATH);

    private static BenchmarkRunnerWrapper benchmarkRunner;

    static {
        loadConfig(configPath);
    }

    protected static void loadConfig(String cfgPath) {
        Properties aopCfgProps = new Properties();
        if (new File(cfgPath).exists()) {
            try (Reader rdr = new BufferedReader(new FileReader(cfgPath))) {
                aopCfgProps.load(rdr);
            } catch (IOException exc) {
                Test2Benchmark.err("failed to load aop config from: " + cfgPath, exc);
            }
        } else {
            String cfgProp = System.getProperty(SYS_PROP_AOP_CONFIG);
            if (cfgProp != null) {
                Test2Benchmark.warn("system property " + SYS_PROP_AOP_CONFIG + " defined aop configuration file "
                        + cfgPath + " not found!");
            } else {
                Test2Benchmark.log("default metadata configuration file " + cfgPath + " not found!");
            }
        }

        String bwClassName = aopCfgProps.getProperty("t2b.benchmark.runner.wrapper",
                "com.gocypher.cybench.t2b.aop.benchmark.wrapper.CybenchRunnerWrapper");
        String bwArgs = aopCfgProps.getProperty("t2b.benchmark.runner.wrapper.args",
                "cfg=config/cybench-launcher.properties");
        try {
            @SuppressWarnings("unchecked")
            Class<? extends BenchmarkRunnerWrapper> bwClass = (Class<? extends BenchmarkRunnerWrapper>) Class
                    .forName(bwClassName);
            Constructor<? extends BenchmarkRunnerWrapper> bwConstructor = bwClass.getConstructor(String.class);
            benchmarkRunner = bwConstructor.newInstance(bwArgs);
        } catch (Exception exc) {
            Test2Benchmark.err("failed to load benchmark runner wrapper", exc);
        }
    }

    public abstract static class AbstractT2BAspect {

        final T2BMapper testMapper;

        public AbstractT2BAspect(T2BMapper t2BMapper) {
            Test2Benchmark.log("Initiating aspect " + getClass().getSimpleName() + "...");

            testMapper = t2BMapper;
        }

        // @Pointcut("execution(@org.junit.jupiter.api.Test * *(..))")
        // public void transactionalMethod() {
        // }

        public void around(ProceedingJoinPoint testPoint) {
            Test2Benchmark
                    .log("test.around.before, class: " + testPoint.getSignature().getDeclaringType().getSimpleName()
                            + ", method: " + testPoint.getSignature().getName());

            MethodSignature signature = (MethodSignature) testPoint.getSignature();
            Method method = signature.getMethod();

            T2BMapper.MethodState state = testMapper.isValid(method);

            if (state == T2BMapper.MethodState.VALID) {
                Thread testRunnerThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
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
            } else {
                Test2Benchmark.warn("Skipping test: " + testPoint.getSignature().getName());
            }

            Test2Benchmark
                    .log("test.around.after, class: " + testPoint.getSignature().getDeclaringType().getSimpleName());
        }
    }

    @Aspect
    public static class JU4TestAspect extends AbstractT2BAspect {
        private static final String TEST_ANNOTATION_CLASS = "org.junit.Test";

        public JU4TestAspect() {
            super(Test2Benchmark.JUNIT4_MAPPER);
        }

        @Override
        @Around("@annotation(" + TEST_ANNOTATION_CLASS + ")")
        public void around(ProceedingJoinPoint testPoint) {
            super.around(testPoint);
        }
    }

    @Aspect
    public static class JU5TestAspect extends AbstractT2BAspect {
        private static final String TEST_ANNOTATION_CLASS = "org.junit.jupiter.api.Test";

        public JU5TestAspect() {
            super(Test2Benchmark.JUNIT5_MAPPER);
        }

        @Override
        @Around("@annotation(" + TEST_ANNOTATION_CLASS + ")")
        public void around(ProceedingJoinPoint testPoint) {
            super.around(testPoint);
        }
    }

    @Aspect
    public static class NGTestAspect extends AbstractT2BAspect {
        private static final String TEST_ANNOTATION_CLASS = "org.testng.annotations.Test";

        public NGTestAspect() {
            super(Test2Benchmark.TESTNG_MAPPER);
        }

        @Override
        @Around("@annotation(" + TEST_ANNOTATION_CLASS + ")")
        public void around(ProceedingJoinPoint testPoint) {
            super.around(testPoint);
        }
    }

    public static void log(JoinPoint joinPoint) {
        if (joinPoint == null) {
            Test2Benchmark.log("JointPoint: {null}");
            return;
        }
        Signature sig = joinPoint.getSignature();
        String kind = joinPoint.getKind();
        Object[] args = joinPoint.getArgs();
        SourceLocation sl = joinPoint.getSourceLocation();
        Object target = joinPoint.getTarget();
        JoinPoint.StaticPart sp = joinPoint.getStaticPart();
        Object tis = joinPoint.getThis();
        Test2Benchmark.log("JointPoint: {" //
                + "\n\t signature=" + sig //
                + "\n\t      kind=" + kind //
                + "\n\t      args=" + Arrays.toString(args) //
                + "\n\t    source=" + sl //
                + "\n\t    target=" + target //
                + "\n\t    static=" + sp //
                + "\n\t      this=" + tis //
                + "\n}");
    }
}
