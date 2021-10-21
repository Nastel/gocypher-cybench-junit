package com.gocypher.cybench.t2b.aop;

import java.lang.reflect.Method;

import org.aspectj.lang.ProceedingJoinPoint;

public interface TestJoinPointHandler {

    public void runTestAsBenchmark(Method testMethod, ProceedingJoinPoint testPoint);
}
