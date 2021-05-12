set JAVA_DEBUGGER="-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005"
java -javaagent:prod/lib/benchmarkTestAgent.jar -cp c:\workspace\tnt4j-streams2\build\tnt4j-streams-1.12.0-SNAPSHOT\lib\*;c:\workspace\tnt4j-streams2\build\tnt4j-streams-1.12.0-SNAPSHOT\;c:\workspace\tnt4j-streams2\tnt4j-streams-core\target\test-classes\;prod\lib\*;prod;build\classes\java\test BenchmarkTest
