package com.gocypher.cybench.t2b.aop;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.aspectj.lang.reflect.SourceLocation;

import com.gocypher.cybench.T2BMapper;
import com.gocypher.cybench.Test2Benchmark;

public class TestAspects {

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
                        TestAspects.executeTest(testPoint);
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

    private static void executeTest(ProceedingJoinPoint testPoint) {
        try {
            int rnd = (int) (10 * Math.random());
            Test2Benchmark.log("Will run test " + rnd + " times...");
            for (int i = 0; i < rnd; i++) {
                Object value = testPoint.proceed();
            }
        } catch (Throwable t) {
            Test2Benchmark.errWithTrace("test.around failed", t);
        }
    }
}
