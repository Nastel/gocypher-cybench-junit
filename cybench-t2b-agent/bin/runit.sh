#!/bin/bash

#
# Copyright (C) 2020-2021, K2N.IO.
#
# This library is free software; you can redistribute it and/or
# modify it under the terms of the GNU Lesser General Public
# License as published by the Free Software Foundation; either
# version 2.1 of the License, or (at your option) any later version.
#
# This library is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
# Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public
# License along with this library; if not, write to the Free Software
# Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
#
#

if command -v realpath >/dev/null 2>&1; then
    SCRIPTPATH=`dirname $(realpath $0)`
else
    SCRIPTPATH=$( cd "$(dirname "$0")" ; pwd -P )
fi

#JAVA_HOME="/usr/local/java/jdk"
read -e -p "Enter your Java Home dir path: " -i "$JAVA_HOME" JAVA_HOME

JAVA_EXEC="java"
if [[ "$JAVA_HOME" == "" ]]; then
  echo '"JAVA_HOME" env. variable is not defined!..'
else
  echo 'Will use java from:' "$JAVA_HOME"
  JAVA_EXEC="$JAVA_HOME/bin/java"
fi

#### Your project config ####
PROJECT_DIR="$HOME/dev/my/project"
read -e -p "Enter your project root dir path: " -i "$PROJECT_DIR" PROJECT_DIR
#### !!! DO not forget to add your app libs to class path !!!
LIBS_DIR="$PROJECT_DIR/libs"
### Gradle
BUILD_DIR="$PROJECT_DIR/build"
CLASS_PATH="$LIBS_DIR/*:$BUILD_DIR/classes/java/test"
### Maven
#BUILD_DIR="$PROJECT_DIR/target"
#CLASS_PATH="$LIBS_DIR/*:$BUILD_DIR/test-classes"
read -e -p "Enter class path to use: " -i "$CLASS_PATH" CLASS_PATH
CFG_DIR="$PROJECT_DIR/config"
read -e -p "Enter configurations dir path: " -i "$CFG_DIR" CFG_DIR
#############################

### Define configuration files to use
AGENT_OPTS="-Dt2b.aop.cfg.path=$CFG_DIR/t2b.properties -Dt2b.metadata.cfg.path=$CFG_DIR/metadata.properties"
### To use custom LOG4J configuration
#AGENT_OPTS="$AGENT_OPTS -Dlog4j2.configurationFile=file:$CFG_DIR/t2b/log4j2.xml"
read -e -p "Enter agent options: " -i "$AGENT_OPTS" AGENT_OPTS

jver=$("${JAVA_PATH}" -fullversion 2>&1 | cut -d'"' -f2 | sed '/^1\./s///' | cut -d'.' -f1 | cut -d'-' -f1 | cut -d'+' -f1 | cut -d'_' -f1)

if [[ $jver -ge 9 ]]; then
  JAVA9_OPTS="--add-exports=java.base/jdk.internal.loader=ALL-UNNAMED --add-opens=java.base/jdk.internal.loader=ALL-UNNAMED"
fi

cd "$PROJECT_DIR"

while true; do

    clear
    echo "-----------------------------"
    echo "SETTINGS:"
    echo "       Script Home Dir: $SCRIPTPATH"
    echo "             Java Home: $JAVA_HOME"
    echo " Project Root Dir Path: $PROJECT_DIR"
    echo "    Configurations Dir: $CFG_DIR"
    echo "            Class Path: $CLASS_PATH"
    echo "         Agent Options: $AGENT_OPTS"
    echo "        Java9+ Options: $JAVA9_OPTS"
    echo "-----------------------------"
    echo "Choose action:"
    echo "1. Run JUnit5/4 tests as benchmarks"
    echo "2. Run JUnit4 tests as benchmarks"
    echo "3. Run TestNG tests as benchmarks"
    echo "9. Exit"
    echo "-----------------------------"

    while true; do
        read -p "Type a number: " yn
        case $yn in
        1 ) ### Run JUnit5/4 tests as benchmarks ###
            TEST_ARGS="--scan-class-path -E=junit-vintage";
            read -e -p "Enter JUnit5 tests arguments: " -i "$TEST_ARGS" TEST_ARGS;
            "$JAVA_EXEC" $JAVA9_OPTS -javaagent:"$LIBS_DIR"/cybench-t2b-agent-1.0.7-SNAPSHOT.jar -cp "$CLASS_PATH" $AGENT_OPTS "org.junit.platform.console.ConsoleLauncher" $TEST_ARGS;
            break;;
        2 ) ### Run JUnit4 tests as benchmarks ###
            TEST_ARGS="org.openjdk.jmh.generators.core.TestScopeBenchmarkGeneratorTestJU4";
            read -e -p "Enter JUnit4 tests arguments: " -i "$TEST_ARGS" TEST_ARGS;
            "$JAVA_EXEC" $JAVA9_OPTS -javaagent:"$LIBS_DIR"/cybench-t2b-agent-1.0.7-SNAPSHOT.jar -cp "$CLASS_PATH" $AGENT_OPTS "org.junit.runner.JUnitCore" $TEST_ARGS
            break;;
        3 ) ### Run TestNG tests as benchmarks ###
            TEST_ARGS="-testclass org.openjdk.jmh.generators.core.TestScopeBenchmarkGeneratorTestNG";
            read -e -p "Enter TestNG tests arguments: " -i "$TEST_ARGS" TEST_ARGS;
            "$JAVA_EXEC" $JAVA9_OPTS -javaagent:"$LIBS_DIR"/cybench-t2b-agent-1.0.7-SNAPSHOT.jar -cp "$CLASS_PATH" $AGENT_OPTS "org.testng.TestNG" $TEST_ARGS
            break;;
        9 ) exit; break;;

        * ) echo "Please select a number";;
        esac
    done
    read -p "Press any key to continue... " -n1 -s
done