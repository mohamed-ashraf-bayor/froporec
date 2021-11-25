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
package org.froporec.processor;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.type.ExecutableType;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.String.format;

/**
 * class in charge of building the record class string content and write it to the generated record file
 *
 * @author Mohamed Ashraf Bayor
 */
public class RecordClassGenerator {

    private static final String RECORD = "Record";

    private final ProcessingEnvironment processingEnv;

    private final Set<String> allAnnotatedElementsTypes;

    RecordClassGenerator(final ProcessingEnvironment processingEnv, final Set<? extends Element> allAnnotatedElements) {
        this.processingEnv = processingEnv;
        this.allAnnotatedElementsTypes = buildAllAnnotatedElementsTypes(allAnnotatedElements);
    }

    private Set<String> buildAllAnnotatedElementsTypes(Set<? extends Element> allAnnotatedElements) {
        return allAnnotatedElements.stream()
                .map(element -> processingEnv.getTypeUtils().asElement(element.asType()).toString())
                .collect(Collectors.toSet());
    }

    void writeRecordClassFile(final String className, final List<? extends Element> gettersList, final Map<String, String> getterMap) throws IOException {
        var recordClassFile = processingEnv.getFiler().createSourceFile(className + RECORD); // if file already exists, this line throws an IOException
        var recordClassString = buildRecordClassContent(className, gettersList, getterMap);
        try (PrintWriter out = new PrintWriter(recordClassFile.openWriter())) {
            out.println(recordClassString);
        }
    }

    private String buildRecordClassContent(final String className, final List<? extends Element> gettersList, final Map<String, String> getterMap) {
        var recordClassContent = new StringBuilder();
        String packageName = null;
        int lastDot = className.lastIndexOf('.');
        if (lastDot > 0) {
            packageName = className.substring(0, lastDot);
        }
        var simpleClassName = className.substring(lastDot + 1);
        var recordClassName = className + RECORD;
        var recordSimpleClassName = recordClassName.substring(lastDot + 1);
        if (packageName != null) {
            recordClassContent.append("package ");
            recordClassContent.append(packageName);
            recordClassContent.append(';');
            recordClassContent.append("\n\n");
        }
        recordClassContent.append(format("public %s ", RECORD.toLowerCase()));
        recordClassContent.append(recordSimpleClassName);
        recordClassContent.append('(');
        buildRecordAttributesFromGettersList(recordClassContent, getterMap, gettersList);
        recordClassContent.append(") {\n\n");
        buildRecordCustom1ArgConstructor(recordClassContent, className, simpleClassName, getterMap, gettersList);
        recordClassContent.append('}');
        return recordClassContent.toString();
    }

    private void buildRecordAttributesFromGettersList(final StringBuilder recordClassContent, final Map<String, String> gettersMap, final List<? extends Element> gettersList) {
        gettersList.forEach(getter -> {
            var getterAsString = getter.toString();
            var getterNameWithoutParenthesis = getterAsString.substring(0, getterAsString.indexOf('('));
            var getterAttributeNonBoolean1stLetter = getterAsString.substring(3, 4).toLowerCase();
            var getterAttributeBoolean1stLetter = getterAsString.substring(2, 3).toLowerCase();
            var getterAttributeNonBooleanAfter1stLetter = getterAsString.substring(4, getterAsString.indexOf('('));
            var getterAttributeBooleanAfter1stLetter = getterAsString.substring(3, getterAsString.indexOf('('));
            var getterReturnTypeStringFromMap = gettersMap.get(getterNameWithoutParenthesis);
            var getterReturnTypeElementOpt = Optional.ofNullable(processingEnv.getTypeUtils().asElement(((ExecutableType) getter.asType()).getReturnType())); // for collections, Element.toString() will NOT return the generic part
            var recordAttributesListFormat = "%s %s%s, ";
            if (getterReturnTypeElementOpt.isEmpty()) {
                // primitives
                processPrimitives(recordClassContent, getterAsString, recordAttributesListFormat, getterReturnTypeStringFromMap, getterAttributeNonBoolean1stLetter, getterAttributeNonBooleanAfter1stLetter, getterAttributeBoolean1stLetter, getterAttributeBooleanAfter1stLetter);
            } else {
                // non-primitives
                if (allAnnotatedElementsTypes.contains(getterReturnTypeElementOpt.get().toString())) {
                    // if the type has already been annotated somewhere else in the code, the attribute type is the corresponding generated record class
                    if (getterAsString.startsWith("get")) {
                        recordClassContent.append(format(recordAttributesListFormat, getterReturnTypeStringFromMap + RECORD, getterAttributeNonBoolean1stLetter, getterAttributeNonBooleanAfter1stLetter));
                    } else if (getterAsString.startsWith("is")) {
                        recordClassContent.append(format(recordAttributesListFormat, getterReturnTypeStringFromMap + RECORD, getterAttributeBoolean1stLetter, getterAttributeBooleanAfter1stLetter));
                    }
                } else if (isCollectionWithGeneric(getterReturnTypeStringFromMap)) {
                    recordClassContent.append(format(recordAttributesListFormat, replaceGenericWithRecordClassNameIfAny(getterReturnTypeStringFromMap), getterAttributeNonBoolean1stLetter, getterAttributeNonBooleanAfter1stLetter));
                } else {
                    // if not annotated and not a collection then keep the received type as is
                    if (getterAsString.startsWith("get")) {
                        // here we also process cases where the getter type is a collection which might contain 1 or many annotated POJOs
                        recordClassContent.append(format(recordAttributesListFormat, getterReturnTypeStringFromMap, getterAttributeNonBoolean1stLetter, getterAttributeNonBooleanAfter1stLetter));
                    } else if (getterAsString.startsWith("is")) {
                        recordClassContent.append(format(recordAttributesListFormat, getterReturnTypeStringFromMap, getterAttributeBoolean1stLetter, getterAttributeBooleanAfter1stLetter));
                    }
                }
            }
        });
        recordClassContent.deleteCharAt(recordClassContent.length() - 1).deleteCharAt(recordClassContent.length() - 1);
    }

