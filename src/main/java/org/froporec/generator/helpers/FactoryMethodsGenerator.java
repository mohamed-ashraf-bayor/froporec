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

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static org.froporec.generator.helpers.CodeGenerator.fieldName;
import static org.froporec.generator.helpers.CodeGenerator.immutableQualifiedNameBasedOnElementType;
import static org.froporec.generator.helpers.CodeGenerator.immutableSimpleNameBasedOnElementType;
import static org.froporec.generator.helpers.StringGenerator.javaConstantNamingConvention;
import static org.froporec.generator.helpers.StringGenerator.lowerCase1stChar;

/**
 * Generates the bodies of all static and non-static factory methods for the Record class being generated.<br><br>
 * ex:<br>
 * public static ImmutableExamReport buildWith(com.bayor.froporec.samples.ExamReport examReport) {<br>
 * return new ImmutableExamReport(examReport.candidateId(), examReport.fullName(), examReport.contactInfo(), ...);<br>
 * }<br>
 * <br>
 * public ImmutableExamReport with(java.util.Map&lt;String, Object&gt; fieldsNameValuePairs) {<br>
 * return new ImmutableExamReport((int) fieldsNameValuePairs.getOrDefault(CANDIDATE_ID, this.candidateId()), ...);<br>
 * }<br><br>
 * <p>
 * The params {@link Map} parameter of the provided implementation of the generateCode() method (from {@link CodeGenerator}) MUST contain
 * the following parameters names:<br>
 * - {@link CodeGenerator#ANNOTATED_ELEMENT}<br>
 * - {@link CodeGenerator#NON_VOID_METHODS_ELEMENTS_LIST}<br>
 * <br>
 * <p>
 * This functionality is NOT YET support for SuperRecord generation. If {@link CodeGenerator#IS_SUPER_RECORD} is part of the parameters,
 * its value must be FALSE
 */
public final class FactoryMethodsGenerator implements CodeGenerator {

    private static final String FACTORY_METHODS_FIELDS_MAP_DECLARATION = "java.util.Map<String, Object> fieldsNameValuePairs";
    private static final String FACTORY_METHODS_FIELDS_MAP_USE_FORMAT = "(%s) fieldsNameValuePairs.getOrDefault(%s, %s)";

    private static final String FACTORY_METHOD_FIELD_NAME_AND_VALUE_DECLARATION = "String fieldName, T fieldValue";
    private static final String FACTORY_METHOD_FIELD_NAME_AND_USE_FORMAT = "fieldName.equals(%s) ? (%s) fieldValue : this.%s()";

    private static final String JAVA_LANG_SUPPRESS_WARNINGS_UNCHECKED = "@java.lang.SuppressWarnings(\"unchecked\")";

    /**
     * Regex expression to read a method body. should be used with Pattern.DOTALL mode
     */
    private static final String METHOD_BODY_CONTENT_REGEX = "\\{(.*?)\\}";

    /**
     * Regex expression to read the string content (params) within the call to a record canonical constructor: "this(...);"
     */
    private static final String CANONICAL_CONSTRUCTOR_PARAMS_REGEX = "this\\((.*?)\\);";

    private final ProcessingEnvironment processingEnv;

    private final Map<String, Set<Element>> allElementsTypesToConvertByAnnotation;

    private final CodeGenerator customConstructorGenerator;

    private final SupportedCollectionsFieldsGenerator collectionsGenerator;


    /**
     * FactoryMethodsGenerator constructor. Instantiates needed instance of {@link CustomConstructorGenerator} and {@link CollectionsGenerator}
     *
     * @param processingEnv                         {@link ProcessingEnvironment} object, needed to access low-level information regarding the used annotations
     * @param allElementsTypesToConvertByAnnotation {@link Set} of {@link Element} instances grouped by the annotation String representation
     */
    public FactoryMethodsGenerator(ProcessingEnvironment processingEnv,
                                   Map<String, Set<Element>> allElementsTypesToConvertByAnnotation) {
        this.processingEnv = processingEnv;
        this.allElementsTypesToConvertByAnnotation = allElementsTypesToConvertByAnnotation;
        this.customConstructorGenerator = new CustomConstructorGenerator(this.processingEnv, this.allElementsTypesToConvertByAnnotation, null);
        this.collectionsGenerator = new CollectionsGenerator(this.processingEnv, this.allElementsTypesToConvertByAnnotation);
    }

