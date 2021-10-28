@echo off

rem ####################################################################
rem # Arguments:                                                       #
rem ####################################################################
rem #  junit4                               # to run JUnit 4 tests     #
rem #  testng                               # to run TestNG tests      #
rem #  junit5 (also works as fallback case) # to run JUnit 5 tests     #
rem ####################################################################

set JAVA_HOME=d:\java\jdk-11

set JAVA_EXEC="java"
IF ["%JAVA_HOME%"] EQU [""] (
  echo "JAVA_HOME" env. variable is not defined!..
) else (
  echo Will use java from: "%JAVA_HOME%"
  set JAVA_EXEC="%JAVA_HOME%\bin\java"
)

set BUILD_DIR=".\build"
set LIBS_DIR=".\libs"
set CLASS_PATH="%LIBS_DIR%\*;%BUILD_DIR%\classes\java\test"

set AGENT_OPTS=-Dt2b.aop.cfg.path=t2b/t2b.properties -Dt2b.metadata.cfg.path=t2b/metadata.properties

set UNIT_FRAMEWORK=%1

IF /I ["%UNIT_FRAMEWORK%"] EQU ["junit4"] (
  rem JUNIT4
  set MAIN_CLASS=org.junit.runner.JUnitCore
  set TEST_ARGS=org.openjdk.jmh.generators.core.TestScopeBenchmarkGeneratorTestJU4
) ELSE IF /I ["%UNIT_FRAMEWORK%"] EQU ["testng"] (
  rem TESTNG
  set MAIN_CLASS=org.testng.TestNG
  set TEST_ARGS=-testclass org.openjdk.jmh.generators.core.TestScopeBenchmarkGeneratorTestNG
) ELSE (
  rem JUNIT5
  set MAIN_CLASS=org.junit.platform.console.ConsoleLauncher
  set TEST_ARGS=--scan-class-path
)

rem set JAVA_DEBUGGER="-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005"
set JAVA_DEBUGGER=
@echo on
%JAVA_EXEC% %JAVA_DEBUGGER% -javaagent:"%LIBS_DIR%\aspectjweaver-1.9.7.jar" %AGENT_OPTS% -cp %CLASS_PATH% %MAIN_CLASS% %TEST_ARGS%
