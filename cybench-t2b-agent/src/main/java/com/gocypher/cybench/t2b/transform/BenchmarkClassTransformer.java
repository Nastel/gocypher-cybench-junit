package com.gocypher.cybench.t2b.transform;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.generators.core.ClassInfo;

import com.gocypher.cybench.Test2Benchmark;
import com.gocypher.cybench.t2b.transform.metadata.BenchmarkMetadata;

import javassist.ClassPool;
import javassist.CtClass;

public class BenchmarkClassTransformer extends AbstractClassTransformer {

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
            Test2Benchmark.log(String.format("%-15.15s: %s", "Alter", "will change class " + className));
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
