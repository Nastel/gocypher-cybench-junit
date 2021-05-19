To setup Maven, you need to:

Add Cybench Maven dependencies
<..>
        <dependency>
            <groupId>com.gocypher.cybench</groupId>
            <artifactId>gocypher-cybench-t2b-annotations</artifactId>
            <version>1.0</version>
        </dependency>

<..>
Setup compiler to run AnnotationProcessors selected
<..>
        <pluginManagement>
            <plugins>
                <plugin>
                    <artifactId>maven-compiler-plugin</artifactId>
                    <version>3.8.0</version>
                    <configuration>

                        <annotationProcessors>
                            <annotationProcessor>org.openjdk.jmh.generators.BenchmarkProcessor</annotationProcessor>
                            <annotationProcessor>com.gocypher.cybench.core.annotation.TestToBenchmarkProcessor</annotationProcessor>
                        </annotationProcessors>
                    </configuration>

                </plugin>
<..>
Setup Maven to append test sources to compile:
<..>
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
<..>

NB!
Add java property -DgenerateBenchmarkFromTest=true on compile