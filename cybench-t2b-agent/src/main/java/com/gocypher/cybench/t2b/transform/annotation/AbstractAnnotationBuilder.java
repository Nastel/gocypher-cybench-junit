package com.gocypher.cybench.t2b.transform.annotation;

import javassist.bytecode.ConstPool;

public abstract class AbstractAnnotationBuilder<T> implements AnnotationBuilder<T> {
    String annotationName;
    T members;

    public AbstractAnnotationBuilder(String annotationName) {
        this.annotationName = annotationName;
    }

    public AbstractAnnotationBuilder(String annotationName, T members) {
        this(annotationName);

        setMembers(members);
    }

    @Override
    public void setMembers(T members) {
        this.members = members;
    }

    @Override
    public javassist.bytecode.annotation.Annotation buildAnnotation(ConstPool constPool) {
        return buildAnnotation(annotationName, constPool, members);
    }

    abstract javassist.bytecode.annotation.Annotation buildAnnotation(String annotationName, ConstPool constPool,
            T members);
}
