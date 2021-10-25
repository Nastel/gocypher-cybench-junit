/*
 * Copyright (C) 2020-2021, K2N.IO.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 *
 */

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

        static final TestJoinPointHandler testJoinPointHandler = new DefaultTestJoinPointHandler();
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
            Method testMethod = signature.getMethod();

            T2BMapper.MethodState state = testMapper.isValid(testMethod);

            if (state == T2BMapper.MethodState.VALID) {
                testJoinPointHandler.runTestAsBenchmark(testMethod, testPoint);
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
