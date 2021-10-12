package com.gocypher.cybench;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.openjdk.jmh.generators.core.ClassInfo;
import org.openjdk.jmh.generators.core.MethodInfo;

import com.gocypher.cybench.t2b.transform.T2BClassTransformer;

public abstract class T2BMapper {

    private Class<? extends Annotation> annotation;
    private Class<? extends Annotation> skipAnnotation;

    public T2BMapper(Class<? extends Annotation> annotation) {
        this.annotation = annotation;
    }

    public T2BMapper(Class<? extends Annotation> annotation, Class<? extends Annotation> skipAnnotation) {
        this.annotation = annotation;
        this.skipAnnotation = skipAnnotation;
    }

    public Class<? extends Annotation> getAnnotation() {
        return annotation;
    }

    public boolean isAnnotated(MethodInfo mi) {
        return mi.getAnnotation(annotation) != null;
    }

    public boolean isAnnotated(Method mi) {
        return mi.getAnnotation(annotation) != null;
    }

    public boolean isSkipAnnotated(MethodInfo mi) {
        return skipAnnotation != null && mi.getAnnotation(skipAnnotation) != null;
    }

    public boolean isSkipAnnotated(Method mi) {
        return skipAnnotation != null && mi.getAnnotation(skipAnnotation) != null;
    }

    public abstract MethodState isAnnotationSkippable(Annotation ann);

    public abstract Class<? extends Annotation> getSetupAnnotation();

    public abstract Class<? extends Annotation> getTearDownAnnotation();

    public MethodState isValid(MethodInfo mi) {
        Annotation ann = mi.getAnnotation(annotation);
        if (ann != null) {
            if (isSkipAnnotated(mi)) {
                return MethodState.DISABLED;
            }
            MethodState ms = isAnnotationSkippable(ann);
            if (isAnnotationSkippable(ann) != MethodState.VALID) {
                return ms;
            }

            if (!mi.isPublic()) {
                ClassInfo cls = mi.getDeclaringClass();
                if (!cls.isPublic()) {
                    String clsName = T2BClassTransformer.getClassName(cls);
                    if (clsName.contains("$")) {
                        return MethodState.VISIBILITY;
                    }
                }
            }

            return MethodState.VALID;
        }

        return MethodState.NOT_TEST;
    }

    public MethodState isValid(Method mi) {
        Annotation ann = mi.getAnnotation(annotation);
        if (ann != null) {
            if (isSkipAnnotated(mi)) {
                return MethodState.DISABLED;
            }
            MethodState ms = isAnnotationSkippable(ann);
            if (isAnnotationSkippable(ann) != MethodState.VALID) {
                return ms;
            }

            if (!Modifier.isPublic(mi.getModifiers())) {
                Class<?> cls = mi.getDeclaringClass();
                if (!Modifier.isPublic(cls.getModifiers())) {
                    String clsName = cls.getName();
                    if (clsName.contains("$")) {
                        return MethodState.VISIBILITY;
                    }
                }
            }

            return MethodState.VALID;
        }

        return MethodState.NOT_TEST;
    }

    public boolean isSetupMethod(MethodInfo mi) {
        Annotation ann = mi.getAnnotation(getSetupAnnotation());

        return ann != null;
    }

    public boolean isSetupMethod(Method mi) {
        Annotation ann = mi.getAnnotation(getSetupAnnotation());

        return ann != null;
    }

    public boolean isSetupMethod(Annotation ann) {
        return getSetupAnnotation().equals(ann.getClass());
    }

    public boolean isTearDownMethod(MethodInfo mi) {
        Annotation ann = mi.getAnnotation(getTearDownAnnotation());

        return ann != null;
    }

    public boolean isTearDownMethod(Method mi) {
        Annotation ann = mi.getAnnotation(getTearDownAnnotation());

        return ann != null;
    }

    public boolean isTearDownMethod(Annotation ann) {
        return getTearDownAnnotation().equals(ann.getClass());
    }

    public enum MethodState {
        VALID, NOT_TEST, DISABLED, EXCEPTION_EXPECTED, VISIBILITY
    }
}
