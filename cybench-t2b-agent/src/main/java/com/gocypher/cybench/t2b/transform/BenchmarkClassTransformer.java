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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.generators.core.ClassInfo;
import org.slf4j.Logger;

import com.gocypher.cybench.T2BUtils;
import com.gocypher.cybench.t2b.transform.metadata.BenchmarkMetadata;

import javassist.ClassPool;
import javassist.CtClass;

public class BenchmarkClassTransformer extends AbstractClassTransformer {
    private static Logger LOGGER = T2BUtils.getLogger(BenchmarkClassTransformer.class);

    public BenchmarkClassTransformer(ClassInfo clsInfo) {
        super(clsInfo);
    }

    public void doTransform(Method testMethod) {
        if (hasNonStaticFields()) {
            annotateClassState();
        }

        // annotateClassMetadataList(getClsInfo());

        for (org.openjdk.jmh.generators.core.MethodInfo methodInfo : getClsInfo().getMethods()) {
            Annotation bAnnotation = methodInfo.getAnnotation(Benchmark.class);
            if (bAnnotation != null) {
                annotateMethod(methodInfo, testMethod);
            }
        }
    }

    public byte[] getClassBytes() throws Exception {
        return getAlteredClass().toBytecode();
    }

    public void annotateBenchmarkTag(org.openjdk.jmh.generators.core.MethodInfo methodInfo, Method testMethod) {
        String methodSignature = getSignature(testMethod);
        annotateBenchmarkTag(methodInfo, methodSignature);
    }

    public void annotateBenchmarkMetadataList(org.openjdk.jmh.generators.core.MethodInfo methodInfo,
            Method testMethod) {
        annotateBenchmarkMetadataList(methodInfo, getMetadata(testMethod));
    }

    public List<Map<String, String>> getMetadata(Method method) {
        Map<String, String> metaDataMap = BenchmarkMetadata.fillMetadata(method);

        return makeMetadataList(metaDataMap);
    }

    @Override
    protected CtClass getCtClass(String className) throws Exception {
        if (getAlteredClass() == null) {
            ClassPool pool = ClassPool.getDefault();
            CtClass ctClass = pool.get(className);
            LOGGER.info(String.format("%-15.15s: %s", "Alter", "will change class " + className));
            setAlteredClass(ctClass);

            return ctClass;
        }

        return getAlteredClass();
    }

    public void annotateMethod(org.openjdk.jmh.generators.core.MethodInfo mi, Method testMethod) {
        annotateBenchmarkTag(mi, testMethod);
        annotateBenchmarkMetadataList(mi, testMethod);
    }
}
