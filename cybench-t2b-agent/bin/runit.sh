#!/bin/bash

if command -v realpath >/dev/null 2>&1; then
    SCRIPTPATH=`dirname $(realpath $0)`
else
    SCRIPTPATH=$( cd "$(dirname "$0")" ; pwd -P )
fi

#JAVA_HOME="/usr/local/java/jdk"
read -e -p "Enter your Java Home dir path: " -i "$JAVA_HOME" JAVA_HOME

#### Your project config ####
BUILD_PATH="$HOME/dev/my/project"
read -e -p "Enter your project build dir path: " -i "$BUILD_PATH" BUILD_PATH
#### !!! DO not forget to add your app libs to class path !!!
CLASS_PATH="$SCRIPTPATH/../libs/*"
read -e -p "Enter class path to use: " -i "$CLASS_PATH" CLASS_PATH
#############################

### Define your project build dir
AGENT_OPTS="-Dt2b.build.dir=$BUILD_PATH"
### Define dir where compiled tests are
#AGENT_OPTS="$AGENT_OPTS -Dt2b.test.dir=$BUILD_PATH/test-classes"
### Define dir where to place generated benchmarks
#AGENT_OPTS="$AGENT_OPTS -Dt2b.bench.dir=$BUILD_PATH/t2b"
### Set JDK to compile generated benchmark classes in case JAVA_HOME refers JRE. Use same level version (e.g. 8, 11, 15) JDK as runner java defined for JAVA_HOME prop.
#AGENT_OPTS="$AGENT_OPTS -Dt2b.jdk.home=C:/Program Files/Java/jdk-13.0.2"
read -e -p "Enter agent options: " -i "$AGENT_OPTS" AGENT_OPTS

jver=$("${JAVA_PATH}" -fullversion 2>&1 | cut -d'"' -f2 | sed '/^1\./s///' | cut -d'.' -f1 | cut -d'-' -f1 | cut -d'+' -f1 | cut -d'_' -f1)

if [[ $jver -ge 9 ]]; then
  JAVA9_OPTS="--add-exports=java.base/jdk.internal.loader=ALL-UNNAMED --add-opens=java.base/jdk.internal.loader=ALL-UNNAMED"
fi

while true; do
    source "$SCRIPTPATH"/.benchRunProps;

    clear
    echo "-----------------------------"
    echo "SETTINGS:"
    echo " Script Home Dir: $SCRIPTPATH"
    echo "       Java Home: $JAVA_HOME"
    echo "      Class Path: $CLASS_PATH"
    echo "  Build Dir Path: $BUILD_PATH"
    echo "   Agent Options: $AGENT_OPTS"
    echo "  Java9+ Options: $JAVA9_OPTS"
    echo "  Benchmarks Dir: $BENCH_DIR"
    echo "  T2B Class Path: $T2B_CLASS_PATH"
    echo "-----------------------------"
    echo "Choose action:"
    echo "1. Compile Tests to Benchmarks"
    echo "2. Run benchmarks using Cybench runner"
    echo "3. Run benchmarks using JMH runner"
    echo "9. Exit"
    echo "-----------------------------"

    while true; do
        read -p "Type a number: " yn
        case $yn in
        1 ) "$JAVA_HOME"/bin/java $JAVA9_OPTS -javaagent:"$SCRIPTPATH"/../libs/cybench-t2b-agent-1.0.3-SNAPSHOT.jar -cp "$CLASS_PATH" "$AGENT_OPTS" com.gocypher.cybench.Test2Benchmark;
            break;;
        2 ) "$JAVA_HOME"/bin/java $JAVA9_OPTS -cp "$CLASS_PATH":"$T2B_CLASS_PATH" com.gocypher.cybench.launcher.BenchmarkRunner cfg="$SCRIPTPATH"/../config/cybench-launcher.properties;
            break;;
        3 ) "$JAVA_HOME"/bin/java $JAVA9_OPTS -cp "$CLASS_PATH":"$T2B_CLASS_PATH" org.openjdk.jmh.Main -f 1 -w 5s -wi 0 -i 1 -r 5s -t 1 -bm Throughput;
            break;;
        9 ) exit; break;;

        * ) echo "Please select a number";;
        esac
    done
    read -p "Press any key to continue... " -n1 -s
done