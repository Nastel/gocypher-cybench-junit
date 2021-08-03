## DEVNotes (a.k.a TODO)

* **P1** tasks (optional for 2021-07-23): 
    * Add cybench annotations: id, metadata. Make it configurable using some template (file or property).
* **P2** tasks (optional for 2021-07-23):
    * Support for `@BeforeXXXX`, `@AfterXXXXX`, `@TearDown`, and etc. test framework annotations
    * Add includes/excludes to narrow the scope of tests to use
    * Make skip with reason: throw exception with message from annotation
    * Large reports
* **P3** tasks (optional at all):
    * Make compilation using java API instead of calling `javac` process
    * Make gradle plugin
    * Make maven plugin
    * Add arguments for shell script to define flow: `-tc` - transform and compile, `-r jmh` - run using JMH runner, `-r cyb` - run using
    CyBench runner
    * Multi-module project support (Maven/Gradle) when running from bat/sh. Scan `workDir` for inner build dirs and run benchmarks for all 
    of them
    * Make shell configuration from properties file. That way both `bat` and `sh` shall use same file and there would be no need to change 
    shell scripts itself

### Debugging
    ```cmd
    mvndebug clean validate -f pom.xml -P test-2-bench
    ```
this command will let you debug the maven process, **NOTE** - you cannot set the breakpoint on instrumented class.

Or simply enable java debugging agent using:
* Maven
    ```xml
    <t2b.debug>-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005</t2b.debug>
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
