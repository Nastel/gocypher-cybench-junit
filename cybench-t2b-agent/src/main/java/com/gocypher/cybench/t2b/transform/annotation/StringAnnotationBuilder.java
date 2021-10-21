package com.gocypher.cybench.t2b.transform.annotation;

import java.util.Map;

import javassist.bytecode.ConstPool;
import javassist.bytecode.annotation.StringMemberValue;

public class StringAnnotationBuilder extends AbstractAnnotationBuilder<Map<String, String>> {

    public StringAnnotationBuilder(String annotationType) {
        super(annotationType);
    }

    public StringAnnotationBuilder(String annotationType, Map<String, String> members) {
        super(annotationType, members);
    }

    @Override
    javassist.bytecode.annotation.Annotation buildAnnotation(String annotationType, ConstPool constPool,
            Map<String, String> membersMap) {
        javassist.bytecode.annotation.Annotation annotation = new javassist.bytecode.annotation.Annotation(
                annotationType, constPool);
        if (membersMap != null) {
            for (Map.Entry<String, String> me : membersMap.entrySet()) {
                StringMemberValue smv = new StringMemberValue(constPool);
                smv.setValue(me.getValue());
                annotation.addMemberValue(me.getKey(), smv);
            }
        }

        return annotation;
    }
}
