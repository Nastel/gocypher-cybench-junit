@echo off
setlocal enabledelayedexpansion

set RUNDIR=%~dp0

rem set JAVA_HOME="c:\java\jdk_180"
set /p JAVA_HOME= Enter your Java Home dir path: [%JAVA_HOME%] :

rem #### Your project config ####
set BUILD_PATH="c:\projects\my"
set /p BUILD_PATH= Enter your project build dir path: [%BUILD_PATH%] :
rem !!! DO not forget to add your app libs to class path !!!
set CLASS_PATH="%RUNDIR%..\libs\*"
set /p CLASS_PATH= Enter class path to use: [%CLASS_PATH%] :
rem #############################

rem ### Define your project build dir
set AGENT_OPTS="-Dt2b.build.dir=%BUILD_PATH%"
rem ### Define dir where compiled tests are
rem set AGENT_OPTS="%AGENT_OPTS% -Dt2b.test.dir=%BUILD_PATH%\test-classes"
rem ### Define dir where to place generated benchmarks
rem set AGENT_OPTS="%AGENT_OPTS% -Dt2b.bench.dir=%BUILD_PATH%\t2b"
rem ### Set JDK to compile generated benchmark classes in case JAVA_HOME refers JRE. Use same level version (e.g. 8, 11, 15) JDK as runner java defined for JAVA_HOME prop.
rem set AGENT_OPTS="%AGENT_OPTS% -Dt2b.jdk.home="C:\Program Files\Java\jdk-13.0.2"
set /p AGENT_OPTS= Enter agent options: [%AGENT_OPTS%] :

for /f tokens^=2-5^ delims^=.+-_^" %%j in ('%JAVA_HOME%\bin\java -fullversion 2^>^&1') do set "jver=%%j%%k"
rem for early access versions replace "ea" part with "00" to get comparable number
set jver=%jver:ea=00%

IF %jver% GTR 18 set JAVA9_OPTS="--add-exports=java.base/jdk.internal.loader=ALL-UNNAMED" "--add-opens=java.base/jdk.internal.loader=ALL-UNNAMED"


:do
    for /f "delims== tokens=1,2" %%G in (%RUNDIR%.benchRunProps) do set %%G=%%H

    cls
    echo -----------------------------
    echo SETTINGS:
    echo  Script Home Dir: %RUNDIR%
    echo        Java Home: %JAVA_HOME%
    echo       Class Path: %CLASS_PATH%
    echo   Build Dir Path: %BUILD_PATH%
    echo    Agent Options: %AGENT_OPTS%
    echo   Java9+ Options: %JAVA9_OPTS%
    echo   Benchmarks Dir: %BENCH_DIR%
    echo   T2B Class Path: %T2B_CLASS_PATH%
    echo -----------------------------
    echo Choose action:
    echo 1. Compile Tests to Benchmarks
    echo 2. Run benchmarks using Cybench runner
    echo 3. Run benchmarks using JMH runner
    echo 9. Exit
    echo -----------------------------

    set /p yn= Type a number :
        if [%yn%] == [1] (
            rem ### Compile Tests to benchmarks ###
            %JAVA_HOME%\bin\java %JAVA9_OPTS% -javaagent:"%RUNDIR%..\libs\cybench-t2b-agent-1.0.2-SNAPSHOT.jar" -cp %CLASS_PATH% %AGENT_OPTS% com.gocypher.cybench.Test2Benchmark
            goto done
            )
        if [%yn%] == [2] (
rem            for /f "delims== tokens=1,2" %%G in (%RUNDIR%.benchRunProps) do set %%G=%%H

            rem ### Run benchmarks using CyBench ###
            %JAVA_HOME%\bin\java %JAVA9_OPTS% -cp %CLASS_PATH%;!T2B_CLASS_PATH! com.gocypher.cybench.launcher.BenchmarkRunner cfg="%RUNDIR%..\config\cybench-launcher.properties"
            goto done
            )
        if [%yn%] == [3] (
rem            for /f "delims== tokens=1,2" %%G in (%RUNDIR%.benchRunProps) do set %%G=%%H

            rem ### Run benchmarks using JMH Runner ###
            %JAVA_HOME%\bin\java %JAVA9_OPTS% -cp %CLASS_PATH%;!T2B_CLASS_PATH! org.openjdk.jmh.Main -f 1 -w 5s -wi 0 -i 1 -r 5s -t 1 -bm Throughput
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
