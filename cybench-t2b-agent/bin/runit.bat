@echo off
setlocal enabledelayedexpansion

set "RUNDIR=%~dp0"

rem set "JAVA_HOME=c:\java\jdk_180"
set /p JAVA_HOME= Enter your Java Home dir path: [%JAVA_HOME%] :

set JAVA_EXEC="java"
IF ["%JAVA_HOME%"] EQU [""] (
  echo "JAVA_HOME" env. variable is not defined!..
) else (
  echo Will use java from: "%JAVA_HOME%"
  set "JAVA_EXEC=%JAVA_HOME%\bin\java"
)

rem #### Your project config ####
set "PROJECT_DIR=c:\projects\my"
set /p PROJECT_DIR= Enter your project root dir path: [%PROJECT_DIR%] :
rem !!! DO not forget to add your app libs to class path !!!
set "LIBS_DIR=%PROJECT_DIR%\libs"
rem ### Gradle
set "BUILD_DIR=%PROJECT_DIR%\build"
set "CLASS_PATH=%LIBS_DIR%\*;%BUILD_DIR%\classes\java\test"
rem ### Maven
rem set "BUILD_DIR=%PROJECT_DIR%\target"
rem set "CLASS_PATH=%LIBS_DIR%\*;%BUILD_DIR%\test-classes"
set /p CLASS_PATH= Enter class path to use: [%CLASS_PATH%] :
set "CFG_DIR=%PROJECT_DIR%\config"
set /p CFG_DIR= Enter configurations dir path: [%CFG_DIR%] :
rem #############################

rem ### Define configuration files to use
set AGENT_OPTS="-Dt2b.aop.cfg.path=%CFG_DIR%\t2b.properties" "-Dt2b.metadata.cfg.path=%CFG_DIR%\metadata.properties"
rem ### To use custom LOG4J configuration
rem set AGENT_OPTS=%AGENT_OPTS% "-Dlog4j2.configurationFile=file:%CFG_DIR%\t2b\log4j2.xml"
set /p AGENT_OPTS= Enter agent options: [%AGENT_OPTS%] :

for /f tokens^=2-5^ delims^=.+-_^" %%j in ('%JAVA_EXEC% -fullversion 2^>^&1') do set "jver=%%j%%k"
rem for early access versions replace "ea" part with "00" to get comparable number
set jver=%jver:ea=00%

IF %jver% GTR 18 set JAVA9_OPTS="--add-exports=java.base/jdk.internal.loader=ALL-UNNAMED" "--add-opens=java.base/jdk.internal.loader=ALL-UNNAMED"

cd /D "%PROJECT_DIR%"

:do
    cls
    echo -----------------------------
    echo SETTINGS:
    echo        Script Home Dir: %RUNDIR%
    echo              Java Home: %JAVA_HOME%
    echo  Project Root Dir Path: %PROJECT_DIR%
    echo     Configurations Dir: %CFG_DIR%
    echo             Class Path: %CLASS_PATH%
    echo          Agent Options: %AGENT_OPTS%
    echo         Java9+ Options: %JAVA9_OPTS%
    echo -----------------------------
    echo Choose action:
    echo 1. Run JUnit5/4 tests as benchmarks
    echo 2. Run JUnit4 tests as benchmarks
    echo 3. Run TestNG tests as benchmarks
    echo 9. Exit
    echo -----------------------------

    set /p yn= Type a number :
        if [%yn%] == [1] (
            rem ### Run JUnit5/4 tests as benchmarks ###
            set TEST_ARGS=--scan-class-path -E=junit-vintage
            set /p TEST_ARGS= Enter JUnit5 tests arguments: [!TEST_ARGS!] :
            %JAVA_EXEC% %JAVA9_OPTS% -javaagent:"%LIBS_DIR%\cybench-t2b-agent-1.0.8-SNAPSHOT.jar" -cp %CLASS_PATH% %AGENT_OPTS% org.junit.platform.console.ConsoleLauncher !TEST_ARGS!
            goto done
            )
        if [%yn%] == [2] (
            rem ### Run JUnit4 tests as benchmarks ###
            set TEST_ARGS=org.openjdk.jmh.generators.core.TestScopeBenchmarkGeneratorTestJU4
            set /p TEST_ARGS= Enter JUnit4 tests arguments: [!TEST_ARGS!] :
            %JAVA_EXEC% %JAVA9_OPTS% -javaagent:"%LIBS_DIR%\cybench-t2b-agent-1.0.8-SNAPSHOT.jar" -cp %CLASS_PATH% %AGENT_OPTS% org.junit.runner.JUnitCore !TEST_ARGS!
            goto done
            )
        if [%yn%] == [3] (
            rem ### Run TestNG tests as benchmarks ###
            set TEST_ARGS=-testclass org.openjdk.jmh.generators.core.TestScopeBenchmarkGeneratorTestNG
            set /p TEST_ARGS= Enter TestNG tests arguments: [!TEST_ARGS!] :
            %JAVA_EXEC% %JAVA9_OPTS% -javaagent:"%LIBS_DIR%\cybench-t2b-agent-1.0.8-SNAPSHOT.jar" -cp %CLASS_PATH% %AGENT_OPTS% org.testng.TestNG !TEST_ARGS!
            goto done
            )
        if [%yn%] == [9] (
            goto exit
            )

        echo Please select a number
:done
        set /p yn=Press Enter to continue...

    goto do
:exit
