export JAVA_DEBUGGER="-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005"
java -javaagent:prod/lib/cybench-t2b-agent.jar -cp "prod/lib/*:prod:build/classes/java/test" com.gocypher.cybench.Test2Benchmark
