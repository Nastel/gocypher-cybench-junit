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
