package com.gocypher.cybench.t2b.transform;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

import org.apache.commons.math3.util.Pair;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.generators.core.ClassInfo;
import org.openjdk.jmh.generators.core.FieldInfo;
import org.openjdk.jmh.generators.core.MetadataInfo;
import org.openjdk.jmh.generators.core.ParameterInfo;

import com.gocypher.cybench.T2BUtils;
import com.gocypher.cybench.Test2Benchmark;
import com.gocypher.cybench.core.annotation.BenchmarkMetaData;
import com.gocypher.cybench.core.annotation.BenchmarkTag;
import com.gocypher.cybench.core.annotation.CyBenchMetadataList;
import com.gocypher.cybench.t2b.transform.annotation.*;
import com.gocypher.cybench.t2b.transform.metadata.BenchmarkMetadata;

import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;
import javassist.Modifier;
import javassist.bytecode.*;

public abstract class AbstractClassTransformer {

    private ClassInfo clsInfo;
    private CtClass alteredClass;

    public AbstractClassTransformer() {
    }

    public AbstractClassTransformer(ClassInfo clsInfo) {
        this.clsInfo = clsInfo;
    }

    protected void setClassInfo(ClassInfo clsInfo) {
        this.clsInfo = clsInfo;
    }

    protected ClassInfo getClsInfo() {
        return clsInfo;
    }

    protected void setAlteredClass(CtClass alteredClass) {
        this.alteredClass = alteredClass;
    }

    protected CtClass getAlteredClass() {
        return alteredClass;
    }

    public boolean hasNonStaticFields() {
        for (FieldInfo fieldInfo : getAllFields(clsInfo)) {
            if (!fieldInfo.isStatic()) {
                return true;
            }
        }

        return false;
    }

    public static Collection<FieldInfo> getAllFields(ClassInfo ci) {
        Collection<FieldInfo> ls = new ArrayList<>();
        do {
            ls.addAll(ci.getFields());
        } while ((ci = ci.getSuperClass()) != null);
        return ls;
    }

    public void annotateClassState() {
        Annotation stateAnnotation = clsInfo.getAnnotation(State.class);
        if (stateAnnotation == null) {
            annotateStateClass(clsInfo, State.class.getName(), STATE_ANNOTATION_MEMBERS);
        }
    }

    public List<Map<String, String>> getMetadata(MetadataInfo metadataInfo) {
        Map<String, String> metaDataMap = BenchmarkMetadata.fillMetadata(metadataInfo);

        return makeMetadataList(metaDataMap);
    }

    protected List<Map<String, String>> makeMetadataList(Map<String, String> metaDataMap) {
        List<Map<String, String>> metaDataList = new ArrayList<>(metaDataMap.size());

        if (metaDataMap != null) {
            for (Map.Entry<String, String> mde : metaDataMap.entrySet()) {
                Map<String, String> tagMembers = new LinkedHashMap<>(2);
                tagMembers.put("key", mde.getKey());
                tagMembers.put("value", mde.getValue());
                metaDataList.add(tagMembers);
            }
        }

        return metaDataList;
    }

    public void annotateBenchmarkTag(org.openjdk.jmh.generators.core.MethodInfo methodInfo, String methodSignature) {
        Map<String, String> tagMembers = new LinkedHashMap<>(1);
        tagMembers.put("tag", UUID.nameUUIDFromBytes(methodSignature.getBytes()).toString());

        annotateBenchmarkMethod(methodInfo, BenchmarkTag.class.getName(), tagMembers);
    }

    public void annotateBenchmarkMetadataList(org.openjdk.jmh.generators.core.MethodInfo methodInfo,
            List<Map<String, String>> metaDataList) {
        if (metaDataList.isEmpty()) {
            return;
        }

        annotateBenchmarkMethod(methodInfo, CyBenchMetadataList.class.getName(), BenchmarkMetaData.class.getName(),
                metaDataList);
    }

    // NOTE: Javassist does not support @Repeatable annotations yet...
    // public void annotateBenchmarkMetadata(org.openjdk.jmh.generators.core.MethodInfo methodInfo) {
    // Map<String, String> metaDataMap = BenchmarkMetadata.fillMetadata(methodInfo);
    //
    // for (Map.Entry<String, String> mde : metaDataMap.entrySet()) {
    // Map<String, String> tagMembers = new LinkedHashMap<>(2);
    // tagMembers.put("key", mde.getKey());
    // tagMembers.put("value", mde.getValue());
    // annotateBenchmarkMethod(methodInfo, BenchmarkMetaData.class.getName(), tagMembers);
    // }
    // }

