> ### Froporec 1.2 released:
> - Added new annotation **@GenerateImmutable**, turning existing Record objects into fully immutable Record objects
> - Enhanced the existing **@GenerateRecord** annotation to support the new **includeTypes** attribute (also supported by the new **@GenerateImmutable** annotation)
> - Bug fixes and performance improvements

Version 1.2 Quick Intro: [https://youtu.be/nkbu6zxV3R0](https://youtu.be/nkbu6zxV3R0)

Project's Pitch Video: [https://youtu.be/IC0aS_biaMs](https://youtu.be/IC0aS_biaMs)

<br>

## Installation

If you are running a Maven project, add the latest release dependency to your pom.xml
```xml
<dependency>
    <groupId>org.froporec</groupId>
    <artifactId>froporec</artifactId>
    <version>1.2</version>
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
                            <version>1.2</version>
                        </path>
                    </annotationProcessorPaths>
                </configuration>
            </plugin>
        </plugins>
    </build>
```

For other build tools, please check: [Maven Central](https://search.maven.org/artifact/org.froporec/froporec/1.2/jar).

<br>

## Provided Annotations

### @GenerateRecord

- on top of a POJO class declaration.<br>
As a result, a record class with the name *pojo_class_name* + **Record** will be generated:  
```java
@GenerateRecord 
public class PojoA { 
    // pojo class content 
}
```

- next to a class field type declaration for classes containing enclosed POJOs.<br>
Add the annotation before the POJO type name, in the field declaration. As a result, a record class will be generated for the classname of the annotated field, and the record class generated for the enclosing POJO will contain a field referencing the corresponding record class generated for the enclosed POJO.<br>
Not needed if the POJO class was already annotated in its own declaration or added to the list of .class values of the **includeTypes** attribute.  
```java
@GenerateRecord 
public class PojoA { 
    private @GenerateRecord PojoB pojoB; 
} 
```

&nbsp;&nbsp;&nbsp;
&#42;&#42;&#42; Above code can be written using the **includeTypes** attribute, avoiding multiple uses of @GenerateRecord:<br>
```java
@GenerateRecord(includeTypes = { PojoB.class }) 
public class PojoA { 
    private PojoB pojoB; 
}
```

- next to a method parameter type.<br>
As a result, a record class will be generated for the classname of the annotated parameter.<br>
Not needed if the POJO class was already annotated in its own declaration.
```java
  public void doSomething(@GenerateRecord PojoA pojoA) {
    // method content... 
  }
```
Important Note: the annotation should be used ONLY on POJO classes created in your own project. Any other types are not supported.<br>

### @GenerateImmutable

- on top of a Record class declaration.<br>
As a result, a record class with the name **Immutable** + *record_class_name* will be generated:
```java
@GenerateImmutable
public record RecordA(int field1, String field2) {
    // record class content 
}
```

- next to a record field type declaration for classes containing enclosed Record objects.<br>
Add the annotation before the Record type name, in the field declaration. As a result, a record class will be generated for the classname of the annotated field and the record class generated for the enclosing Record will contain a field referencing the corresponding immutable record class generated for the enclosed Record object.<br>
  Not needed if the Record class was already annotated in its own declaration or added to the list of .class values of the **includeTypes** attribute.
```java
@GenerateImmutable
public record RecordA(int field1, String field2, @GenerateImmutable RecordB recordB) {
}
```

&nbsp;&nbsp;&nbsp;
&#42;&#42;&#42; Above code can be written using the **includeTypes** attribute, avoiding multiple uses of @GenerateImmutable:<br>
```java
@GenerateImmutable(includeTypes = { RecordB.class })
public record RecordA(int field1, String field2, RecordB recordB) {
}
```

- next to a method parameter type.<br>
As a result, a record class will be generated for the classname of the annotated parameter.<br>
Not needed if the Record class was already annotated in its own declaration.
```java
  public void doSomething(@GenerateRecord PojoA pojoA) {
    // method content... 
  }
```
Important Note: the annotation should be used ONLY on Record classes created in your own project. Any other types are not supported.<br>

<br>

> The **includeTypes** attribute available for both @GenerateRecord and @GenerateImmutable, allows specifying additional types to be transformed into their fully immutable equivalent.<br>
> The provided includeTypes array value may contain a mix of your existing Records or POJOs .class values.

<br>

## Sample POJO and Record classes for testing
[https://github.com/mohamed-ashraf-bayor/froporec-annotation-client](https://github.com/mohamed-ashraf-bayor/froporec-annotation-client)


## Issues, Bugs, Suggestions
Contribute to the project's growth by reporting issues or making improvement suggestions [here](https://github.com/mohamed-ashraf-bayor/froporec/issues/new/choose)

<br>
<br>

###### &#169; 2021-2022, [Froporec](https://github.com/mohamed-ashraf-bayor/froporec) is an open source project, currently distributed under the [MIT License](https://github.com/mohamed-ashraf-bayor/froporec/blob/master/LICENSE)
