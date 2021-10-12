package com.gocypher.cybench.t2b.aop;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.aspectj.lang.reflect.SourceLocation;

import com.gocypher.cybench.T2BMapper;
import com.gocypher.cybench.Test2Benchmark;

public class TestAspects {
    private static final String ASPECT_ADVICE_EXPRESSION = ""//
            + "    @annotation(org.junit.Test)" // JUnit4
            + " || @annotation(org.junit.jupiter.api.Test)" // JUnit5
            + " || @annotation(org.testng.annotations.Test)"; // TestNG

    @Aspect
    public static class TestBeforeAfterAspect {

        public TestBeforeAfterAspect() {
            Test2Benchmark.log("Initiating aspect " + getClass().getSimpleName() + "...");
        }

        // @Pointcut("execution(@org.junit.jupiter.api.Test * *(..))")
        // public void transactionalMethod() {
        // }

        @Before(TestAspects.ASPECT_ADVICE_EXPRESSION)
        public void before(JoinPoint joinPoint) throws Throwable {
            Test2Benchmark.log("test.before, class: " + joinPoint.getSignature().getDeclaringType().getSimpleName()
                    + ", method: " + joinPoint.getSignature().getName());

            TestAspects.log(joinPoint);
        }

        @After(TestAspects.ASPECT_ADVICE_EXPRESSION)
        public void after(JoinPoint joinPoint) throws Throwable {
            Test2Benchmark.log("test.after, class: " + joinPoint.getSignature().getDeclaringType().getSimpleName()
                    + ", method: " + joinPoint.getSignature().getName());
        }
    }

    // @Aspect
    public abstract static class TestAroundAspect {

        final T2BMapper testMapper;

        public TestAroundAspect(T2BMapper t2BMapper) {
            Test2Benchmark.log("Initiating aspect " + getClass().getSimpleName() + "...");

            testMapper = t2BMapper;
        }

        // @Pointcut("execution(@org.junit.jupiter.api.Test * *(..))")
        // public void transactionalMethod() {
        // }

        // @Around(TestAspects.ASPECT_ADVICE_EXPRESSION)
        public void around(ProceedingJoinPoint joinPoint) {
            Test2Benchmark
                    .log("test.around.before, class: " + joinPoint.getSignature().getDeclaringType().getSimpleName()
                            + ", method: " + joinPoint.getSignature().getName());

            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();

            T2BMapper.MethodState state = testMapper.isValid(method);

            if (state == T2BMapper.MethodState.VALID) {
                Thread testRunnerThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        TestAspects.executeTest(joinPoint);
                    }
                });
                testRunnerThread.start();
                try {
                    testRunnerThread.join();
                } catch (Exception exc) {
                }
            } else {
                Test2Benchmark.warn("Skipping test: " + joinPoint.getSignature().getName());
            }

            Test2Benchmark
                    .log("test.around.after, class: " + joinPoint.getSignature().getDeclaringType().getSimpleName());
        }
    }

    @Aspect
    public static class JUTestAroundAspect extends TestAroundAspect {
        public JUTestAroundAspect() {
            super(Test2Benchmark.T2B_MAPPERS[0]);
        }

        @Override
        @Around("@annotation(org.junit.Test)")
        public void around(ProceedingJoinPoint joinPoint) {
            super.around(joinPoint);
        }
    }

    @Aspect
    public static class JU5TestAroundAspect extends TestAroundAspect {
        public JU5TestAroundAspect() {
            super(Test2Benchmark.T2B_MAPPERS[1]);
        }

        @Override
        @Around("@annotation(org.junit.jupiter.api.Test)")
        public void around(ProceedingJoinPoint joinPoint) {
            super.around(joinPoint);
        }
    }

    @Aspect
    public static class NGTestAroundAspect extends TestAroundAspect {
        public NGTestAroundAspect() {
            super(Test2Benchmark.T2B_MAPPERS[2]);
        }

        @Override
        @Around("@annotation(org.testng.annotations.Test)")
        public void around(ProceedingJoinPoint joinPoint) {
            super.around(joinPoint);
        }
    }

    private static void log(JoinPoint joinPoint) {
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

    private static void executeTest(ProceedingJoinPoint joinPoint) {
        try {
            int rnd = (int) (10 * Math.random());
            // TestAspects.log(joinPoint);
            for (int i = 0; i < rnd; i++) {
                Object value = joinPoint.proceed();
            }
        } catch (Throwable t) {
            Test2Benchmark.errWithTrace("test.around failed", t);
        }
    }
}
