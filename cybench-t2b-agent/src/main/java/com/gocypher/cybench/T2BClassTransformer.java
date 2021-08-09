package com.gocypher.cybench;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

import org.apache.commons.math3.util.Pair;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.generators.core.ClassInfo;
import org.openjdk.jmh.generators.core.FieldInfo;
import org.openjdk.jmh.generators.core.ParameterInfo;
import org.openjdk.jmh.generators.reflection.MyClassInfo;

import com.gocypher.cybench.core.annotation.BenchmarkTag;

import javassist.*;
import javassist.bytecode.*;
import javassist.bytecode.annotation.EnumMemberValue;
import javassist.bytecode.annotation.StringMemberValue;

public class T2BClassTransformer {

    static final String NEW_CLASS_NAME_SUFFIX = "_T2B_JMH_Benchmark";

    private ClassInfo clsInfo;
    private CtClass alteredClass;

    public T2BClassTransformer(ClassInfo clsInfo) {
        this.clsInfo = checkClassLoaderForAlteredClass(clsInfo);
    }

    private ClassInfo checkClassLoaderForAlteredClass(ClassInfo clsInfo) {
        String clsName = getClassName(clsInfo);
        String alteredClassName = getAlteredClassName(clsName);
        try {
            Class<?> alteredCLClass = Class.forName(alteredClassName);
            return new MyClassInfo(alteredCLClass);
        } catch (Exception exc) {
            try {
                getCtClass(clsName);
            } catch (Exception exc2) {
                exc2.printStackTrace(); // TODO:
            }
            return clsInfo;
        }
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
            String clsName = getClassName(clsInfo);
            annotateClass(clsName);
        }
    }

    public void annotateMethodTag(org.openjdk.jmh.generators.core.MethodInfo methodInfo,
            AnnotationCondition[] testAnnotations) {
        String methodSignature = getSignature(methodInfo);
        Map<String, String> tagMembers = new HashMap<>(1);
        tagMembers.put("tag", UUID.nameUUIDFromBytes(methodSignature.getBytes()).toString());

        annotateMethod(methodInfo, testAnnotations, BenchmarkTag.class.getName(), tagMembers);
    }

    public String getClassName() {
        return alteredClass == null ? getClassName(clsInfo) : alteredClass.getName();
    }

    public static String getAlteredClassName(String className) {
        if (className.contains("$")) {
            String[] cnt = className.split("\\$");
            cnt[0] = cnt[0] + NEW_CLASS_NAME_SUFFIX;
            return String.join("$", cnt);
        } else {
            return className + NEW_CLASS_NAME_SUFFIX;
        }
    }

    private static Map<String, Pair<String, String>> STATE_ANNOTATION_MEMBERS = new HashMap<>();
    static {
        STATE_ANNOTATION_MEMBERS.put("value", new Pair<>(Scope.class.getName(), Scope.Benchmark.name()));
    }

    public void annotateClass(String clsName) {
        try {
            CtClass annotatedClass = addClassAnnotation(clsName, State.class.getName(), STATE_ANNOTATION_MEMBERS);
            Test2Benchmark.log(String.format("%-20.20s: %s", "Added",
                    "@State annotation for class " + clsName + " and named it " + annotatedClass.getName()));
        } catch (Exception exc) {
            Test2Benchmark
                    .err("failed to add @State annotation for " + clsName + ", reason: " + exc.getLocalizedMessage());
            exc.printStackTrace();
        }
    }

    public void annotateMethod(org.openjdk.jmh.generators.core.MethodInfo method, AnnotationCondition[] testAnnotations,
            String annotationName, Map<String, String> tagMembersMap) {
        try {
            String methodName = method.getName();

            addMethodAnnotation(methodName, testAnnotations, annotationName, tagMembersMap);
        } catch (Exception exc) {
            Test2Benchmark.err("failed to add @" + annotationName + " annotation for method "
                    + method.getQualifiedName() + ", reason: " + exc.getLocalizedMessage());
            exc.printStackTrace();
        }
    }

    public static String getSignature(Method m) {
        String sig;
        try {
            sig = (String) T2BUtils.getFieldValue(m, "signature");
            if (sig != null) {
                return sig;
            }
        } catch (Exception exc) {
            // err("Failed to get method signature field, reason: " + exc.getLocalizedMessage());
            // exc.printStackTrace();
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

    private CtClass getCtClass(String className) throws Exception {
        if (alteredClass == null) {
            ClassPool pool = ClassPool.getDefault();
            CtClass ctClass = pool.getAndRename(className, getAlteredClassName(className));
            alteredClass = ctClass;

            return ctClass;
        }

        return alteredClass;
    }

    public CtClass addClassAnnotation(String className, String annotationName,
            Map<String, Pair<String, String>> membersMap) throws Exception {
        CtClass ctClass = getCtClass(className);

        if (ctClass.isFrozen()) {
            ctClass.defrost();
        }

        ClassFile classFile = ctClass.getClassFile();
        ConstPool constPool = classFile.getConstPool();

        if (!isNestedClass(ctClass) && !Modifier.isPublic(ctClass.getModifiers())) {
            ctClass.setModifiers(ctClass.getModifiers() | Modifier.PUBLIC);
            Test2Benchmark.log(
                    String.format("%-20.20s: %s", "Changed", "visibility to PUBLIC for class " + ctClass.getName()));
        }

        for (CtConstructor constructor : ctClass.getConstructors()) {
            if (constructor.getParameterTypes().length < 2) {
                if (!Modifier.isPublic(constructor.getModifiers())) {
                    constructor.setModifiers(constructor.getModifiers() | Modifier.PUBLIC);
                    Test2Benchmark.log(String.format("%-20.20s: %s", "Changed",
                            "visibility to PUBLIC for constructor " + constructor.getLongName()));
                }
            }
        }

        List<AttributeInfo> classFileAttributes = classFile.getAttributes();
        AnnotationsAttribute annotationsAttribute = getAnnotationAttribute(classFileAttributes);

        if (annotationsAttribute == null) {
            annotationsAttribute = new AnnotationsAttribute(constPool, AnnotationsAttribute.visibleTag);
            classFile.addAttribute(annotationsAttribute);
        }

        javassist.bytecode.annotation.Annotation annotation = new javassist.bytecode.annotation.Annotation(
                annotationName, constPool);
        if (membersMap != null) {
            for (Map.Entry<String, Pair<String, String>> me : membersMap.entrySet()) {
                EnumMemberValue emv = new EnumMemberValue(constPool);
                emv.setType(me.getValue().getKey());
                emv.setValue(me.getValue().getValue());
                annotation.addMemberValue(me.getKey(), emv);
            }
        }

        annotationsAttribute.addAnnotation(annotation);

        return ctClass;
    }

    public CtClass addMethodAnnotation(String methodName, AnnotationCondition[] testAnnotations, String annotationName,
            Map<String, String> membersMap) throws Exception {
        CtClass ctClass = getCtClass(getClassName());

        if (ctClass.isFrozen()) {
            ctClass.defrost();
        }

        CtMethod method = ctClass.getDeclaredMethod(methodName);
        MethodInfo methodInfo = method.getMethodInfo();
        ConstPool constPool = methodInfo.getConstPool();

        if (!isNestedClass(ctClass) && !Modifier.isPublic(ctClass.getModifiers())) {
            ctClass.setModifiers(ctClass.getModifiers() | Modifier.PUBLIC);
            Test2Benchmark.log(
                    String.format("%-20.20s: %s", "Changed", "visibility to PUBLIC for class " + ctClass.getName()));
        }
        if (!Modifier.isPublic(method.getModifiers())) {
            method.setModifiers(method.getModifiers() | Modifier.PUBLIC);
            Test2Benchmark.log(String.format("%-20.20s: %s", "Changed",
                    "visibility to PUBLIC for method " + method.getLongName()));
        }

        List<AttributeInfo> methodAttributes = methodInfo.getAttributes();
        AnnotationsAttribute annotationsAttribute = getAnnotationAttribute(methodAttributes);

        if (annotationsAttribute == null) {
            annotationsAttribute = new AnnotationsAttribute(constPool, AnnotationsAttribute.visibleTag);
            methodInfo.addAttribute(annotationsAttribute);
        }

        for (AnnotationCondition testAnnotation : testAnnotations) {
            annotationsAttribute.removeAnnotation(testAnnotation.getAnnotation().getName());
        }

        javassist.bytecode.annotation.Annotation annotation = new javassist.bytecode.annotation.Annotation(
                Benchmark.class.getName(), constPool);

        annotationsAttribute.addAnnotation(annotation);

        annotation = new javassist.bytecode.annotation.Annotation(annotationName, constPool);
        if (membersMap != null) {
            for (Map.Entry<String, String> me : membersMap.entrySet()) {
                StringMemberValue smv = new StringMemberValue(constPool);
                smv.setValue(me.getValue());
                annotation.addMemberValue(me.getKey(), smv);
            }
        }

        annotationsAttribute.addAnnotation(annotation);

        return ctClass;
    }

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

    public void toClass() throws Exception {
        if (alteredClass != null) {
            Class<?> cls = alteredClass.toClass();
            clsInfo = new MyClassInfo(cls);
        }
    }

    public Collection<org.openjdk.jmh.generators.core.MethodInfo> getMethods() {
        return clsInfo.getMethods();
    }

    public ClassInfo getClassInfo() {
        return clsInfo;
    }

    public boolean isClassAltered() {
        return alteredClass != null;
    }

    public void storeClass(String classDir) throws Exception {
        alteredClass.writeFile(new File(classDir).getCanonicalPath());
    }

    public String getAlteredClassName() {
        return alteredClass == null ? "null" : alteredClass.getName();
    }
}