    // NOTE: Javassist does not support @Repeatable annotations yet...
    // public void annotateClassMetadata(ClassInfo classInfo) {
    // Map<String, String> metaDataMap = BenchmarkMetadata.fillMetadata(classInfo);
    //
    // for (Map.Entry<String, String> mde : metaDataMap.entrySet()) {
    // Map<String, String> tagMembers = new LinkedHashMap<>(2);
    // tagMembers.put("key", mde.getKey());
    // tagMembers.put("value", mde.getValue());
    // annotateBenchmarkClass(classInfo, BenchmarkMetaData.class.getName(), tagMembers);
    // }
    // }

    public void annotateClassMetadataList(ClassInfo classInfo) {
        List<Map<String, String>> metaDataList = getMetadata(classInfo);
        if (metaDataList.isEmpty()) {
            return;
        }

        annotateBenchmarkClass(classInfo, CyBenchMetadataList.class.getName(), BenchmarkMetaData.class.getName(),
                metaDataList);
    }

    private static Map<String, Pair<String, String>> LEVEL_ANNOTATION_MEMBERS = new LinkedHashMap<>();
    static {
        LEVEL_ANNOTATION_MEMBERS.put("value", new Pair<>(Level.class.getName(), Level.Trial.name()));
    }

    public void annotateMethodSetup(org.openjdk.jmh.generators.core.MethodInfo methodInfo) {
        annotateStateMethod(methodInfo, Setup.class.getName(), LEVEL_ANNOTATION_MEMBERS);
    }

    public void annotateMethodTearDown(org.openjdk.jmh.generators.core.MethodInfo methodInfo) {
        annotateStateMethod(methodInfo, TearDown.class.getName(), LEVEL_ANNOTATION_MEMBERS);
    }

    public String getClassName() {
        return alteredClass == null ? getClassName(clsInfo) : alteredClass.getName();
    }

    private static Map<String, Pair<String, String>> STATE_ANNOTATION_MEMBERS = new LinkedHashMap<>();
    static {
        STATE_ANNOTATION_MEMBERS.put("value", new Pair<>(Scope.class.getName(), Scope.Benchmark.name()));
    }

    public void annotateStateClass(ClassInfo classInfo, String annotationName,
            Map<String, Pair<String, String>> membersMap) {
        String clsName = getClassName(classInfo);
        try {
            addClassEnumAnnotation(clsName, annotationName, membersMap);
            Test2Benchmark.log(
                    String.format("%-20.20s: %s", "Added", "@" + annotationName + " annotation for class " + clsName));
        } catch (Exception exc) {
            Test2Benchmark.errWithTrace("failed to add @" + annotationName + " annotation for class " + clsName, exc);
        }
    }

    public void annotateBenchmarkClass(ClassInfo classInfo, String annotationName, Map<String, String> membersMap) {
        String clsName = getClassName(classInfo);
        try {
            addClassAnnotation(clsName, annotationName, membersMap);
            Test2Benchmark.log(
                    String.format("%-20.20s: %s", "Added", "@" + annotationName + " annotation for class " + clsName));
        } catch (Exception exc) {
            Test2Benchmark.errWithTrace("failed to add @" + annotationName + " annotation for class " + clsName, exc);
        }
    }

    public void annotateBenchmarkClass(ClassInfo classInfo, String arrayAnnotationName, String annotationsName,
            List<Map<String, String>> memberList) {
        String clsName = getClassName(classInfo);
        try {
            addClassAnnotation(clsName, arrayAnnotationName, annotationsName, memberList);
            Test2Benchmark.log(String.format("%-20.20s: %s", "Added",
                    "@" + arrayAnnotationName + " annotation for class " + clsName));
        } catch (Exception exc) {
            Test2Benchmark.errWithTrace("failed to add @" + arrayAnnotationName + " annotation for class " + clsName,
                    exc);
        }
    }

    public void annotateBenchmarkMethod(org.openjdk.jmh.generators.core.MethodInfo method, String annotationName,
            Map<String, String> membersMap) {
        try {
            String methodName = method.getName();

            addMethodAnnotation(methodName, annotationName, membersMap);
            Test2Benchmark.log(String.format("%-20.20s: %s", "Added",
                    "@" + annotationName + " annotation for method " + method.getQualifiedName()));
        } catch (Exception exc) {
            Test2Benchmark.errWithTrace(
                    "failed to add @" + annotationName + " annotation for method " + method.getQualifiedName(), exc);
        }
    }