    private void processPrimitives(final StringBuilder recordClassContent, final String getterAsString, final String recordAttributesListFormat,
                                   final String getterReturnTypeStringFromMap, final String getterAttributeNonBoolean1stLetter, final String getterAttributeNonBooleanAfter1stLetter,
                                   final String getterAttributeBoolean1stLetter, final String getterAttributeBooleanAfter1stLetter) {
        if (getterAsString.startsWith("get")) {
            // %s = attribute type qualified name , %s = 1st letter of attribute name (lowercase) , %s = rest of the attribute name after 1st letter
            recordClassContent.append(format(recordAttributesListFormat, getterReturnTypeStringFromMap, getterAttributeNonBoolean1stLetter, getterAttributeNonBooleanAfter1stLetter));
        } else if (getterAsString.startsWith("is")) {
            recordClassContent.append(format(recordAttributesListFormat, getterReturnTypeStringFromMap, getterAttributeBoolean1stLetter, getterAttributeBooleanAfter1stLetter));
        }
    }

    private void buildRecordCustom1ArgConstructor(final StringBuilder recordClassContent, final String className, String simpleClassName, final Map<String, String> gettersMap, final List<? extends Element> gettersList) {
        // %s = simple class name , %s = "Record" , %s = pojo class fully qualified name , %s = 1st letter of the param name in lowercase , %s = rest of the param name
        var attributeName1stLetter = simpleClassName.substring(0, 1).toLowerCase();
        String attributeNameAfter1stLetter = simpleClassName.substring(1);
        recordClassContent.append(format("\tpublic %s%s(%s %s%s) {%n", simpleClassName, RECORD, className, attributeName1stLetter, attributeNameAfter1stLetter));
        recordClassContent.append("\t\tthis("); // calling canonical constructor
        gettersList.forEach(getter -> {
            var getterAsString = getter.toString();
            var getterNameWithoutParenthesis = getterAsString.substring(0, getterAsString.indexOf('('));
            var getterReturnTypeStringFromMap = gettersMap.get(getterNameWithoutParenthesis);
            var getterReturnTypeElementOpt = Optional.ofNullable(processingEnv.getTypeUtils().asElement(((ExecutableType) getter.asType()).getReturnType()));
            // if the pojo constructor param is another pojo check if it's been annotated. if yes use the corresponding generated record class
            if (getterReturnTypeElementOpt.isPresent() && allAnnotatedElementsTypes.contains(getterReturnTypeElementOpt.get().toString())) {
                recordClassContent.append(format("new %s%s(%s%s.%s), ", getterReturnTypeElementOpt.get(), RECORD, attributeName1stLetter, attributeNameAfter1stLetter, getterAsString));
            } else if (isCollectionWithGeneric(getterReturnTypeStringFromMap)) {
                recordClassContent.append(generateCollectionAttributeLogic(attributeName1stLetter + attributeNameAfter1stLetter, getterAsString, getterReturnTypeStringFromMap));
            } else {
                // if not just call the getter as is
                recordClassContent.append(format("%s%s.%s, ", attributeName1stLetter, attributeNameAfter1stLetter, getterAsString));
            }
        });
        recordClassContent.deleteCharAt(recordClassContent.length() - 1).deleteCharAt(recordClassContent.length() - 1);
        recordClassContent.append(");\n");
        recordClassContent.append("\t}\n");
    }

