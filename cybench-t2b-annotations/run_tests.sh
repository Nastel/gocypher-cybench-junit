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

####################################################################
# Arguments:                                                       #
####################################################################
#  junit4                               # to run JUnit 4 tests     #
#  testng                               # to run TestNG tests      #
#  junit5 (also works as fallback case) # to run JUnit 5/4 tests   #
####################################################################

#JAVA_HOME="/usr/local/java/jdk"

JAVA_EXEC="java"
if [[ "$JAVA_HOME" == "" ]]; then
  echo '"JAVA_HOME" env. variable is not defined!..'
else
  echo 'Will use java from:' "$JAVA_HOME"
  JAVA_EXEC="$JAVA_HOME/bin/java"
fi

LIBS_DIR="./libs"
### Gradle
BUILD_DIR="./build"
CLASS_PATH="$LIBS_DIR/*:$BUILD_DIR/classes/java/test"
### Maven
#BUILD_DIR="./target"
#CLASS_PATH="$LIBS_DIR/*:$BUILD_DIR/test-classes"

AGENT_OPTS="-Dt2b.aop.cfg.path=./t2b/t2b.properties -Dt2b.metadata.cfg.path=./t2b/metadata.properties"
### To use custom LOG4J configuration
#AGENT_OPTS="$AGENT_OPTS -Dlog4j2.configurationFile="file:./t2b/log4j2.xml"

UNIT_FRAMEWORK=$1

if [[ "${UNIT_FRAMEWORK,,}" == "junit4" ]]; then
  #JUNIT4
  MAIN_CLASS="org.junit.runner.JUnitCore"
  TEST_ARGS="org.openjdk.jmh.generators.core.TestScopeBenchmarkGeneratorTestJU4"
elif [[ "${UNIT_FRAMEWORK,,}" == "testng" ]]; then
  #TESTNG
  MAIN_CLASS="org.testng.TestNG"
  TEST_ARGS="-testclass org.openjdk.jmh.generators.core.TestScopeBenchmarkGeneratorTestNG"
else
  #JUNIT5/4
  MAIN_CLASS="org.junit.platform.console.ConsoleLauncher"
  TEST_ARGS="--scan-class-path"
  ### In case you dont want to run JUnit4 tests, exclude vintage engine
  #TEST_ARGS="--scan-class-path -E=junit-vintage"
fi

#JAVA_DEBUGGER="-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005"
JAVA_DEBUGGER=

"$JAVA_EXEC" $JAVA_DEBUGGER -javaagent:"$LIBS_DIR"/cybench-t2b-agent-1.0.7-SNAPSHOT.jar $AGENT_OPTS -cp "$CLASS_PATH" $MAIN_CLASS "$TEST_ARGS"
