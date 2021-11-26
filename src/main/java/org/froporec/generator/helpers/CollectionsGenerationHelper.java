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

import java.util.Set;

import static java.lang.String.format;
import static org.froporec.generator.RecordSourceFileGenerator.RECORD;

/**
 * Class dedicated to generating code fragments where generics are used within collections, the generics types being annotated POJO classes
 * also provides a utility method to check whether an annotated type is a collection with a generic.
 * For now only Lists, Sets and Maps are supported.
 */
public class CollectionsGenerationHelper {

    private static final String EMPTY = "";

    private static final String NEW = "new "; // new<SPACE>

    private final Set<String> allAnnotatedElementsTypes;

    /**
     * Constructor of CollectionsGenerationHelper
     *
     * @param allAnnotatedElementsTypes - collection of all the annotated elements types
     */
    public CollectionsGenerationHelper(final Set<String> allAnnotatedElementsTypes) {
        this.allAnnotatedElementsTypes = allAnnotatedElementsTypes;
    }

    /**
     * Checks whether the provided type is a collection with a generic. The check is simply based on the string representation (fully qualified name) of the type
     * the check is based on the string representation. first checked is the presence of &lt;&gt; and then whether the name has "List", "Set" or "Map" in its name
     *
     * @param getterReturnType - the provided type qualified name
     * @return true or false
     */
    public boolean isCollectionWithGeneric(final String getterReturnType) {
        if (getterReturnType.indexOf('<') == -1 && getterReturnType.indexOf('>') == -1) {
            return false;
        }
        var collectionType = extractCollectionType(getterReturnType);
        return collectionType.contains(SupportedCollectionTypes.LIST.type)
                || collectionType.contains(SupportedCollectionTypes.SET.type)
                || collectionType.contains(SupportedCollectionTypes.MAP.type);
    }

    /**
     * Replaces every POJO class within a generic with its generated record class name. (only if Person was also annotated)
     * ex: if List&lt;Person&gt; is a member of an annotated POJO class, the generated record class of the POJO will have
     * a member of List&lt;PersonRecord&gt;
     *
     * @param getterReturnType type of the POJO being processed
     * @return the string replacing the POJO with its generated record class name. In case it's not a generic no replacement is performed
     */
    public String replaceGenericWithRecordClassNameIfAny(final String getterReturnType) {
        int idxFirstSign = getterReturnType.indexOf('<');
        if (idxFirstSign > -1) {
            var genericType = extractGenericType(getterReturnType);
            var collectionType = extractCollectionType(getterReturnType);
            if (collectionType.contains(SupportedCollectionTypes.MAP.type)) {
                // only for maps
                var keyValueArray = genericType.split(","); // the key/value entries in a Map genericType
                return format("%s<%s, %s>",
                        collectionType,
                        allAnnotatedElementsTypes.contains(keyValueArray[0]) ? keyValueArray[0] + RECORD : keyValueArray[0],
                        allAnnotatedElementsTypes.contains(keyValueArray[1]) ? keyValueArray[1] + RECORD : keyValueArray[1]);
            }
            if (allAnnotatedElementsTypes.contains(genericType)) {
                return format("%s<%s%s>", collectionType, genericType, RECORD);
            }
        }
        return getterReturnType;
    }

    /**
     * Builds a Collection.stream()... logic to allow the mapping of a collection of POJO classes to a collection of the corresponding generated Record classes,
     * only if the POJOs defined as generic(s) is/are annotated
     * This happens inside the generated custom constructor inside which we call the canonical constructor of the Record class being generated
     *
     * @param fieldName        field name being processed
     * @param getterAsString   getter name
     * @param getterReturnType getter return type, also the type of the field being processed
     * @return return the mapping logic for the collection field being processed
     */
    public String generateCollectionFieldMappingIfGenericIsAnnotated(final String fieldName, final String getterAsString, final String getterReturnType) {
        var collectionType = extractCollectionType(getterReturnType);
        var genericType = extractGenericType(getterReturnType);
        if (collectionType.contains(SupportedCollectionTypes.LIST.type)) {
            return format(
                    "%s.%s.stream().map(object -> %s(object)).collect(java.util.stream.Collectors.toUnmodifiableList()), ",
                    fieldName,
                    getterAsString,
                    allAnnotatedElementsTypes.contains(genericType) ? NEW + genericType + RECORD : EMPTY
            );
        }
        if (collectionType.contains(SupportedCollectionTypes.SET.type)) {
            return format(
                    "%s.%s.stream().map(object -> %s(object)).collect(java.util.stream.Collectors.toUnmodifiableSet()), ",
                    fieldName,
                    getterAsString,
                    allAnnotatedElementsTypes.contains(genericType) ? NEW + genericType + RECORD : EMPTY
            );
        }
        if (collectionType.contains(SupportedCollectionTypes.MAP.type)) {
            // only for maps
            var keyValueArray = genericType.split(","); // the key/value entries in a Map genericType
            if (!allAnnotatedElementsTypes.contains(keyValueArray[0]) && !allAnnotatedElementsTypes.contains(keyValueArray[1])) {
                // both key and value are annotated pojos
                return format("%s.%s, ", fieldName, getterAsString);
            }
            // either the key or the value types are annotated pojos
            return format(
                    "%s.%s.entrySet().stream().collect(java.util.stream.Collectors.toUnmodifiableMap(entry -> %s(entry.getKey()), entry -> %s(entry.getValue()))), ",
                    fieldName,
                    getterAsString,
                    allAnnotatedElementsTypes.contains(keyValueArray[0]) ? NEW + keyValueArray[0] + RECORD : EMPTY,
                    allAnnotatedElementsTypes.contains(keyValueArray[1]) ? NEW + keyValueArray[1] + RECORD : EMPTY
            );
        }
        throw new UnsupportedOperationException(format("%s not supported as a collection type", collectionType));
    }

    private enum SupportedCollectionTypes {
        LIST("List"),
        SET("Set"),
        MAP("Map");
        private final String type;

        SupportedCollectionTypes(final String type) {
            this.type = type;
        }
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
}