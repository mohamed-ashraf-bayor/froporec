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

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ExecutableType;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static org.froporec.generator.helpers.StringGenerator.constructImmutableSimpleNameBasedOnElementType;
import static org.froporec.generator.helpers.StringGenerator.constructImmutableQualifiedNameBasedOnElementType;
import static org.froporec.generator.helpers.StringGenerator.constructSuperRecordSimpleNameBasedOnElementType;
import static org.froporec.generator.helpers.StringGenerator.lowerCase1stChar;

/**
 * Builds the custom 1-arg constructor section for the record class being generated.<br>
 * Starts with "public RecordName(list of fields)" and includes a call to the canonical constructor inside the body of the custom constructor.<br>
 * The params {@link Map} parameter of the provided implementation of the generateCode() method (from {@link CodeGenerator}) MUST contain
 * the following parameters names:<br>
 * - {@link CodeGenerator#ANNOTATED_ELEMENT}<br>
 * - {@link CodeGenerator#NON_VOID_METHODS_ELEMENTS_LIST}<br>
 */
public final class CustomConstructorGenerator implements CodeGenerator {

    private final ProcessingEnvironment processingEnvironment;

    private final Map<String, Set<Element>> allElementsTypesToConvertByAnnotation;

    private final Map<String, Map<Element, List<Element>>> mergeWithListByAnnotatedElementAndByAnnotation;

    private final SupportedCollectionsMappingLogicGenerator collectionsGenerator;

    /**
     * // TODO jdoc
     * CustomConstructorGenerationHelper constructor. Instantiates needed instances of {@link ProcessingEnvironment} and {@link CollectionsGenerator}
     *
     * @param processingEnvironment                 {@link ProcessingEnvironment} object, needed to access low-level information regarding the used annotations
     * @param allElementsTypesToConvertByAnnotation {@link Set} of all annotated elements types
     */
    public CustomConstructorGenerator(ProcessingEnvironment processingEnvironment,
                                      Map<String, Set<Element>> allElementsTypesToConvertByAnnotation,
                                      Map<String, Map<Element, List<Element>>> mergeWithListByAnnotatedElementAndByAnnotation) {
        this.processingEnvironment = processingEnvironment;
        this.allElementsTypesToConvertByAnnotation = allElementsTypesToConvertByAnnotation;
        this.mergeWithListByAnnotatedElementAndByAnnotation = mergeWithListByAnnotatedElementAndByAnnotation;
        this.collectionsGenerator = new CollectionsGenerator(this.processingEnvironment, this.allElementsTypesToConvertByAnnotation);
    }

    private void buildRecordCustom1ArgConstructor(StringBuilder recordClassContent, Element annotatedElement,
                                                  Map<? extends Element, String> nonVoidMethodsElementsReturnTypesMap,
                                                  List<? extends Element> nonVoidMethodsElementsList,
                                                  boolean isSuperRecord) {
        var annotatedTypeElement = (TypeElement) processingEnvironment.getTypeUtils().asElement(annotatedElement.asType());
        var annotatedElementQualifiedName = annotatedTypeElement.getQualifiedName().toString();
        var annotatedElementSimpleName = annotatedElementQualifiedName.substring(annotatedElementQualifiedName.lastIndexOf(DOT) + 1);
        var annotatedElementFieldName = lowerCase1stChar(annotatedElementSimpleName);
        // %s = generated simple class name, %s = pojo class qualified name + field name // if isSuperRecord = false
        recordClassContent.append(format( // line declaring the constructor
                TAB + PUBLIC + " %s(%s) " + OPENING_BRACE + NEW_LINE,
                isSuperRecord
                        ? constructSuperRecordSimpleNameBasedOnElementType(annotatedTypeElement)
                        : constructImmutableSimpleNameBasedOnElementType(annotatedTypeElement),
                annotatedElementQualifiedName + SPACE + annotatedElementFieldName
                        + (isSuperRecord ? COMMA_SEPARATOR + buildConstructorArgsForMergeWithElements(annotatedElement) : EMPTY_STRING)
        ));
        recordClassContent.append(TAB + TAB + THIS + OPENING_PARENTHESIS); // calling canonical constructor
        // building canonical constructor content
        nonVoidMethodsElementsList.forEach(nonVoidMethodElement -> {
            var enclosingElement = nonVoidMethodElement.getEnclosingElement();
            var enclosingElementIsRecord = ElementKind.RECORD.equals(enclosingElement.getKind());
            var nonVoidMethodReturnTypeAsString = nonVoidMethodsElementsReturnTypesMap.get(nonVoidMethodElement);
            var nonVoidMethodReturnTypeElementOpt = Optional.ofNullable(
                    processingEnvironment.getTypeUtils().asElement(((ExecutableType) nonVoidMethodElement.asType()).getReturnType())); // for collections, Element.toString() will NOT return the generic part
            // Consumer to run in case of non-primitives i.e nonVoidMethodReturnTypeElementOpt.isPresent()
            Consumer<Element> consumer = nonVoidMethodReturnTypeElement ->
                    buildCanonicalConstructorCallSingleParameter(recordClassContent, lowerCase1stChar(enclosingElement.getSimpleName().toString()), nonVoidMethodElement,
                            nonVoidMethodReturnTypeAsString, isElementAnnotatedAsRecordOrImmutable(allElementsTypesToConvertByAnnotation).test(nonVoidMethodReturnTypeElement),
                            enclosingElementIsRecord);
            // Runnable to execute in case of primitives i.e nonVoidMethodReturnTypeElementOpt.isEmpty()
            Runnable runnable = () -> buildCanonicalConstructorCallSingleParameter(recordClassContent, lowerCase1stChar(enclosingElement.getSimpleName().toString()),
                    nonVoidMethodElement, nonVoidMethodReturnTypeAsString, false, enclosingElementIsRecord);
            // if the type has already been annotated somewhere else in the code, the field type is the corresponding generated record class (consumer is executed)
            // if not annotated and not a collection then keep the received type as is (runnable is executed)
            nonVoidMethodReturnTypeElementOpt.ifPresentOrElse(consumer, runnable);
        });
        recordClassContent.deleteCharAt(recordClassContent.length() - 1).deleteCharAt(recordClassContent.length() - 1);
        // finished building canonical constructor content
        recordClassContent.append(CLOSING_PARENTHESIS + SEMI_COLON + NEW_LINE);
        recordClassContent.append(TAB + CLOSING_BRACE + NEW_LINE);
    }

