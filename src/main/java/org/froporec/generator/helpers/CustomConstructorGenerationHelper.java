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
     * @param qualifiedClassName qualified class name
     * @param gettersMap         map containing getters names as keys and their corresponding types as values. ex: {getAge=int, getSchool=org.froporec.data1.School, getLastname=java.lang.String}
     * @param gettersList        list of public getters of the POJO class being processed. ex:[getLastname(), getAge(), getMark(), getGrade(), getSchool()]
     */
    public void buildRecordCustom1ArgConstructor(final StringBuilder recordClassContent, final String qualifiedClassName, final Map<String, String> gettersMap, final List<? extends Element> gettersList) {
        var simpleClassName = qualifiedClassName.substring(qualifiedClassName.lastIndexOf('.') + 1);
        var fieldName = simpleClassName.substring(0, 1).toLowerCase() + simpleClassName.substring(1); // simpleClassName starting with lowercase char
        // %s = simple class name , %s = "Record" , %s = pojo class qualified name , %s = field name
        recordClassContent.append(format("\tpublic %s%s(%s %s) {%n", simpleClassName, RECORD, qualifiedClassName, fieldName)); // line declaring the constructor
        recordClassContent.append("\t\tthis("); // calling canonical constructor
        // building canonical constructor content
        System.out.println("^^^^^^^^allAnnotatedElementsTypes: " + allAnnotatedElementsTypes);
        gettersList.forEach(getter -> {
            var getterAsString = getter.toString();
            System.out.println("^^^^^^^^getterAsString: " + getterAsString);
            var getterReturnTypeFromMap = gettersMap.get(getterAsString.substring(0, getterAsString.indexOf('(')));
            System.out.println("^^^^^^^^getterReturnTypeFromMap: " + getterReturnTypeFromMap);
            var getterReturnTypeElementOpt = Optional.ofNullable(processingEnvironment.getTypeUtils().asElement(((ExecutableType) getter.asType()).getReturnType()));
            System.out.println("^^^^^^^^getterReturnTypeElementOpt: " + getterReturnTypeElementOpt);
            // if the pojo constructor param is another pojo, check if it's been annotated. if yes, use the corresponding generated record class
            if (getterReturnTypeElementOpt.isEmpty()) {
                // primitives
                buildCanonicalConstructorCallSingleParameter(recordClassContent, fieldName, getterAsString, getterReturnTypeFromMap, false);
            } else {
                // non-primitives
                buildCanonicalConstructorCallSingleParameter(recordClassContent, fieldName, getterAsString, getterReturnTypeFromMap, allAnnotatedElementsTypes.contains(getterReturnTypeElementOpt.get().toString()));
            }
        });
        recordClassContent.deleteCharAt(recordClassContent.length() - 1).deleteCharAt(recordClassContent.length() - 1);
        // finished building canonical constructor content
        recordClassContent.append(");\n");
        recordClassContent.append("\t}\n");
    }

    private void buildCanonicalConstructorCallSingleParameter(final StringBuilder recordClassContent, final String fieldName, final String getterAsString, final String getterReturnTypeFromMap, final boolean processAsRecord) {
        if (getterAsString.startsWith("get")) {
            var getterFieldNonBoolean = getterAsString.substring(3, 4).toLowerCase() + getterAsString.substring(4, getterAsString.indexOf('('));
            if (collectionsGenerationHelper.isCollectionWithGeneric(getterReturnTypeFromMap)) {
                recordClassContent.append(collectionsGenerationHelper.generateCollectionFieldMappingIfGenericIsAnnotated(fieldName, getterAsString, getterReturnTypeFromMap));
            } else {
                if (processAsRecord) {
                    recordClassContent.append(format("new %s%s(%s.%s), ", getterReturnTypeFromMap, RECORD, fieldName, getterAsString));
                } else {
                    // applies to primitives and any pojo which hasn't been annotated
                    // %s.%s = fieldname getterAsString
                    recordClassContent.append(format("%s.%s, ", fieldName, getterAsString));
                }
            }
        } else if (getterAsString.startsWith("is")) {
            recordClassContent.append(format("%s.%s, ", fieldName, getterAsString));
        }
    }
}