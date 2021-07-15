@echo off
set JAVA_DEBUGGER="-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005"
set JAVA11_OPTS=--add-exports=java.base/jdk.internal.loader=ALL-UNNAMED --add-opens=java.base/jdk.internal.loader=ALL-UNNAMED

rem #### Streams config ####
set LIB_PATH="D:\JAVA\PROJECTS\Nastel\jKoolLLC\tnt4j-streams\build\tnt4j-streams-1.11.7"
set BUILD_PATH="D:\JAVA\PROJECTS\Nastel\jKoolLLC\tnt4j-streams\tnt4j-streams-core\target"
set CLASS_PATH="%LIB_PATH%\lib\*;prod\lib\*"
rem ########################

rem #### AnnProcessor config ####
set BUILD_PATH="D:\JAVA\PROJECTS\Nastel\cybench\gocypher-cybench-junit\annProcessor\target"
set CLASS_PATH="prod\lib\*"
rem #############################

set AGENT_OPTS=-DbuildDir=%BUILD_PATH%
rem set AGENT_OPTS=-DbuildDir=%BUILD_PATH% -DtestDir=%BUILD_PATH%\test-classes  -DbenchDir=%BUILD_PATH%\t2b

set JAVA_HOME="D:\JAVA\jdk180"
set JAVA_DEBUGGER=
set JAVA11_OPTS=

:do
    cls
    echo Choose action:
    echo 1. Compile Tests to Benchmarks
    echo 2. Run benchmarks using Cybench runner
    echo 3. Run benchmarks using JMH runner
    echo 9. Exit

     set /p yn= Type a number :
        if [%yn%] == [1] (
            rem ### Compile Tests to benchmarks ###
            %JAVA_HOME%\bin\java %JAVA_DEBUGGER% %JAVA11_OPTS% -javaagent:prod/lib/benchmarkTestAgent.jar -cp %CLASS_PATH% %AGENT_OPTS% com.gocypher.cybench.BenchmarkTest
            goto done
            )
        if [%yn%] == [2] (
            for /f "delims== tokens=1,2" %%G in (.benchRunProps) do set %%G=%%H

            rem ### Run benchmarks using CyBench ###
            %JAVA_HOME%\bin\java %JAVA_DEBUGGER% %JAVA11_OPTS% -cp %RUN_CLASS_PATH% com.gocypher.cybench.launcher.BenchmarkRunner cfg=src/main/resources/cybench-launcher.properties
            goto done
            )
        if [%yn%] == [3] (
            for /f "delims== tokens=1,2" %%G in (.benchRunProps) do set %%G=%%H
            
            rem ### Run benchmarks using JMH Runner ###
            %JAVA_HOME%\bin\java %JAVA_DEBUGGER% %JAVA11_OPTS% -cp %RUN_CLASS_PATH% org.openjdk.jmh.Main -f 1 -w 5s -wi 0 -i 1 -r 5s -t 1 -bm Throughput
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
