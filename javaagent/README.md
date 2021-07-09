# gocypher-cybench-junit

## Java agent implementation

* Step 1: to run maven agent from maven, edit POM of your project first by adding these plugins and configurations:
```xml
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
                <!--<argument>-DtestDir=${project.build.testOutputDirectory}</argument>-->
                <!--<argument>-DbenchDir=${project.build.directory}/t2b</argument>-->
                <argument>-cp</argument>
                <classpath/>
                <argument>com.gocypher.cybench.BenchmarkTest</argument>
            </arguments>
        </configuration>
    </plugin>
```
* Step 2: execute command:
```cmd
mvn initialize test-compile exec:exec 
```
**Note:**
* `initialize` = set variable `${com.gocypher:testToBenchmarkAgent:jar}` using maven-dependency-plugin
* `test-compile` = you need to compile tests
* `exec:exec` = and run the command

## DEVNotes (a.k.a TODO)

* Add cybench annotations
* Support for `@BeforeXXXX`, `@AfterXXXXX`, `@TearDown`, and etc. test framework annotations
* Make gradle plugin
* Make maven plugin
* (KNOWN BUG) If test class has methods having same name just different casing, on Windows it creates file and class having different casing and benchmarks 
compile fails with:
  ```
  class MyTests_testUsecTimestamp_jmhTest is public, should be declared in a file named MyTests_testUsecTimestamp_jmhTest.java
  ```
  On linux it shall be OK, since files are case sensitive.

```cmd
mvndebug initialize test-compile exec:exec 
```
this command will let you debug the maven process, **NOTE** - you cannot set the breakpoint on instrumented class.
