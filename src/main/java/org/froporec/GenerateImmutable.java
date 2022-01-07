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
 * - on top of a Record class declaration. As a result, a record class with the name "Immutable" + recordclassname will be generated<br><br>
 * <p>
 * &#64;GenerateImmutable<br>
 * public record RecordA(int field1, String field2) {<br>
 * // record class content<br>
 * }<br><br>
 * <p>
 * - next to a record field type for classes containing nested POJO and/or Record classes. Add the annotation before the type name, during the field declaration.<br>
 * As a result, a record class will be generated for the classname of the annotated field and the record class generated for
 * the enclosing Record will contain references to corresponding immutable record class generated for each one of the annotated enclosed Records.<br>
 * Not needed if the Record class was already annotated during its declaration.<br><br>
 * <p>
 * &#64;GenerateImmutable<br>
 * public record RecordA(int field1, String field2, &#64;GenerateImmutable RecordB recordB) {<br>
 * }<br><br>
 * <p>
 * - next to a method parameter type. As a result, a record class will be generated for the recordclassname of the annotated parameter.<br>
 * Not needed if the Record class was already annotated during its declaration.<br><br>
 * <p>
 * public void doSomething(&#64;GenerateImmutable RecordA recordA) {<br>
 * // method content...<br>
 * }<br><br>
 * <p>
 * Important Note: the annotation should be used ONLY on Record classes created in your own project. Any other types (including the JVMs) are not supported. <br><br>
 *
 * The annotation can be used along with the "includeTypes" attribute which allows specifying additional types (POJOs or Records) to be
 * transformed in their fully immutable equivalent (Records for POJOs and Immutable Records for Records).
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Documented
public @interface GenerateImmutable {
    /**
     * allows to specify additional types (POJOs or Records) to be transformed in their fully immutable equivalent (Records for POJOs and Immutable Records for Records)
     *
     * @return an array of .class values
     */
    Class<?>[] includeTypes() default {};
}