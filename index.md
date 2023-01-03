Froporec is a Java Annotation Processor, requiring a minimum of Java 17 and providing annotations which can be used to:
- turn existing POJOs into Records: by generating Record classes with same data structure as the annotated POJO classes,
- turn existing Records into "fully" immutable Records: one of the current limitations of Java Records is that a Record can contain either a mutable Collection object or a mutable POJO as part of its fields or attributes, making it only "partially" or "shallowly" immutable. Annotating such existing Record classes with Froporec will generate "fully" immutable versions, which can be used in your project to ensure data security. 

As of v1.4 Froporec also provides a bunch of "convenient" Factory Methods which are really helpful while handling the creation or use of several instances of the same Record class within the same program.


<br>
<br>

> ### Latest Release: Froporec 1.4
> - Added **5 Static Factory Methods** and **2 Instance Factory Methods** to all generated Record classes (except for SuperRecord classes).
>   - The generated static factory methods are convenient for creating new instances of the generated Record class, with data from either instances of the POJO (or Record) class being converted, or instances of the Record class being generated, with the possibility of 'overriding' the instances fields values by combining with the use of a Map of custom values for each field.
>   - The generated instance factory methods are convenient for creating new instances of the generated Record class, with data from the current instance, and with the possibility of 'overriding' any field value by providing custom values for the desired fields. 
> - Added **Constants Declarations** for fields names, in all generated Record classes (except for SuperRecord classes). Each constant is a String literal with its value being the name of one of the fields of the generated Record class. They are used by the generated factory methods, and can also be accessed from anywhere in your project.
> - Major improvement of collections handling.
> - Minor bug fixes.


<br>

## Videos

