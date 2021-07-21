# cybench-t2b-agent

* Use Maven dependency:
```xml
<dependency>
    <groupId>com.gocypher.cybench</groupId>
    <artifactId>cybench-t2b-agent</artifactId>
    <version>1.0-SNAPSHOT</version>  
    <scope>runtime</scope>  
</dependency>
```

* Use Gradle dependency:
```gradle
runtimeOnly 'com.gocypher.cybench:cybench-t2b-agent:1.0-SNAPSHOT'
``` 

## Configuration

### Test2Benchmark (T2B) configuration

TBD

### CyBench Launcher configuration

TBD

### JMH Runner configuration (optional)

TBD  

## Running Test2Benchmark (T2B)

### From Maven

* Step 1: to run maven agent from maven, edit POM of your project first by adding these properties and profiles:
    ```xml
    <project>
        <...>
        <properties>
            <...>
            <!-- ### Java 9+ OPTIONS ### -->
            <t2b.module.prop></t2b.module.prop>
            <...>
        </properties>
        <...>
        <profiles>
            <profile>
                <id>java9-plus</id>
                <activation>
                    <jdk>[9.0,)</jdk>
                </activation>
                <properties>
                    <!-- ### Java 9+ OPTIONS ### -->
                    <t2b.module.prop>--add-exports=java.base/jdk.internal.loader=ALL-UNNAMED
                        --add-opens=java.base/jdk.internal.loader=ALL-UNNAMED
                    </t2b.module.prop>
                </properties>
            </profile>
            <profile>
                <id>test-2-bench</id>
                <!-- @@@ Maven central snapshots repository to get dependency artifacts snapshot releases @@@ -->
                <repositories>
                    <repository>
                        <id>oss.sonatype.org</id>
                        <url>https://s01.oss.sonatype.org/content/repositories/snapshots</url>
                        <releases>
                            <enabled>false</enabled>
                        </releases>
                        <snapshots>
                            <enabled>true</enabled>
                        </snapshots>
                    </repository>
                </repositories>
                <dependencies>
                    <!-- @@@ T2B agent app dependency @@@ -->
                    <dependency>
                        <groupId>com.gocypher.cybench</groupId>
                        <artifactId>cybench-t2b-agent</artifactId>
                        <version>1.0-SNAPSHOT</version>
                        <scope>runtime</scope>
                    </dependency>
                    <!-- @@@ CyBench runner dependency @@@ -->
                    <dependency>
                        <groupId>com.gocypher.cybench.client</groupId>
                        <artifactId>gocypher-cybench-runner</artifactId>
                        <version>1.2-SNAPSHOT</version>
                        <scope>runtime</scope>
                    </dependency>
                </dependencies>
                <properties>
                    <!-- ### Debugger Options ### -->
                    <!--<t2b.debug>-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005</t2b.debug>-->
                    <!-- @@@ Now we turn them off @@@ -->
                    <t2b.debug></t2b.debug>
                    <!-- ### JDK home used for javac command, shall match java version running this script to work as expected ### -->
                    <t2b.jdk.home>c:/java/jdk180</t2b.jdk.home>
                    <!-- ### Java executable to use ### -->
                    <t2b.exec.java>${java.home}/bin/java</t2b.exec.java>
                    <!-- ### Java system properties used by benchmarks builder ###-->
                    <!-- @@@ Additional Dir props to customize @@@ -->
                    <!-- -Dt2b.testDir=${project.build.testOutputDirectory}-->
                    <!-- -Dt2b.benchDir=${project.build.directory}/t2b-->
                    <t2b.sys.props>-Dt2b.buildDir=${project.build.directory} -Dt2b.jdkHome=${t2b.jdk.home}</t2b.sys.props>
                    <!-- ### Skip running built benchmarks ### -->
                    <t2b.bench.runner.skip>false</t2b.bench.runner.skip>
                    <!-- ### Config for CyBench runner ### -->
                    <t2b.bench.runner.class>com.gocypher.cybench.launcher.BenchmarkRunner</t2b.bench.runner.class>
                    <t2b.bench.runner.class.args>cfg=t2b/cybench-launcher.properties</t2b.bench.runner.class.args>
                    <!-- ### Config for JMH runner ### -->
                    <!--<t2b.bench.runner.class>org.openjdk.jmh.Main</t2b.bench.runner.class>-->
                    <!--<t2b.bench.runner.class.args>-f 1 -w 5s -wi 0 -i 1 -r 5s -t 1 -bm Throughput</t2b.bench.runner.class.args>-->
                </properties>
                <build>
                    <plugins>
                        <!-- @@@ Make classpath entries as properties to ease access @@@ -->
                        <plugin>
                            <groupId>org.apache.maven.plugins</groupId>
                            <artifactId>maven-dependency-plugin</artifactId>
                            <version>3.1.2</version>
                            <executions>
                                <execution>
                                    <id>get-classpath-filenames</id>
                                    <goals>
                                        <goal>properties</goal>
                                    </goals>
                                </execution>
                                <execution>
                                    <phase>generate-sources</phase>
                                    <goals>
                                        <goal>build-classpath</goal>
                                    </goals>
                                    <configuration>
                                        <outputProperty>t2b.compile.classpath</outputProperty>
                                        <pathSeparator>${path.separator}</pathSeparator>
                                    </configuration>
                                </execution>
                            </executions>
                        </plugin>
                        <!-- @@@ Load benchmarks run configuration made by t2b @@@ -->
                        <plugin>
                            <groupId>org.codehaus.mojo</groupId>
                            <artifactId>properties-maven-plugin</artifactId>
                            <version>1.0.0</version>
                            <executions>
                                <execution>
                                    <phase>integration-test</phase>
                                    <goals>
                                        <goal>read-project-properties</goal>
                                    </goals>
                                    <configuration>
                                        <files>
                                            <file>.benchRunProps</file>
                                        </files>
                                    </configuration>
                                </execution>
                            </executions>
                        </plugin>
                        <plugin>
                            <groupId>org.codehaus.mojo</groupId>
                            <artifactId>exec-maven-plugin</artifactId>
                            <version>3.0.0</version>
                            <executions>
                                <!-- @@@ Build benchmarks for project/module defined unit tests @@@ -->
                                <execution>
                                    <id>build-benchmarks</id>
                                    <goals>
                                        <goal>exec</goal>
                                    </goals>
                                    <!-- ### Maven phase when to build benchmarks for project/module defined unit tests ### -->
                                    <phase>pre-integration-test</phase>
                                    <configuration>
                                        <executable>${t2b.exec.java}</executable>
                                        <classpathScope>test</classpathScope>
                                        <commandlineArgs>
                                            ${t2b.debug}
                                            ${t2b.module.prop}
                                            -javaagent:${com.gocypher.cybench:cybench-t2b-agent:jar}
                                            -cp ${t2b.compile.classpath}
                                            ${t2b.sys.props}
                                            com.gocypher.cybench.Test2Benchmark
                                        </commandlineArgs>
                                    </configuration>
                                </execution>
                                <!-- @@@ Run built benchmarks @@@ -->
                                <execution>
                                    <id>run-benchmarks</id>
                                    <goals>
                                        <goal>exec</goal>
                                    </goals>
                                    <!-- ### Maven phase when to run built benchmarks ### -->
                                    <phase>integration-test</phase>
                                    <configuration>
                                        <skip>${t2b.bench.runner.skip}</skip>
                                        <executable>${t2b.exec.java}</executable>
                                        <classpathScope>test</classpathScope>
                                        <commandlineArgs>
                                            ${t2b.debug}
                                            ${t2b.module.prop}
                                            -cp ${RUN_CLASS_PATH}
                                            ${t2b.bench.runner.class}
                                            ${t2b.bench.runner.class.args}
                                        </commandlineArgs>
                                    </configuration>
                                </execution>
                            </executions>
                        </plugin>
                    </plugins>
                </build>
            </profile>
            <...>
        </profiles>
        <...>
    </project>
    ```
    **Note:** configurable sections are marked with comments starting `<!-- ###`.

    **Note:** to run CyBench runner you'll need configuration file [cybench-launcher.properties](src/main/resources/cybench-launcher.properties).
    Put it somewhere in your project scope and set it over `t2b.bench.runner.class.args` property:
    ```xml
    <t2b.bench.runner.class.args>cfg=t2b/cybench-launcher.properties</t2b.bench.runner.class.args>
    ```

