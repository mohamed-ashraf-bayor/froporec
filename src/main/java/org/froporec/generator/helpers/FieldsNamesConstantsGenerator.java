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
import java.util.Optional;
import java.util.Set;

import static java.lang.String.format;
import static org.froporec.generator.helpers.StringGenerator.javaConstantNamingConvention;

/**
 * // TODO chnge jdoc
 * Builds the list of fields of the record class being generated. ex: int a, String s, Person p.<br>
 * Considerations will be made for fields whose types have also been annotated or added as a .class value within the
 * "alsoConvert" attribute of {@link org.froporec.annotations.Record} or {@link org.froporec.annotations.Immutable}).<br>
 * The params {@link Map} parameter of the provided implementation of the generateCode() method (from {@link CodeGenerator}) MUST contain
 * the following parameters names:<br>
 * - {@link CodeGenerator#NON_VOID_METHODS_ELEMENTS_LIST}<br>
 * - {@link CodeGenerator#IS_SUPER_RECORD}<br>
 */
public final class FieldsNamesConstantsGenerator implements CodeGenerator {

    private static final String CONSTANT_DECLARATION_FORMAT = "public static final String %s = \"%s\"; // type: %s";

    private final ProcessingEnvironment processingEnvironment;

    private final Map<String, Set<Element>> allElementsTypesToConvertByAnnotation;

    private final SupportedCollectionsFieldsGenerator collectionsGenerator;

    /**
     * TODO cmplt...
     *
     * @param processingEnvironment
     * @param allElementsTypesToConvertByAnnotation
     */
    public FieldsNamesConstantsGenerator(ProcessingEnvironment processingEnvironment,
                                         Map<String, Set<Element>> allElementsTypesToConvertByAnnotation) {
        this.processingEnvironment = processingEnvironment;
        this.allElementsTypesToConvertByAnnotation = allElementsTypesToConvertByAnnotation;
        this.collectionsGenerator = new CollectionsGenerator(this.processingEnvironment, this.allElementsTypesToConvertByAnnotation);
    }

    private void buildFieldsConstantsFromNonVoidMethodsList(StringBuilder recordClassContent, List<Element> nonVoidMethodsElementsList) {
        recordClassContent.append(NEW_LINE + TAB);
        var nonVoidMethodsElementsReturnTypesMap = nonVoidMethodsElementsReturnTypesMapFromList(nonVoidMethodsElementsList);
        nonVoidMethodsElementsList.forEach(nonVoidMethodElement -> {
            var nameTypePairMapEntry = fieldNameAndTypePair(nonVoidMethodElement, nonVoidMethodsElementsReturnTypesMap,
                    allElementsTypesToConvertByAnnotation, processingEnvironment, collectionsGenerator)
                    .entrySet().iterator().next();
            recordClassContent.append(format(
                    CONSTANT_DECLARATION_FORMAT,
                    javaConstantNamingConvention(nameTypePairMapEntry.getKey()),
                    nameTypePairMapEntry.getKey(),
                    nameTypePairMapEntry.getValue()
            ));
            recordClassContent.append(NEW_LINE + TAB);
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public void generateCode(StringBuilder recordClassContent, Map<String, Object> params) {
        var nonVoidMethodsElementsList = new ArrayList<Element>((List<? extends Element>) params.get(CodeGenerator.NON_VOID_METHODS_ELEMENTS_LIST));
        var isSuperRecord = Optional.ofNullable((Boolean) params.get(CodeGenerator.IS_SUPER_RECORD)).orElse(false).booleanValue();
        if (isSuperRecord) {
            return; // fields names constants generation NOT YET supported for SuperRecord classes
        }
        buildFieldsConstantsFromNonVoidMethodsList(recordClassContent, nonVoidMethodsElementsList);
    }
}