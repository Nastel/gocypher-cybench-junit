export JAVA_DEBUGGER="-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005"
java -javaagent:prod/lib/benchmarkTestAgent.jar -cp "prod/lib/*:prod:build/classes/java/test" BenchmarkTest
