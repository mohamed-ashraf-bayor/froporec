> ### Froporec 1.3 released:
> - Deprecated existing annotations: ~~@GenerateRecord~~, ~~@GenerateImmutable~~. Replaced with: **@Record** and **@Immutable**, which also provide the new **alsoConvert** attribute as a replacement of the ~~includeTypes~~ attribute.
> - Added new annotation **@SuperRecord** allowing to bypass the known limitation of Java Records not able to extend any other class. The mandatory **mergeWith** attribute allows to specify a list of existing POJO and/or Record classes to extend from. The annotation can also be applied on existing POJO classes.
> - Added new attribute **superInterfaces** to all annotations. Allows to provide a list of interfaces to be implemented by the generated Record class.
> - Bug fixes and improvements


<br>

## Videos

Froporec and Beyond: [https://youtu.be/nkbu6zxV3R0](https://youtu.be/nkbu6zxV3R0)

v1.2 Quick Intro: [https://youtu.be/Yu3bR8ZkpYE](https://youtu.be/Yu3bR8ZkpYE)

Project's Pitch (v1.0): [https://youtu.be/IC0aS_biaMs](https://youtu.be/IC0aS_biaMs)

<br>

## Installation

If you are running a Maven project, add the latest release dependency to your pom.xml
```xml
<dependency>
    <groupId>org.froporec</groupId>
    <artifactId>froporec</artifactId>
    <version>1.3</version>
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
                            <version>1.3</version>
                        </path>
                    </annotationProcessorPaths>
                </configuration>
            </plugin>
        </plugins>
    </build>
```

For other build tools, please check: [Maven Central](https://search.maven.org/artifact/org.froporec/froporec/1.3/jar).

<br>

## Provided Annotations

<br>

### @Record

- on top of a POJO class declaration.<br>
As a result, a record class with the name _pojo_class_name_ + **Record** will be generated:  
```java
@Record 
public class PojoA { 
    // pojo class content 
}
```
- next to a class field type declaration for classes containing enclosed POJOs.<br>
Add the annotation before the POJO type name, in the field declaration. As a result, a record class will be generated for the classname of the annotated field, and the record class generated for the enclosing POJO will contain a field referencing the corresponding record class generated for the enclosed POJO.<br>
Not needed if the POJO class was already annotated in its own declaration or added to the list of .class values of the **alsoConvert** attribute.  
```java
@Record 
public class PojoA { 
    private @Record PojoB pojoB; 
} 
```

&nbsp;&nbsp;&nbsp;
Above code can be written using the **alsoConvert** attribute, avoiding multiple uses of @Record:<br>
```java
@Record(alsoConvert = { PojoB.class }) 
public class PojoA { 
    private PojoB pojoB; 
}
```
- next to a method parameter type.<br>
As a result, a record class will be generated for the classname of the annotated parameter.<br>
Not needed if the POJO class was already annotated in its own declaration.
```java
  public void doSomething(@Record PojoA pojoA) {
    // method content... 
  }
```
Important Note: the annotation should be used ONLY on POJO classes created in your own project. Any other types are not supported.<br>

<br>

### @Immutable

- on top of a Record class declaration.<br>
As a result, a record class with the name **Immutable** + _record_class_name_ will be generated:
```java
@Immutable
public record RecordA(int field1, String field2) {
    // record class content 
}
```
- next to a record field type declaration for classes containing enclosed Record objects.<br>
Add the annotation before the Record type name, in the field declaration. As a result, a record class will be generated for the classname of the annotated field and the record class generated for the enclosing Record will contain a field referencing the corresponding immutable record class generated for the enclosed Record object.<br>
  Not needed if the Record class was already annotated in its own declaration or added to the list of .class values of the **alsoConvert** attribute.
```java
@Immutable
public record RecordA(int field1, String field2, @Immutable RecordB recordB) {
}
```

&nbsp;&nbsp;&nbsp;
Above code can be written using the **alsoConvert** attribute, avoiding multiple uses of @Immutable:<br>
```java
@Immutable(alsoConvert = { RecordB.class })
public record RecordA(int field1, String field2, RecordB recordB) {
}
```
- next to a method parameter type.<br>
As a result, a record class will be generated for the classname of the annotated parameter.<br>
Not needed if the Record class was already annotated in its own declaration.
```java
  public void doSomething(@Record PojoA pojoA) {
    // method content... 
  }
```
Important Note: the annotation should be used ONLY on Record classes created in your own project. Any other types are not supported.<br>

<br>

### @SuperRecord

To be used <b>only</b> on top of either POJO or Record classes.<br>
As a result, a record class with the name _pojo_or_record_class_name_ + "SuperRecord" will be generated and all fields from the list of
Pojo and/or Record classes provided in the <u>mandatory</u> <b>mergeWith</b> attribute, will be added to the fields list of the
annotated POJO or Record class:<br>
```java
@SuperRecord(mergeWith = { Pojo1.class, Record1.class, Pojo2.class,... })
public class PojoA {
    // class content...
}
```
<p>
Important Note: the annotation should be used ONLY on POJO or Record classes created in your own project. Any other types are not supported.<br>


> The **alsoConvert** attribute available for both @Record and @Immutable, allows specifying additional types to be transformed into their fully immutable equivalent.
> The provided alsoConvert array value may contain a mix of your existing Records or POJOs .class values.

> The **superInterfaces** attribute available for all annotations, allows specifying a list of interfaces to be implemented by the generated Record class.<br>


## Sample POJO and Record classes for testing
[https://github.com/mohamed-ashraf-bayor/froporec-annotation-client](https://github.com/mohamed-ashraf-bayor/froporec-annotation-client)


## Issues, Bugs, Suggestions
Contribute to the project's growth by reporting issues or making improvement suggestions [here](https://github.com/mohamed-ashraf-bayor/froporec/issues/new/choose)

<br>
<br>

###### &#169; 2021-2022, [Froporec](https://github.com/mohamed-ashraf-bayor/froporec) is an open source project, currently distributed under the [MIT License](https://github.com/mohamed-ashraf-bayor/froporec/blob/master/LICENSE)