    private enum SupportedCollectionTypes {
        LIST("List"),
        SET("Set"),
        MAP("Map");
        private final String collectionName;
        SupportedCollectionTypes(String collectionName) {
            this.collectionName = collectionName;
        }
    }

    private String replaceGenericWithRecordClassNameIfAny(final String getterReturnType) {
        int idxFirstSign = getterReturnType.indexOf('<');
        if (idxFirstSign > -1) {
            var genericTypeString = extractGenericType(getterReturnType);
            var keyValueArray = genericTypeString.split(","); // the key/value entries in the genericType
            var collectionType = extractCollectionType(getterReturnType);
            if (extractCollectionType(getterReturnType).contains(SupportedCollectionTypes.MAP.collectionName)) {
                return format("%s<%s%s, %s%s>", collectionType, keyValueArray[0], RECORD, keyValueArray[1], RECORD);
            }
            if (allAnnotatedElementsTypes.contains(genericTypeString)) {
                return format("%s<%s%s>", collectionType, genericTypeString, RECORD);
            }
        }
        return getterReturnType;
    }

    private boolean isCollectionWithGeneric(final String getterReturnTypeString) {
        if (getterReturnTypeString.indexOf('<') == -1 && getterReturnTypeString.indexOf('>') == -1) {
            return false;
        }
        var collectionType = extractCollectionType(getterReturnTypeString);
        return collectionType.contains(SupportedCollectionTypes.LIST.collectionName)
                || collectionType.contains(SupportedCollectionTypes.SET.collectionName)
                || collectionType.contains(SupportedCollectionTypes.MAP.collectionName);
    }

    private String extractGenericType(final String getterReturnTypeString) {
        int idxFirstSign = getterReturnTypeString.indexOf('<');
        int idxLastSign = getterReturnTypeString.indexOf('>');
        return getterReturnTypeString.substring(0, idxLastSign).substring(idxFirstSign + 1);
    }

    private String extractCollectionType(final String getterReturnTypeString) {
        int idxFirstSign = getterReturnTypeString.indexOf('<');
        return getterReturnTypeString.substring(0, idxFirstSign);
    }

    private String generateCollectionAttributeLogic(final String attributeName, final String getterAsString, final String getterReturnTypeString) {
        var collectionType = extractCollectionType(getterReturnTypeString);
        var genericType = extractGenericType(getterReturnTypeString);
        if (collectionType.contains(SupportedCollectionTypes.LIST.collectionName)) {
            return format("%s.%s.stream().map(object -> new %s%s(object)).collect(java.util.stream.Collectors.toUnmodifiable%s()), ", attributeName, getterAsString, genericType, RECORD, SupportedCollectionTypes.LIST.collectionName);
        }
        if (collectionType.contains(SupportedCollectionTypes.SET.collectionName)) {
            return format("%s.%s.stream().map(object -> new %s%s(object)).collect(java.util.stream.Collectors.toUnmodifiable%s()), ", attributeName, getterAsString, genericType, RECORD, SupportedCollectionTypes.SET.collectionName);
        }
        if (collectionType.contains(SupportedCollectionTypes.MAP.collectionName)) {
            var keyValueArray = genericType.split(","); // the key/value entries in the genericType
            return format("%s.%s.entrySet().stream().collect(java.util.stream.Collectors.toUnmodifiable%s(entry -> new %s%s(entry.getKey()), entry -> new %s%s(entry.getValue()))), ", attributeName, getterAsString, SupportedCollectionTypes.MAP.collectionName, keyValueArray[0].trim(), RECORD, keyValueArray[1].trim(), RECORD);
        }
        throw new UnsupportedOperationException(format("%s not supported as a collection type", collectionType));
    }
}