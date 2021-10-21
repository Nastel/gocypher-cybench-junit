package com.gocypher.cybench.t2b.transform.annotation;

import javassist.bytecode.ConstPool;

public interface AnnotationBuilder<T> {

    String getAnnotationType();

    javassist.bytecode.annotation.Annotation buildAnnotation(ConstPool constPool);

    void setMembers(T members);
}
