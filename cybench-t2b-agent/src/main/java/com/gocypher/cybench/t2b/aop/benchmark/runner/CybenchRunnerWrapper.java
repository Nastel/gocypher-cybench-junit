/*
 * Copyright (C) 2020-2022, K2N.IO.
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

import org.aspectj.lang.ProceedingJoinPoint;
import org.slf4j.Logger;

import com.gocypher.cybench.launcher.BenchmarkRunner;
import com.gocypher.cybench.launcher.model.BenchmarkingContext;
import com.gocypher.cybench.t2b.utils.LogUtils;

public class CybenchRunnerWrapper extends AbstractBenchmarkRunnerWrapper {
    private static Logger LOGGER = LogUtils.getLogger(CybenchRunnerWrapper.class);

    protected final BenchmarkingContext benchmarkContext;

    public CybenchRunnerWrapper(String args) {
        super(args);

        benchmarkContext = initBenchmarkingContext();
    }

    protected BenchmarkingContext initBenchmarkingContext() {
        BenchmarkingContext benchmarkContext = BenchmarkRunner.initContext(System.currentTimeMillis(), args);

        try {
            BenchmarkRunner.initStaticContext(benchmarkContext);
            BenchmarkRunner.checkProjectMetadataExists(benchmarkContext.getProjectMetadata());

            BenchmarkRunner.analyzeBenchmarkClasses(benchmarkContext);
            BenchmarkRunner.buildOptions(benchmarkContext);
        } catch (Exception exc) {
            LOGGER.error("Benchmarking context initialization failed", exc);
        }

        return benchmarkContext;
    }

    @Override
    public void run(ProceedingJoinPoint testPoint) throws Exception {
        setTestPoint(testPoint);
        cleanContext();

        LOGGER.info("Starting CyBench Runner...");
        try {
            BenchmarkRunner.runBenchmarks(benchmarkContext);
        } finally {
            LOGGER.info("CyBench Runner completed!..");
        }
    }

    @Override
    public void complete() {
        if (benchmarkContext != null) {
            BenchmarkRunner.processResults(benchmarkContext);
        }
    }
}
