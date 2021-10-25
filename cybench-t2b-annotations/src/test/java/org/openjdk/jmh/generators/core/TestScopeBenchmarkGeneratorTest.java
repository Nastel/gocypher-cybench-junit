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

package org.openjdk.jmh.generators.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;

public class TestScopeBenchmarkGeneratorTest {

    @BeforeEach
    public void setupTest() {

    }

    @AfterEach
    public void teardownTest() {

    }

    @Test
    public void getAndRunPrivateMethod() throws Exception {
        A a = new A();
        System.out.println(TestScopeBenchmarkGenerator._getAndRunPrivateMethod(a, "privMethod", "a"));
    }

    @Test
    @Disabled
    public void disabledTest() throws Exception {
    }

    @Test
    public void setProcessingEnvTest() throws Exception {
        TestScopeBenchmarkGenerator tbg = new TestScopeBenchmarkGenerator();
        tbg.setProcessingEnv(JavacProcessingEnvironment.instance(new Context()));
    }

    public static class A extends B {

    }

    private static class B {
        private String privMethod(String a) {
            System.out.println("ABC");
            return "DEF" + a;
        }
    }
}
