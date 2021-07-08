@echo off
set JAVA_DEBUGGER="-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005"
set JAVA11_OPTS=--add-exports=java.base/jdk.internal.loader=ALL-UNNAMED --add-opens=java.base/jdk.internal.loader=ALL-UNNAMED

rem #### Streams config ####
set LIB_PATH="D:\JAVA\PROJECTS\Nastel\jKoolLLC\tnt4j-streams\build\tnt4j-streams-1.11.7"
set TEST_PATH="D:\JAVA\PROJECTS\Nastel\jKoolLLC\tnt4j-streams\tnt4j-streams-core"
set CLASS_PATH="%LIB_PATH%\lib\*;%LIB_PATH%\*;prod\lib\*;%TEST_PATH%\target\test-classes;%TEST_PATH%\target\classes;%TEST_PATH%\target\t2b"
set AGENT_OPTS=-DbuildDir=%TEST_PATH%\target
rem set AGENT_OPTS=-DbuildDir=%TEST_PATH%\target -DtestDir=%TEST_PATH%\target\test-classes
rem ########################

rem #### AnnProcessor config ####
rem set TEST_PATH="D:\JAVA\PROJECTS\Nastel\cybench\gocypher-cybench-junit\annProcessor"
rem set CLASS_PATH="prod\lib\*;%TEST_PATH%\target\test-classes;%TEST_PATH%\target\classes;%TEST_PATH%\target\t2b"
rem set AGENT_OPTS=-DbuildDir=%TEST_PATH%\target
rem #############################

set JAVA_HOME="D:\JAVA\jdk180"
set JAVA_DEBUGGER=
set JAVA11_OPTS=

@echo on
%JAVA_HOME%\bin\java %JAVA_DEBUGGER% %JAVA11_OPTS% -javaagent:prod/lib/benchmarkTestAgent.jar -cp %CLASS_PATH% %AGENT_OPTS% com.gocypher.cybench.BenchmarkTest
