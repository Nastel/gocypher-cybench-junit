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

package com.gocypher.cybench.t2b.transform;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.generators.core.ClassInfo;
import org.openjdk.jmh.generators.reflection.T2BClassInfo;
import org.slf4j.Logger;

import com.gocypher.cybench.T2BMapper;
import com.gocypher.cybench.T2BUtils;

import javassist.ClassPool;
import javassist.CtClass;

public class TestClassTransformer extends AbstractClassTransformer {
    private static Logger LOGGER = T2BUtils.getLogger(TestClassTransformer.class);

    private static final String SYS_PROP_CLASS_NAME_SUFFIX = "t2b.bench.class.name.suffix";
    private static final String DEFAULT_CLASS_NAME_SUFFIX = "BenchmarkByT2B";
    private static String benchClassNameSuffix = System.getProperty(SYS_PROP_CLASS_NAME_SUFFIX,
            DEFAULT_CLASS_NAME_SUFFIX);

    List<org.openjdk.jmh.generators.core.MethodInfo> benchmarksList = new ArrayList<>();
    private ClassInfo aClsInfo;

    public TestClassTransformer(ClassInfo clsInfo) {
        super();

        setClassInfo(checkClassLoaderForAlteredClass(clsInfo));
    }

    private ClassInfo checkClassLoaderForAlteredClass(ClassInfo clsInfo) {
        String clsName = getClassName(clsInfo);
        String alteredClassName = getAlteredClassName(clsName);
        try {
            Class<?> alteredCLClass = Class.forName(alteredClassName);
            return new T2BClassInfo(alteredCLClass);
        } catch (Exception exc) {
            try {
                getCtClass(clsName);
            } catch (Exception exc2) {
                exc2.printStackTrace(); // TODO:
            }
            return clsInfo;
        }
    }

    public void doTransform(T2BMapper... t2bMappers) {
        if (hasNonStaticFields()) {
            annotateClassState();
        }

        annotateClassMetadataList(getClsInfo());

        for (org.openjdk.jmh.generators.core.MethodInfo methodInfo : getClsInfo().getMethods()) {
            annotateMethod(methodInfo, t2bMappers);
        }
    }

    public void storeTransformedClass(String dir) {
        if (isClassAltered()) {
            try {
                storeClass(dir);
                toClass();
            } catch (Exception exc) {
                LOGGER.error(MessageFormat.format("failed to use altered class: {0}", getAlteredClassName()), exc);
            }
        }
    }

    public void annotateBenchmark(org.openjdk.jmh.generators.core.MethodInfo methodInfo) {
        annotateBenchmarkMethod(methodInfo, Benchmark.class.getName(), null);
    }

    public void annotateBenchmarkTag(org.openjdk.jmh.generators.core.MethodInfo methodInfo) {
        String methodSignature = getSignature(methodInfo);
        annotateBenchmarkTag(methodInfo, methodSignature);
    }

    public void annotateBenchmarkMetadataList(org.openjdk.jmh.generators.core.MethodInfo methodInfo) {
        annotateBenchmarkMetadataList(methodInfo, getMetadata(methodInfo));
    }

    public static String getAlteredClassName(String className) {
        if (className.contains("$")) {
            String[] cnt = className.split("\\$");
            cnt[0] = cnt[0] + benchClassNameSuffix;
            return String.join("$", cnt);
        } else {
            return className + benchClassNameSuffix;
        }
    }

    @Override
    protected CtClass getCtClass(String className) throws Exception {
        if (getAlteredClass() == null) {
            ClassPool pool = ClassPool.getDefault();
            CtClass ctClass = pool.getAndRename(className, getAlteredClassName(className));
            LOGGER.info(String.format("%-15.15s: %s", "Rename",
                    "altering class " + className + " and named it " + ctClass.getName()));
            setAlteredClass(ctClass);

            return ctClass;
        }

        return getAlteredClass();
    }