    private void buildFactoryMethods(StringBuilder recordClassContent, Element annotatedElement, List<Element> nonVoidMethodsElementsList) {

        var annotatedTypeElement = (TypeElement) processingEnv.getTypeUtils().asElement(annotatedElement.asType());
        var annotatedElementQualifiedName = annotatedTypeElement.getQualifiedName().toString();
        var annotatedElementFieldName = lowerCase1stChar(annotatedTypeElement.getSimpleName().toString());

        recordClassContent.append(NEW_LINE);

        // static factory mthd with annotated elemnt instance as single param
        recordClassContent.append(
                factoryMethodDeclarationAndBody(
                        immutableSimpleNameBasedOnElementType(annotatedTypeElement),
                        annotatedElementQualifiedName + SPACE + annotatedElementFieldName,
                        extractParamsFromCanonicalConstructorCall(annotatedElement, nonVoidMethodsElementsList),
                        true
                )
        );

        recordClassContent.append(NEW_LINE);

        // static factory mthd with generated record instance as single param
        var methodParamName = lowerCase1stChar(immutableSimpleNameBasedOnElementType(annotatedTypeElement));
        recordClassContent.append(
                factoryMethodDeclarationAndBody(
                        immutableSimpleNameBasedOnElementType(annotatedTypeElement),
                        immutableQualifiedNameBasedOnElementType(annotatedTypeElement) + SPACE + methodParamName,
                        nonVoidMethodsElementsList.stream()
                                .map(element -> methodParamName + DOT + fieldName(element).get() + OPENING_PARENTHESIS + CLOSING_PARENTHESIS)
                                .collect(Collectors.joining(COMMA + SPACE)),
                        true
                )
        );

        recordClassContent.append(NEW_LINE);

        var nonVoidMethodsElementsReturnTypesMap = nonVoidMethodsElementsReturnTypesMapFromList(nonVoidMethodsElementsList);

        // static factory mthd with Map as single param
        recordClassContent.append(TAB + JAVA_LANG_SUPPRESS_WARNINGS_UNCHECKED + NEW_LINE);
        recordClassContent.append(
                factoryMethodDeclarationAndBody(
                        immutableSimpleNameBasedOnElementType(annotatedTypeElement),
                        FACTORY_METHODS_FIELDS_MAP_DECLARATION,
                        nonVoidMethodsElementsList.stream()
                                .map(nonVoidMethodElement -> format(
                                        //"(<returnType>) fieldsNameValuePairs.getOrDefault("<fieldName>", <defaultValue>)"
                                        FACTORY_METHODS_FIELDS_MAP_USE_FORMAT,
                                        fieldNameAndTypePair(nonVoidMethodElement, nonVoidMethodsElementsReturnTypesMap, allElementsTypesToConvertByAnnotation,
                                                processingEnv, collectionsGenerator).get(fieldName(nonVoidMethodElement).get()),
                                        javaConstantNamingConvention(fieldName(nonVoidMethodElement).get()),
                                        defaultReturnValueForMethod(nonVoidMethodElement)
                                ))
                                .collect(Collectors.joining(COMMA + SPACE)),
                        true
                )
        );

        recordClassContent.append(NEW_LINE);

        // static factory mthd with annotated elemnt instance + Map as params
        recordClassContent.append(TAB + JAVA_LANG_SUPPRESS_WARNINGS_UNCHECKED + NEW_LINE);
        var paramsFromCanonicalConstructorCall = extractParamsFromCanonicalConstructorCall(annotatedElement, nonVoidMethodsElementsList)
                .replaceAll(COMMA + SPACE + ENTRY + SPACE + LAMBDA_SYMB, AT_SIGN) // replacing occurrences of ", entry ->" with "@" to avoid issues while splitting
                .split(COMMA + SPACE);
        recordClassContent.append(
                factoryMethodDeclarationAndBody(
                        immutableSimpleNameBasedOnElementType(annotatedTypeElement),
                        annotatedElementQualifiedName + SPACE + annotatedElementFieldName + COMMA + SPACE + FACTORY_METHODS_FIELDS_MAP_DECLARATION,
                        nonVoidMethodsElementsList.stream()
                                .map(nonVoidMethodElement -> format(
                                        // "(returnType) fieldsNameValuePairs.getOrDefault("<fieldName>", <annotatedElementFieldName.getterMthd>())"
                                        FACTORY_METHODS_FIELDS_MAP_USE_FORMAT,
                                        fieldNameAndTypePair(nonVoidMethodElement, nonVoidMethodsElementsReturnTypesMap, allElementsTypesToConvertByAnnotation,
                                                processingEnv, collectionsGenerator).get(fieldName(nonVoidMethodElement).get()),
                                        javaConstantNamingConvention(fieldName(nonVoidMethodElement).get()),
                                        stream(paramsFromCanonicalConstructorCall)
                                                .filter(param -> param.contains(annotatedElementFieldName + DOT + nonVoidMethodElement))
                                                .findFirst()
                                                .orElse(annotatedElementFieldName + DOT + nonVoidMethodElement)
                                                .replaceAll(AT_SIGN, COMMA + SPACE + ENTRY + SPACE + LAMBDA_SYMB)
                                ))
                                .collect(Collectors.joining(COMMA + SPACE)),
                        true
                ));

        recordClassContent.append(NEW_LINE);

        // static factory mthd with generated record instance + Map as params
        recordClassContent.append(TAB + JAVA_LANG_SUPPRESS_WARNINGS_UNCHECKED + NEW_LINE);
        var method1stParamName = lowerCase1stChar(immutableSimpleNameBasedOnElementType(annotatedTypeElement));
        recordClassContent.append(
                factoryMethodDeclarationAndBody(
                        immutableSimpleNameBasedOnElementType(annotatedTypeElement),
                        immutableQualifiedNameBasedOnElementType(annotatedTypeElement) + SPACE + method1stParamName + COMMA + SPACE + FACTORY_METHODS_FIELDS_MAP_DECLARATION,
                        nonVoidMethodsElementsList.stream()
                                .map(nonVoidMethodElement -> format(
                                        // "(<returnType>) fieldsNameValuePairs.getOrDefault("<fieldName>", <generatedRecordFieldName.accessorMthd>())"
                                        FACTORY_METHODS_FIELDS_MAP_USE_FORMAT,
                                        fieldNameAndTypePair(nonVoidMethodElement, nonVoidMethodsElementsReturnTypesMap, allElementsTypesToConvertByAnnotation,
                                                processingEnv, collectionsGenerator).get(fieldName(nonVoidMethodElement).get()),
                                        javaConstantNamingConvention(fieldName(nonVoidMethodElement).get()),
                                        method1stParamName + DOT + fieldName(nonVoidMethodElement).get() + OPENING_PARENTHESIS + CLOSING_PARENTHESIS
                                ))
                                .collect(Collectors.joining(COMMA + SPACE)),
                        true
                )
        );

        recordClassContent.append(NEW_LINE);

        // instance factory mthd with Map as single param
        recordClassContent.append(TAB + JAVA_LANG_SUPPRESS_WARNINGS_UNCHECKED + NEW_LINE);
        recordClassContent.append(
                factoryMethodDeclarationAndBody(
                        immutableSimpleNameBasedOnElementType(annotatedTypeElement),
                        FACTORY_METHODS_FIELDS_MAP_DECLARATION,
                        nonVoidMethodsElementsList.stream()
                                .map(nonVoidMethodElement -> format(
                                        //"(<returnType>) fieldsNameValuePairs.getOrDefault("<fieldName>", this.<fieldValueFromRecordAccesorMthd()>)"
                                        FACTORY_METHODS_FIELDS_MAP_USE_FORMAT,
                                        fieldNameAndTypePair(nonVoidMethodElement, nonVoidMethodsElementsReturnTypesMap, allElementsTypesToConvertByAnnotation,
                                                processingEnv, collectionsGenerator).get(fieldName(nonVoidMethodElement).get()),
                                        javaConstantNamingConvention(fieldName(nonVoidMethodElement).get()),
                                        THIS + DOT + fieldName(nonVoidMethodElement).get() + OPENING_PARENTHESIS + CLOSING_PARENTHESIS
                                ))
                                .collect(Collectors.joining(COMMA + SPACE)),
                        false
                )
        );

        recordClassContent.append(NEW_LINE);

        // instance factory mthd with "fieldName" as 1st param and "fieldValue" as 2nd param with generic T as 2nd param's type
        recordClassContent.append(TAB + JAVA_LANG_SUPPRESS_WARNINGS_UNCHECKED + NEW_LINE);
        recordClassContent.append(
                factoryMethodDeclarationAndBody(
                        immutableSimpleNameBasedOnElementType(annotatedTypeElement),
                        FACTORY_METHOD_FIELD_NAME_AND_VALUE_DECLARATION,
                        nonVoidMethodsElementsList.stream()
                                .map(nonVoidMethodElement -> format(
                                        // "<fieldName>.equals(%s) ? (%s) <fieldValue> : this.%s()"
                                        FACTORY_METHOD_FIELD_NAME_AND_USE_FORMAT,
                                        javaConstantNamingConvention(fieldName(nonVoidMethodElement).get()),
                                        fieldNameAndTypePair(nonVoidMethodElement, nonVoidMethodsElementsReturnTypesMap, allElementsTypesToConvertByAnnotation,
                                                processingEnv, collectionsGenerator).get(fieldName(nonVoidMethodElement).get()),
                                        fieldName(nonVoidMethodElement).get()
                                ))
                                .collect(Collectors.joining(COMMA + SPACE)),
                        false,
                        true
                )
        );
    }

