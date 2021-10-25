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

package com.gocypher.cybench.t2b.aop.benchmark;

import org.aspectj.lang.ProceedingJoinPoint;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import com.gocypher.cybench.t2b.aop.TestAspects;
import com.gocypher.cybench.t2b.aop.benchmark.runner.AbstractBenchmarkRunnerWrapper;

@State(Scope.Benchmark)
public class T2BTestBenchmark {
    private ProceedingJoinPoint testPoint;

    @Benchmark
    public void testBenchmark(Blackhole b) throws Throwable {
        if (testPoint != null) {
            testPoint.proceed();
        }
    }

    @Setup(Level.Trial)
    public void setupTrial() {
        testPoint = AbstractBenchmarkRunnerWrapper.getTestPoint();
        TestAspects.log(testPoint);
    }

    @Setup(Level.Iteration)
    public void setupIteration() {
    }

    @Setup(Level.Invocation)
    public void setupInvocation() {
    }

    @TearDown(Level.Trial)
    public void teardownTrial() {
        testPoint = null;
    }

    @TearDown(Level.Iteration)
    public void teardownIteration() {
    }

    @TearDown(Level.Invocation)
    public void teardownInvocation() {
    }
}