    public void annotateBenchmarkMethod(org.openjdk.jmh.generators.core.MethodInfo method, String arrayAnnotationName,
            String annotationsName, List<Map<String, String>> memberList) {
        try {
            String methodName = method.getName();

            addMethodArrayAnnotation(methodName, arrayAnnotationName, annotationsName, memberList);
            Test2Benchmark.log(String.format("%-20.20s: %s", "Added",
                    "@" + arrayAnnotationName + " annotation for method " + method.getQualifiedName()));
        } catch (Exception exc) {
            Test2Benchmark.errWithTrace(
                    "failed to add @" + arrayAnnotationName + " annotation for method " + method.getQualifiedName(),
                    exc);
        }
    }

    public void annotateStateMethod(org.openjdk.jmh.generators.core.MethodInfo method, String annotationName,
            Map<String, Pair<String, String>> membersMap) {
        try {
            String methodName = method.getName();

            addMethodEnumAnnotation(methodName, annotationName, membersMap);
            Test2Benchmark.log(String.format("%-20.20s: %s", "Added",
                    "@" + annotationName + " annotation for method " + method.getQualifiedName()));
        } catch (Exception exc) {
            Test2Benchmark.errWithTrace(
                    "failed to add @" + annotationName + " annotation for method " + method.getQualifiedName(), exc);
        }

        annotateClassState();
    }

    public static String getSignature(Method m) {
        String sig;
        try {
            sig = (String) T2BUtils.getFieldValue(m, "signature");
            if (sig != null) {
                return sig;
            }
        } catch (Exception exc) {
            // Test2Benchmark.errWithTrace("failed to get method signature field", exc);
        }

        StringBuilder sb = new StringBuilder(m.getDeclaringClass().getName() + "." + m.getName() + "(");
        for (Class<?> c : m.getParameterTypes()) {
            sb.append((sig = Array.newInstance(c, 0).toString()), 1, sig.indexOf('@'));
        }
        return sb.append(')')
                .append(m.getReturnType() == void.class ? "V"
                        : (sig = Array.newInstance(m.getReturnType(), 0).toString()).substring(1, sig.indexOf('@')))
                .toString();
    }

    public static String getSignature(org.openjdk.jmh.generators.core.MethodInfo methodInfo) {
        try {
            Method m = (Method) T2BUtils.getFieldValue(methodInfo, "m");
            return getSignature(m);
        } catch (Exception exc) {
            StringBuilder sb = new StringBuilder(
                    methodInfo.getDeclaringClass().getName() + "." + methodInfo.getName() + "(");
            for (ParameterInfo pi : methodInfo.getParameters()) {
                sb.append(pi.getType().getName());
            }
            return sb.append(')').append(methodInfo.getReturnType().equals("void") ? "V" : methodInfo.getReturnType())
                    .toString();
        }
    }

    public static Class<?> getClass(ClassInfo classInfo) throws Exception {
        Field f = classInfo.getClass().getSuperclass().getDeclaredField("klass");
        f.setAccessible(true);
        Class<?> cls = (Class<?>) f.get(classInfo);
        return cls;
    }

    public static String getClassName(ClassInfo classInfo) {
        try {
            Class<?> cls = getClass(classInfo);
            return cls.getName();
        } catch (Throwable exc) {
            return classInfo.getQualifiedName();
        }
    }

    private static void alterClass(CtClass ctClass) throws Exception {
        if (ctClass.isFrozen()) {
            ctClass.defrost();
        }

        makeClassPublic(ctClass);
        makeConstructorPublic(ctClass);
    }

    private static void makeClassPublic(CtClass ctClass) {
        if (!isNestedClass(ctClass) && !Modifier.isPublic(ctClass.getModifiers())) {
            ctClass.setModifiers(ctClass.getModifiers() | Modifier.PUBLIC);
            Test2Benchmark.log(
                    String.format("%-20.20s: %s", "Changed", "visibility to PUBLIC for class " + ctClass.getName()));
        }
    }

    private static void makeConstructorPublic(CtClass ctClass) throws Exception {
        for (CtConstructor constructor : ctClass.getConstructors()) {
            if (constructor.getParameterTypes().length < 2) {
                if (!Modifier.isPublic(constructor.getModifiers())) {
                    constructor.setModifiers(constructor.getModifiers() | Modifier.PUBLIC);
                    Test2Benchmark.log(String.format("%-20.20s: %s", "Changed",
                            "visibility to PUBLIC for constructor " + constructor.getLongName()));
                }
            }
        }
    }

