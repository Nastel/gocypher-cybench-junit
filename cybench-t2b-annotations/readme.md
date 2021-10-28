To set up Maven, you need to:

Add CyBench Maven dependencies:
```xml
<dependency>
    <groupId>com.gocypher.cybench</groupId>
    <artifactId>cybench-t2b-annotations</artifactId>
    <version>1.0</version>
</dependency>
```

Setup compiler to run AnnotationProcessors selected:
```xml
<pluginManagement>
    <plugins>
        <plugin>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.8.0</version>
            <configuration>
                <annotationProcessors>
                    <annotationProcessor>org.openjdk.jmh.generators.BenchmarkProcessor</annotationProcessor>
                    <annotationProcessor>com.gocypher.cybench.core.annotation.Test2BenchmarkProcessor</annotationProcessor>
                </annotationProcessors>
            </configuration>
        </plugin>
    </plugins>
</pluginManagement>
```

Setup Maven to append test sources to compile:
```xml
<plugins>
    <plugin>
        <groupId>org.codehaus.mojo</groupId>
        <artifactId>build-helper-maven-plugin</artifactId>
        <version>3.2.0</version>
        <executions>
            <execution>
                <phase>generate-sources</phase>
                <goals>
                    <goal>add-source</goal>
                </goals>
                <configuration>
                    <sources>
                        <source>src/test/java</source>
                    </sources>
                </configuration>
            </execution>
        </executions>
    </plugin>
</plugins>
```

**NOTE:** Add java property -Dt2b.generateBenchmarkFromTest=true on compile.
