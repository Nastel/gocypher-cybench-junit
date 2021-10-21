package com.gocypher.cybench.t2b.aop;

import java.io.*;
import java.lang.reflect.Constructor;
import java.util.Properties;

import com.gocypher.cybench.Test2Benchmark;
import com.gocypher.cybench.t2b.aop.benchmark.runner.BenchmarkRunnerWrapper;

public class AOPConfigHandler {

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

    public static BenchmarkRunnerWrapper getBenchmarkRunner() {
        return benchmarkRunner;
    }
}
