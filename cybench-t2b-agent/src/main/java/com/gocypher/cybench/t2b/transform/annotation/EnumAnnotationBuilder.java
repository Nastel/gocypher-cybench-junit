package com.gocypher.cybench.t2b.transform.annotation;

import java.util.Map;

import org.apache.commons.math3.util.Pair;

import javassist.bytecode.ConstPool;
import javassist.bytecode.annotation.EnumMemberValue;

public class EnumAnnotationBuilder extends AbstractAnnotationBuilder<Map<String, Pair<String, String>>> {
    public EnumAnnotationBuilder(String annotationName) {
        super(annotationName);
    }

    public EnumAnnotationBuilder(String annotationName, Map<String, Pair<String, String>> members) {
        super(annotationName, members);
    }

    @Override
    javassist.bytecode.annotation.Annotation buildAnnotation(String annotationName, ConstPool constPool,
            Map<String, Pair<String, String>> membersMap) {
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

        return annotation;
    }
}