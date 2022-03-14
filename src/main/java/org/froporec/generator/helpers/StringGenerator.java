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

import org.froporec.annotations.GenerateImmutable;
import org.froporec.annotations.GenerateRecord;
import org.froporec.annotations.Immutable;
import org.froporec.annotations.Record;

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
     * @ string literal
     */
    String AT_SIGN = "@";

    /**
     * "Pojo" string literal
     */
    String POJO = "Pojo";

    /**
     * "Record" string literal
     */
    String RECORD = "Record";

    /**
     * "Immutable" String literal
     */
    String IMMUTABLE = "Immutable";

    /**
     * "SuperRecord" string literal
     */
    String SUPER_RECORD = "SuperRecord";

    /**
     * {@link GenerateRecord} qualified name
     */
    @Deprecated(forRemoval = true, since = "1.3")
    String ORG_FROPOREC_GENERATE_RECORD = "org.froporec.annotations.GenerateRecord";

    /**
     * {@link GenerateImmutable} qualified name
     */
    @Deprecated(forRemoval = true, since = "1.3")
    String ORG_FROPOREC_GENERATE_IMMUTABLE = "org.froporec.annotations.GenerateImmutable";

    /**
     * {@link Record} qualified name
     */
    String ORG_FROPOREC_RECORD = "org.froporec.annotations.Record";

    /**
     * {@link Record} qualified name
     */
    String ORG_FROPOREC_SUPER_RECORD = "org.froporec.annotations.SuperRecord";

    /**
     * {@link Immutable} qualified name
     */
    String ORG_FROPOREC_IMMUTABLE = "org.froporec.annotations.Immutable";

    /**
     * "includeTypes" attribute String literal
     * @deprecated use 'alsoConvert' instead
     */
    @Deprecated
    String INCLUDE_TYPES_ATTRIBUTE = "includeTypes";

    /**
     * "alsoConvert" attribute String literal
     */
    String ALSO_CONVERT_ATTRIBUTE = "alsoConvert";

    /**
     * "mergeWith" attribute String literal
     */
    String MERGE_WITH_ATTRIBUTE = "mergeWith";

    /**
     * "superInterfaces" attribute String literal
     */
    String SUPER_INTERFACES_ATTRIBUTE = "superInterfaces";

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
     * "Success" String literal
     */
    String SUCCESS = "Success";

    /**
     * "Failure" String literal
     */
    String FAILURE = "Failure";

    /**
     * Message displayed during code compilation, along with the name of a successfully generated Record source file
     */
    String GENERATION_SUCCESS_MSG = "\t> Successfully generated";

    /**
     * Generation report info message format
     */
    String GENERATION_REPORT_MSG_FORMAT = "%s - %s:%n\t\t";

    /**
     * Separator used while displaying each one of the generated or skipped filenames
     */
    String GENERATION_REPORT_ELEMENTS_SEPARATOR = "%n\t\t";

    /**
     * Warning message displayed during code compilation, indicating annotated elements skipped during generation process
     */
    String SKIPPED_ELEMENTS_WARNING_MSG_FORMAT = "\t> Skipped %s annotated elements (must be %s classes):%n\t\t%s";

    /**
     * Message displayed during code compilation, in case an error occurred during a Record source file generation process
     */
    String GENERATION_FAILURE_MSG = "\t> Error generating";

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
    static String constructImmutableRecordQualifiedName(String qualifiedClassName) {
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
    static String constructImmutableQualifiedNameBasedOnElementType(Element element) {
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
    static String constructImmutableSimpleNameBasedOnElementType(Element element) {
        var immutableQualifiedName = constructImmutableQualifiedNameBasedOnElementType(element);
        return immutableQualifiedName.substring(immutableQualifiedName.lastIndexOf(DOT) + 1);
    }
}