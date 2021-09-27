package com.gocypher.cybench.t2b.transform.annotation;

import javassist.bytecode.ConstPool;

public interface AnnotationArrayBuilder {
    public javassist.bytecode.annotation.Annotation[] buildAnnotations(ConstPool constPool);
}
