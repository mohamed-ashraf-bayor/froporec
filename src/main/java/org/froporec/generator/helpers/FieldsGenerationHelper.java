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
 * Dedicated to builds the list of fields of the record class being generated. ex: int a, String s, Person p
 * Considerations will be made for fields whose types have also annotated
 */
public class FieldsGenerationHelper {

    private final ProcessingEnvironment processingEnvironment;

    private final Set<String> allAnnotatedElementsTypes;

    private final CollectionsGenerationHelper collectionsGenerationHelper;

    /**
     * Constructor of FieldsGenerationHelper
     * @param processingEnvironment needed to access sourceversion and other useful info
     * @param allAnnotatedElementsTypes all annotated elements in the client program
     */
    public FieldsGenerationHelper(final ProcessingEnvironment processingEnvironment, final Set<String> allAnnotatedElementsTypes) {
        this.processingEnvironment = processingEnvironment;
        this.allAnnotatedElementsTypes = allAnnotatedElementsTypes;
        this.collectionsGenerationHelper = new CollectionsGenerationHelper(this.allAnnotatedElementsTypes);
    }

    /**
     * Builds the list of fields' block of the record class. ex: int a, String s, Person p
     * @param recordClassContent content being built, containing the record source string
     * @param gettersMap map containing getters names as keys and their corresponding types as values. ex: {getAge=int, getSchool=org.froporec.data1.School, getLastname=java.lang.String}
     * @param gettersList list of public getters of the POJO class being processed. ex:[getLastname(), getAge(), getMark(), getGrade(), getSchool()]
     */
    public void buildRecordFieldsFromGettersList(final StringBuilder recordClassContent, final Map<String, String> gettersMap, final List<? extends Element> gettersList) {
        gettersList.forEach(getter -> {
            var getterAsString = getter.toString();
            var getterNameWithoutParenthesis = getterAsString.substring(0, getterAsString.indexOf('('));
            var getterReturnTypeStringFromMap = gettersMap.get(getterNameWithoutParenthesis);
            var getterReturnTypeElementOpt = Optional.ofNullable(processingEnvironment.getTypeUtils().asElement(((ExecutableType) getter.asType()).getReturnType())); // for collections, Element.toString() will NOT return the generic part
            // var recordFieldsListFormat = "%s %s%s, "; // type field1stLetter fieldNameAfter1stLetter
            if (getterReturnTypeElementOpt.isEmpty()) {
                // primitives
                buildSingleField(recordClassContent, getterAsString, getterReturnTypeStringFromMap, false);
            } else {
                // non-primitives
                if (allAnnotatedElementsTypes.contains(getterReturnTypeElementOpt.get().toString())) {
                    // if the type has already been annotated somewhere else in the code, the field type is the corresponding generated record class
                    buildSingleField(recordClassContent, getterAsString, getterReturnTypeStringFromMap, true);
                } else {
                    // if not annotated and not a collection then keep the received type as is
                    buildSingleField(recordClassContent, getterAsString, getterReturnTypeStringFromMap, false);
                }
            }
        });
        recordClassContent.deleteCharAt(recordClassContent.length() - 1).deleteCharAt(recordClassContent.length() - 1);
    }

    private void buildSingleField(final StringBuilder recordClassContent, final String getterAsString, final String getterReturnTypeStringFromMap, final boolean processAsRecord) {
        final String recordFieldsListFormat = "%s %s%s, "; // type field1stLetter fieldNameAfter1stLetter
        if (getterAsString.startsWith("get")) {
            var getterFieldNonBoolean1stLetter = getterAsString.substring(3, 4).toLowerCase();
            var getterFieldNonBooleanAfter1stLetter = getterAsString.substring(4, getterAsString.indexOf('('));
            // if the type of the field being processed is a collection process it differently and return
            if (collectionsGenerationHelper.isCollectionWithGeneric(getterReturnTypeStringFromMap)) {
                recordClassContent.append(format(recordFieldsListFormat, collectionsGenerationHelper.replaceGenericWithRecordClassNameIfAny(getterReturnTypeStringFromMap), getterFieldNonBoolean1stLetter, getterFieldNonBooleanAfter1stLetter));
                return;
            }
            recordClassContent.append(format(recordFieldsListFormat, processAsRecord ? getterReturnTypeStringFromMap + RECORD : getterReturnTypeStringFromMap, getterFieldNonBoolean1stLetter, getterFieldNonBooleanAfter1stLetter));
        } else if (getterAsString.startsWith("is")) {
            var getterFieldBoolean1stLetter = getterAsString.substring(2, 3).toLowerCase();
            var getterFieldBooleanAfter1stLetter = getterAsString.substring(3, getterAsString.indexOf('('));
            recordClassContent.append(format(recordFieldsListFormat, processAsRecord ? getterReturnTypeStringFromMap + RECORD : getterReturnTypeStringFromMap, getterFieldBoolean1stLetter, getterFieldBooleanAfter1stLetter));
        }
    }
}