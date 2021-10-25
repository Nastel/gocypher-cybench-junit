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

package com.gocypher.cybench.t2b.aop.benchmark.runner;

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
