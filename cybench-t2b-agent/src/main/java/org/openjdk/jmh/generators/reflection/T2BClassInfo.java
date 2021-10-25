
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

// https://openjdk.java.net/projects/code-tools/jmh/
// https://github.com/openjdk/jmh
// http://javadox.com/org.openjdk.jmh/jmh-core/1.32/org/openjdk/jmh
// https://www.baeldung.com/java-microbenchmark-harness
package org.openjdk.jmh.generators.reflection;

public class T2BClassInfo extends RFClassInfo {

    public T2BClassInfo(Class<?> klass) {
        super(klass);
    }
}
