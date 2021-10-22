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
