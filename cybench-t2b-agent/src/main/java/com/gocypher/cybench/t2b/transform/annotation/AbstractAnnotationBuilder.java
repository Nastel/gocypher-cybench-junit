/*
 * Copyright (C) 2020-2021, K2N.IO.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 *
 */

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
