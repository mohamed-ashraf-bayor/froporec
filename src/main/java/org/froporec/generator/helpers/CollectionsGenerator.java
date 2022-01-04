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
import java.util.Map;
import java.util.Set;

import static java.lang.String.format;

/**
 * Class dedicated to generating code fragments where generics are used within collections, the generics types being annotated POJO classes.<br>
 * Also provides a utility method to check whether an annotated type is a collection with a generic.<br>
 * For now only Lists, Sets and Maps are supported.
 */
public final class CollectionsGenerator implements SupportedCollectionsFieldsGenerator, SupportedCollectionsMappingLogicGenerator {

    private static final String NEW = "new "; // new<SPACE>
    public static final String LIST_FIELD_MAPPING_LOGIC_STRING_FORMAT = "java.util.Optional.ofNullable(%s.%s).isEmpty() ? java.util.List.of() : " +
            "%s.%s.stream().map(object -> %s(object)).collect(java.util.stream.Collectors.toUnmodifiableList()), ";
    public static final String SET_FIELD_MAPPING_LOGIC_STRING_FORMAT = "java.util.Optional.ofNullable(%s.%s).isEmpty() ? java.util.Set.of() : " +
            "%s.%s.stream().map(object -> %s(object)).collect(java.util.stream.Collectors.toUnmodifiableSet()), ";
    public static final String MAP_FIELD_MAPPING_LOGIC_STRING_FORMAT = "java.util.Optional.ofNullable(%s.%s).isEmpty() ? java.util.Map.of() : " +
            "%s.%s.entrySet().stream().collect(java.util.stream.Collectors.toUnmodifiableMap(entry -> %s(entry.getKey()), entry -> %s(entry.getValue()))), ";

    private final Set<String> allAnnotatedElementsTypes;

    private final ProcessingEnvironment processingEnvironment;

    /**
     * CollectionsGenerationHelper constructor. Instantiates needed instance of {@link ProcessingEnvironment}
     *
     * @param allAnnotatedElementsTypes - {@link Set} of all annotated elements types
     */
    public CollectionsGenerator(final ProcessingEnvironment processingEnvironment, final Set<String> allAnnotatedElementsTypes) {
        this.processingEnvironment = processingEnvironment;
        this.allAnnotatedElementsTypes = allAnnotatedElementsTypes;
    }

    @Override
    public void replaceGenericWithRecordClassNameIfAny(final StringBuilder recordClassContent, final String fieldName, final String nonVoidMethodReturnType) {
        String replacementString = nonVoidMethodReturnType;
        int idxFirstSign = nonVoidMethodReturnType.indexOf('<');
        if (idxFirstSign > -1) {
            var genericType = extractGenericType(nonVoidMethodReturnType);
            var collectionType = extractCollectionType(nonVoidMethodReturnType);
            if (collectionType.contains(SupportedCollectionTypes.MAP.getType())) {
                // only for maps
                var keyValueArray = genericType.split(","); // the key/value entries in a Map genericType
                replacementString = format("%s<%s, %s>",
                        collectionType,
                        allAnnotatedElementsTypes.contains(keyValueArray[0]) ? constructImmutableQualifiedNameBasedOnElementType(constructElementInstanceFromTypeString(processingEnvironment, keyValueArray[0]).get()) : keyValueArray[0],
                        allAnnotatedElementsTypes.contains(keyValueArray[1]) ? constructImmutableQualifiedNameBasedOnElementType(constructElementInstanceFromTypeString(processingEnvironment, keyValueArray[1]).get()) : keyValueArray[1]
                );
            }
            if (allAnnotatedElementsTypes.contains(genericType)) {
                replacementString = format("%s<%s>", collectionType, constructImmutableQualifiedNameBasedOnElementType(constructElementInstanceFromTypeString(processingEnvironment, genericType).get()));
            }
        }
        // "%s %s," type fieldName,
        recordClassContent.append(format("%s %s, ", replacementString, fieldName));
    }

    @Override
    public void generateCollectionFieldMappingIfGenericIsAnnotated(final StringBuilder recordClassContent, final String fieldName, final String nonVoidMethodElementAsString, final String nonVoidMethodReturnTypeAsString) {
        var collectionType = extractCollectionType(nonVoidMethodReturnTypeAsString);
        var genericType = extractGenericType(nonVoidMethodReturnTypeAsString);
        if (collectionType.contains(SupportedCollectionTypes.LIST.getType())) {
            recordClassContent.append(format(
                    LIST_FIELD_MAPPING_LOGIC_STRING_FORMAT,
                    fieldName,
                    nonVoidMethodElementAsString,
                    fieldName,
                    nonVoidMethodElementAsString,
                    allAnnotatedElementsTypes.contains(genericType) ? NEW + constructImmutableQualifiedNameBasedOnElementType(constructElementInstanceFromTypeString(processingEnvironment, genericType).get()) : EMPTY_STRING
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
                    allAnnotatedElementsTypes.contains(genericType) ? NEW + constructImmutableQualifiedNameBasedOnElementType(constructElementInstanceFromTypeString(processingEnvironment, genericType).get()) : EMPTY_STRING
            ));
            return;
        }
        if (collectionType.contains(SupportedCollectionTypes.MAP.getType())) {
            // only for maps
            var keyValueArray = genericType.split(","); // the key/value entries in a Map genericType
            if (!allAnnotatedElementsTypes.contains(keyValueArray[0]) && !allAnnotatedElementsTypes.contains(keyValueArray[1])) {
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
                    allAnnotatedElementsTypes.contains(keyValueArray[0]) ? NEW + constructImmutableQualifiedNameBasedOnElementType(constructElementInstanceFromTypeString(processingEnvironment, keyValueArray[0]).get()) : EMPTY_STRING,
                    allAnnotatedElementsTypes.contains(keyValueArray[1]) ? NEW + constructImmutableQualifiedNameBasedOnElementType(constructElementInstanceFromTypeString(processingEnvironment, keyValueArray[1]).get()) : EMPTY_STRING
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