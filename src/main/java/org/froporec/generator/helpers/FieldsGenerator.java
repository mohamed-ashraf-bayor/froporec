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

import static java.lang.String.format;
import static org.froporec.generator.helpers.CodeGenerator.buildNonVoidMethodsElementsList;
import static org.froporec.generator.helpers.StringGenerator.constructImmutableQualifiedNameBasedOnElementType;

/**
 * Builds the list of fields of the record class being generated. ex: int a, String s, Person p.<br>
 * Considerations will be made for fields whose types have also been annotated or added as a .class value within the
 * "includeTypes" attribute of {@link GenerateRecord} or {@link GenerateImmutable}).<br>
 * The params {@link Map} parameter of the provided implementation of the generateCode() method (from {@link CodeGenerator}) MUST contain
 * the following parameters names:<br>
 * - {@link CodeGenerator#NON_VOID_METHODS_ELEMENTS_LIST}<br>
 */
public final class FieldsGenerator implements CodeGenerator {

    private final ProcessingEnvironment processingEnvironment;

    private final Map<String, Set<Element>> allElementsTypesToConvertByAnnotation;

    private final Map<String, Map<Element, List<Element>>> mergeWithListByAnnotatedElementAndByAnnotation;

    private final SupportedCollectionsFieldsGenerator collectionsGenerator;

    /**
     * FieldsGenerationHelper constructor. Instantiates needed instance of {@link CollectionsGenerator}
     *
     * @param processingEnvironment                 {@link ProcessingEnvironment} object, needed to access low-level information regarding the used annotations
     * @param allElementsTypesToConvertByAnnotation {@link Set} of all annotated elements types string representations
     */
    public FieldsGenerator(ProcessingEnvironment processingEnvironment,
                           Map<String, Set<Element>> allElementsTypesToConvertByAnnotation,
                           Map<String, Map<Element, List<Element>>> mergeWithListByAnnotatedElementAndByAnnotation) {
        this.processingEnvironment = processingEnvironment;
        this.allElementsTypesToConvertByAnnotation = allElementsTypesToConvertByAnnotation;
        this.mergeWithListByAnnotatedElementAndByAnnotation = mergeWithListByAnnotatedElementAndByAnnotation;
        this.collectionsGenerator = new CollectionsGenerator(this.processingEnvironment, this.allElementsTypesToConvertByAnnotation);
    }

    private void buildRecordFieldsFromNonVoidMethodsList(StringBuilder recordClassContent,
                                                         Element annotatedElement,
                                                         List<Element> nonVoidMethodsElementsList,
                                                         boolean isSuperRecord) {
        var annotatedTypeElement = (TypeElement) processingEnvironment.getTypeUtils().asElement(annotatedElement.asType());
        // 1st, build fields list of the annotated elmnt
        buildFieldsList(recordClassContent, nonVoidMethodsElementsList, constructNonVoidMethodsElementsReturnTypesMapFromList(nonVoidMethodsElementsList),
                isSuperRecord ? annotatedTypeElement.getSimpleName().toString() : EMPTY_STRING); // if isSuperRecord each field is suffixed with (...)
        // 2nd, add fields list of provided mergeWith elemetns if isSuperRecord
        if (isSuperRecord) {
            mergeWithListByAnnotatedElementAndByAnnotation.get(ORG_FROPOREC_SUPER_RECORD).get(annotatedElement).forEach(element -> {
                var typeElement = (TypeElement) processingEnvironment.getTypeUtils().asElement(element.asType());
                var nonVoidMthodsElementsList = new ArrayList<Element>(buildNonVoidMethodsElementsList(element, processingEnvironment));
                var nonVoidMethodsElementsReturnTypesMap = constructNonVoidMethodsElementsReturnTypesMapFromList(nonVoidMthodsElementsList);
                buildFieldsList(recordClassContent, nonVoidMthodsElementsList, nonVoidMethodsElementsReturnTypesMap, typeElement.getSimpleName().toString());
            });
        }
        recordClassContent.deleteCharAt(recordClassContent.length() - 1).deleteCharAt(recordClassContent.length() - 1);
    }

