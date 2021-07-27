package com.gocypher.cybench;

import java.lang.annotation.Annotation;

import org.openjdk.jmh.generators.core.MethodInfo;

public abstract class AnnotationCondition {

    private Class<? extends Annotation> annotation;
    private Class<? extends Annotation> skipAnnotation;

    public AnnotationCondition(Class<? extends Annotation> annotation) {
        this.annotation = annotation;
    }

    public AnnotationCondition(Class<? extends Annotation> annotation, Class<? extends Annotation> skipAnnotation) {
        this.annotation = annotation;
        this.skipAnnotation = skipAnnotation;
    }

    public boolean isAnnotated(MethodInfo mi) {
        return mi.getAnnotation(annotation) != null;
    }

    public boolean isSkipAnnotated(MethodInfo mi) {
        return skipAnnotation != null && mi.getAnnotation(skipAnnotation) != null;
    }

    public abstract MethodState isAnnotationSkippable(Annotation ann);

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

            return MethodState.VALID;
        }

        return MethodState.NOT_TEST;
    }

    public enum MethodState {
        VALID, NOT_TEST, DISABLED, EXCEPTION_EXPECTED
    }
}
