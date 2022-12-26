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
import static org.froporec.generator.helpers.StringGenerator.constructFieldName;
import static org.froporec.generator.helpers.StringGenerator.constructImmutableQualifiedNameBasedOnElementType;
import static org.froporec.generator.helpers.StringGenerator.constructImmutableSimpleNameBasedOnElementType;
import static org.froporec.generator.helpers.StringGenerator.javaConstantNamingConvention;
import static org.froporec.generator.helpers.StringGenerator.lowerCase1stChar;

/**
 * // TODO chnge jdoc
 * Builds the list of fields of the record class being generated. ex: int a, String s, Person p.<br>
 * Considerations will be made for fields whose types have also been annotated or added as a .class value within the
 * "alsoConvert" attribute of {@link org.froporec.annotations.Record} or {@link org.froporec.annotations.Immutable}).<br>
 * The params {@link Map} parameter of the provided implementation of the generateCode() method (from {@link CodeGenerator}) MUST contain
 * the following parameters names:<br>
 * - {@link CodeGenerator#ANNOTATED_ELEMENT}<br>
 * - {@link CodeGenerator#NON_VOID_METHODS_ELEMENTS_LIST}<br>
 * - {@link CodeGenerator#IS_SUPER_RECORD}<br>
 */
public final class FactoryMethodsGenerator implements CodeGenerator {

    private static final String FACTORY_METHODS_FIELDS_MAP_DECLARATION = "java.util.Map<String, Object> fieldsNameValuePairs";
    private static final String FACTORY_METHODS_FIELDS_MAP_USE_FORMAT = "(%s) fieldsNameValuePairs.getOrDefault(%s, %s)";

    private static final String FACTORY_METHOD_FIELD_NAME_AND_VALUE_DECLARATION = "String fieldName, T fieldValue";
    private static final String FACTORY_METHOD_FIELD_NAME_AND_USE_FORMAT = "fieldName.equals(%s) ? (%s) fieldValue : this.%s()";

    private static final String JAVA_LANG_SUPPRESS_WARNINGS_UNCHECKED = "@java.lang.SuppressWarnings(\"unchecked\")";

    private final ProcessingEnvironment processingEnvironment;

    private final Map<String, Set<Element>> allElementsTypesToConvertByAnnotation;

    private final CodeGenerator customConstructorGenerator;

    private final SupportedCollectionsFieldsGenerator collectionsGenerator;


    /**
     * // TODO chnge jdoc
     * FieldsGenerationHelper constructor. Instantiates needed instance of {@link CollectionsGenerator}
     *
     * @param processingEnvironment                 {@link ProcessingEnvironment} object, needed to access low-level information regarding the used annotations
     * @param allElementsTypesToConvertByAnnotation {@link Set} of {@link Element} instances grouped by the annotation String representation
     */
    public FactoryMethodsGenerator(ProcessingEnvironment processingEnvironment,
                                   Map<String, Set<Element>> allElementsTypesToConvertByAnnotation) {
        this.processingEnvironment = processingEnvironment;
        this.allElementsTypesToConvertByAnnotation = allElementsTypesToConvertByAnnotation;
        this.customConstructorGenerator = new CustomConstructorGenerator(this.processingEnvironment, this.allElementsTypesToConvertByAnnotation, null);
        this.collectionsGenerator = new CollectionsGenerator(this.processingEnvironment, this.allElementsTypesToConvertByAnnotation);
    }

