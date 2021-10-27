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

import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;

public class TestScopeBenchmarkGeneratorTestNG {

    @org.testng.annotations.BeforeTest
    public void setupTest() {

    }

    @org.testng.annotations.AfterTest
    public void teardownTest() {

    }

    @org.testng.annotations.Test
    public void getAndRunPrivateMethod() throws Exception {
        A a = new A();
        System.out.println(TestScopeBenchmarkGenerator._getAndRunPrivateMethod(a, "privMethod", "a"));
    }

    @org.testng.annotations.Test
    @org.testng.annotations.Ignore
    public void disabledTest() throws Exception {
    }

    @org.testng.annotations.Test
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
