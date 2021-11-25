/**
 * Copyright (c) 2021 Mohamed Ashraf Bayor
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
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
 * Annotation to apply in 3 different ways:<br><br>
 *
 * - on top of a class name while the class is being defined. As a result, a record class with the name classname + "Record" will be generated<br><br>
 *
 * &#64;GenerateRecord<br>
 * public class PojoA {<br>
 *     // class content<br>
 * }<br><br>
 *
 * - next to a class field type for classes containing other POJO classes. Add the annotation before the POJO type name, while the field is being defined.<br>
 * As a result, a record class will be generated for the classname of the annotated generic and the record class generated for
 * the enclosing POJO will contain references to corresponding record class generated for each one of the annotated enclosed POJOs.<br>
 * Not needed if the POJO class was already annotated while being defined.<br><br>
 *
 * &#64;GenerateRecord<br>
 * public class PojoA {<br>
 *     private &#64;GenerateRecord PojoB pojoB;<br>
 * }<br><br>
 *
 * - next to a method parameter type. As a result, a record class will be generated for the classname of the annotated parameter.<br>
 *  Not needed if the POJO class was already annotated while being defined.<br><br>
 *
 *  public void doSomething(&#64;GenerateRecord PojoA pojoA) {<br>
 *      // method content...<br>
 *  }<br><br>
 *
 * Important Note: the annotation should be used ONLY on POJO classes created in your own project. Any other types (including the JVMs) are not supported
 *
 * @author Mohamed Ashraf Bayor
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE, ElementType.TYPE_USE})
@Documented
public @interface GenerateRecord {
}