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
import java.util.Map;
import java.util.Set;

import static java.lang.String.format;
import static org.froporec.generator.helpers.StringGenerator.constructImmutableQualifiedNameBasedOnElementType;

/**
 * Generates code fragments where generics are used within collections, the generics types being annotated POJO or Record classes.<br>
 * Also provides a utility method to check whether an annotated type is a collection with a generic.<br>
 * For now, only Lists, Sets and Maps are supported.
 */
public final class CollectionsGenerator implements SupportedCollectionsFieldsGenerator, SupportedCollectionsMappingLogicGenerator {

    private static final String NEW = "new "; // new<SPACE>

    private static final String LIST_FIELD_MAPPING_LOGIC_STRING_FORMAT = "java.util.Optional.ofNullable(%s.%s).isEmpty() ? java.util.List.of() : " +
            "%s.%s.stream().map(object -> %s(object)).collect(java.util.stream.Collectors.toUnmodifiableList()), ";

    private static final String SET_FIELD_MAPPING_LOGIC_STRING_FORMAT = "java.util.Optional.ofNullable(%s.%s).isEmpty() ? java.util.Set.of() : " +
            "%s.%s.stream().map(object -> %s(object)).collect(java.util.stream.Collectors.toUnmodifiableSet()), ";

    private static final String MAP_FIELD_MAPPING_LOGIC_STRING_FORMAT = "java.util.Optional.ofNullable(%s.%s).isEmpty() ? java.util.Map.of() : " +
            "%s.%s.entrySet().stream().collect(java.util.stream.Collectors.toUnmodifiableMap(entry -> %s(entry.getKey()), entry -> %s(entry.getValue()))), ";

    private final Map<String, Set<Element>> allElementsTypesToConvertByAnnotation;

    private final ProcessingEnvironment processingEnvironment;

    /**
     * CollectionsGenerationHelper constructor
     *
     * @param allElementsTypesToConvertByAnnotation - {@link Set} of all annotated elements types
     */
    public CollectionsGenerator(ProcessingEnvironment processingEnvironment, Map<String, Set<Element>> allElementsTypesToConvertByAnnotation) {
        this.processingEnvironment = processingEnvironment;
        this.allElementsTypesToConvertByAnnotation = allElementsTypesToConvertByAnnotation;
    }

    @Override
    public void replaceGenericWithRecordClassNameIfAny(StringBuilder recordClassContent, String fieldName, String nonVoidMethodReturnTypeAsString) {
        int idxFirstSign = nonVoidMethodReturnTypeAsString.indexOf(INFERIOR_SIGN);
        // "%s %s," type fieldName,
        var typeAndFieldNameDeclarationFormat = "%s %s, ";
        if (idxFirstSign == -1) {
            // no generic found, use return type as is
            recordClassContent.append(format(typeAndFieldNameDeclarationFormat, nonVoidMethodReturnTypeAsString, fieldName));
        } else {
            // generic found, process and replace return type with record class if any
            var genericType = extractGenericType(nonVoidMethodReturnTypeAsString);
            var collectionType = extractCollectionType(nonVoidMethodReturnTypeAsString);
            if (collectionType.contains(SupportedCollectionTypes.MAP.getType())) {
                // only for maps
                var keyValueArray = genericType.split(COMMA_SEPARATOR); // the key/value entries in a Map genericType
                recordClassContent.append(format(
                        typeAndFieldNameDeclarationFormat,
                        buildReplacementStringForMapGeneric(collectionType, keyValueArray[0], keyValueArray[1]),
                        fieldName
                ));
            } else if (collectionType.contains(SupportedCollectionTypes.LIST.getType()) || collectionType.contains(SupportedCollectionTypes.SET.getType())) {
                recordClassContent.append(format(
                        typeAndFieldNameDeclarationFormat,
                        buildReplacementStringForListOrSetGeneric(collectionType, genericType, nonVoidMethodReturnTypeAsString),
                        fieldName));
            }
        }
    }

    private String buildReplacementStringForListOrSetGeneric(String collectionType,
                                                             String genericType,
                                                             String nonVoidMethodReturnTypeAsString) {
        if (isTypeAnnotatedAsRecordOrImmutable(processingEnvironment, allElementsTypesToConvertByAnnotation).test(genericType)) {
            var elementFromGenericTypeStringOpt = constructElementInstanceFromTypeString(processingEnvironment, genericType);
            return format(
                    "%s<%s>",
                    collectionType,
                    elementFromGenericTypeStringOpt.isEmpty()
                            ? genericType
                            : constructImmutableQualifiedNameBasedOnElementType(elementFromGenericTypeStringOpt.get())
            );
        } else return nonVoidMethodReturnTypeAsString;
    }