* Step 2: run your maven script with `test-2-bench` profile enabled:
    ```cmd
    mvn clean validate -f pom.xml -P test-2-bench 
    ```

    **Note:**
    * `clean` - this goal is optional, but in most cases we want to have clean build
    * `validate` - this goal is used to cover full build process lifecycle, since our default benchmark build and run phases are bound to 
    `pre-integration-test` and `integration-test`. But you may change accordingly to adopt your project build lifecycle, but **note** those 
    phases must go after `test-compile` phase, since we are dealing with the product of this phase.
    * `-f pom.xml` - you can replace it with any path and file name to match your environment

### Gradle

TBD

### OS shell

* MS Windows

Use [runit.bat](runit.bat) batch script file to run.

* *nix

Use [runit.sh](runit.sh) bash script file to run.

To change configuration making it to meet your needs, please edit these shell script files. See [Configuration](#configuration) section for 
details.

TODO: make shell configuration from properties file. That way both bat and sh shall use same file and there would be no need to change shell
scripts itself.

## Known Bugs
* If test class has methods having same name just different casing, on Windows it creates file and class having different casing and 
benchmarks compile fails with:
  ```
  class MyTests_testUsecTimestamp_jmhTest is public, should be declared in a file named MyTests_testUsecTimestamp_jmhTest.java
  ```
  On linux it shall be OK, since files are case sensitive.