    private void buildFactoryMethods(StringBuilder recordClassContent, Element annotatedElement, List<Element> nonVoidMethodsElementsList) {

        var annotatedTypeElement = (TypeElement) processingEnvironment.getTypeUtils().asElement(annotatedElement.asType());
        var annotatedElementQualifiedName = annotatedTypeElement.getQualifiedName().toString();
        var annotatedElementFieldName = lowerCase1stChar(annotatedTypeElement.getSimpleName().toString());

        recordClassContent.append(NEW_LINE);

        // static factory mthd with annotated elemnt instance as single param
        recordClassContent.append(
                buildFactoryMethodDeclarationAndBody(
                        constructImmutableSimpleNameBasedOnElementType(annotatedTypeElement),
                        annotatedElementQualifiedName + SPACE + annotatedElementFieldName,
                        extractParamsFromCanonicalConstructorCall(annotatedElement, nonVoidMethodsElementsList),
                        true
                )
        );

        recordClassContent.append(NEW_LINE);

        // static factory mthd with generated record instance as single param
        var methodParamName = lowerCase1stChar(constructImmutableSimpleNameBasedOnElementType(annotatedTypeElement));
        recordClassContent.append(
                buildFactoryMethodDeclarationAndBody(
                        constructImmutableSimpleNameBasedOnElementType(annotatedTypeElement),
                        constructImmutableQualifiedNameBasedOnElementType(annotatedTypeElement) + SPACE + methodParamName,
                        nonVoidMethodsElementsList.stream()
                                .map(element -> methodParamName + DOT + constructFieldName(element).get() + OPENING_PARENTHESIS + CLOSING_PARENTHESIS)
                                .collect(Collectors.joining(COMMA + SPACE)),
                        true
                )
        );

        recordClassContent.append(NEW_LINE);

        var nonVoidMethodsElementsReturnTypesMap = constructNonVoidMethodsElementsReturnTypesMapFromList(nonVoidMethodsElementsList);

        // static factory mthd with Map as single param
        recordClassContent.append(TAB + JAVA_LANG_SUPPRESS_WARNINGS_UNCHECKED + NEW_LINE);
        recordClassContent.append(
                buildFactoryMethodDeclarationAndBody(
                        constructImmutableSimpleNameBasedOnElementType(annotatedTypeElement),
                        FACTORY_METHODS_FIELDS_MAP_DECLARATION,
                        nonVoidMethodsElementsList.stream()
                                .map(nonVoidMethodElement -> format(
                                        //"(<returnType>) fieldsNameValuePairs.getOrDefault("<fieldName>", <defaultValue>)"
                                        FACTORY_METHODS_FIELDS_MAP_USE_FORMAT,
                                        constructFieldNameTypePair(nonVoidMethodElement, nonVoidMethodsElementsReturnTypesMap, allElementsTypesToConvertByAnnotation,
                                                processingEnvironment, collectionsGenerator).get(constructFieldName(nonVoidMethodElement).get()),
                                        javaConstantNamingConvention(constructFieldName(nonVoidMethodElement).get()),
                                        defaultReturnValueForMethod(nonVoidMethodElement)
                                ))
                                .collect(Collectors.joining(COMMA + SPACE)),
                        true
                )
        );

        recordClassContent.append(NEW_LINE);

        // static factory mthd with annotated elemnt instance + Map as params
        // "(type) fieldsNameValuePairs.getOrDefault("<fieldName>", annotatedElementFieldName.getterMthd())"
        recordClassContent.append(TAB + JAVA_LANG_SUPPRESS_WARNINGS_UNCHECKED + NEW_LINE);
        var paramsFromCanonicalConstructorCall = extractParamsFromCanonicalConstructorCall(annotatedElement, nonVoidMethodsElementsList)
                .replaceAll(COMMA + SPACE + ENTRY + SPACE + LAMBDA_SIGN, AT_SIGN) // replacing occurrences of ", entry ->" with "@" to avoid issues while splitting
                .split(COMMA + SPACE);
        recordClassContent.append(
                buildFactoryMethodDeclarationAndBody(
                        constructImmutableSimpleNameBasedOnElementType(annotatedTypeElement),
                        annotatedElementQualifiedName + SPACE + annotatedElementFieldName + COMMA + SPACE + FACTORY_METHODS_FIELDS_MAP_DECLARATION,
                        nonVoidMethodsElementsList.stream()
                                .map(nonVoidMethodElement -> format(
                                        //"(<returnType>) fieldsNameValuePairs.getOrDefault("<fieldName>", <defaultValue>)"
                                        FACTORY_METHODS_FIELDS_MAP_USE_FORMAT,
                                        constructFieldNameTypePair(nonVoidMethodElement, nonVoidMethodsElementsReturnTypesMap, allElementsTypesToConvertByAnnotation,
                                                processingEnvironment, collectionsGenerator).get(constructFieldName(nonVoidMethodElement).get()),
                                        javaConstantNamingConvention(constructFieldName(nonVoidMethodElement).get()),
                                        stream(paramsFromCanonicalConstructorCall)
                                                .filter(param -> param.contains(annotatedElementFieldName + DOT + nonVoidMethodElement)) // TODO revw that cndtion
                                                .findFirst()
                                                .orElse(annotatedElementFieldName + DOT + nonVoidMethodElement)
                                                .replaceAll(AT_SIGN, COMMA + SPACE + ENTRY + SPACE + LAMBDA_SIGN)
                                ))
                                .collect(Collectors.joining(COMMA + SPACE)),
                        true
                ));

        recordClassContent.append(NEW_LINE);

        // static factory mthd with generated record instance + Map as params
        // "(type) fieldsNameValuePairs.getOrDefault("<fieldName>", generatedRecordFieldName.getterMthd())"
        recordClassContent.append(TAB + JAVA_LANG_SUPPRESS_WARNINGS_UNCHECKED + NEW_LINE);
        var method1stParamName = lowerCase1stChar(constructImmutableSimpleNameBasedOnElementType(annotatedTypeElement));
        recordClassContent.append(
                buildFactoryMethodDeclarationAndBody(
                        constructImmutableSimpleNameBasedOnElementType(annotatedTypeElement),
                        constructImmutableQualifiedNameBasedOnElementType(annotatedTypeElement) + SPACE + method1stParamName + COMMA + SPACE + FACTORY_METHODS_FIELDS_MAP_DECLARATION,
                        nonVoidMethodsElementsList.stream()
                                .map(nonVoidMethodElement -> format(
                                        //"(<returnType>) fieldsNameValuePairs.getOrDefault("<fieldName>", <defaultValue>)"
                                        FACTORY_METHODS_FIELDS_MAP_USE_FORMAT,
                                        constructFieldNameTypePair(nonVoidMethodElement, nonVoidMethodsElementsReturnTypesMap, allElementsTypesToConvertByAnnotation,
                                                processingEnvironment, collectionsGenerator).get(constructFieldName(nonVoidMethodElement).get()),
                                        javaConstantNamingConvention(constructFieldName(nonVoidMethodElement).get()),
                                        method1stParamName + DOT + constructFieldName(nonVoidMethodElement).get() + OPENING_PARENTHESIS + CLOSING_PARENTHESIS
                                ))
                                .collect(Collectors.joining(COMMA + SPACE)),
                        true
                )
        );

        recordClassContent.append(NEW_LINE);

        // instance factory mthd with Map as single param
        recordClassContent.append(TAB + JAVA_LANG_SUPPRESS_WARNINGS_UNCHECKED + NEW_LINE);
        recordClassContent.append(
                buildFactoryMethodDeclarationAndBody(
                        constructImmutableSimpleNameBasedOnElementType(annotatedTypeElement),
                        FACTORY_METHODS_FIELDS_MAP_DECLARATION,
                        nonVoidMethodsElementsList.stream()
                                .map(nonVoidMethodElement -> format(
                                        //"(<returnType>) fieldsNameValuePairs.getOrDefault("<fieldName>", this.<fieldValueFromRecordAccesor>)"
                                        FACTORY_METHODS_FIELDS_MAP_USE_FORMAT,
                                        constructFieldNameTypePair(nonVoidMethodElement, nonVoidMethodsElementsReturnTypesMap, allElementsTypesToConvertByAnnotation,
                                                processingEnvironment, collectionsGenerator).get(constructFieldName(nonVoidMethodElement).get()),
                                        javaConstantNamingConvention(constructFieldName(nonVoidMethodElement).get()),
                                        THIS + DOT + constructFieldName(nonVoidMethodElement).get() + OPENING_PARENTHESIS + CLOSING_PARENTHESIS
                                ))
                                .collect(Collectors.joining(COMMA + SPACE)),
                        false
                )
        );

        recordClassContent.append(NEW_LINE);

        // instance factory mthd with "fieldName" as 1st param and "fieldValue" as 2nd param with generic T as 2nd param's type
        recordClassContent.append(TAB + JAVA_LANG_SUPPRESS_WARNINGS_UNCHECKED + NEW_LINE);
        recordClassContent.append(
                buildFactoryMethodDeclarationAndBody(
                        constructImmutableSimpleNameBasedOnElementType(annotatedTypeElement),
                        FACTORY_METHOD_FIELD_NAME_AND_VALUE_DECLARATION,
                        nonVoidMethodsElementsList.stream()
                                .map(nonVoidMethodElement -> format(
                                        //"fieldName.equals(%s) ? (%s) fieldValue : this.%s()"
                                        FACTORY_METHOD_FIELD_NAME_AND_USE_FORMAT,
                                        javaConstantNamingConvention(constructFieldName(nonVoidMethodElement).get()),
                                        constructFieldNameTypePair(nonVoidMethodElement, nonVoidMethodsElementsReturnTypesMap, allElementsTypesToConvertByAnnotation,
                                                processingEnvironment, collectionsGenerator).get(constructFieldName(nonVoidMethodElement).get()),
                                        constructFieldName(nonVoidMethodElement).get()
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