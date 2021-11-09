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

import org.aspectj.lang.ProceedingJoinPoint;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.CommandLineOptions;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.slf4j.Logger;

import com.gocypher.cybench.T2BUtils;

public class JMHRunnerWrapper extends AbstractBenchmarkRunnerWrapper {
    private static Logger LOGGER = T2BUtils.getLogger(JMHRunnerWrapper.class);

    public JMHRunnerWrapper(String args) {
        super(args);
    }

    @Override
    public void run(ProceedingJoinPoint testPoint) throws Throwable {
        setTestPoint(testPoint);
        cleanContext();

        LOGGER.info("Starting JMH Runner...");
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
            LOGGER.info("JMH Runner completed...");
        }
    }
}
