## DEVNotes (a.k.a TODO)

* **P1** tasks:
    * Use shared memory to exchange `JoinPoint` between different JVM instances and allow JMH to have it's own VM
      instance as it is by default.
    * Make test pointcut to check calle method and apply right aspect for tests having multiple frameworks test
      annotations.
    * Integrate T2B
      with [opentelemetry-java-instrumentation](https://github.com/open-telemetry/opentelemetry-java-instrumentation)
* **P2** tasks:
    * Method/class annotations definition over template file (something similar to metadata template). Needs to define
      method name matcher (method,class,package) to allow individual method configurations.
* **P3** tasks (optional at all):
    * Make skip with reason: throw exception with message from annotation
    * Multi-module project support (Maven/Gradle) when running from bat/sh. Scan `workDir` for inner build dirs and run
      benchmarks for all of them
    * Make shell configuration from properties file. That way both `bat` and `sh` shall use same file and there would be
      no need to change shell scripts itself
    * Make gradle plugin
    * Make maven plugin
    * Check why JMH generates empty files `./BenchmakList` and `./CompilerHints` after compilation.

### Multiple agents

It is possible to define multiple agents for `java` command:

```cmd
java -javaagent:agentA.jar -javaagent:agentB.jar MyJavaProgram
```

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