    private String extractParamsFromCanonicalConstructorCall(Element annotatedElement, List<Element> nonVoidMethodsElementsList) {
        var customConstructorString = new StringBuilder();
        customConstructorGenerator.generateCode(customConstructorString, Map.of(ANNOTATED_ELEMENT, annotatedElement, NON_VOID_METHODS_ELEMENTS_LIST, nonVoidMethodsElementsList));
        var matcher = Pattern.compile(METHOD_BODY_CONTENT_REGEX, Pattern.DOTALL).matcher(customConstructorString);
        if (matcher.find()) {
            var canonicalConstructorCallString = matcher.group(1).strip();
            var canonicalParamsMatcher = Pattern.compile(CANONICAL_CONSTRUCTOR_PARAMS_REGEX).matcher(canonicalConstructorCallString);
            if (canonicalParamsMatcher.find()) {
                return canonicalParamsMatcher.group(1).strip();
            }
        }
        return EMPTY_STRING;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void generateCode(StringBuilder recordClassContent, Map<String, Object> params) {
        var annotatedElement = (Element) params.get(CodeGenerator.ANNOTATED_ELEMENT);
        var nonVoidMethodsElementsList = new ArrayList<Element>((List<? extends Element>) params.get(CodeGenerator.NON_VOID_METHODS_ELEMENTS_LIST));
        var isSuperRecord = Optional.ofNullable((Boolean) params.get(CodeGenerator.IS_SUPER_RECORD)).orElse(false).booleanValue();
        if (isSuperRecord) {
            return; // factory mthds generation NOT YET supported for SuperRecord classes
        }
        buildFactoryMethods(recordClassContent, annotatedElement, nonVoidMethodsElementsList);
    }
}