dir /s /B prod\*.java > sources.txt
javac -cp c:\workspace\tnt4j-streams2\build\tnt4j-streams-1.12.0-SNAPSHOT\lib\*;c:\workspace\tnt4j-streams2\build\tnt4j-streams-1.12.0-SNAPSHOT\;c:\workspace\tnt4j-streams2\tnt4j-streams-core\target\test-classes\;prod\lib\*;build/classes/java/test @sources.txt
rm sources.txt


rem c -cp c:\workspace\tnt4j-streams2\build\tnt4j-streams-1.12.0-SNAPSHOT\lib\*;c:\workspace\tnt4j-streams2\build\tnt4j-streams-1.12.0-SNAPSHOT\;c:\workspace\tnt4j-streams2\tnt4j-streams-core\target\test-classes\;prod\lib\*;build/classes/java/test prod\com\company\jmh_generated\*.java
