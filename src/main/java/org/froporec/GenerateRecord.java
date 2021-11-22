package org.froporec;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to apply in 3 different ways:
 * - on top of a class name while the class is being defined. As a result, a record class with the name classname + "Record" will be generated
 * - on top of a method name. As a result, a record class will be generated for the classname of each parameter of the annotated method
 * - next to a method parameter type. As a result, a record class will be generated for the classname of the annotated parameter
 * Applying the annotation on a Collection object will generate the Record class for the class of the object stored in the collection
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.PARAMETER})
@Documented
public @interface GenerateRecord {
}