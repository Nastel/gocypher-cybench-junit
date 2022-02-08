/*
 * Copyright (C) 2020-2022, K2N.IO.
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
