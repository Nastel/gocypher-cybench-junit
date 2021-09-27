package com.gocypher.cybench.t2b.transform.annotation;

import java.util.List;

import javassist.bytecode.ConstPool;

public class DefaultAnnotationArrayBuilder<T> implements AnnotationArrayBuilder {
    private AnnotationBuilder<T> annotationBuilder;
    private List<T> members;

    public DefaultAnnotationArrayBuilder(AnnotationBuilder<T> annotationBuilder, List<T> members) {
        this.annotationBuilder = annotationBuilder;
        this.members = members;
    }

    @Override
    public javassist.bytecode.annotation.Annotation[] buildAnnotations(ConstPool constPool) {
        return buildAnnotations(constPool, members, annotationBuilder);
    }

    javassist.bytecode.annotation.Annotation[] buildAnnotations(ConstPool constPool, List<T> members,
            AnnotationBuilder<T> annotationBuilder) {
        javassist.bytecode.annotation.Annotation[] memberArray = new javassist.bytecode.annotation.Annotation[members
                .size()];

        if (members != null) {
            for (int i = 0; i < members.size(); i++) {
                T member = members.get(i);
                annotationBuilder.setMembers(member);
                memberArray[i] = annotationBuilder.buildAnnotation(constPool);
            }
        }

        return memberArray;
    }
}
