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
import org.slf4j.Logger;

import com.gocypher.cybench.T2BMapper;
import com.gocypher.cybench.Test2Benchmark;
import com.gocypher.cybench.t2b.utils.LogUtils;

public class TestAspects {
    private static Logger LOGGER = LogUtils.getLogger(TestAspects.class);

    public abstract static class AbstractT2BAspect {

        static final TestJoinPointHandler testJoinPointHandler = new DefaultTestJoinPointHandler();
        final T2BMapper testMapper;

        public AbstractT2BAspect(T2BMapper t2BMapper) {
            LOGGER.info("Initiating aspect {} ...", getClass().getSimpleName());

            testMapper = t2BMapper;
        }

        // @Pointcut("execution(@org.junit.jupiter.api.Test * *(..))")
        // public void transactionalMethod() {
        // }

        public void around(ProceedingJoinPoint testPoint) {
            LOGGER.debug("Test around before, class: {}, method: {}",
                    testPoint.getSignature().getDeclaringType().getSimpleName(), testPoint.getSignature().getName());

            MethodSignature signature = (MethodSignature) testPoint.getSignature();
            Method testMethod = signature.getMethod();

            T2BMapper.MethodState state = testMapper.isValid(testMethod);

            if (state == T2BMapper.MethodState.VALID) {
                testJoinPointHandler.runTestAsBenchmark(testMethod, testPoint);
            } else {
                LOGGER.warn("Skipping test: {}", testPoint.getSignature().getName());
            }

            LOGGER.debug("Test around after, class: {}", testPoint.getSignature().getDeclaringType().getSimpleName());
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
            LOGGER.debug("JointPoint: {null}");
            return;
        }
        Signature sig = joinPoint.getSignature();
        String kind = joinPoint.getKind();
        Object[] args = joinPoint.getArgs();
        SourceLocation sl = joinPoint.getSourceLocation();
        Object target = joinPoint.getTarget();
        JoinPoint.StaticPart sp = joinPoint.getStaticPart();
        Object tis = joinPoint.getThis();
        LOGGER.debug("JointPoint: {" //
                + System.lineSeparator() + "\t signature=" + sig //
                + System.lineSeparator() + "\t      kind=" + kind //
                + System.lineSeparator() + "\t      args=" + Arrays.toString(args) //
                + System.lineSeparator() + "\t    source=" + sl //
                + System.lineSeparator() + "\t    target=" + target //
                + System.lineSeparator() + "\t    static=" + sp //
                + System.lineSeparator() + "\t      this=" + tis //
                + System.lineSeparator() + "}");
    }
}
