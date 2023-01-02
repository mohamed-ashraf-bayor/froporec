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

import static java.lang.String.format;
import static org.froporec.generator.helpers.CodeGenerator.nonVoidMethodsElementsList;
import static org.froporec.generator.helpers.StringGenerator.removeLastChars;

/**
 * Builds the list of fields of the record class being generated. ex: int a, String s, Person p.<br>
 * Considerations will be made for fields whose types have also been annotated or added as a .class value within the
 * "alsoConvert" attribute of {@link org.froporec.annotations.Record} or {@link org.froporec.annotations.Immutable}).<br>
 * The params {@link Map} parameter of the provided implementation of the generateCode() method (from {@link CodeGenerator}) MUST contain
 * the following parameters names:<br>
 * - {@link CodeGenerator#ANNOTATED_ELEMENT}<br>
 * - {@link CodeGenerator#NON_VOID_METHODS_ELEMENTS_LIST}<br>
 * - {@link CodeGenerator#IS_SUPER_RECORD}<br>
 */
public final class FieldsGenerator implements CodeGenerator {

    private static final String RECORD_FIELDS_LIST_FORMAT = "%s %s, "; // fieldType<SPACE>fieldName<COMMA><SPACE>

    private final ProcessingEnvironment processingEnv;

    private final Map<String, Set<Element>> allElementsTypesToConvertByAnnotation;

    private final Map<String, Map<Element, List<Element>>> mergeWithListByAnnotatedElementAndByAnnotation;

    private final SupportedCollectionsFieldsGenerator collectionsGenerator;

    /**
     * FieldsGenerationHelper constructor. Instantiates needed instance of {@link CollectionsGenerator}
     *
     * @param processingEnv                                  {@link ProcessingEnvironment} object, needed to access low-level information regarding the used annotations
     * @param allElementsTypesToConvertByAnnotation          {@link Set} of {@link Element} instances grouped by the annotation String representation
     * @param mergeWithListByAnnotatedElementAndByAnnotation {@link List} of provided 'mergeWith' {@link Element} POJO and/or Record instances
     *                                                       grouped by their respective annotated {@link Element} instances
     *                                                       and by their respective annotation String representation
     */
    public FieldsGenerator(ProcessingEnvironment processingEnv,
                           Map<String, Set<Element>> allElementsTypesToConvertByAnnotation,
                           Map<String, Map<Element, List<Element>>> mergeWithListByAnnotatedElementAndByAnnotation) {
        this.processingEnv = processingEnv;
        this.allElementsTypesToConvertByAnnotation = allElementsTypesToConvertByAnnotation;
        this.mergeWithListByAnnotatedElementAndByAnnotation = mergeWithListByAnnotatedElementAndByAnnotation;
        this.collectionsGenerator = new CollectionsGenerator(this.processingEnv, this.allElementsTypesToConvertByAnnotation);
    }

    private void buildRecordFieldsFromNonVoidMethodsList(StringBuilder recordClassContent,
                                                         Element annotatedElement,
                                                         List<Element> nonVoidMethodsElementsList,
                                                         boolean isSuperRecord) {
        var annotatedTypeElement = (TypeElement) processingEnv.getTypeUtils().asElement(annotatedElement.asType());
        // 1st, build fields list of the annotated elmnt
        buildFieldsList(recordClassContent, nonVoidMethodsElementsList, nonVoidMethodsElementsReturnTypesMapFromList(nonVoidMethodsElementsList),
                isSuperRecord ? annotatedTypeElement.getSimpleName().toString() : EMPTY_STRING); // if isSuperRecord each field is suffixed with the annotated type simple name
        // 2nd, add fields list of provided mergeWith elmnts if isSuperRecord
        if (isSuperRecord) {
            mergeWithListByAnnotatedElementAndByAnnotation.get(ORG_FROPOREC_SUPER_RECORD).get(annotatedElement).forEach(element -> {
                var typeElement = (TypeElement) processingEnv.getTypeUtils().asElement(element.asType());
                var nonVoidMthdsElmntsList = nonVoidMethodsElementsList(element, processingEnv);
                var nonVoidMethodsElementsReturnTypesMap = nonVoidMethodsElementsReturnTypesMapFromList(nonVoidMthdsElmntsList);
                buildFieldsList(recordClassContent, nonVoidMthdsElmntsList, nonVoidMethodsElementsReturnTypesMap, typeElement.getSimpleName().toString());
            });
        }
        removeLastChars(recordClassContent, 2);
    }

    private void buildFieldsList(StringBuilder recordClassContent,
                                 List<Element> nonVoidMethodsElementsList,
                                 Map<Element, String> nonVoidMethodsElementsReturnTypesMap,
                                 String fieldSuffix) {
        nonVoidMethodsElementsList.forEach(nonVoidMethodElement -> {
            var nameTypePairMapEntry = fieldNameAndTypePair(nonVoidMethodElement, nonVoidMethodsElementsReturnTypesMap,
                    allElementsTypesToConvertByAnnotation, processingEnv, collectionsGenerator)
                    .entrySet().iterator().next();
            recordClassContent.append(format(
                    RECORD_FIELDS_LIST_FORMAT,
                    nameTypePairMapEntry.getValue(),
                    nameTypePairMapEntry.getKey() + fieldSuffix.strip()
            ));
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public void generateCode(StringBuilder recordClassContent, Map<String, Object> params) {
        var annotatedElement = (Element) params.get(CodeGenerator.ANNOTATED_ELEMENT);
        var nonVoidMethodsElementsList = new ArrayList<Element>((List<? extends Element>) params.get(CodeGenerator.NON_VOID_METHODS_ELEMENTS_LIST));
        var isSuperRecord = Optional.ofNullable((Boolean) params.get(CodeGenerator.IS_SUPER_RECORD)).orElse(false).booleanValue();
        buildRecordFieldsFromNonVoidMethodsList(recordClassContent, annotatedElement, nonVoidMethodsElementsList, isSuperRecord);
    }
}