    private void buildFieldsList(StringBuilder recordClassContent,
                                 List<Element> nonVoidMethodsElementsList,
                                 Map<Element, String> nonVoidMethodsElementsReturnTypesMap,
                                 String fieldSuffix) {
        nonVoidMethodsElementsList.forEach(nonVoidMethodElement -> {
            var enclosingElementIsRecord = ElementKind.RECORD.equals(nonVoidMethodElement.getEnclosingElement().getKind());
            var nonVoidMethodReturnTypeAsString = nonVoidMethodsElementsReturnTypesMap.get(nonVoidMethodElement);
            var nonVoidMethodReturnTypeElementOpt = Optional.ofNullable(
                    processingEnvironment.getTypeUtils().asElement(((ExecutableType) nonVoidMethodElement.asType()).getReturnType())
            ); // for collections, Element.toString() will NOT return the generic part
            // Consumer to run in case of non-primitives i.e nonVoidMethodReturnTypeElementOpt.isPresent()
            Consumer<Element> consumer = nonVoidMethodReturnTypeElement ->
                    buildSingleField(recordClassContent, nonVoidMethodElement, nonVoidMethodReturnTypeAsString,
                            isElementAnnotatedAsRecordOrImmutable(allElementsTypesToConvertByAnnotation).test(nonVoidMethodReturnTypeElement),
                            enclosingElementIsRecord, fieldSuffix);
            // Runnable to execute in case of primitives i.e nonVoidMethodReturnTypeElementOpt.isEmpty()
            Runnable runnable = () -> buildSingleField(recordClassContent, nonVoidMethodElement, nonVoidMethodReturnTypeAsString,
                    false, enclosingElementIsRecord, fieldSuffix);
            // if the type has already been annotated somewhere else in the code, the field type is the corresponding generated record class (consumer is executed)
            // if not annotated and not a collection then keep the received type as is (runnable is executed)
            nonVoidMethodReturnTypeElementOpt.ifPresentOrElse(consumer, runnable);
        });
    }

    private void buildSingleField(StringBuilder recordClassContent, Element nonVoidMethodElement,
                                  String nonVoidMethodReturnTypeAsString, boolean processAsImmutable,
                                  boolean enclosingElementIsRecord, String fieldSuffix) {
        var recordFieldsListFormat = "%s %s, "; // fieldType<SPACE>fieldName<COMMA><SPACE>
        if (enclosingElementIsRecord) {
            // Record class, handle all non-void methods
            var nonVoidMethodElementAsString = nonVoidMethodElement.toString();
            var fieldName = nonVoidMethodElementAsString.substring(0, nonVoidMethodElementAsString.indexOf(OPENING_PARENTHESIS)) + fieldSuffix;
            // if the type of the field being processed is a collection process it differently and return
            if (collectionsGenerator.isCollectionWithGeneric(nonVoidMethodReturnTypeAsString)) {
                collectionsGenerator.replaceGenericWithRecordClassNameIfAny(recordClassContent, fieldName, nonVoidMethodReturnTypeAsString);
                return;
            }
            recordClassContent.append(format(
                    recordFieldsListFormat,
                    processAsImmutable
                            ? constructImmutableQualifiedNameBasedOnElementType(constructElementInstanceValueFromTypeString(processingEnvironment, nonVoidMethodReturnTypeAsString))
                            : nonVoidMethodReturnTypeAsString,
                    fieldName));
        } else {
            // POJO class, handle only getters (only methods starting with get or is)
            var getterAsString = nonVoidMethodElement.toString();
            var fieldNameNonBoolean = getterAsString.substring(3, 4).toLowerCase() + getterAsString.substring(4, getterAsString.indexOf(OPENING_PARENTHESIS)) + fieldSuffix;
            if (getterAsString.startsWith(GET)) {
                // if the type of the field being processed is a collection process it differently and return
                if (collectionsGenerator.isCollectionWithGeneric(nonVoidMethodReturnTypeAsString)) {
                    collectionsGenerator.replaceGenericWithRecordClassNameIfAny(recordClassContent, fieldNameNonBoolean, nonVoidMethodReturnTypeAsString);
                    return;
                }
                recordClassContent.append(format(
                        recordFieldsListFormat,
                        processAsImmutable
                                ? constructImmutableQualifiedNameBasedOnElementType(constructElementInstanceValueFromTypeString(processingEnvironment, nonVoidMethodReturnTypeAsString))
                                : nonVoidMethodReturnTypeAsString,
                        fieldNameNonBoolean));
            } else if (getterAsString.startsWith(IS)) {
                var fieldNameBoolean = getterAsString.substring(2, 3).toLowerCase() + getterAsString.substring(3, getterAsString.indexOf(OPENING_PARENTHESIS)) + fieldSuffix;
                recordClassContent.append(format(recordFieldsListFormat, nonVoidMethodReturnTypeAsString, fieldNameBoolean));
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void generateCode(StringBuilder recordClassContent, Map<String, Object> params) {
        var annotatedElement = (Element) params.get(CodeGenerator.ANNOTATED_ELEMENT);
        var nonVoidMethodsElementsList = new ArrayList<Element>((List<? extends Element>) params.get(CodeGenerator.NON_VOID_METHODS_ELEMENTS_LIST));
        var isSuperRecord = (Boolean) params.get(CodeGenerator.IS_SUPER_RECORD);
        buildRecordFieldsFromNonVoidMethodsList(recordClassContent, annotatedElement, nonVoidMethodsElementsList, isSuperRecord);
    }
}