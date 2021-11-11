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

package com.gocypher.cybench.t2b.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gocypher.cybench.Test2BenchmarkAgent;

public final class LogUtils {

    private static final Date logSessionDate = new Date();

    private LogUtils() {
    }

    public static Logger getLogger(LoggerProvider loggerProvider) {
        boolean addLogHeader = false;
        synchronized (logSessionDate) {
            if (System.getProperty("t2b.session.time") == null) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HHmmss");
                System.setProperty("t2b.session.time", dateFormat.format(logSessionDate));

                initSystemOutRedirectToLogger();

                addLogHeader = true;
            }
        }

        Logger logger = loggerProvider.getLogger();

        if (addLogHeader) {
            logger.info("CyBench T2B v.{} session starting..." + System.lineSeparator() + "Runtime environment: {}",
                    pkgVersion(), runEnv());
        }

        return logger;
    }

    public static Logger getLogger(String name) {
        return getLogger(new NameLoggerProvider(name));
    }

    public static Logger getLogger(Class<?> cls) {
        return getLogger(new ClassLoggerProvider(cls));
    }

    private interface LoggerProvider {
        Logger getLogger();
    }

    private static class NameLoggerProvider implements LoggerProvider {
        private String loggerName;

        NameLoggerProvider(String loggerName) {
            this.loggerName = loggerName;
        }

        @Override
        public Logger getLogger() {
            return LoggerFactory.getLogger(loggerName);
        }
    }

    private static class ClassLoggerProvider implements LoggerProvider {
        private Class<?> loggerCls;

        ClassLoggerProvider(Class<?> loggerCls) {
            this.loggerCls = loggerCls;
        }

        @Override
        public Logger getLogger() {
            return LoggerFactory.getLogger(loggerCls);
        }
    }

    private static void initSystemOutRedirectToLogger() {
        System.setOut(new LineReadingPrintStream(new Consumer<String>() {
            private Logger LOGGER = getLogger("out");

            @Override
            public void accept(String s) {
                LOGGER.info(s);
            }
        }, System.out));

        System.setErr(new LineReadingPrintStream(new Consumer<String>() {
            private Logger LOGGER = getLogger("err");

            @Override
            public void accept(String s) {
                LOGGER.error(s);
            }
        }, System.err));
    }

    private static String pkgVersion() {
        Package sPkg = Test2BenchmarkAgent.class.getPackage();
        return sPkg.getImplementationVersion();
    }

    private static String runEnv() {
        Map<String, String> envProps = new LinkedHashMap<>();
        envProps.put("java.version", "Java version");
        envProps.put("java.vendor", "Java vendor");
        envProps.put("java.vm.name", "VM name");
        envProps.put("java.vm.version", "VM version");
        envProps.put("os.name", "OS name");
        envProps.put("os.version", "OS version");

        StringBuilder sb = new StringBuilder();
        sb.append(System.lineSeparator());
        sb.append("------------------------------------------------------------------------")
                .append(System.lineSeparator()); // NON-NLS
        for (Map.Entry<String, String> property : envProps.entrySet()) {
            sb.append(String.format("%20s: %s", // NON-NLS
                    property.getValue(), System.getProperty(property.getKey())));
            sb.append(System.lineSeparator());
        }
        sb.append("------------------------------------------------------------------------")
                .append(System.lineSeparator()); // NON-NLS

        return sb.toString();
    }
}