    public void toClass() throws Exception {
        if (getAlteredClass() != null) {
            Class<?> cls = getAlteredClass().toClass();
            aClsInfo = new T2BClassInfo(cls);
        }
    }

    public ClassInfo getClassInfo() {
        return aClsInfo == null ? getClsInfo() : aClsInfo;
    }

    public boolean isClassAltered() {
        return getAlteredClass() != null;
    }

    public void storeClass(String classDir) throws Exception {
        getAlteredClass().writeFile(new File(classDir).getCanonicalPath());
    }

    public String getAlteredClassName() {
        return getAlteredClass() == null ? "null" : getAlteredClass().getName();
    }

    public boolean hasBenchmarks() {
        return !benchmarksList.isEmpty();
    }

    public Collection<org.openjdk.jmh.generators.core.MethodInfo> getBenchmarkMethods() {
        if (isClassAltered()) {
            Collection<org.openjdk.jmh.generators.core.MethodInfo> amil = aClsInfo.getMethods();
            for (int i = 0; i < benchmarksList.size(); i++) {
                org.openjdk.jmh.generators.core.MethodInfo mi = benchmarksList.get(i);
                org.openjdk.jmh.generators.core.MethodInfo ami = getAlteredMethod(mi, amil);
                if (ami != null) {
                    benchmarksList.set(i, ami);
                }
            }
        }

        return benchmarksList;
    }

    private static org.openjdk.jmh.generators.core.MethodInfo getAlteredMethod(
            org.openjdk.jmh.generators.core.MethodInfo mi,
            Collection<org.openjdk.jmh.generators.core.MethodInfo> amil) {
        for (org.openjdk.jmh.generators.core.MethodInfo ami : amil) {
            if (ami.getName().equals(mi.getName())) {
                return ami;
            }
        }

        return null;
    }

    public void annotateMethod(org.openjdk.jmh.generators.core.MethodInfo mi, T2BMapper... t2BMappers) {
        T2BMapper.MethodState testValid = isValidTest(mi, t2BMappers);
        if (testValid == T2BMapper.MethodState.VALID) {
            annotateBenchmark(mi);
            annotateBenchmarkTag(mi);
            annotateBenchmarkMetadataList(mi);
            benchmarksList.add(mi);
        } else if (isSetupMethod(mi, t2BMappers)) {
            annotateMethodSetup(mi);
        } else if (isTearDownMethod(mi, t2BMappers)) {
            annotateMethodTearDown(mi);
        } else if (testValid != T2BMapper.MethodState.NOT_TEST) {
            LOGGER.info(String.format("%-20.20s: %s", "Skipping",
                    "test method " + mi.getQualifiedName() + ", reason: " + testValid.name()));
        }
    }

    private static T2BMapper.MethodState isValidTest(org.openjdk.jmh.generators.core.MethodInfo mi,
            T2BMapper... t2bMappers) {
        if (t2bMappers != null) {
            for (T2BMapper mapper : t2bMappers) {
                T2BMapper.MethodState ms = mapper.isValid(mi);
                if (ms == T2BMapper.MethodState.NOT_TEST) {
                    continue;
                } else {
                    return ms;
                }
            }
        }

        return T2BMapper.MethodState.NOT_TEST;
    }

    private static boolean isSetupMethod(org.openjdk.jmh.generators.core.MethodInfo mi, T2BMapper... t2bMappers) {
        if (t2bMappers != null) {
            for (T2BMapper mapper : t2bMappers) {
                boolean setupMethod = mapper.isSetupMethod(mi);
                if (setupMethod) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean isTearDownMethod(org.openjdk.jmh.generators.core.MethodInfo mi, T2BMapper... t2bMappers) {
        if (t2bMappers != null) {
            for (T2BMapper mapper : t2bMappers) {
                boolean setupMethod = mapper.isTearDownMethod(mi);
                if (setupMethod) {
                    return true;
                }
            }
        }

        return false;
    }

}
