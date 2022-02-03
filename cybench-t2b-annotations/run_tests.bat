@echo off

rem ####################################################################
rem # Arguments:                                                       #
rem ####################################################################
rem #  junit4                               # to run JUnit 4 tests     #
rem #  testng                               # to run TestNG tests      #
rem #  junit5 (also works as fallback case) # to run JUnit 5/4 tests   #
rem ####################################################################

set "RUNDIR=%~dp0"

rem set "JAVA_HOME=c:\java\jdk_180"

set "JAVA_EXEC=java"
IF ["%JAVA_HOME%"] EQU [""] (
  echo "JAVA_HOME" env. variable is not defined!..
) else (
  echo Will use java from: "%JAVA_HOME%"
  set "JAVA_EXEC=%JAVA_HOME%\bin\java"
)

set "LIBS_DIR=%RUNDIR%\libs"
rem ### Gradle
set "BUILD_DIR=%RUNDIR%\build"
set "CLASS_PATH=%LIBS_DIR%\*;%BUILD_DIR%\classes\java\test"
rem ### Maven
rem set "BUILD_DIR=%RUNDIR%\target"
rem set "CLASS_PATH=%LIBS_DIR%\*;%BUILD_DIR%\test-classes"

set AGENT_OPTS="-Dt2b.aop.cfg.path=t2b\t2b.properties" "-Dt2b.metadata.cfg.path=t2b\metadata.properties"
rem ### To use custom LOG4J configuration
rem set AGENT_OPTS=%AGENT_OPTS% "-Dlog4j2.configurationFile="file:t2b\log4j2.xml"

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
  rem JUNIT5/4
  set MAIN_CLASS=org.junit.platform.console.ConsoleLauncher
  set TEST_ARGS=--scan-class-path
  rem ### In case you dont want to run JUnit4 tests, exclude vintage engine
  rem set TEST_ARGS=--scan-class-path -E=junit-vintage
)

rem set JAVA_DEBUGGER="-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005"
set JAVA_DEBUGGER=
@echo on
%JAVA_EXEC% %JAVA_DEBUGGER% -javaagent:"%LIBS_DIR%\cybench-t2b-agent-1.0.8-SNAPSHOT.jar" %AGENT_OPTS% -cp %CLASS_PATH% %MAIN_CLASS% %TEST_ARGS%