    public CtClass addClassEnumAnnotation(String className, String annotationName,
            Map<String, Pair<String, String>> membersMap) throws Exception {
        return addClassAnnotation(className, new EnumAnnotationBuilder(annotationName, membersMap));
    }

    public CtClass addClassAnnotation(String className, String annotationName, Map<String, String> membersMap)
            throws Exception {
        return addClassAnnotation(className, new StringAnnotationBuilder(annotationName, membersMap));
    }

    public CtClass addClassAnnotation(String className, String arrayAnnotationName, String annotationsName,
            List<Map<String, String>> memberList) throws Exception {
        return addClassAnnotation(className, new ArrayAnnotationBuilder(arrayAnnotationName,
                new DefaultAnnotationArrayBuilder<>(new StringAnnotationBuilder(annotationsName), memberList)));
    }

    public CtClass addClassAnnotation(String className, AnnotationBuilder<?> annotationBuilder) throws Exception {
        CtClass ctClass = getCtClass(className);
        alterClass(ctClass);

        ClassFile classFile = ctClass.getClassFile();
        ConstPool constPool = classFile.getConstPool();

        List<AttributeInfo> classFileAttributes = classFile.getAttributes();
        AnnotationsAttribute annotationsAttribute = getAnnotationAttribute(classFileAttributes);

        if (annotationsAttribute == null) {
            annotationsAttribute = new AnnotationsAttribute(constPool, AnnotationsAttribute.visibleTag);
            classFile.addAttribute(annotationsAttribute);
        }

        annotationsAttribute.addAnnotation(annotationBuilder.buildAnnotation(constPool));

        return ctClass;
    }

    public CtClass addMethodAnnotation(String methodName, String annotationName, Map<String, String> membersMap)
            throws Exception {
        return addMethodAnnotation(methodName, new StringAnnotationBuilder(annotationName, membersMap));
    }

    public CtClass addMethodEnumAnnotation(String methodName, String annotationName,
            Map<String, Pair<String, String>> membersMap) throws Exception {
        return addMethodAnnotation(methodName, new EnumAnnotationBuilder(annotationName, membersMap));
    }

    public CtClass addMethodArrayAnnotation(String methodName, String arrayAnnotationName, String annotationsName,
            List<Map<String, String>> memberList) throws Exception {
        return addMethodAnnotation(methodName, new ArrayAnnotationBuilder(arrayAnnotationName,
                new DefaultAnnotationArrayBuilder<>(new StringAnnotationBuilder(annotationsName), memberList)));
    }

    private static void makeMethodPublic(CtMethod method) {
        if (!Modifier.isPublic(method.getModifiers())) {
            method.setModifiers(method.getModifiers() | Modifier.PUBLIC);
            Test2Benchmark.log(String.format("%-20.20s: %s", "Changed",
                    "visibility to PUBLIC for method " + method.getLongName()));
        }
    }

    public CtClass addMethodAnnotation(String methodName, AnnotationBuilder<?> annotationBuilder) throws Exception {
        CtClass ctClass = getCtClass(getClassName());
        alterClass(ctClass);

        CtMethod method = ctClass.getDeclaredMethod(methodName);
        makeMethodPublic(method);

        MethodInfo methodInfo = method.getMethodInfo();
        ConstPool constPool = methodInfo.getConstPool();

        List<AttributeInfo> methodAttributes = methodInfo.getAttributes();
        AnnotationsAttribute annotationsAttribute = getAnnotationAttribute(methodAttributes);

        if (annotationsAttribute == null) {
            annotationsAttribute = new AnnotationsAttribute(constPool, AnnotationsAttribute.visibleTag);
            methodInfo.addAttribute(annotationsAttribute);
        }

        annotationsAttribute.addAnnotation(annotationBuilder.buildAnnotation(constPool));

        return ctClass;
    }

    protected abstract CtClass getCtClass(String className) throws Exception;

    static AnnotationsAttribute getAnnotationAttribute(List<AttributeInfo> attributes) {
        for (Object object : attributes) {
            if (AnnotationsAttribute.class.isAssignableFrom(object.getClass())) {
                return (AnnotationsAttribute) object;
            }
        }

        return null;
    }

    private static boolean isNestedClass(CtClass cls) {
        return cls.getName().contains("$");
    }
}
