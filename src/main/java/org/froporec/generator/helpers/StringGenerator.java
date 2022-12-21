/**
 * Copyright (c) 2021-2023 Mohamed Ashraf Bayor
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

import org.froporec.annotations.Immutable;
import org.froporec.annotations.Record;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import java.util.List;

import static java.lang.Character.isUpperCase;
import static java.lang.Character.toUpperCase;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

/**
 * A bunch of String literals and commonly used string handling functions
 */
public interface StringGenerator {

    /**
     * Empty string
     */
    String EMPTY_STRING = "";

    /**
     * " "
     */
    String SPACE = " ";

    /**
     * "="
     */
    String EQUALS_STR = "=";

    /**
     * "\""
     */
    String DOUBLE_QUOTES = "\"";

    /**
     * "@"
     */
    String AT_SIGN = "@";

    /**
     * "_"
     */
    String UNDERSCORE = "_";

    /**
     * "Pojo"
     */
    String POJO = "Pojo";

    /**
     * "Record"
     */
    String RECORD = "Record";

    /**
     * "Immutable"
     */
    String IMMUTABLE = "Immutable";

    /**
     * "SuperRecord"
     */
    String SUPER_RECORD = "SuperRecord";

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
     * all Froporec annotations qualified names
     */
    List<String> ALL_ANNOTATIONS_QUALIFIED_NAMES = List.of(ORG_FROPOREC_RECORD, ORG_FROPOREC_IMMUTABLE, ORG_FROPOREC_SUPER_RECORD);

    /**
     * "alsoConvert"
     */
    String ALSO_CONVERT_ATTRIBUTE = "alsoConvert";

    /**
     * "mergeWith"
     */
    String MERGE_WITH_ATTRIBUTE = "mergeWith";

    /**
     * "superInterfaces"
     */
    String SUPER_INTERFACES_ATTRIBUTE = "superInterfaces";

    /**
     * "implements"
     */
    String IMPLEMENTS = "implements";

    /**
     * ","
     */
    String COMMA_SEPARATOR = ",";

    /**
     * ";"
     */
    String SEMI_COLON = ";";

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
     * "{"
     */
    String OPENING_BRACE = "{";

    /**
     * "}"
     */
    String CLOSING_BRACE = "}";

    /**
     * ".class"
     */
    String DOT_CLASS = ".class";

    /**
     * "."
     */
    String DOT = ".";

    /**
     * New line
     */
    String NEW_LINE = "\n";

    /**
     * Tabulation
     */
    String TAB = "\t";

    /**
     * "this"
     */
    String THIS = "this";

    /**
     * "public"
     */
    String PUBLIC = "public";

    /**
     * "get" - Starting string of Pojos non-boolean getters
     */
    String GET = "get";

    /**
     * "is" - Starting string of Pojos boolean getters
     */
    String IS = "is";

    /**
     * "Success"
     */
    String SUCCESS = "Success";

    /**
     * "Failure"
     */
    String FAILURE = "Failure";

    /**
     * Message displayed during code compilation, along with the name of a successfully generated Record source file
     */
    String GENERATION_SUCCESS_MSG = "\t> Successfully generated";

    /**
     * Generation report info message format
     */
    String GENERATION_REPORT_MSG_FORMAT = "%s for %s:\n\t\t%s";

    /**
     * Separator used while displaying each one of the generated or skipped filenames
     */
    String GENERATION_REPORT_ELEMENTS_SEPARATOR = "\n\t\t";

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
     * "static"
     */
    String STATIC = "static";

    /**
     * "String"
     */
    String STRING = "String";

    /**
     * "buildWith"
     */
    String BUILD_WITH = "buildWith";

    /**
     * "return"
     */
    String RETURN = "return";

    /**
     * "true"
     */
    String TRUE = "true";

    /**
     * "false"
     */
    String FALSE = "false";

    /**
     * "null"
     */
    String NULL = "null";

    /**
     * "null"
     */
    String NEW = "new";

    /**
     * Regex expression to read any attribute value provided within {}
     */
    String METHOD_BODY_CONTENT_REGEX = "\\{(.*?)\\}";

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

    /**
     * Constructs the qualified name of the generated super record class.
     * Based on whether or not the qualified name of the passed in element ends with 'Record'
     *
     * @param element {@link Element} instance of the annotated class
     * @return the qualified name of the fully immutable super record class being generated from an annotated Pojo or Record class.<br>
     * ex: ...RecordClassName+"SuperRecord" or ...PojoClassName+"SuperRecord"
     */
    static String constructSuperRecordQualifiedNameBasedOnElementType(Element element) {
        var qualifiedName = element.toString();
        return qualifiedName.endsWith(RECORD)
                ? qualifiedName.substring(0, qualifiedName.lastIndexOf(RECORD)) + SUPER_RECORD
                : qualifiedName + SUPER_RECORD;
    }

    /**
     * Constructs the simple name of the generated super record class.
     * Based on whether or not the simple name of the passed in element ends with 'Record'
     *
     * @param element {@link Element} instance of the annotated class
     * @return the qualified name of the fully immutable super record class being generated from an annotated Pojo or Record class.<br>
     * ex: ...RecordClassName+"SuperRecord" or ...PojoClassName+"SuperRecord"
     */
    static String constructSuperRecordSimpleNameBasedOnElementType(Element element) {
        var simpleName = element.getSimpleName().toString();
        return simpleName.endsWith(RECORD)
                ? simpleName.substring(0, simpleName.lastIndexOf(RECORD)) + SUPER_RECORD
                : simpleName + SUPER_RECORD;
    }

    /**
     * Removes all commas from the provided string
     *
     * @param text contains commas as a string separator
     * @return provided text with all commas removed
     */
    static String removeCommaSeparator(String text) {
        return stream(text.split(COMMA_SEPARATOR)).collect(joining());
    }

    /**
     * Returns the provided string with its 1st char 'lowercased'
     *
     * @param text string to convert
     * @return the provided string with its 1st char 'lowercased'
     */
    static String lowerCase1stChar(String text) {
        return text.substring(0, 1).toLowerCase() + text.substring(1);
    }

    static String javaConstantNamingConvention(String fieldName) {
        var allChars = fieldName.toCharArray();
        var constantNameChars = new StringBuilder().append(toUpperCase(allChars[0]));
        for (int i = 1; i < allChars.length; i++) {
            constantNameChars.append(isUpperCase(allChars[i]) ? UNDERSCORE + toUpperCase(allChars[i]) : toUpperCase(allChars[i]));
        }
        return constantNameChars.toString();
    }
}