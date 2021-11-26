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
 * Builds the custom 1 arg constructor for the record class being generated
 */
public class CustomConstructorGenerationHelper {

    private final ProcessingEnvironment processingEnvironment;

    private final Set<String> allAnnotatedElementsTypes;

    private final CollectionsGenerationHelper collectionsGenerationHelper;

    /**
     * Constructor of CustomConstructorGenerationHelper
     *
     * @param processingEnvironment     the processing environment needed to getTypeUtil() and getFile() methods
     * @param allAnnotatedElementsTypes all annotated elements in the client program
     */
    public CustomConstructorGenerationHelper(final ProcessingEnvironment processingEnvironment, final Set<String> allAnnotatedElementsTypes) {
        this.processingEnvironment = processingEnvironment;
        this.allAnnotatedElementsTypes = allAnnotatedElementsTypes;
        this.collectionsGenerationHelper = new CollectionsGenerationHelper(this.allAnnotatedElementsTypes);
    }

    /**
     * Builds the custom 1 arg constructor section for the record class being generated.
     * Starts with public RecordName(list of fields) and includes a call to the canonical constructor inside the body of the custom constructor
     *
     * @param recordClassContent content being built, containing the record source string
     * @param className          qualified class name
     * @param simpleClassName    simple class name
     * @param gettersMap         map containing getters names as keys and their corresponding types as values. ex: {getAge=int, getSchool=org.froporec.data1.School, getLastname=java.lang.String}
     * @param gettersList        list of public getters of the POJO class being processed. ex:[getLastname(), getAge(), getMark(), getGrade(), getSchool()]
     */
    public void buildRecordCustom1ArgConstructor(final StringBuilder recordClassContent, final String className, String simpleClassName, final Map<String, String> gettersMap, final List<? extends Element> gettersList) {
        // %s = simple class name , %s = "Record" , %s = pojo class qualified name , %s = 1st letter of the param name in lowercase , %s = rest of the param name
        var attributeName1stLetter = simpleClassName.substring(0, 1).toLowerCase();
        String attributeNameAfter1stLetter = simpleClassName.substring(1);
        recordClassContent.append(format("\tpublic %s%s(%s %s%s) {%n", simpleClassName, RECORD, className, attributeName1stLetter, attributeNameAfter1stLetter)); // line declaring the constructor
        recordClassContent.append("\t\tthis("); // calling canonical constructor
        // building canonical constructor content
        gettersList.forEach(getter -> {
            var getterAsString = getter.toString();
            var getterNameWithoutParenthesis = getterAsString.substring(0, getterAsString.indexOf('('));
            var getterReturnTypeStringFromMap = gettersMap.get(getterNameWithoutParenthesis);
            var getterReturnTypeElementOpt = Optional.ofNullable(processingEnvironment.getTypeUtils().asElement(((ExecutableType) getter.asType()).getReturnType()));
            // if the pojo constructor param is another pojo, check if it's been annotated. if yes, use the corresponding generated record class
            if (getterReturnTypeElementOpt.isPresent() && allAnnotatedElementsTypes.contains(getterReturnTypeElementOpt.get().toString())) {
                if (collectionsGenerationHelper.isCollectionWithGeneric(getterReturnTypeStringFromMap)) {
                    recordClassContent.append(collectionsGenerationHelper.generateCollectionFieldMappingLogic(attributeName1stLetter + attributeNameAfter1stLetter, getterAsString, getterReturnTypeStringFromMap));
                } else {
                    recordClassContent.append(format("new %s%s(%s%s.%s), ", getterReturnTypeElementOpt.get(), RECORD, attributeName1stLetter, attributeNameAfter1stLetter, getterAsString));
                }
            } else {
                // if not just call the getter as is - applies to primitives and any pojo which hasn't been annotated
                // %s%s.%s = attributeName1stLetter attributeNameAfter1stLetter getterAsString
                recordClassContent.append(format("%s%s.%s, ", attributeName1stLetter, attributeNameAfter1stLetter, getterAsString));
            }
        });
        recordClassContent.deleteCharAt(recordClassContent.length() - 1).deleteCharAt(recordClassContent.length() - 1);
        // finished building canonical constructor content
        recordClassContent.append(");\n");
        recordClassContent.append("\t}\n");
    }
}