#!/bin/bash

JAVA_HOME="D:\JAVA\jdk180"
export JAVA_DEBUGGER="-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005"
export JAVA_DEBUGGER=""

#### Your project config ####
BUILD_PATH="D:\JAVA\PROJECTS\Nastel\cybench\gocypher-cybench-junit\annProcessor\target"
#### !!! DO not forget to add your app libs to class path !!!
CLASS_PATH="prod\lib\*"
#############################

### Define your project build dir
AGENT_OPTS="-Dt2b.buildDir=$BUILD_PATH"
### Define dir where compiled tests are
#AGENT_OPTS="$AGENT_OPTS -Dt2b.testDir=$BUILD_PATH\test-classes"
### Define dir where to place generated benchmarks
#AGENT_OPTS="$AGENT_OPTS -Dt2b.benchDir=$BUILD_PATH\t2b"
### Set JDK to compile generated benchmark classes in case JAVA_HOME refers JRE. Use same level version (e.g. 8, 11, 15) JDK as runner java defined for JAVA_HOME prop.
#AGENT_OPTS=$AGENT_OPTS$ -Dt2b.jdkHome="C:\Program Files\Java\jdk-13.0.2"

jver=$("${JAVA_PATH}" -fullversion 2>&1 | cut -d'"' -f2 | sed '/^1\./s///' | cut -d'.' -f1 | cut -d'-' -f1 | cut -d'+' -f1 | cut -d'_' -f1)

if [[ $jver -ge 9 ]]; then
  JAVA9_OPTS="--add-exports=java.base/jdk.internal.loader=ALL-UNNAMED --add-opens=java.base/jdk.internal.loader=ALL-UNNAMED"
fi

while true; do
    clear
    echo "Choose action:"
    echo "1. Compile Tests to Benchmarks"
    echo "2. Run benchmarks using Cybench runner"
    echo "3. Run benchmarks using JMH runner"
    echo "9. Exit"

    while true; do
        read -p "Type a number: " yn
        case $yn in
        1 ) $JAVA_HOME/bin/java $JAVA_DEBUGGER $JAVA9_OPTS -javaagent:prod/lib/cybench-t2b-agent-1.0-SNAPSHOT.jar -cp $CLASS_PATH $AGENT_OPTS com.gocypher.cybench.Test2Benchmark;
            break;;
        2 ) source ../.benchRunProps;
            $JAVA_HOME/bin/java $JAVA_DEBUGGER $JAVA9_OPTS -cp $RUN_CLASS_PATH com.gocypher.cybench.launcher.BenchmarkRunner cfg=config/cybench-launcher.properties;
            break;;
        3 ) source ../.benchRunProps;
            $JAVA_HOME/bin/java $JAVA_DEBUGGER $JAVA9_OPTS -cp $RUN_CLASS_PATH org.openjdk.jmh.Main -f 1 -w 5s -wi 0 -i 1 -r 5s -t 1 -bm Throughput;
            break;;
        9 ) exit; break;;

        * ) echo "Please select a number";;
        esac
    done
    read -p "Press any key to continue... " -n1 -s
done