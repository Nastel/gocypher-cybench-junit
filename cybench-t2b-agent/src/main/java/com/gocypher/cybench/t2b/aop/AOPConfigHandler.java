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

import java.io.*;
import java.lang.reflect.Constructor;
import java.util.Properties;

import org.slf4j.Logger;

import com.gocypher.cybench.T2BUtils;
import com.gocypher.cybench.t2b.aop.benchmark.runner.BenchmarkRunnerWrapper;

public class AOPConfigHandler {
    private static Logger LOGGER = T2BUtils.getLogger(AOPConfigHandler.class);

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
                LOGGER.error("Failed to load aop config from: {}, reason: {}", cfgPath, exc.getLocalizedMessage());
            }
        } else {
            String cfgProp = System.getProperty(SYS_PROP_AOP_CONFIG);
            if (cfgProp != null) {
                LOGGER.warn("System property {} defined aop configuration file {} not found!", SYS_PROP_AOP_CONFIG,
                        cfgPath);
            } else {
                LOGGER.info("Default metadata configuration file {} not found!", cfgPath);
            }
        }

        String bwClassName = aopCfgProps.getProperty("t2b.benchmark.runner.wrapper",
                "com.gocypher.cybench.t2b.aop.benchmark.runner.CybenchRunnerWrapper");
        String bwArgs = aopCfgProps.getProperty("t2b.benchmark.runner.wrapper.args",
                "cfg=config/cybench-launcher.properties");
        try {
            @SuppressWarnings("unchecked")
            Class<? extends BenchmarkRunnerWrapper> bwClass = (Class<? extends BenchmarkRunnerWrapper>) Class
                    .forName(bwClassName);
            Constructor<? extends BenchmarkRunnerWrapper> bwConstructor = bwClass.getConstructor(String.class);
            benchmarkRunner = bwConstructor.newInstance(bwArgs);
        } catch (Exception exc) {
            LOGGER.error("Failed to load benchmark runner wrapper, reason: {}", exc);
        }
    }

    public static BenchmarkRunnerWrapper getBenchmarkRunner() {
        return benchmarkRunner;
    }
}
