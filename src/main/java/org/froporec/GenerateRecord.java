/**
 * Copyright (c) 2021-2022 Mohamed Ashraf Bayor
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.froporec;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to be applied in 3 different ways:<br><br>
 * <p>
 * - on top of a POJO class declaration.<br>
 * As a result, a record class with the name pojo_class_name + "Record" will be generated:<br><br>
 * <p>
 * &#64;GenerateRecord<br>
 * public class PojoA {<br>
 * // class content<br>
 * }<br><br>
 * <p>
 * - next to a class field type declaration for classes containing enclosed POJOs.<br>
 * Add the annotation before the POJO type name, in the field declaration. As a result, a record class will be generated for the
 * classname of the annotated field, and the record class generated for the enclosing POJO will contain a field referencing the
 * corresponding record class generated for the enclosed POJO.<br>
 * Not needed if the POJO class was already annotated in its own declaration or added to the list of .class values of the "includeTypes" attribute.<br><br>
 * <p>
 * &#64;GenerateRecord<br>
 * public class PojoA {<br>
 * private &#64;GenerateRecord PojoB pojoB;<br>
 * }<br><br>
 * <p>
 * Above code can be written using the "includeTypes" attribute, avoiding multiple uses of &#64;GenerateRecord:<br><br>
 * &#64;GenerateRecord(includeTypes = { PojoB.class })<br>
 * public class PojoA {<br>
 * private PojoB pojoB;<br>
 * }<br><br>
 * <p>
 * - next to a method parameter type.<br>
 * As a result, a record class will be generated for the classname of the annotated parameter.<br>
 * Not needed if the POJO class was already annotated in its own declaration.<br><br>
 * <p>
 * public void doSomething(&#64;GenerateRecord PojoA pojoA) {<br>
 * // method content...<br>
 * }<br><br>
 * <p>
 * Important Note: the annotation should be used ONLY on POJO classes created in your own project. Any other types are not supported. <br><br>
 * <p>
 * The "includeTypes" attribute allows specifying additional types to be transformed into their fully immutable equivalent.<br>
 * The provided "includeTypes" array value can contain a mix of your existing Records or POJOs .class values.
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Documented
public @interface GenerateRecord {
    /**
     * allows specifying additional types (POJOs or Records) to be transformed in their fully immutable equivalent (Records for POJOs and Immutable Records for Records)
     *
     * @return an array of .class values
     */
    Class<?>[] includeTypes() default {};
}