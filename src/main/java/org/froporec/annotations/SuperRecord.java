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
package org.froporec.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * May be used <b>only</b> on top of either POJO or Record classes<br><br>
 * <p>
 * As a result, a record class with the name pojo_class_name + "SuperRecord" will be generated and all fields from the list of
 * Pojo and/or Record classes provided in the <u>mandatory</u> <b>mergeWith</b> attribute will be added to the fields list of the
 * annotated POJO or Record class:<br><br>
 * <p>
 * &#64;SuperRecord(mergeWith = { Pojo1.class, Record1.class, Pojo2.class,... })<br>
 * public class PojoA {<br>
 * // class content<br>
 * }<br><br>
 * <p>
 * Important Note: the annotation should be used ONLY on POJO or Record classes created in your own project. Any other types are not supported. <br><br>
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE})
@Documented
public @interface SuperRecord {

    /**
     * <b>MANDATORY</b> attribute.<br>
     * Allows specifying a list of POJO and/or Record classes whose fields will be added along with the fields of the Record class to be generated
     *
     * @return an array of .class values
     */
    Class<?>[] mergeWith();

    /**
     * allows specifying a list of interfaces implemented by the generated Record class
     *
     * @return an array of .class values
     */
    Class<?>[] superInterfaces() default {};
}