package com.gocypher.cybench.t2b.transform.annotation;

import java.util.ArrayList;

import javassist.bytecode.ConstPool;
import javassist.bytecode.annotation.AnnotationMemberValue;
import javassist.bytecode.annotation.ArrayMemberValue;
import javassist.bytecode.annotation.MemberValue;

public class ArrayAnnotationBuilder extends AbstractAnnotationBuilder<AnnotationArrayBuilder> {

    private static final String MEMBER_NAME = "value";
    private static final MemberValue[] EMPTY_ARRAY = new MemberValue[0];

    public ArrayAnnotationBuilder(String annotationName, AnnotationArrayBuilder membersBuilder) {
        super(annotationName, membersBuilder);
    }

    @Override
    javassist.bytecode.annotation.Annotation buildAnnotation(String annotationName, ConstPool constPool,
            AnnotationArrayBuilder membersBuilder) {
        javassist.bytecode.annotation.Annotation[] membersArray = membersBuilder.buildAnnotations(constPool);
        javassist.bytecode.annotation.Annotation annotation = new javassist.bytecode.annotation.Annotation(
                annotationName, constPool);
        if (membersArray != null) {
            ArrayList<AnnotationMemberValue> memberValues = new ArrayList<>(membersArray.length);
            AnnotationMemberValue annmv;
            for (javassist.bytecode.annotation.Annotation member : membersArray) {
                annmv = new AnnotationMemberValue(constPool);
                annmv.setValue(member);
                memberValues.add(annmv);
            }

            ArrayMemberValue amv = new ArrayMemberValue(constPool);
            amv.setValue(memberValues.toArray(EMPTY_ARRAY));
            annotation.addMemberValue(MEMBER_NAME, amv);
        }

        return annotation;
    }
}
