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
For other build tools, please check: [Maven Central](https://search.maven.org/artifact/org.froporec/froporec).


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