    private String buildConstructorArgsForMergeWithElements(Element annotatedElement) {
        return mergeWithListByAnnotatedElementAndByAnnotation.get(ORG_FROPOREC_SUPER_RECORD).get(annotatedElement).stream()
                .map(element -> element.toString() + SPACE + lowerCase1stChar(element.getSimpleName().toString()))
                .collect(joining(COMMA_SEPARATOR + SPACE));
    }

    private void buildCanonicalConstructorCallSingleParameter(
            final StringBuilder recordClassContent,
            final String fieldName,
            final Element nonVoidMethodElement,
            final String nonVoidMethodReturnTypeAsString,
            final boolean processAsImmutable,
            final boolean enclosingElementIsRecord) {
        var nonVoidMethodElementAsString = nonVoidMethodElement.toString();
        if (nonVoidMethodElementAsString.startsWith(GET) || enclosingElementIsRecord) {
            if (collectionsGenerator.isCollectionWithGeneric(nonVoidMethodReturnTypeAsString)) {
                collectionsGenerator.generateCollectionFieldMappingIfGenericIsAnnotated(recordClassContent, fieldName,
                        nonVoidMethodElementAsString, nonVoidMethodReturnTypeAsString);
            } else {
                if (processAsImmutable) {
                    recordClassContent.append(format(
                            "new %s(%s.%s), ",
                            constructImmutableQualifiedNameBasedOnElementType(constructElementInstanceValueFromTypeString(processingEnvironment, nonVoidMethodReturnTypeAsString)),
                            fieldName,
                            nonVoidMethodElementAsString
                    ));
                } else {
                    // applies to primitives and any pojo or record which hasn't been annotated
                    // %s.%s = fieldname.nonVoidMethodElementAsString
                    recordClassContent.append(format("%s.%s, ", fieldName, nonVoidMethodElementAsString));
                }
            }
        } else if (nonVoidMethodElementAsString.startsWith(IS)) {
            recordClassContent.append(format("%s.%s, ", fieldName, nonVoidMethodElementAsString));
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void generateCode(StringBuilder recordClassContent, Map<String, Object> params) {
        var annotatedElement = (Element) params.get(CodeGenerator.ANNOTATED_ELEMENT);
        var nonVoidMethodsElementsList = (List<? extends Element>) params.get(CodeGenerator.NON_VOID_METHODS_ELEMENTS_LIST);
        var isSuperRecord = (Boolean) params.get(CodeGenerator.IS_SUPER_RECORD);
        buildRecordCustom1ArgConstructor(recordClassContent, annotatedElement,
                constructNonVoidMethodsElementsReturnTypesMapFromList(nonVoidMethodsElementsList),
                nonVoidMethodsElementsList, isSuperRecord);
    }
}