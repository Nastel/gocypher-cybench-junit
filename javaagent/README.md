# gocypher-cybench-junit


## Javaagent implementation

* Step 1
To run maven agent from maven edit POM first:

```
            <plugin>
                <artifactId>maven-dependency-plugin</artifactId>
                <version>3.0.0</version>
                <executions>
                    <execution>
                        <id>getClasspathFilenames</id>
                        <goals>
                            <goal>properties</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
            <plugin>
                <groupId>org.codehaus.mojo</groupId>
                <artifactId>exec-maven-plugin</artifactId>
                <version>3.0.0</version>
                <executions>
                    <execution>
                        <goals>
                            <goal>java</goal>
                        </goals>
                    </execution>
                </executions>
                <configuration>
                    <executable>java</executable>
                    <classpathScope>test</classpathScope>
                    <mainClass>com.gocypher.cybench.BenchmarkTest</mainClass>
                    <arguments combine.self="override">
                        <argument>-javaagent:${com.gocypher:testToBenchmarkAgent:jar}</argument>
                        <argument>-DbuildDir=${project.build.outputDirectory}</argument>
                        <argument>-cp</argument>
                        <classpath/>
                        <argument>com.gocypher.cybench.BenchmarkTest</argument>
                    </arguments>

                </configuration>
            </plugin>
			
```


* Step 2
```
mvn initialize test-compile exec:exec 
```
note:
initialize = set variable ${com.gocypher:testToBenchmarkAgent:jar} using maven-dependency-plugin
test-compile = you need to compile tests
exec:exec = and run the command

## DEVNotes (a.k.a TODO)

delete temp file for javac - com.gocypher.cybench.CompileProcess.WindowsCompileProcess:56
set the classpath to `javac` - com.gocypher.cybench.CompileProcess.WindowsCompileProcess:39

```
mvndebug initialize test-compile exec:exec 
```
this command will let you debug the maven process, note - you cannot set the breakpoint on instrumented class 







