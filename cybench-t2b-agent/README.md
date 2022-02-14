# cybench-t2b-agent

This app is based on Aspect Oriented Programming (AOP) framework `AspectJ`. `cybench-t2b-agent` provides `@Aspect`
definitions for `AspectJ` allowing unit test to be executed as benchmarks. That way there is no need to change already
existing unit tests in any way, and they can be used for benchmarking. To initiate tests execution unit test framework
provided launcher class shall be used as Java main class. `AspectJ` binds as a JVM agent `-javaagent`
and `cybench-t2b-agent` shall be referenced over class path.

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
        <version>1.0.8-SNAPSHOT</version>
        <scope>test</scope>
    </dependency>
    ```

* Gradle:
    ```groovy 
    repositories {
        mavenCentral()
        maven { url 'https://s01.oss.sonatype.org/content/repositories/snapshots' }
    }
    // ...
    dependencies {
        // ... 
        testRuntimeOnly 'com.gocypher.cybench:cybench-t2b-agent:1.0.8-SNAPSHOT'
    }
    ```

## Configuration

### Test2Benchmark (T2B) configuration

#### Java command arguments

* If Java used to run this app is version `9+` it is needed to add module access arguments:
    ```cmd
    --add-exports=java.base/jdk.internal.loader=ALL-UNNAMED
    --add-opens=java.base/jdk.internal.loader=ALL-UNNAMED
    ```
* Add Java agent argument:
    ```cmd
    -javaagent:<YOUR_PROJECT_PATH>/cybench-t2b-agent-<VERSION>.jar
    ``` 
* Add `cybench-t2b-agent-1.0.8-SNAPSHOT.jar` into Java class path. It provides aspect definitions for `AspectJ`
  framework to intercept unit tests execution and run them as benchmarks.
* Set your project unit tests matching launcher as main class:
    * `org.junit.platform.console.ConsoleLauncher` - to run JUnit5/JUnit4 tests
    * `org.junit.runner.JUnitCore` - to run JUnit4 tests
    * `org.testng.TestNG` - to run TestNG tests
* Set unit tests launcher arguments, for details see:
    * [JUnit5 Console Launcher options](https://junit.org/junit5/docs/current/user-guide/#running-tests-console-launcher-options)
    * [JUnit4 wiki section "Test runners"](https://github.com/junit-team/junit4/wiki/Test-runners)
    * [TestNG documentation section "4 - Running TestNG"](https://testng.org/doc/documentation-main.html)

##### Java system properties

* `t2b.aop.cfg.path` - defines CyBench T2B benchmarks runner configuration file path. **Default
  value**: `config/t2b.properties`.
* `t2b.metadata.cfg.path` - defines CyBench T2B metadata annotations configuration file path. **Default
  value**: `config/metadata.properties`.
* `log4j2.configurationFile` - defines LOG4J configuration properties file path. **Default value** `log4j2.xml` bundled
  within `cybench-t2b-agent` jar.
* `t2b.session.id` - allows defining custom benchmarking session identifier. **Default value** none, T2B setts random
  UUID at runtime if it is not provided.

#### Benchmark metadata configuration

CyBench Launcher allows benchmark to be annotated with a list of metadata annotations. Metadata annotation is simple key
and value pair (kind of map entry). Both benchmark class and benchmark method can have own annotations, but in benchmark
report class and method annotations are merged into single benchmark method metadata map.

To configure metadata entries for benchmark classes and methods, provide system property `t2b.metadata.cfg.path` defined
properties file (default is [config/metadata.properties](config/metadata.properties)),
e.g. `-Dt2b.metadata.cfg.path=t2b/metadata.properties`

* Metadata scopes (property name prefix):
    * `class.` - for class metadata
    * `method.` (can be omitted) - for method metadata
* Static value definition: `key=value`
* Variable value definition:
    * `key=${variable}` - to define single variable
    * `key=${variable1}:${variable2}:{defaultValue}` - to define multiple variables. variables resolution stops on first
      resolved variable value, or defined default value (optional, **NOTE** - it has no `$` symbol before `{`)
* Default unresolved variable value is `-`
* Metadata value can combine both static and variable content like: `Method ${method.name} benchmark`
* **JVM ENVIRONMENT** scope variables:
    * `sys#<propName>` - JVM system property value
        * JVM system properties set by T2B at runtime:
            * `t2b.session.id` - T2B runtime session identifier - UUID string
            * `t2b.session.time` - T2B runtime session start date and time - string formatted as `yyyy-MM-dd_HHmmss`
    * `env#<varName>` - OS environment variable value
    * `vm#<varName>` - JVM calculated variable value
        * `time.millis` - current time in milliseconds (timestamp)
        * `time.nanos` - current time in nanoseconds
        * `uuid` - random UUID
        * `random` - random integer number ranging `0-10000`
* **PACKAGE** scope variables: **NOTE** - all package scope values (except `package.name`) are available only
  when `META-INF/MANIFEST.MF` file is loaded by class loader!
    * `package.name` - package name
    * `package.version` - package implementation version
    * `package.title` - package implementation title
    * `package.vendor` - package implementation vendor
    * `package.spec.version` - package specification version
    * `package.spec.title` - package specification title
    * `package.spec.vendor` - package specification vendor
* **CLASS** scope variables:
    * `class.name` - class name
    * `class.qualified.name` - class qualified name
    * `class.package` - class package name
    * `class.super` - class superclass qualified name
* **METHOD** scope variables:
    * `method.name` - method name
    * `method.signature` - method signature
    * `method.class` - method declaring class qualified name
    * `method.return.type` - method return type
    * `method.qualified.name` - method qualified name
    * `method.parameters` - method parameters list

#### CyBench runner metadata

Some metadata values can be determined dynamically during benchmark tests with the newest version of CyBench runner,
specifically project and version. For Maven (`pom.xml`) projects, no additional build profiles or instructions are
needed. For Gradle (both groovy and kotlin) projects, users must modify their build task to include generating a
`project.properties` file containing this metadata. Instructions for modifying your gradle build file to generate this
properties file are given below.

* Gradle (Groovy)
    * For Gradle build projects written in Groovy, modify one of your build tasks (or use the runBenchmark task detailed
      in lower sections) to generate a `project.properties` file via an ant task:
      ```groovy
      ant.mkdir(dir: "${projectDir}/config/")
      ant.propertyfile(file: "${projectDir}/config/project.properties") {
          entry(key: "PROJECT_ARTIFACT", value: project.name)
          entry(key: "PROJECT_ROOT", value: project.rootProject)
          entry(key: "PROJECT_VERSION", value: project.version)
          entry(key: "PROJECT_PARENT", value: project.parent)
          entry(key: "PROJECT_BUILD_DATE", value: new Date())
          entry(key: "PROJECT_GROUP", value: project.group)
      }
      ```
    * This will generate `project.properties` inside the config folder (with other cybench configuration files) in your
      build directory. This file will contain project name, version, build date, and group if detailed.
* Gradle (Kotlin)
    * For Gradle build projects written in Kotlin (.kts), add the following ant task anywhere in your main
      `build.gradle.kts` build file:
      ```kotlin
      ant.withGroovyBuilder {
         "mkdir"("dir" to "${projectDir}/config/")
         "propertyfile"("file" to "$projectDir/config/project.properties") {
             "entry"("key" to "PROJECT_ARTIFACT", "value" to project.name)
             "entry"("key" to "PROJECT_ROOT", "value" to project.rootProject)
             "entry"("key" to "PROJECT_VERSION", "value" to project.version)
             "entry"("key" to "PROJECT_PARENT", "value" to project.parent)
             "entry"("key" to "PROJECT_GROUP", "value" to project.group)
         }
      }
      ```
    * This will generate `project.properties` inside the config folder (along with other cybench configuration files) in
      your build directory. This file will contain project name and version.
* Maven
    * For Maven projects, no additional modification is needed, and project name/version will automatically be grabbed
      dynamically if possible.

### Benchmark runners configuration

Benchmark runners used by CyBench T2B are configured using [t2b.properties](config/t2b.properties) file. It defines such
properties:

* `t2b.benchmark.runner.wrapper` - T2B wrapper class for benchmarks runner. It can be:
    * `com.gocypher.cybench.t2b.aop.benchmark.runner.CybenchRunnerWrapper` - to use CyBench Launcher benchmarks runner
    * `com.gocypher.cybench.t2b.aop.benchmark.runner.JMHRunnerWrapper` - to use JMH benchmarks runner
    * custom one extending class `com.gocypher.cybench.t2b.aop.benchmark.runner.AbstractBenchmarkRunnerWrapper`
* `t2b.benchmark.runner.wrapper.args` - benchmarks runner supported arguments.

#### CyBench Launcher configuration

To run [CyBench Launcher](https://github.com/K2NIO/gocypher-cybench-java#what-is-cybench-launcher) you'll need
configuration file [cybench-launcher.properties](config/cybench-launcher.properties). Put it somewhere in your project
scope and bind it to CyBench Launcher wrapper `com.gocypher.cybench.t2b.aop.benchmark.runner.CybenchRunnerWrapper` class
argument `cfg=<YOUR_PROJECT_PATH>/cybench-launcher.properties` in T2B benchmarks runner configuration
file [t2b.properties](config/t2b.properties).

Dependencies for your project:

* Maven:
    ```xml
    <dependency>
        <groupId>com.gocypher.cybench.client</groupId>
        <artifactId>gocypher-cybench-runner</artifactId>
        <version>1.3.1</version>
        <scope>test</scope>
    </dependency>
    ```

* Gradle:
    ```groovy
    testRuntimeOnly 'com.gocypher.cybench.client:gocypher-cybench-runner:1.3.1'
    ```

See [CyBench Launcher Configuration document](https://github.com/K2NIO/gocypher-cybench-java#cybench-launcher-configuration)
for configuration options and details.

#### JMH Runner configuration (optional)

[CyBench Launcher](https://github.com/K2NIO/gocypher-cybench-java#what-is-cybench-launcher) is preferred app to run
benchmarks and have benchmarking report posted
into [CyBench](https://cybench.io/) [repo](https://app.cybench.io/cybench/) for later comparison and evaluation of
benchmark results.

But it is also possible to run benchmarks using original JMH runner. Difference is that instead of
`com.gocypher.cybench.t2b.aop.benchmark.runner.CybenchRunnerWrapper` class you shall
use `com.gocypher.cybench.t2b.aop.benchmark.runner.JMHRunnerWrapper` class in T2B benchmarks runner configuration
file [t2b.properties](config/t2b.properties).

See [JMH Runner Configuration document](https://github.com/guozheng/jmh-tutorial/blob/master/README.md#jmh-command-line-options)
for configuration options and details.

## Running Test2Benchmark (T2B)

### Maven

* Step 1: to run T2B agent from Maven, edit POM of your project first by adding these properties and profiles:
    ```xml
    <project>
        <...>
        <properties>
            <...>
            <!-- ### Java 9+ OPTIONS ### -->
            <t2b.module.prop></t2b.module.prop> 
            <!-- ### Config for JUnit5 launcher - default one, and it is also capable to run JUnit4 tests ### -->
            <t2b.test.runner.class>org.junit.platform.console.ConsoleLauncher</t2b.test.runner.class>
            <t2b.test.runner.class.args>
                <YOUR_TESTS_LAUNCHER_ARGUMENTS>
            </t2b.test.runner.class.args>
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
            <!-- Profile to use JUnit4 tests launcher --> 
            <profile>
                <id>run-junit4</id>
                <properties>
                    <!-- ### Config for JUnit4 launcher ### -->
                    <t2b.test.runner.class>org.junit.runner.JUnitCore</t2b.test.runner.class>
                    <t2b.test.runner.class.args>
                        <YOUR_TESTS_LAUNCHER_ARGUMENTS>
                    </t2b.test.runner.class.args>
                </properties>
            </profile>
            <!-- Profile to use TestNG tests launcher -->
            <profile>
                <id>run-testng</id>
                <properties>
                    <!-- ### Config for TestNG launcher ### -->
                    <t2b.test.runner.class>org.testng.TestNG</t2b.test.runner.class>
                    <t2b.test.runner.class.args>
                        <YOUR_TESTS_LAUNCHER_ARGUMENTS>
                    </t2b.test.runner.class.args>
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
                        <version>1.0.8-SNAPSHOT</version>
                        <scope>test</scope>
                    </dependency>
                    <dependency>
                        <groupId>org.junit.platform</groupId>
                        <artifactId>junit-platform-console-standalone</artifactId>
                        <version>1.8.1</version>
                        <scope>test</scope>
                    </dependency>
                </dependencies>
                <properties>
                    <!-- ### Java executable to use ### -->
                    <t2b.java.home>${java.home}</t2b.java.home>
                    <t2b.java.exec>"${t2b.java.home}/bin/java"</t2b.java.exec>
                    <!-- ### Java system properties used by T2B ###-->
                    <t2b.sys.props>
                        -Dt2b.aop.cfg.path="${project.basedir}"/t2b/t2b.properties
                        -Dt2b.metadata.cfg.path="${project.basedir}"/t2b/metadata.properties
                        <!-- ### To use custom LOG4J configuration -->
                        <!-- -Dlog4j2.configurationFile=file:"${project.basedir}"/t2b/log4j2.xml-->
                    </t2b.sys.props>
                    <!--  ### Class path used to run tests: libs;classes;test-classes -->
                    <t2b.run.class.path>
                        ${t2b.compile.classpath}${path.separator}${project.build.outputDirectory}${path.separator}${project.build.testOutputDirectory}
                    </t2b.run.class.path>
                    <!-- ### Skip running unit tests as benchmarks ### -->
                    <t2b.bench.runner.skip>false</t2b.bench.runner.skip>
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
                        <plugin>
                            <groupId>org.codehaus.mojo</groupId>
                            <artifactId>exec-maven-plugin</artifactId>
                            <version>3.0.0</version>
                            <executions>
                                <!-- @@@ Run unit tests as benchmarks @@@ -->
                                <execution>
                                    <id>run-benchmarks</id>
                                    <goals>
                                        <goal>exec</goal>
                                    </goals>
                                    <!-- ### Maven phase when to run unit tests as benchmarks ### -->
                                    <phase>integration-test</phase>
                                    <configuration>
                                        <skip>${t2b.bench.runner.skip}</skip>
                                        <executable>${t2b.java.exec}</executable>
                                        <classpathScope>test</classpathScope>
                                        <commandlineArgs>
                                            ${t2b.module.prop}
                                            -javaagent:${com.gocypher.cybench:cybench-t2b-agent:jar}
                                            ${t2b.sys.props}
                                            -cp ${t2b.run.class.path}
                                            ${t2b.test.runner.class}
                                            ${t2b.test.runner.class.args}
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

  **Note:** since `cybench-t2b-agent` now is in pre-release state, you have to add maven central snapshots repo
  `https://s01.oss.sonatype.org/content/repositories/snapshots` to your project repositories list.

  **Note:** replace `<YOUR_TESTS_LAUNCHER_ARGUMENTS>` placeholder with your project unit testing framework
  configuration, e.g. `--scan-class-path -E=junit-vintage` for JUnit5.

  **Note:** change system properties defined path values to match your project layout.

  **Note:** to run CyBench Launcher runner you'll need configuration file
  [cybench-launcher.properties](config/cybench-launcher.properties). Put it somewhere in your project scope and bind it
  to CyBench Launcher wrapper `com.gocypher.cybench.t2b.aop.benchmark.runner.CybenchRunnerWrapper` class
  argument `cfg=<YOUR_PROJECT_PATH>/cybench-launcher.properties` in T2B benchmarks runner configuration
  file [t2b.properties](config/t2b.properties).

* Step 2: run your Maven script with `test-2-bench` profile enabled:
    ```cmd
    mvn clean validate -f pom.xml -P test-2-bench 
    ```

  **Note:**
    * `clean` - this goal is optional, but in most cases we want to have clean build
    * `validate` - this goal is used to cover full build process lifecycle, since our predefined phase to run tests as
      benchmarks is `integration-test`. But you may change accordingly to adopt your project build lifecycle, just
      **note** those phases must go after `test-compile` phase, since we are dealing with the product of this phase.
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
        // System properties to choose unit testing framework. JUnit5 is default
        ext {
            // JUnit4
            isJU4 = System.properties['unitTests'] == 'junit4'
            // TestNG
            isTNG = System.properties['unitTests'] == 'testng'
        } 
        // ...
        configurations {
            t2b
        }
        // ...
        dependencies {
            // ...
            // Needed to run JUnit5 tests
            testRuntimeOnly 'org.junit.platform:junit-platform-console-standalone:1.8.1'
            // T2B runtime dependency
            t2b 'com.gocypher.cybench:cybench-t2b-agent:1.0.8-SNAPSHOT'
        }
        // ...
        task runBenchmarksFromUnitTests(type: JavaExec, dependsOn: testClasses) {
           group = 'cybench-t2b'
           description = 'Run unit tests as JMH benchmarks'
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
               't2b.aop.cfg.path'     : "$project.projectDir/t2b/t2b.properties",
               't2b.metadata.cfg.path': "$project.projectDir/t2b/metadata.properties",
               // ### To use custom LOG4J configuration
               //'log4j2.configurationFile'  : "file:$project.projectDir/t2b/log4j2.xml"
           ]

           if (isJU4) {
               //    ### Config for JUnit4 runner ###
               main = 'org.junit.runner.JUnitCore'
               args = [
                   <YOUR_TESTS_LAUNCHER_ARGUMENTS>
               ]
           } else if (isTNG) {
               //    ### Config for TestNG runner ###
               main = 'org.testng.TestNG'
               args = [
                   <YOUR_TESTS_LAUNCHER_ARGUMENTS>
               ]
           } else {
               //    ### Config for JUnit5/JUnit4 runner ###
               main = 'org.junit.platform.console.ConsoleLauncher'
               args = [
                   <YOUR_TESTS_LAUNCHER_ARGUMENTS>
               ]
           }

          ant.mkdir(dir: "${projectDir}/config/")
          ant.propertyfile(file: "${projectDir}/config/project.properties") {
              entry(key: "PROJECT_ARTIFACT", value: project.name)
              entry(key: "PROJECT_ROOT", value: project.rootProject)
              entry(key: "PROJECT_VERSION", value: project.version)
              entry(key: "PROJECT_PARENT", value: project.parent)
              entry(key: "PROJECT_BUILD_DATE", value: new Date())
              entry(key: "PROJECT_GROUP", value: project.group)
          }
        }
        ```

    * Kotlin
        ```kotlin
        // ...
        repositories {
          mavenLocal()
          mavenCentral()
          maven {
            setUrl("https://s01.oss.sonatype.org/content/repositories/snapshots")
            mavenContent {
              snapshotsOnly()
            }
          }
        }
        // ...
        val t2b by configurations.creating {
          isCanBeResolved = true
          isCanBeConsumed = false
        }
        // ...
        dependencies {
          // ...
          // Needed to run JUnit5 tests
          testRuntimeOnly ("org.junit.platform:junit-platform-console-standalone:1.8.1")
          // T2B runtime dependency
          t2b ("com.gocypher.cybench:cybench-t2b-agent:1.0.8-SNAPSHOT")
        }
        // ...
        val launcher = javaToolchains.launcherFor {
          languageVersion.set(JavaLanguageVersion.of(11))
        }

        tasks {
          val runBenchmarksFromUnitTests by registering(JavaExec::class) {
            group = "cybench-t2b"
            description = "Run unit tests as JMH benchmarks"
            dependsOn(testClasses)

            if (JavaVersion.current().isJava9Compatible) {
              jvmArgs("-javaagent:\"${configurations.getByName("t2b").iterator().next()}\"")
              jvmArgs("--add-exports=java.base/jdk.internal.loader=ALL-UNNAMED")
              jvmArgs("--add-opens=java.base/jdk.internal.loader=ALL-UNNAMED")
            } else {
              jvmArgs("-javaagent:\"${configurations.getByName("t2b").iterator().next()}\"")
            }

            systemProperty("t2b.aop.cfg.path", "${project.rootDir}/t2b/t2b.properties")
            systemProperty("t2b.metadata.cfg.path", "${project.rootDir}/t2b/metadata.properties")
            // ### To use custom LOG4J configuration
            //systemProperty("log4j2.configurationFile", "file:${project.rootDir}/t2b/log4j2.xml")

            classpath(
              sourceSets["main"].runtimeClasspath,
              sourceSets["test"].runtimeClasspath,
              configurations.getByName("t2b")
            )

            mainClass.set("org.junit.platform.console.ConsoleLauncher")
            args("<YOUR_TESTS_LAUNCHER_ARGUMENTS>")

            ant.withGroovyBuilder {
               "mkdir"("dir" to "${projectDir}/config/")
               "propertyfile"("file" to "$projectDir/config/project.properties") {
                   "entry"("key" to "PROJECT_ARTIFACT", "value" to project.name)
                   "entry"("key" to "PROJECT_ROOT", "value" to project.rootProject)
                   "entry"("key" to "PROJECT_VERSION", "value" to project.version)
                   "entry"("key" to "PROJECT_PARENT", "value" to project.parent)
                   "entry"("key" to "PROJECT_GROUP", "value" to project.group)
               }
            }
          }
        }
        ```

  **Note:** `configurations` section defines custom ones to make it easier to access particular cybench dependencies.

  **Note:** since `cybench-t2b-agent` now is in pre-release state, you have to add maven central snapshots repo
  `https://s01.oss.sonatype.org/content/repositories/snapshots` to your project repositories list.

  **Note:** replace `<YOUR_TESTS_LAUNCHER_ARGUMENTS>` placeholder with your project unit testing framework
  configuration, e.g. `--scan-class-path -E=junit-vintage` for JUnit5.

  **Note:** change system properties defined path values to match your project layout.

  **Note:** to run CyBench Launcher runner you'll need configuration file
  [cybench-launcher.properties](config/cybench-launcher.properties). Put it somewhere in your project scope and bind it
  to CyBench Launcher wrapper `com.gocypher.cybench.t2b.aop.benchmark.runner.CybenchRunnerWrapper` class
  argument `cfg=<YOUR_PROJECT_PATH>/cybench-launcher.properties` in T2B benchmarks runner configuration
  file [t2b.properties](config/t2b.properties).

* Step 2: run your Gradle script:
    * To run your project unit tests as benchmarks
      ```cmd
      gradle :runBenchmarksFromUnitTests 
      ```

### OS shell

* MS Windows

Use [runit.bat](bin/runit.bat) batch script file to run.

* *nix

Use [runit.sh](bin/runit.sh) bash script file to run.

Bash scripts are kind of run wizards: it will ask you for you about your environment configuration and having required
variables set, it will run your project tests as benchmarks.

To change configuration to meet your environment, please edit these shell script files.
See [Configuration](#configuration) section for details.

## Known Bugs

* If test method is annotated as test using annotations of multiple unit test frameworks (
  e.g. `@org.junit.jupiter.api.Test` and ` @org.junit.Test`), `AspectJ` can apply incorrect aspect (JUnit4 while it is
  run using JUnit5) and test execution as benchmark will fail.
