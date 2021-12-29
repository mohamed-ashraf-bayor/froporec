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

import java.util.Map;
import java.util.Set;

import static java.lang.String.format;
import static org.froporec.generator.RecordSourceFileGenerator.RECORD;

/**
 * Class dedicated to generating code fragments where generics are used within collections, the generics types being annotated POJO classes.<br>
 * Also provides a utility method to check whether an annotated type is a collection with a generic.<br>
 * For now only Lists, Sets and Maps are supported.
 */
public final class CollectionsGenerator implements SupportedCollectionsFieldsGenerator, SupportedCollectionsMappingLogicGenerator {

    private static final String EMPTY = "";

    private static final String NEW = "new "; // new<SPACE>

    private final Set<String> allAnnotatedElementsTypes;

    /**
     * CollectionsGenerationHelper constructor
     *
     * @param allAnnotatedElementsTypes - {@link Set} of all annotated elements types
     */
    public CollectionsGenerator(final Set<String> allAnnotatedElementsTypes) {
        this.allAnnotatedElementsTypes = allAnnotatedElementsTypes;
    }

    @Override
    public void replaceGenericWithRecordClassNameIfAny(final StringBuilder recordClassContent, final String fieldName, final String getterReturnType) {
        String replacementString = getterReturnType;
        int idxFirstSign = getterReturnType.indexOf('<');
        if (idxFirstSign > -1) {
            var genericType = extractGenericType(getterReturnType);
            var collectionType = extractCollectionType(getterReturnType);
            if (collectionType.contains(SupportedCollectionTypes.MAP.getType())) {
                // only for maps
                var keyValueArray = genericType.split(","); // the key/value entries in a Map genericType
                replacementString = format("%s<%s, %s>",
                        collectionType,
                        allAnnotatedElementsTypes.contains(keyValueArray[0]) ? keyValueArray[0] + RECORD : keyValueArray[0],
                        allAnnotatedElementsTypes.contains(keyValueArray[1]) ? keyValueArray[1] + RECORD : keyValueArray[1]);
            }
            if (allAnnotatedElementsTypes.contains(genericType)) {
                replacementString = format("%s<%s%s>", collectionType, genericType, RECORD);
            }
        }
        // "%s %s," type fieldName,
        recordClassContent.append(format("%s %s, ", replacementString, fieldName));
    }

    @Override
    public void generateCollectionFieldMappingIfGenericIsAnnotated(final StringBuilder recordClassContent, final String fieldName, final String getterAsString, final String getterReturnType) {
        var collectionType = extractCollectionType(getterReturnType);
        var genericType = extractGenericType(getterReturnType);
        if (collectionType.contains(SupportedCollectionTypes.LIST.getType())) {
            recordClassContent.append(format(
                    "%s.%s.stream().map(object -> %s(object)).collect(java.util.stream.Collectors.toUnmodifiableList()), ",
                    fieldName,
                    getterAsString,
                    allAnnotatedElementsTypes.contains(genericType) ? NEW + genericType + RECORD : EMPTY
            ));
            return;
        }
        if (collectionType.contains(SupportedCollectionTypes.SET.getType())) {
            recordClassContent.append(format(
                    "%s.%s.stream().map(object -> %s(object)).collect(java.util.stream.Collectors.toUnmodifiableSet()), ",
                    fieldName,
                    getterAsString,
                    allAnnotatedElementsTypes.contains(genericType) ? NEW + genericType + RECORD : EMPTY
            ));
            return;
        }
        if (collectionType.contains(SupportedCollectionTypes.MAP.getType())) {
            // only for maps
            var keyValueArray = genericType.split(","); // the key/value entries in a Map genericType
            if (!allAnnotatedElementsTypes.contains(keyValueArray[0]) && !allAnnotatedElementsTypes.contains(keyValueArray[1])) {
                // both key and value are annotated pojos
                recordClassContent.append(format("%s.%s, ", fieldName, getterAsString));
                return;
            }
            // either the key or the value types are annotated pojos
            recordClassContent.append(format(
                    "%s.%s.entrySet().stream().collect(java.util.stream.Collectors.toUnmodifiableMap(entry -> %s(entry.getKey()), entry -> %s(entry.getValue()))), ",
                    fieldName,
                    getterAsString,
                    allAnnotatedElementsTypes.contains(keyValueArray[0]) ? NEW + keyValueArray[0] + RECORD : EMPTY,
                    allAnnotatedElementsTypes.contains(keyValueArray[1]) ? NEW + keyValueArray[1] + RECORD : EMPTY
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