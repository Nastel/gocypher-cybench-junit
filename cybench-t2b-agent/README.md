# cybench-t2b-agent

This app running with Java agent builds JMH benchmarks for provided unit tests. It does not copy unit test code into benchmark - it just 
links unit test to be executed from benchmark, generated for that test. That way there is no need to change already existing unit tests in 
any way and they can be used for benchmarking.

Supported unit testing frameworks:

* JUnit4
* JUnit5
* TestNG

Dependencies for your project:
* Maven:
    ```xml
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
    ...
    <dependency>
        <groupId>com.gocypher.cybench</groupId>
        <artifactId>cybench-t2b-agent</artifactId>
        <version>1.0.1-SNAPSHOT</version>
        <scope>test</scope>
    </dependency>
    ```

* Gradle:
    ```groovy
    runtime 'com.gocypher.cybench:cybench-t2b-agent:1.0.1-SNAPSHOT'
    ```

## Configuration

### Test2Benchmark (T2B) configuration

#### Java command arguments

* If Java used to run  this app is version `9+` it is needed to add module access arguments:
    ```cmd
    --add-exports=java.base/jdk.internal.loader=ALL-UNNAMED
    --add-opens=java.base/jdk.internal.loader=ALL-UNNAMED
    ```
* Add Java agent argument:
    ```cmd
    -javaagent:<YOUR_PROJECT_PATH>/cybench-t2b-agent-1.0.1-SNAPSHOT.jar
    ```

##### Java system properties

* `t2b.buildDir` - defines path where unit tests are build. It can be root dir for `Maven` or `Gradle` builders. App will find inner paths 
for compiled app and test classes. **Default value**: java system property `user.dir` if defined, `.` - otherwise.
* `t2b.testDir` - defines path of built unit tests directory. **Optional** property, shall be used if `t2b.buildDir` fails to find where unit 
tests are compiled. Most likely it may happen if unit test are build using any other build tool than `Maven` or `Gradle`. Also it may happen
if your project uses non default `Maven`/`Gradle` build directories layout.
* `t2b.benchDir` - defines path where to place generated benchmarks. **Default value**: `${t2b.buildDir}/t2b`.
* `t2b.jdkHome` - defines JDK home path to be used for benchmarks compilation. Now `javac` command is ued to compile generated benchmark 
classes. **Optional** property, shall be used if Java used to run this app is from JRE. If app runner Java is from JDK, there is no need to 
set this property. **NOTE**: to avoid any compatibility issues it is recommended to use same Java level version (`8`, `11`, `15`, etc) JDK 
as it is Java used to run this app.

#### Application

* Main class: `com.gocypher.cybench.Test2Benchmark`

### CyBench Launcher configuration

