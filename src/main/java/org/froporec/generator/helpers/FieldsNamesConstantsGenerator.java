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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;
import static org.froporec.generator.helpers.StringGenerator.javaConstantNamingConvention;

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
public final class FieldsNamesConstantsGenerator implements CodeGenerator {

    private final ProcessingEnvironment processingEnvironment;

    private final Map<String, Set<Element>> allElementsTypesToConvertByAnnotation;

    private final Map<String, Map<Element, List<Element>>> mergeWithListByAnnotatedElementAndByAnnotation;

    private final CodeGenerator fieldsGenerator;

    /**
     * // TODO chnge jdoc
     * FieldsGenerationHelper constructor. Instantiates needed instance of {@link CollectionsGenerator}
     *
     * @param processingEnvironment                          {@link ProcessingEnvironment} object, needed to access low-level information regarding the used annotations
     * @param allElementsTypesToConvertByAnnotation          {@link Set} of {@link Element} instances grouped by the annotation String representation
     * @param mergeWithListByAnnotatedElementAndByAnnotation {@link List} of provided 'mergeWith' {@link Element} POJO and/or Record instances
     *                                                       grouped by their respective annotated {@link Element} instances
     *                                                       and by their respective annotation String representation
     */
    public FieldsNamesConstantsGenerator(ProcessingEnvironment processingEnvironment,
                                         Map<String, Set<Element>> allElementsTypesToConvertByAnnotation,
                                         Map<String, Map<Element, List<Element>>> mergeWithListByAnnotatedElementAndByAnnotation) {
        this.processingEnvironment = processingEnvironment;
        this.allElementsTypesToConvertByAnnotation = allElementsTypesToConvertByAnnotation;
        this.mergeWithListByAnnotatedElementAndByAnnotation = mergeWithListByAnnotatedElementAndByAnnotation;
        this.fieldsGenerator = new FieldsGenerator(this.processingEnvironment, this.allElementsTypesToConvertByAnnotation, this.mergeWithListByAnnotatedElementAndByAnnotation);
    }

    private void buildFieldsConstantsFromNonVoidMethodsList(StringBuilder recordClassContent, Element annotatedElement, ArrayList<Element> nonVoidMethodsElementsList, Boolean isSuperRecord) {
        var commaSeparatedFieldsTypesNamesPairs = new StringBuilder();
        var separator = "##"; // make it 2 chars for now
        fieldsGenerator.generateCode(commaSeparatedFieldsTypesNamesPairs,
                Map.of(ANNOTATED_ELEMENT, annotatedElement, NON_VOID_METHODS_ELEMENTS_LIST, nonVoidMethodsElementsList, IS_SUPER_RECORD, isSuperRecord, FIELDS_LIST_SEPARATOR, separator));
        recordClassContent.append(TAB);
        recordClassContent.append(
                stream(commaSeparatedFieldsTypesNamesPairs.toString().split(separator))
                        .map(fieldDeclaration -> fieldDeclaration.strip().substring(fieldDeclaration.strip().lastIndexOf(SPACE) + 1))
                        .map(fieldName -> PUBLIC + SPACE + STATIC + SPACE + STRING + SPACE + javaConstantNamingConvention(fieldName) + SPACE + EQUALS_STR + SPACE + DOUBLE_QUOTES + fieldName + DOUBLE_QUOTES + SEMI_COLON)
                        .collect(Collectors.joining(NEW_LINE + TAB))
        );
    }

    @Override
    @SuppressWarnings("unchecked")
    public void generateCode(StringBuilder recordClassContent, Map<String, Object> params) {
        var annotatedElement = (Element) params.get(CodeGenerator.ANNOTATED_ELEMENT);
        var nonVoidMethodsElementsList = new ArrayList<Element>((List<? extends Element>) params.get(CodeGenerator.NON_VOID_METHODS_ELEMENTS_LIST));
        var isSuperRecord = (Boolean) params.get(CodeGenerator.IS_SUPER_RECORD);
        buildFieldsConstantsFromNonVoidMethodsList(recordClassContent, annotatedElement, nonVoidMethodsElementsList, isSuperRecord);
    }
}