## DEVNotes (a.k.a TODO)

* **P1** tasks (must be for 2021-07-23):
    * Run from Gradle using `application` plugin
    * Wiki/Docs
* **P2** tasks (optional for 2021-07-23): 
    * Add cybench annotations: id, metadata
* **P3** tasks (optional for 2021-07-23):
    * Support for `@BeforeXXXX`, `@AfterXXXXX`, `@TearDown`, and etc. test framework annotations
    * Add includes/excludes to narrow the scope of tests to use
    * Make skip with reason: throw exception with message from annotation
    * Large reports
* **P4** tasks (optional at all):
    * Make compilation using java API instead of calling `javac` process
    * Make gradle plugin
    * Make maven plugin
    * Add arguments for shell script to define flow: `-tc` - transform and compile, `-r jmh` - run using JMH runner, `-r cyb` - run using
    * Make shell configuration from properties file. That way both `bat` and `sh` shall use same file and there would be no need to change 
    shell scripts itself. 
    CyBench runner
    * Make sh script

### Debugging
```cmd
mvndebug clean validate -f pom.xml -P test-2-bench
```
this command will let you debug the maven process, **NOTE** - you cannot set the breakpoint on instrumented class.

Or simply enable java debugging agent using `t2b.debug` property:
* Maven
```xml
<t2b.debug>-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005</t2b.debug>
```
* Gradle
```groovy
TBD
```
* Shell
```cmd
set JAVA_DEBUGGER="-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005"
```
