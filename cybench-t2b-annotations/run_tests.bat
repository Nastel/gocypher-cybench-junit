@echo off
set JAVA_HOME=d:\java\jdk-11
rem JUNIT4
rem %JAVA_HOME%/bin/java -javaagent:"D:\JAVA\PROJECTS\Nastel\cybench\gocypher-cybench-junit\cybench-t2b-agent\libs\cybench-t2b-agent-1.0.5-SNAPSHOT.jar" -cp D:\JAVA\PROJECTS\Nastel\cybench\gocypher-cybench-junit\cybench-t2b-agent\libs\*;D:\JAVA\PROJECTS\Nastel\cybench\gocypher-cybench-junit\cybench-t2b-annotations\target\*;D:\JAVA\PROJECTS\Nastel\cybench\gocypher-cybench-junit\cybench-t2b-annotations\target\test-classes org.junit.runner.JUnitCore org.openjdk.jmh.generators.core.TestScopeBenchmarkGeneratorTest
rem JUNIT5
rem %JAVA_HOME%/bin/java -javaagent:"D:\JAVA\PROJECTS\Nastel\cybench\gocypher-cybench-junit\cybench-t2b-agent\libs\cybench-t2b-agent-1.0.5-SNAPSHOT.jar" -cp D:\JAVA\PROJECTS\Nastel\cybench\gocypher-cybench-junit\cybench-t2b-agent\libs\*;D:\JAVA\PROJECTS\Nastel\cybench\gocypher-cybench-junit\cybench-t2b-annotations\target\*;D:\JAVA\PROJECTS\Nastel\cybench\gocypher-cybench-junit\cybench-t2b-annotations\*;D:\JAVA\PROJECTS\Nastel\cybench\gocypher-cybench-junit\cybench-t2b-annotations\target\test-classes org.junit.platform.console.ConsoleLauncher -c org.openjdk.jmh.generators.core.TestScopeBenchmarkGeneratorTest
rem JUNIT5 AspectJ

rem set JAVA_DEBUGGER="-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005"
set JAVA_DEBUGGER=
@echo on
%JAVA_HOME%/bin/java %JAVA_DEBUGGER% -javaagent:"D:\JAVA\PROJECTS\Nastel\cybench\gocypher-cybench-junit\cybench-t2b-agent\libs\aspectjweaver-1.9.7.jar" -Dt2b.aop.cfg.path=t2b/t2b.properties -Dt2b.metadata.cfg.path=t2b/metadata.properties -cp D:\JAVA\PROJECTS\Nastel\cybench\gocypher-cybench-junit\cybench-t2b-agent\libs\*;D:\JAVA\PROJECTS\Nastel\cybench\gocypher-cybench-junit\cybench-t2b-annotations\target\*;D:\JAVA\PROJECTS\Nastel\cybench\gocypher-cybench-junit\cybench-t2b-annotations\*;D:\JAVA\PROJECTS\Nastel\cybench\gocypher-cybench-junit\cybench-t2b-annotations\target\test-classes org.junit.platform.console.ConsoleLauncher -c org.openjdk.jmh.generators.core.TestScopeBenchmarkGeneratorTest
