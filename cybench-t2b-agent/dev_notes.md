## DEVNotes (a.k.a TODO)

* **P1** tasks:
    * Test execution shall be intercepted by T2B and executed as benchmark. This would allow us to have fully prepared
      test used parameters and fields. It would also cover dynamically generated test (JUnit5) benchmarking.
    * Support for `@BeforeXXXX`, `@AfterXXXXX`, `@TearDown`, etc. test framework annotations. Some may not have direct
      mapping to JMH, so will have to make it with use of some "wrapper" methods and listeners.
    * Integrate T2B
      with [opentelemetry-java-instrumentation](https://github.com/open-telemetry/opentelemetry-java-instrumentation)
* **P2** tasks:
    * Add includes/excludes to narrow the scope of tests to use
    * Large reports
* **P3** tasks (optional at all):
    * Make skip with reason: throw exception with message from annotation
    * Add arguments for shell script to define flow: `-tc` - transform and compile, `-r jmh` - run using JMH
      runner, `-r cyb` - run using CyBench runner
    * Multi-module project support (Maven/Gradle) when running from bat/sh. Scan `workDir` for inner build dirs and run
      benchmarks for all of them
    * Make shell configuration from properties file. That way both `bat` and `sh` shall use same file and there would be
      no need to change shell scripts itself
    * Make gradle plugin
    * Make maven plugin

### Debugging

    ```cmd
    mvndebug clean validate -f pom.xml -P test-2-bench
    ```

this command will let you debug the maven process, **NOTE** - you cannot set the breakpoint on instrumented class.

Or simply enable java debugging agent using:

* Maven
    ```xml
    <t2b.debug.args>-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005</t2b.debug.args>
    ```
* Gradle `JavaExec` arguments
    ```groovy
    jvmArgs = [
        '-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005'
        // ...
    ]
    ```
  or
    ```groovy
    debug = true
    ```
  or for Gradle `5.6+`
    ```groovy
    debugOptions {
        enabled = true
        port = 5005
        server = true
        suspend = true
    }
    ```
* Shell
    ```cmd
    set JAVA_DEBUGGER="-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005"
    ```