v1.4 - Factory Methods: [Coming Soon](https://www.youtube.com/channel/UCLhc2NBAbsw-WDJBlsxFCEg)

Code Migration to Java 17 using FROPOREC and JISEL: [https://youtu.be/iML8EjMIDLc](https://youtu.be/iML8EjMIDLc)

v1.3: [https://youtu.be/Gzv65UmWmzw](https://youtu.be/Gzv65UmWmzw)

v1.2 Quick Intro: [https://youtu.be/Yu3bR8ZkpYE](https://youtu.be/Yu3bR8ZkpYE)

Project's Pitch (v1.0): [https://youtu.be/IC0aS_biaMs](https://youtu.be/IC0aS_biaMs)

<br>

## Installation

If you are running a Maven project, add the latest release dependency to your pom.xml
```xml
<dependency>
    <groupId>org.froporec</groupId>
    <artifactId>froporec</artifactId>
    <version>1.4</version>
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
                            <version>1.4</version>
                        </path>
                    </annotationProcessorPaths>
                </configuration>
            </plugin>
        </plugins>
    </build>
```

For other build tools, please check: [Maven Central](https://search.maven.org/artifact/org.froporec/froporec/1.4/jar).

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
- Above code can be written using the **alsoConvert** attribute, avoiding multiple uses of @Record:<br>
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
- Above code can be written using the **alsoConvert** attribute, avoiding multiple uses of @Immutable:<br>
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
As a result, a record class with the name _pojo_or_record_class_name_ + **SuperRecord** will be generated and all fields from the list of
Pojo and/or Record classes provided in the <u>mandatory</u> <b>mergeWith</b> attribute, will be added to the fields list of the
annotated POJO or Record class:<br>
```java
@SuperRecord(mergeWith = { Pojo1.class, Record1.class, Pojo2.class,... })
public class PojoA {
    // class content...
}
```
Important Note: the annotation should be used ONLY on POJO or Record classes created in your own project. Any other types are not supported.<br><br>

<br>

> The **alsoConvert** attribute available for both @Record and @Immutable, allows specifying additional types to be transformed into their fully immutable equivalent.
> The provided alsoConvert array value may contain a mix of your existing Records or POJOs .class values.

> The **superInterfaces** attribute available for all annotations, allows specifying a list of interfaces to be implemented by the generated Record class.<br>

<br><br>



## Constants Declarations for Fields Names

For all generated Record classes (except for SuperRecord), constants declarations are added within the Record class body.<br>
<br>
Each constant is a String literal with its value being the name of one of the fields of the generated Record class. They are used by the generated factory methods, and can also be accessed from anywhere in your project.<br>
<br>
Below sample code shows the constants declarations added to the generated ImmutableExamReport Record class:
<br>

```java
public record ImmutableExamReport(int candidateId, java.lang.String fullName, com.bayor.froporec.annotation.client.factorymthdsdemo.factorymthds.ContactInfo contactInfo, java.lang.Integer examId, java.lang.String submittedExamContent, java.time.LocalDate examDate, java.lang.Double score, java.lang.Boolean passed) {

    public static final String CANDIDATE_ID = "candidateId"; // type: int
    public static final String FULL_NAME = "fullName"; // type: java.lang.String
    public static final String CONTACT_INFO = "contactInfo"; // type: com.bayor.froporec.annotation.client.factorymthdsdemo.factorymthds.ContactInfo
    public static final String EXAM_ID = "examId"; // type: java.lang.Integer
    public static final String SUBMITTED_EXAM_CONTENT = "submittedExamContent"; // type: java.lang.String
    public static final String EXAM_DATE = "examDate"; // type: java.time.LocalDate
    public static final String SCORE = "score"; // type: java.lang.Double
    public static final String PASSED = "passed"; // type: java.lang.Boolean
    
    // custom constructor and factory methods follow...
    // ...
}
```

<br><br>

## Factory Methods

For all generated Record classes (except for SuperRecord), factory methods are added within the body of the classes.

The generated static factory methods are convenient for creating new instances of the generated Record class, with data from either instances of the POJO (or Record) class being converted, or instances of the Record class being generated, with the possibility of 'overriding' the instances fields values by combining with the use of a Map of custom values for each field.<br>
<br>
Below sample code shows the 5 static factory methods added to the generated ImmutableExamReport Record class: 

```java
public static ImmutableExamReport buildWith(com.bayor.froporec.annotation.client.factorymthdsdemo.factorymthds.ExamReport examReport) {
    return new ImmutableExamReport(examReport.candidateId(), examReport.fullName(), examReport.contactInfo(), examReport.examId(), examReport.submittedExamContent(), examReport.examDate(), examReport.score(), examReport.passed());
}

public static ImmutableExamReport buildWith(com.bayor.froporec.annotation.client.factorymthdsdemo.factorymthds.ImmutableExamReport immutableExamReport) {
    return new ImmutableExamReport(immutableExamReport.candidateId(), immutableExamReport.fullName(), immutableExamReport.contactInfo(), immutableExamReport.examId(), immutableExamReport.submittedExamContent(), immutableExamReport.examDate(), immutableExamReport.score(), immutableExamReport.passed());
}

@java.lang.SuppressWarnings("unchecked")
public static ImmutableExamReport buildWith(java.util.Map<String, Object> fieldsNameValuePairs) {
    return new ImmutableExamReport((int) fieldsNameValuePairs.getOrDefault(CANDIDATE_ID, 0), (java.lang.String) fieldsNameValuePairs.getOrDefault(FULL_NAME, null), (com.bayor.froporec.annotation.client.factorymthdsdemo.factorymthds.ContactInfo) fieldsNameValuePairs.getOrDefault(CONTACT_INFO, null), (java.lang.Integer) fieldsNameValuePairs.getOrDefault(EXAM_ID, null), (java.lang.String) fieldsNameValuePairs.getOrDefault(SUBMITTED_EXAM_CONTENT, null), (java.time.LocalDate) fieldsNameValuePairs.getOrDefault(EXAM_DATE, null), (java.lang.Double) fieldsNameValuePairs.getOrDefault(SCORE, null), (java.lang.Boolean) fieldsNameValuePairs.getOrDefault(PASSED, null));
}

@java.lang.SuppressWarnings("unchecked")
public static ImmutableExamReport buildWith(com.bayor.froporec.annotation.client.factorymthdsdemo.factorymthds.ExamReport examReport, java.util.Map<String, Object> fieldsNameValuePairs) {
    return new ImmutableExamReport((int) fieldsNameValuePairs.getOrDefault(CANDIDATE_ID, examReport.candidateId()), (java.lang.String) fieldsNameValuePairs.getOrDefault(FULL_NAME, examReport.fullName()), (com.bayor.froporec.annotation.client.factorymthdsdemo.factorymthds.ContactInfo) fieldsNameValuePairs.getOrDefault(CONTACT_INFO, examReport.contactInfo()), (java.lang.Integer) fieldsNameValuePairs.getOrDefault(EXAM_ID, examReport.examId()), (java.lang.String) fieldsNameValuePairs.getOrDefault(SUBMITTED_EXAM_CONTENT, examReport.submittedExamContent()), (java.time.LocalDate) fieldsNameValuePairs.getOrDefault(EXAM_DATE, examReport.examDate()), (java.lang.Double) fieldsNameValuePairs.getOrDefault(SCORE, examReport.score()), (java.lang.Boolean) fieldsNameValuePairs.getOrDefault(PASSED, examReport.passed()));
}

@java.lang.SuppressWarnings("unchecked")
public static ImmutableExamReport buildWith(com.bayor.froporec.annotation.client.factorymthdsdemo.factorymthds.ImmutableExamReport immutableExamReport, java.util.Map<String, Object> fieldsNameValuePairs) {
    return new ImmutableExamReport((int) fieldsNameValuePairs.getOrDefault(CANDIDATE_ID, immutableExamReport.candidateId()), (java.lang.String) fieldsNameValuePairs.getOrDefault(FULL_NAME, immutableExamReport.fullName()), (com.bayor.froporec.annotation.client.factorymthdsdemo.factorymthds.ContactInfo) fieldsNameValuePairs.getOrDefault(CONTACT_INFO, immutableExamReport.contactInfo()), (java.lang.Integer) fieldsNameValuePairs.getOrDefault(EXAM_ID, immutableExamReport.examId()), (java.lang.String) fieldsNameValuePairs.getOrDefault(SUBMITTED_EXAM_CONTENT, immutableExamReport.submittedExamContent()), (java.time.LocalDate) fieldsNameValuePairs.getOrDefault(EXAM_DATE, immutableExamReport.examDate()), (java.lang.Double) fieldsNameValuePairs.getOrDefault(SCORE, immutableExamReport.score()), (java.lang.Boolean) fieldsNameValuePairs.getOrDefault(PASSED, immutableExamReport.passed()));
}
```

<br>
The generated instance factory methods are convenient for creating new instances of the generated Record class, with data from the current instance, and with the possibility of 'overriding' any field value by providing custom values for the desired fields.<br>
<br>
Below sample code shows the 2 instance factory methods added to the generated ImmutableExamReport Record class:<br>

```java
@java.lang.SuppressWarnings("unchecked")
public ImmutableExamReport with(java.util.Map<String, Object> fieldsNameValuePairs) {
    return new ImmutableExamReport((int) fieldsNameValuePairs.getOrDefault(CANDIDATE_ID, this.candidateId()), (java.lang.String) fieldsNameValuePairs.getOrDefault(FULL_NAME, this.fullName()), (com.bayor.froporec.annotation.client.factorymthdsdemo.factorymthds.ContactInfo) fieldsNameValuePairs.getOrDefault(CONTACT_INFO, this.contactInfo()), (java.lang.Integer) fieldsNameValuePairs.getOrDefault(EXAM_ID, this.examId()), (java.lang.String) fieldsNameValuePairs.getOrDefault(SUBMITTED_EXAM_CONTENT, this.submittedExamContent()), (java.time.LocalDate) fieldsNameValuePairs.getOrDefault(EXAM_DATE, this.examDate()), (java.lang.Double) fieldsNameValuePairs.getOrDefault(SCORE, this.score()), (java.lang.Boolean) fieldsNameValuePairs.getOrDefault(PASSED, this.passed()));
}

@java.lang.SuppressWarnings("unchecked")
public <T> ImmutableExamReport with(String fieldName, T fieldValue) {
    return new ImmutableExamReport(fieldName.equals(CANDIDATE_ID) ? (int) fieldValue : this.candidateId(), fieldName.equals(FULL_NAME) ? (java.lang.String) fieldValue : this.fullName(), fieldName.equals(CONTACT_INFO) ? (com.bayor.froporec.annotation.client.factorymthdsdemo.factorymthds.ContactInfo) fieldValue : this.contactInfo(), fieldName.equals(EXAM_ID) ? (java.lang.Integer) fieldValue : this.examId(), fieldName.equals(SUBMITTED_EXAM_CONTENT) ? (java.lang.String) fieldValue : this.submittedExamContent(), fieldName.equals(EXAM_DATE) ? (java.time.LocalDate) fieldValue : this.examDate(), fieldName.equals(SCORE) ? (java.lang.Double) fieldValue : this.score(), fieldName.equals(PASSED) ? (java.lang.Boolean) fieldValue : this.passed());
}
```

<br>
<br>
<br>


## Sample POJO and Record classes for testing
[https://github.com/mohamed-ashraf-bayor/froporec-annotation-client](https://github.com/mohamed-ashraf-bayor/froporec-annotation-client)


## Issues, Bugs, Suggestions
Contribute to the project's growth by reporting issues or making improvement suggestions [here](https://github.com/mohamed-ashraf-bayor/froporec/issues/new/choose)

<br>
<br>

###### &#169; 2021-2023, [Froporec](https://github.com/mohamed-ashraf-bayor/froporec) is an open source project, currently distributed under the [MIT License](https://github.com/mohamed-ashraf-bayor/froporec/blob/master/LICENSE)
