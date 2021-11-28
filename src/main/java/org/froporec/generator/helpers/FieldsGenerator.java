/**
 * Copyright (c) 2021 Mohamed Ashraf Bayor
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
import javax.lang.model.type.ExecutableType;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.lang.String.format;
import static org.froporec.generator.RecordSourceFileGenerator.RECORD;

/**
 * Builds the list of fields of the record class being generated. ex: int a, String s, Person p<br>
 * Considerations will be made for fields whose types have also been annotated<br>
 * The generateRecord() method params map MUST contain the following parameters names:<br>
 * gettersMap         @see {@link CodeGenerator#GETTERS_MAP}<br>
 * gettersList        @see {@link CodeGenerator#GETTERS_LIST}<br>
 */
public final class FieldsGenerator implements CodeGenerator {

    private final ProcessingEnvironment processingEnvironment;

    private final Set<String> allAnnotatedElementsTypes;

    private final SupportedCollectionsFieldsGenerator collectionsGenerator;

    /**
     * Constructor of FieldsGenerationHelper
     *
     * @param processingEnvironment     needed to access sourceversion and other useful info
     * @param allAnnotatedElementsTypes all annotated elements in the client program
     */
    public FieldsGenerator(final ProcessingEnvironment processingEnvironment, final Set<String> allAnnotatedElementsTypes) {
        this.processingEnvironment = processingEnvironment;
        this.allAnnotatedElementsTypes = allAnnotatedElementsTypes;
        this.collectionsGenerator = new CollectionsGenerator(this.allAnnotatedElementsTypes);
    }

    private void buildRecordFieldsFromGettersList(final StringBuilder recordClassContent, final Map<String, String> gettersMap, final List<? extends Element> gettersList) {
        gettersList.forEach(getter -> {
            var getterAsString = getter.toString();
            var getterReturnTypeFromMap = gettersMap.get(getterAsString.substring(0, getterAsString.indexOf('(')));
            var getterReturnTypeElementOpt = Optional.ofNullable(processingEnvironment.getTypeUtils().asElement(((ExecutableType) getter.asType()).getReturnType())); // for collections, Element.toString() will NOT return the generic part
            if (getterReturnTypeElementOpt.isEmpty()) {
                // primitives
                buildSingleField(recordClassContent, getterAsString, getterReturnTypeFromMap, false);
            } else {
                // non-primitives
                // if the type has already been annotated somewhere else in the code, the field type is the corresponding generated record class
                // if not annotated and not a collection then keep the received type as is
                buildSingleField(recordClassContent, getterAsString, getterReturnTypeFromMap, allAnnotatedElementsTypes.contains(getterReturnTypeElementOpt.get().toString()));
            }
        });
        recordClassContent.deleteCharAt(recordClassContent.length() - 1).deleteCharAt(recordClassContent.length() - 1);
    }

    private void buildSingleField(final StringBuilder recordClassContent, final String getterAsString, final String getterReturnTypeFromMap, final boolean processAsRecord) {
        final String recordFieldsListFormat = "%s %s, "; // type fieldName
        var getterFieldNonBoolean = getterAsString.substring(3, 4).toLowerCase() + getterAsString.substring(4, getterAsString.indexOf('('));
        if (getterAsString.startsWith("get")) {
            // if the type of the field being processed is a collection process it differently and return
            if (collectionsGenerator.isCollectionWithGeneric(getterReturnTypeFromMap)) {
                collectionsGenerator.replaceGenericWithRecordClassNameIfAny(recordClassContent, getterFieldNonBoolean, getterReturnTypeFromMap);
                return;
            }
            recordClassContent.append(format(recordFieldsListFormat, processAsRecord ? getterReturnTypeFromMap + RECORD : getterReturnTypeFromMap, getterFieldNonBoolean));
        } else if (getterAsString.startsWith("is")) {
            var getterFieldBoolean = getterAsString.substring(2, 3).toLowerCase() + getterAsString.substring(3, getterAsString.indexOf('('));
            recordClassContent.append(format(recordFieldsListFormat, getterReturnTypeFromMap, getterFieldBoolean));
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void generateCode(final StringBuilder recordClassContent, final Map<String, Object> params) {
        var gettersMap = (Map<String, String>) params.get(CodeGenerator.GETTERS_MAP);
        var gettersList = (List<? extends Element>) params.get(CodeGenerator.GETTERS_LIST);
        buildRecordFieldsFromGettersList(recordClassContent, gettersMap, gettersList);
    }
}