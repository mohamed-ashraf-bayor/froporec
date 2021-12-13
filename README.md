## FROPOREC: From POJOs to Records
Turn your POJOs into fully immutable Record objects with Froporec annotation processor (min. Java 17 required)

Pitch Video:
[https://youtu.be/IC0aS_biaMs](https://youtu.be/IC0aS_biaMs)

### How to Install ?

If you are running a Maven project, add the latest release dependency to your pom.xml
```xml
<dependency>
    <groupId>org.froporec</groupId>
    <artifactId>froporec</artifactId>
    <version>1.1</version>
</dependency>
``` 
You will also need to include the same dependency as an additional annotation processor in the Maven Compiler plugin of your project
```xml
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>${maven-compiler-plugin.version}</version>
                <configuration>
                    <release>17</release>
                    <compilerArgs>-Xlint:unchecked</compilerArgs>
                    <annotationProcessorPaths>
                        <path>
                            <groupId>org.froporec</groupId>
                            <artifactId>froporec</artifactId>
                            <version>1.1</version>
                        </path>
                    </annotationProcessorPaths>
                </configuration>
            </plugin>
        </plugins>
    </build>
```

For other build tools, please check: [Maven Central](https://search.maven.org/artifact/org.froporec/froporec/1.1/jar).

### Use on your declared POJO classes 
```java
@GenerateRecord
public class PojoA {
    // class content
}
```

### Use on fields of your declared POJO classes 
```java
@GenerateRecord
public class PojoA {
    private @GenerateRecord PojoB pojoB;
    // ...
}
```

### Use on your defined method parameters
```java
public void doSomething(@GenerateRecord PojoA pojoA) {
    // method content...
}
```

### Sample POJO classes for testing
[https://github.com/mohamed-ashraf-bayor/froporec-annotation-client](https://github.com/mohamed-ashraf-bayor/froporec-annotation-client)

### Invalid Uses of Froporec
The annotation should be used ONLY on POJO classes created in your own project. Any other types (including Java types) are not supported

### Issues, Bugs, Suggestions
Contribute to the project's growth by reporting issues or making improvement suggestions [here](https://github.com/mohamed-ashraf-bayor/froporec/issues/new/choose)
