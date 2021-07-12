package org.openjdk.jmh.generators.core;

import org.junit.jupiter.api.Test;

public class TestScopeBenchmarkGeneratorTest {

    @Test
    public void getAndRunPrivateMethod() throws Exception {
        TestScopeBenchmarkGenerator gen = new TestScopeBenchmarkGenerator();
        A a = new A();
        System.out.println(gen._getAndRunPrivateMethod(a, "privMethod", "a"));
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
