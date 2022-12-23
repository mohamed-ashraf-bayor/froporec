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
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ExecutableType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static org.froporec.generator.helpers.StringGenerator.constructFieldName;
import static org.froporec.generator.helpers.StringGenerator.constructImmutableQualifiedNameBasedOnElementType;
import static org.froporec.generator.helpers.StringGenerator.constructImmutableSimpleNameBasedOnElementType;
import static org.froporec.generator.helpers.StringGenerator.javaConstantNamingConvention;
import static org.froporec.generator.helpers.StringGenerator.lowerCase1stChar;
import static org.froporec.generator.helpers.StringGenerator.removeLastChars;

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
    public static final String FACTORY_METHODS_FIELDS_MAP_USE_FORMAT = "(%s) fieldsNameValuePairs.getOrDefault(%s, %s)";
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

    private void buildStaticFactoryMethods(StringBuilder recordClassContent, Element annotatedElement, List<Element> nonVoidMethodsElementsList) {

        var annotatedTypeElement = (TypeElement) processingEnvironment.getTypeUtils().asElement(annotatedElement.asType());
        var annotatedElementQualifiedName = annotatedTypeElement.getQualifiedName().toString();
        var annotatedElementFieldName = lowerCase1stChar(annotatedTypeElement.getSimpleName().toString());

        recordClassContent.append(NEW_LINE);

        // static factory mthd with annotated elemnt instance as single param
        buildMethodDeclarationAndBody(
                recordClassContent,
                constructImmutableSimpleNameBasedOnElementType(annotatedTypeElement),
                annotatedElementQualifiedName + SPACE + annotatedElementFieldName,
                extractParamsFromCanonicalConstructorCall(annotatedElement, nonVoidMethodsElementsList)
        );

        recordClassContent.append(NEW_LINE);

        var nonVoidMethodsElementsReturnTypesMap = constructNonVoidMethodsElementsReturnTypesMapFromList(nonVoidMethodsElementsList);

        // static factory mthd with Map as single param
        recordClassContent.append(TAB + JAVA_LANG_SUPPRESS_WARNINGS_UNCHECKED + NEW_LINE);
        buildMethodDeclarationAndBody(
                recordClassContent,
                constructImmutableSimpleNameBasedOnElementType(annotatedTypeElement),
                FACTORY_METHODS_FIELDS_MAP_DECLARATION,
                nonVoidMethodsElementsList.stream()
                        .map(nonVoidMethodElement -> format(
                                //"(<returnType>) fieldsNameValuePairs.getOrDefault("<fieldName>", <defaultValue>)"
                                FACTORY_METHODS_FIELDS_MAP_USE_FORMAT,
                                constructFieldNameTypePair(nonVoidMethodElement, nonVoidMethodsElementsReturnTypesMap).get(constructFieldName(nonVoidMethodElement).get()),
                                javaConstantNamingConvention(constructFieldName(nonVoidMethodElement).get()),
                                defaultReturnValueForMethod(nonVoidMethodElement)
                        ))
                        .collect(Collectors.joining(COMMA + SPACE))
        );

        recordClassContent.append(NEW_LINE);

        // static factory mthd with annotated elemnt instance + Map as params
        // "(type) fieldsNameValuePairs.getOrDefault("<fieldName>", annotatedElementFieldName.getterMthd())"
        recordClassContent.append(TAB + JAVA_LANG_SUPPRESS_WARNINGS_UNCHECKED + NEW_LINE);
        var paramsFromCanonicalConstructorCall = extractParamsFromCanonicalConstructorCall(annotatedElement, nonVoidMethodsElementsList)
                .replaceAll(COMMA + SPACE + ENTRY + SPACE + LAMBDA_SIGN, AT_SIGN) // replacing occurrences of ", entry ->" with "@" to avoid issues while splitting
                .split(COMMA + SPACE);
        buildMethodDeclarationAndBody(
                recordClassContent,
                constructImmutableSimpleNameBasedOnElementType(annotatedTypeElement),
                annotatedElementQualifiedName + SPACE + annotatedElementFieldName + COMMA + SPACE + FACTORY_METHODS_FIELDS_MAP_DECLARATION,
                nonVoidMethodsElementsList.stream()
                        .map(nonVoidMethodElement -> format(
                                //"(<returnType>) fieldsNameValuePairs.getOrDefault("<fieldName>", <defaultValue>)"
                                FACTORY_METHODS_FIELDS_MAP_USE_FORMAT,
                                constructFieldNameTypePair(nonVoidMethodElement, nonVoidMethodsElementsReturnTypesMap).get(constructFieldName(nonVoidMethodElement).get()),
                                javaConstantNamingConvention(constructFieldName(nonVoidMethodElement).get()),
                                stream(paramsFromCanonicalConstructorCall)
                                        .filter(param -> param.contains(annotatedElementFieldName + DOT + nonVoidMethodElement))
                                        .findFirst()
                                        .orElse(annotatedElementFieldName + DOT + nonVoidMethodElement)
                                        .replaceAll(AT_SIGN, COMMA + SPACE + ENTRY + SPACE + LAMBDA_SIGN)
                        ))
                        .collect(Collectors.joining(COMMA + SPACE))
        );

        recordClassContent.append(NEW_LINE);

        // static factory mthd with generated record instance + Map as params
        // "(type) fieldsNameValuePairs.getOrDefault("<fieldName>", generatedRecordFieldName.getterMthd())"
        recordClassContent.append(TAB + JAVA_LANG_SUPPRESS_WARNINGS_UNCHECKED + NEW_LINE);
        var method1stParamName = lowerCase1stChar(constructImmutableSimpleNameBasedOnElementType(annotatedTypeElement));
        buildMethodDeclarationAndBody(
                recordClassContent,
                constructImmutableSimpleNameBasedOnElementType(annotatedTypeElement),
                constructImmutableQualifiedNameBasedOnElementType(annotatedTypeElement) + SPACE + method1stParamName + COMMA + SPACE + FACTORY_METHODS_FIELDS_MAP_DECLARATION,
                nonVoidMethodsElementsList.stream()
                        .map(nonVoidMethodElement -> format(
                                //"(<returnType>) fieldsNameValuePairs.getOrDefault("<fieldName>", <defaultValue>)"
                                FACTORY_METHODS_FIELDS_MAP_USE_FORMAT,
                                constructFieldNameTypePair(nonVoidMethodElement, nonVoidMethodsElementsReturnTypesMap).get(constructFieldName(nonVoidMethodElement).get()),
                                javaConstantNamingConvention(constructFieldName(nonVoidMethodElement).get()),
                                method1stParamName + DOT + constructFieldName(nonVoidMethodElement, ElementKind.RECORD.equals(nonVoidMethodElement.getEnclosingElement().getKind())).get() + OPENING_PARENTHESIS + CLOSING_PARENTHESIS
                        ))
                        .collect(Collectors.joining(COMMA + SPACE))
        );
    }

    /**
     * // TODO cmplte...
     *
     * @param nonVoidMethodElement
     * @param nonVoidMethodsElementsReturnTypesMap
     * @return java.util.Map with key being the field name and the value being the type of the field after conversion
     */
    private Map<String, String> constructFieldNameTypePair(Element nonVoidMethodElement, Map<Element, String> nonVoidMethodsElementsReturnTypesMap) {
        var nonVoidMethodReturnTypeAsString = nonVoidMethodsElementsReturnTypesMap.get(nonVoidMethodElement);
        var nonVoidMethodReturnTypeElementOpt = Optional.ofNullable(
                processingEnvironment.getTypeUtils().asElement(((ExecutableType) nonVoidMethodElement.asType()).getReturnType())
        );
        var fieldType = new StringBuilder();
        // Consumer to run in case of non-primitives i.e nonVoidMethodReturnTypeElementOpt.isPresent()
        Consumer<Element> consumer = nonVoidMethodReturnTypeElement ->
                buildFieldType(fieldType, nonVoidMethodElement, nonVoidMethodReturnTypeAsString,
                        isElementAnnotatedAsRecordOrImmutable(allElementsTypesToConvertByAnnotation).test(nonVoidMethodReturnTypeElement));
        // Runnable to execute in case of primitives i.e nonVoidMethodReturnTypeElementOpt.isEmpty()
        Runnable runnable = () -> buildFieldType(fieldType, nonVoidMethodElement, nonVoidMethodReturnTypeAsString, false);
        nonVoidMethodReturnTypeElementOpt.ifPresentOrElse(consumer, runnable);
        return Map.of(constructFieldName(nonVoidMethodElement).orElseThrow(), fieldType.toString());
    }

    private void buildFieldType(StringBuilder fieldType, Element nonVoidMethodElement, String nonVoidMethodReturnTypeAsString, boolean processAsImmutable) {
        var fieldName = constructFieldName(nonVoidMethodElement).orElseThrow();
        // if the type of the field being processed is a collection process it differently and return
        if (collectionsGenerator.isCollectionWithGeneric(nonVoidMethodReturnTypeAsString)) {
            var typeAndNameSpaceSeparated = new StringBuilder();
            collectionsGenerator.replaceGenericWithRecordClassNameIfAny(typeAndNameSpaceSeparated, fieldName, nonVoidMethodReturnTypeAsString);
            removeLastChars(typeAndNameSpaceSeparated, 2);
            fieldType.append(typeAndNameSpaceSeparated.substring(0, typeAndNameSpaceSeparated.lastIndexOf(SPACE)));
            return;
        }
        fieldType.append(
                processAsImmutable
                        ? constructImmutableQualifiedNameBasedOnElementType(constructElementInstanceValueFromTypeString(processingEnvironment, nonVoidMethodReturnTypeAsString))
                        : nonVoidMethodReturnTypeAsString
        );
    }

    private void buildMethodDeclarationAndBody(StringBuilder recordClassContent, String methodReturnType, String methodParams, String canonicalConstructorParams) {
        // "public static <GeneratedRecordSimpleName><SPACE>"
        recordClassContent.append(TAB + PUBLIC + SPACE + STATIC + SPACE + methodReturnType + SPACE);
        // "buildWith(<AnnotatedPojoOrRecordQualifiedName><SPACE><AnnotatedPojoOrRecordSimpleName>)"
        // "buildWith(java.util.Map<String, Object><SPACE>fieldsNameValuePairs)"
        // "buildWith(<AnnotatedPojoOrRecordQualifiedName><SPACE><AnnotatedPojoOrRecordSimpleName>,<SPACE>java.util.Map<String, Object><SPACE>fieldsNameValuePairs)"
        recordClassContent.append(BUILD_WITH + OPENING_PARENTHESIS + methodParams + CLOSING_PARENTHESIS + SPACE + OPENING_BRACE + NEW_LINE + TAB + TAB);
        // method body
        recordClassContent.append(RETURN + SPACE + NEW + SPACE + methodReturnType + OPENING_PARENTHESIS + canonicalConstructorParams + CLOSING_PARENTHESIS + SEMI_COLON + NEW_LINE);
        // closing
        recordClassContent.append(TAB + CLOSING_BRACE + NEW_LINE);
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
            return; // static factory mthds generation NOT YET supported for SuperRecord classes
        }
        buildStaticFactoryMethods(recordClassContent, annotatedElement, nonVoidMethodsElementsList);
    }
}