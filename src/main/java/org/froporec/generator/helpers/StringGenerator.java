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

import java.util.List;

import static java.lang.Character.isUpperCase;
import static java.lang.Character.toUpperCase;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

/**
 * A bunch of String literals and commonly used string handling utils functions
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
     * "or"
     */
    String OR = "or";

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
    String COMMA = ",";

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
     * '&lt;' sign used in java collection generic definition
     */
    String INFERIOR_SIGN = "<";

    /**
     * '&gt;' sign used in java collection generic definition
     */
    String SUPERIOR_SIGN = ">";

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
     * "package"
     */
    String PACKAGE = "package";

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
     * "static"
     */
    String STATIC = "static";

    /**
     * "with"
     */
    String WITH = "with";

    /**
     * "buildWith"
     */
    String BUILD_WITH = "buildWith";

    /**
     * "return"
     */
    String RETURN = "return";

    /**
     * "new"
     */
    String NEW = "new";

    /**
     * "entry"
     */
    String ENTRY = "entry";

    /**
     * "-&gt;"
     */
    String LAMBDA_SYMB = "->";

    /**
     * Default value to use for boolean returned values
     */
    String DEFAULT_BOOLEAN_VALUE = "false";

    /**
     * Default value to use for numeric returned values (int, long, float, double,...)
     */
    String DEFAULT_LONG_VALUE = "0L";

    /**
     * Default value to use for numeric returned values (int, long, float, double,...)
     */
    String DEFAULT_FLOAT_VALUE = "0F";

    /**
     * Default value to use for numeric returned values (int, long, float, double,...)
     */
    String DEFAULT_DOUBLE_VALUE = "0.0";

    /**
     * Default value to use for numeric returned values (int, long, float, double,...)
     */
    String DEFAULT_NUMBER_VALUE = "0";

    /**
     * Default value to use for Object returned values
     */
    String DEFAULT_NULL_VALUE = "null";

    /**
     * "&lt;T&gt;"
     */
    String GENERIC_T_SYMB = "<T>";

    /**
     * Removes all commas from the provided string
     *
     * @param text contains commas as a string separator
     * @return provided text with all commas removed
     */
    static String removeCommaSeparator(String text) {
        return stream(text.split(COMMA)).collect(joining());
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

    /**
     * Applies the Java constants naming convention to the provided field name
     *
     * @param fieldName provided field name to process. ex: firstName
     * @return the transformed field name after applying the java constant naming convention. ex: FIRST_NAME
     */
    static String javaConstantNamingConvention(String fieldName) {
        var allChars = fieldName.toCharArray();
        var constantNameChars = new StringBuilder().append(toUpperCase(allChars[0]));
        for (int i = 1; i < allChars.length; i++) {
            constantNameChars.append(isUpperCase(allChars[i]) ? UNDERSCORE + allChars[i] : toUpperCase(allChars[i]));
        }
        return constantNameChars.toString();
    }

    /**
     * Modifies the provided {@link StringBuilder} instance by removing the last (amountOfChars) characters
     *
     * @param text          instance of StringBuilder to process
     * @param amountOfChars amount of characters to remove by the end of the provided StringBuilder instance
     */
    static void removeLastChars(StringBuilder text, int amountOfChars) {
        if (amountOfChars > 0) {
            text.deleteCharAt(text.length() - 1);
            removeLastChars(text, amountOfChars - 1);
        }
    }
}