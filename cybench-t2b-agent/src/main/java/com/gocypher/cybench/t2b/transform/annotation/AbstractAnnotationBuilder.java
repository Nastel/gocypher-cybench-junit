package com.gocypher.cybench.t2b.transform.annotation;

import javassist.bytecode.ConstPool;

public abstract class AbstractAnnotationBuilder<T> implements AnnotationBuilder<T> {
    String annotationType;
    T members;

    public AbstractAnnotationBuilder(String annotationType) {
        this.annotationType = annotationType;
    }

    public AbstractAnnotationBuilder(String annotationType, T members) {
        this(annotationType);

        setMembers(members);
    }

    @Override
    public String getAnnotationType() {
        return annotationType;
    }

    @Override
    public void setMembers(T members) {
        this.members = members;
    }

    @Override
    public javassist.bytecode.annotation.Annotation buildAnnotation(ConstPool constPool) {
        return buildAnnotation(annotationType, constPool, members);
    }

    abstract javassist.bytecode.annotation.Annotation buildAnnotation(String annotationType, ConstPool constPool,
            T members);
}