    private String buildReplacementStringForMapGeneric(String collectionType, String keyType, String valueType) {
        return format("%s<%s, %s>",
                collectionType,
                isTypeAnnotatedAsRecordOrImmutable(processingEnvironment, allElementsTypesToConvertByAnnotation).test(keyType)
                        ? constructImmutableQualifiedNameBasedOnElementType(constructElementInstanceValueFromTypeString(processingEnvironment, keyType))
                        : keyType,
                isTypeAnnotatedAsRecordOrImmutable(processingEnvironment, allElementsTypesToConvertByAnnotation).test(valueType)
                        ? constructImmutableQualifiedNameBasedOnElementType(constructElementInstanceValueFromTypeString(processingEnvironment, valueType))
                        : valueType
        );
    }

    @Override
    public void generateCollectionFieldMappingIfGenericIsAnnotated(StringBuilder recordClassContent,
                                                                   String fieldName,
                                                                   String nonVoidMethodElementAsString,
                                                                   String nonVoidMethodReturnType) {
        var collectionType = extractCollectionType(nonVoidMethodReturnType);
        var genericType = extractGenericType(nonVoidMethodReturnType);
        if (collectionType.contains(SupportedCollectionTypes.LIST.getType())) {
            recordClassContent.append(format(
                    LIST_FIELD_MAPPING_LOGIC_STRING_FORMAT,
                    fieldName,
                    nonVoidMethodElementAsString,
                    fieldName,
                    nonVoidMethodElementAsString,
                    isTypeAnnotatedAsRecordOrImmutable(processingEnvironment, allElementsTypesToConvertByAnnotation).test(genericType)
                            ? NEW + constructImmutableQualifiedNameBasedOnElementType(constructElementInstanceValueFromTypeString(processingEnvironment, genericType))
                            : EMPTY_STRING
            ));
            return;
        }
        if (collectionType.contains(SupportedCollectionTypes.SET.getType())) {
            recordClassContent.append(format(
                    SET_FIELD_MAPPING_LOGIC_STRING_FORMAT,
                    fieldName,
                    nonVoidMethodElementAsString,
                    fieldName,
                    nonVoidMethodElementAsString,
                    isTypeAnnotatedAsRecordOrImmutable(processingEnvironment, allElementsTypesToConvertByAnnotation).test(genericType)
                            ? NEW + constructImmutableQualifiedNameBasedOnElementType(constructElementInstanceValueFromTypeString(processingEnvironment, genericType))
                            : EMPTY_STRING
            ));
            return;
        }
        if (collectionType.contains(SupportedCollectionTypes.MAP.getType())) {
            // only for maps
            var keyValueArray = genericType.split(COMMA_SEPARATOR); // the key/value entries in a Map genericType
            if (!isTypeAnnotatedAsRecordOrImmutable(processingEnvironment, allElementsTypesToConvertByAnnotation).test(keyValueArray[0])
                    && !isTypeAnnotatedAsRecordOrImmutable(processingEnvironment, allElementsTypesToConvertByAnnotation).test(keyValueArray[1])) {
                // both key and value are annotated pojos
                recordClassContent.append(format("%s.%s, ", fieldName, nonVoidMethodElementAsString));
                return;
            }
            // either the key or the value types are annotated pojos
            recordClassContent.append(format(
                    MAP_FIELD_MAPPING_LOGIC_STRING_FORMAT,
                    fieldName,
                    nonVoidMethodElementAsString,
                    fieldName,
                    nonVoidMethodElementAsString,
                    isTypeAnnotatedAsRecordOrImmutable(processingEnvironment, allElementsTypesToConvertByAnnotation).test(keyValueArray[0])
                            ? NEW + constructImmutableQualifiedNameBasedOnElementType(constructElementInstanceValueFromTypeString(processingEnvironment, keyValueArray[0]))
                            : EMPTY_STRING,
                    isTypeAnnotatedAsRecordOrImmutable(processingEnvironment, allElementsTypesToConvertByAnnotation).test(keyValueArray[1])
                            ? NEW + constructImmutableQualifiedNameBasedOnElementType(constructElementInstanceValueFromTypeString(processingEnvironment, keyValueArray[1]))
                            : EMPTY_STRING
            ));
            return;
        }
        throw new UnsupportedOperationException(format("%s not supported as a collection type", collectionType));
    }

    @Override
    public void generateCode(StringBuilder recordClassContent, Map<String, Object> params) {
        throw new UnsupportedOperationException("Call replaceGenericWithRecordClassNameIfAny or generateCollectionFieldMappingIfGenericIsAnnotated methods instead");
        // best would be to provide an additional parameter in the params map and based on its value call either replaceGenericWithRecordClassNameIfAny or generateCollectionFieldMappingIfGenericIsAnnotated
    }
}