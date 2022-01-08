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
package org.froporec.generator.helpers;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;

/**
 * A bunch of String literals and commonly used string handling functions
 */
public interface StringGenerator {

    /**
     * Empty string literal
     */
    String EMPTY_STRING = "";

    /**
     * " " String literal
     */
    String SPACE = " ";

    /**
     * "Record" string literal
     */
    String RECORD = "Record";

    /**
     * "Immutable" String literal
     */
    String IMMUTABLE = "Immutable";

    /**
     * {@link org.froporec.GenerateRecord} qualified name
     */
    String GENERATE_RECORD_QUALIFIED_NAME = "org.froporec.GenerateRecord";

    /**
     * {@link org.froporec.GenerateImmutable} qualified name
     */
    String GENERATE_IMMUTABLE_QUALIFIED_NAME = "org.froporec.GenerateImmutable";

    /**
     * "includeTypes" attribute String literal
     */
    String INCLUDE_TYPES_ATTRIBUTE = "includeTypes";

    /**
     * "," String literal
     */
    String COMMA_SEPARATOR = ",";

    /**
     * "("
     */
    String OPENING_PARENTHESIS = "(";

    /**
     * "("
     */
    String CLOSING_PARENTHESIS = ")";

    /**
     * '<' sign used in java collection generic definition
     */
    char INFERIOR_SIGN = '<';

    /**
     * '>' sign used in java collection generic definition
     */
    char SUPERIOR_SIGN = '>';

    /**
     * "{" String literal
     */
    String OPENING_BRACE = "{";

    /**
     * "}" String literal
     */
    String CLOSING_BRACE = "}";

    /**
     * ".class" String literal
     */
    String DOT_CLASS = ".class";

    /**
     * "." String literal
     */
    String DOT = ".";

    /**
     * "get" String literal. Starting string of Pojos non-boolean getters
     */
    String GET = "get";

    /**
     * "is" String literal. Starting string of Pojos boolean getters
     */
    String IS = "is";

    /**
     * Message displayed during code compilation, along with the name of a successfully generated Record source file
     */
    String GENERATION_SUCCESS_MSG_FORMAT = "\t> Successfully generated %s";

    /**
     * Message displayed during code compilation, in case an error occurred during a Record source file generation process
     */
    String GENERATION_FAILURE_MSG_FORMAT = "\t> Error generating %s";

    /**
     * Array of methods to exclude while pulling the list of all methods of a Pojo or Record class
     */
    String[] METHODS_TO_EXCLUDE = {"getClass", "wait", "notifyAll", "hashCode", "equals", "notify", "toString", "clone", "finalize"};

    /**
     * Constructs the qualified name of the fully immutable record class being generated from an annotated Record class
     *
     * @param qualifiedClassName qualified name of the annotated class
     * @return the qualified name of the fully immutable record class being generated from an annotated Record class. ex: "Immutable"+RecordClassName
     */
    default String constructImmutableRecordQualifiedName(final String qualifiedClassName) {
        return qualifiedClassName.contains(DOT)
                ? qualifiedClassName.substring(0, qualifiedClassName.lastIndexOf(DOT)) + DOT + IMMUTABLE + qualifiedClassName.substring(qualifiedClassName.lastIndexOf(DOT) + 1)
                : IMMUTABLE + qualifiedClassName;
    }

    /**
     * Constructs the qualified name of the generated record class. Handles both cases of the class being processed as either a Pojo or a Record
     *
     * @param element {@link Element} instance of the annotated class
     * @return the qualified name of the fully immutable record class being generated from an annotated Pojo or Record class.<br>
     * ex: ..."Immutable" + RecordClassName or ...PojoClassName+"Record"
     */
    default String constructImmutableQualifiedNameBasedOnElementType(final Element element) {
        return ElementKind.RECORD.equals(element.getKind())
                ? constructImmutableRecordQualifiedName(element.toString())
                : element + RECORD;
    }

    /**
     * Constructs the simple name of the generated record class. Handles both cases of the class being processed as either a Pojo or a Record
     *
     * @param element {@link Element} instance of the annotated class
     * @return the simple name of the fully immutable record class being generated from an annotated Pojo or Record class.<br>
     * ex: "Immutable" + RecordClassName or PojoClassName+"Record"
     */
    default String constructImmutableSimpleNameBasedOnElementType(final Element element) {
        var immutableQualifiedName = constructImmutableQualifiedNameBasedOnElementType(element);
        return immutableQualifiedName.substring(immutableQualifiedName.lastIndexOf(DOT) + 1);
    }
}