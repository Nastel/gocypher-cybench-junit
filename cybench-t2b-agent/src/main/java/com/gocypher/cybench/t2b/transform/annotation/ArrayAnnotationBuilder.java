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

import java.util.ArrayList;

import javassist.bytecode.ConstPool;
import javassist.bytecode.annotation.AnnotationMemberValue;
import javassist.bytecode.annotation.ArrayMemberValue;
import javassist.bytecode.annotation.MemberValue;

public class ArrayAnnotationBuilder extends AbstractAnnotationBuilder<AnnotationArrayBuilder> {

    private static final String MEMBER_NAME = "value";
    private static final MemberValue[] EMPTY_ARRAY = new MemberValue[0];

    public ArrayAnnotationBuilder(String annotationType, AnnotationArrayBuilder membersBuilder) {
        super(annotationType, membersBuilder);
    }

    @Override
    javassist.bytecode.annotation.Annotation buildAnnotation(String annotationType, ConstPool constPool,
            AnnotationArrayBuilder membersBuilder) {
        javassist.bytecode.annotation.Annotation[] membersArray = membersBuilder.buildAnnotations(constPool);
        javassist.bytecode.annotation.Annotation annotation = new javassist.bytecode.annotation.Annotation(
                annotationType, constPool);
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