To run [CyBench Launcher](https://github.com/K2NIO/gocypher-cybench-java#what-is-cybench-launcher) you'll need configuration file 
[cybench-launcher.properties](config/cybench-launcher.properties). Put it somewhere in your project scope and set it over 
`com.gocypher.cybench.launcher.BenchmarkRunner` class argument `cfg=<YOUR_PROJECT_PATH>/cybench-launcher.properties`.

Dependencies for your project:
* Maven:
    ```xml
    <dependency>
        <groupId>com.gocypher.cybench.client</groupId>
        <artifactId>gocypher-cybench-runner</artifactId>
        <version>1.1</version>
        <scope>test</scope>
    </dependency>
    ```

* Gradle:
    ```groovy
    runtime 'com.gocypher.cybench.client:gocypher-cybench-runner:1.1'
    ```

See [CyBench Launcher Configuration document](https://github.com/K2NIO/gocypher-cybench-java#cybench-launcher-configuration) for 
configuration options and details.

### JMH Runner configuration (optional)

[CyBench Launcher](https://github.com/K2NIO/gocypher-cybench-java#what-is-cybench-launcher) is preferred app to run generated benchmarks and 
have benchmarking report posted into [CyBench](https://cybench.io/) [repo](https://app.cybench.io/cybench/) for later comparison and 
evaluation of benchmark results.

But it is also possible to run generated benchmarks using original JMH runner. Difference is that instead of 
`com.gocypher.cybench.launcher.BenchmarkRunner` class you shall use `org.openjdk.jmh.Main` class.

See [JMH Runner Configuration document](https://github.com/guozheng/jmh-tutorial/blob/master/README.md#jmh-command-line-options) for 
configuration options and details.

## Running Test2Benchmark (T2B)

### From Maven

* Step 1: to run T2B agent from Maven, edit POM of your project first by adding these properties and profiles:
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
                        <version>1.0.1-SNAPSHOT</version>
                        <scope>test</scope>
                    </dependency>
                    <!-- @@@ CyBench Launcher runner dependency @@@ -->
                    <dependency>
                        <groupId>com.gocypher.cybench.client</groupId>
                        <artifactId>gocypher-cybench-runner</artifactId>
                        <version>1.2-SNAPSHOT</version>
                        <scope>test</scope>
                    </dependency>
                </dependencies>
                <properties>
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
                    <!-- ### Config for CyBench Launcher runner ### -->
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
                                            ${t2b.module.prop}
                                            -cp ${t2b.compile.classpath}${path.separator}${T2B_CLASS_PATH}
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

    **Note:** to run CyBench Launcher runner you'll need configuration file [cybench-launcher.properties](config/cybench-launcher.properties).
    Put it somewhere in your project scope and set it over `t2b.bench.runner.class.args` property:
    ```xml
    <t2b.bench.runner.class.args>cfg=t2b/cybench-launcher.properties</t2b.bench.runner.class.args>
    ```

* Step 2: run your Maven script with `test-2-bench` profile enabled:
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

* Step 1: to run T2B agent from Gradle, edit `build.gradle` of your project first by adding these `repository`, 
`configurations`, `dependnecies` and `task` definitions:
    * Groovy
        ```groovy
        repositories {
            mavenCentral()
            maven { url 'https://s01.oss.sonatype.org/content/repositories/snapshots' }
        }
        // ...
        configurations {
            t2b
            cybench
        }
        // ...
        dependencies {
            // ...
            t2b 'com.gocypher.cybench:cybench-t2b-agent:1.0.1-SNAPSHOT'
            cybench 'com.gocypher.cybench.client:gocypher-cybench-runner:1.2-SNAPSHOT'
        }
        // ...
        task buildBenchmarksFromUnitTests(type: JavaExec, dependsOn: testClasses) {
            group = 'CyBench-T2B'
            description = 'Run Test2Benchmarks benchmarks generator'
            classpath = files(
                    project.sourceSets.main.runtimeClasspath,
                    project.sourceSets.test.runtimeClasspath,
                    configurations.t2b
            )
            if (JavaVersion.current().isJava9Compatible()) {
                jvmArgs = [
                        "-javaagent:\"${configurations.t2b.iterator().next()}\"",
                        '--add-exports=java.base/jdk.internal.loader=ALL-UNNAMED',
                        '--add-opens=java.base/jdk.internal.loader=ALL-UNNAMED'
                ]
            } else {
                jvmArgs = [
                        "-javaagent:\"${configurations.t2b.iterator().next()}\""
                ]
            }
            systemProperties = [
                    't2b.buildDir': "$buildDir",
                    't2b.jdkHome' : 'c:/java/jdk180'
            ]
            main = 'com.gocypher.cybench.Test2Benchmark'
        }

        task runBenchmarksFromUnitTestsCybench(type: JavaExec, dependsOn: buildBenchmarksFromUnitTests) {
            def benchRunProps = new Properties()
            def pFile = file(".benchRunProps")
            if (pFile.exists()) {
                pFile.withInputStream { benchRunProps.load(it) }
            }

            def t2bClassPath = benchRunProps.getProperty('T2B_CLASS_PATH')
            if (t2bClassPath != null) {
                t2bClassPath = t2bClassPath.replaceAll("\"", "")
            }

            group = 'CyBench-T2B'
            description = 'Run JMH benchmarks over Cybench Launcher runner'
            classpath = files(
                    project.sourceSets.main.runtimeClasspath,
                    project.sourceSets.test.runtimeClasspath,
                    [t2bClassPath],
                    configurations.cybench
            )
            if (JavaVersion.current().isJava9Compatible()) {
                jvmArgs = [
                        '--add-exports=java.base/jdk.internal.loader=ALL-UNNAMED',
                        '--add-opens=java.base/jdk.internal.loader=ALL-UNNAMED'
                ]
            }

            //    ### Config for CyBench Launcher runner ###
            main = 'com.gocypher.cybench.launcher.BenchmarkRunner'
            args = [
                    'cfg=t2b/cybench-launcher.properties'
            ]

            //    ### Config for JMH runner ###
            //    main = 'org.openjdk.jmh.Main'
            //    args = [
            //            '-f', '1', '-w', '5s', '-wi', '0', '-i', '1', '-r', '5s', '-t', '1', '-bm', 'Throughput'
            //    ]
        }
        ```

    * Kotlin
        ```kotlin
        import java.util.Properties;
        // ...
        repositories {
          mavenCentral();
          maven {
            setUrl("https://s01.oss.sonatype.org/content/repositories/snapshots")
          }
        }
        // ...
        val t2b by configurations.creating {
          isCanBeResolved = true
          isCanBeConsumed = false
        }
        val cybench by configurations.creating {
          isCanBeResolved = true
          isCanBeConsumed = false
        }
        // ...
        dependencies {
          // ...
          t2b ("com.gocypher.cybench:cybench-t2b-agent:1.0.1-SNAPSHOT")
          cybench ("com.gocypher.cybench.client:gocypher-cybench-runner:1.2-SNAPSHOT")
        }
        // ...
        val launcher = javaToolchains.launcherFor {
          languageVersion.set(JavaLanguageVersion.of(11))
        }

        tasks {
          val buildBenchmarksFromUnitTests by registering(JavaExec::class) {
            group = "cybench-t2b"
            description = "Run Test2Benchmarks benchmarks generator"
            dependsOn(testClasses)
            javaLauncher.set(launcher)

            if (JavaVersion.current().isJava9Compatible) {
              jvmArgs("-javaagent:\"${configurations.getByName("t2b").iterator().next()}\"")
              jvmArgs("--add-exports=java.base/jdk.internal.loader=ALL-UNNAMED")
              jvmArgs("--add-opens=java.base/jdk.internal.loader=ALL-UNNAMED")
            } else {
              jvmArgs("-javaagent:\"${configurations.getByName("t2b").iterator().next()}\"");
            }

            systemProperty("t2b.buildDir", "$buildDir")
            systemProperty("t2b.jdkHome", "c:/java/jdk180")

            classpath(
              sourceSets["main"].runtimeClasspath,
              sourceSets["test"].runtimeClasspath,
              configurations.getByName("t2b")
            )

            mainClass.set("com.gocypher.cybench.Test2Benchmark")
          }

          val runBenchmarksFromUnitTestsCybench by registering(JavaExec::class) {
            group = "cybench-t2b"
            description = "Run JMH benchmarks over Cybench Launcher runner"
            dependsOn(buildBenchmarksFromUnitTests)
            javaLauncher.set(launcher)

            if (JavaVersion.current().isJava9Compatible) {
              jvmArgs("--add-exports=java.base/jdk.internal.loader=ALL-UNNAMED")
              jvmArgs("--add-opens=java.base/jdk.internal.loader=ALL-UNNAMED")
            }

            classpath(
              sourceSets["main"].runtimeClasspath,
              sourceSets["test"].runtimeClasspath,
              configurations.getByName("cybench")
            )

            val benchRunProps = Properties();
            val pFile = file(".benchRunProps")
            if (pFile.exists()) {
              val fis = pFile.inputStream();
              benchRunProps.load(fis);
              fis.close();
            }

            var t2bClassPath = benchRunProps.getProperty("T2B_CLASS_PATH")
            if (t2bClassPath != null) {
              t2bClassPath = t2bClassPath.replace("\"", "")

              classpath(
                t2bClassPath.split("${File.pathSeparator}")
              )
            }

            //    ### Config for CyBench Launcher runner ###
            mainClass.set("com.gocypher.cybench.launcher.BenchmarkRunner")
            args ("cfg=t2b/cybench-launcher.properties")

            //    ### Config for JMH runner ###
            //    mainClass.set("org.openjdk.jmh.Main")
            //    args ("-f", "1", "-w", "5s", "-wi", "0", "-i", "1", "-r", "5s", "-t", "1", "-bm", "Throughput")
          }
        }
        ```

    **Note:** since `cybench-t2b-agent` now is in pre-release state, you have to add maven central snapshots repo 
    `https://s01.oss.sonatype.org/content/repositories/snapshots` to your project repositories list.

    **Note:** `configurations` section defines custom ones to make it easier to access particular cybench dependencies.

    **Note:** to run CyBench Launcher runner you'll need configuration file [cybench-launcher.properties](config/cybench-launcher.properties).
    Put it somewhere in your project scope and set it over `t2b.bench.runner.class.args` property:
    ```xml
    <t2b.bench.runner.class.args>cfg=t2b/cybench-launcher.properties</t2b.bench.runner.class.args>
    ```

* Step 2: run your Gradle script:
    * To build benchmarks from unit tests
    ```cmd
    gradle :buildBenchmarksFromUnitTests 
    ```
    * To build and run benchmarks
    ```cmd
    gradle :runBenchmarksFromUnitTestsCybench 
    ```

### OS shell

* MS Windows

Use [runit.bat](bin/runit.bat) batch script file to run.

* *nix

Use [runit.sh](bin/runit.sh) bash script file to run.

Bash scripts are kind of run wizards: it will ask you for you about your environment configuration and having required variables set, it 
will allow you to select options on what you want to do:
* Build benchmarks from tests - it shall be run very fist time and after every rebuild of your project.
* Run benchmarks using Cybench runner.
* Run benchmarks using JMH runner.

To change configuration to meet your environment, please edit these shell script files. See [Configuration](#configuration) section for 
details.

## Known Bugs
* If test class has methods having same name just different casing, on Windows it creates file and class having different casing and 
benchmarks compile fails with:
  ```
  class MyTests_testUsecTimestamp_jmhTest is public, should be declared in a file named MyTests_testUsecTimestamp_jmhTest.java
  ```
  On Linux it shall be OK, since files are case sensitive.
