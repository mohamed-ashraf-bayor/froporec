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
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.lang.String.format;
import static org.froporec.generator.helpers.StringGenerator.immutableQualifiedNameBasedOnElementType;
import static org.froporec.generator.helpers.StringGenerator.removeLastChars;
import static org.froporec.generator.helpers.SupportedCollectionsGenerator.SupportedCollectionTypes.LIST;
import static org.froporec.generator.helpers.SupportedCollectionsGenerator.SupportedCollectionTypes.MAP;
import static org.froporec.generator.helpers.SupportedCollectionsGenerator.SupportedCollectionTypes.SET;

/**
 * Generates code fragments where generics are used within collections, the generics types being annotated POJO or Record classes.<br>
 * Also provides a utility method to check whether an annotated type is a collection with a generic.<br>
 * For now, only Lists, Sets and Maps are supported.
 */
public final class CollectionsGenerator implements SupportedCollectionsFieldsGenerator, SupportedCollectionsMappingLogicGenerator {

    private static final String LIST_FIELD_MAPPING_LOGIC_STRING_FORMAT = "java.util.Optional.ofNullable(%s.%s).isEmpty() ? java.util.List.of() : " +
            "%s.%s.stream().map(object -> %s(object)).collect(java.util.stream.Collectors.toUnmodifiableList()), ";

    private static final String SET_FIELD_MAPPING_LOGIC_STRING_FORMAT = "java.util.Optional.ofNullable(%s.%s).isEmpty() ? java.util.Set.of() : " +
            "%s.%s.stream().map(object -> %s(object)).collect(java.util.stream.Collectors.toUnmodifiableSet()), ";

    private static final String MAP_FIELD_MAPPING_LOGIC_STRING_FORMAT = "java.util.Optional.ofNullable(%s.%s).isEmpty() ? java.util.Map.of() : " +
            "%s.%s.entrySet().stream().collect(java.util.stream.Collectors.toUnmodifiableMap(" +
            ENTRY + SPACE + LAMBDA_SYMB + SPACE + "%s(" + ENTRY + ".getKey()), " +
            ENTRY + SPACE + LAMBDA_SYMB + SPACE + "%s(" + ENTRY + ".getValue()))), ";

    private final Map<String, Set<Element>> allElementsTypesToConvertByAnnotation;

    private final ProcessingEnvironment processingEnv;

    /**
     * CollectionsGenerationHelper constructor
     *
     * @param processingEnv                         {@link ProcessingEnvironment} object, needed to access low-level information regarding the used annotations
     * @param allElementsTypesToConvertByAnnotation {@link Set} of all annotated elements types
     */
    public CollectionsGenerator(ProcessingEnvironment processingEnv, Map<String, Set<Element>> allElementsTypesToConvertByAnnotation) {
        this.processingEnv = processingEnv;
        this.allElementsTypesToConvertByAnnotation = allElementsTypesToConvertByAnnotation;
    }

    @Override
    public void replaceGenericWithRecordClassNameIfAny(StringBuilder recordClassContent, String fieldName, String nonVoidMethodReturnTypeAsString) {
        var typeAndFieldNameDeclarationFormat = "%s %s, "; // type<SPACE>fieldName<COMMA><SPACE>
        if (!hasGeneric(nonVoidMethodReturnTypeAsString)) {
            // no generic found, use return type as is
            recordClassContent.append(format(
                    typeAndFieldNameDeclarationFormat,
                    parentCollectionInterface(processingEnv, nonVoidMethodReturnTypeAsString, this),
                    fieldName));
            removeLastChars(recordClassContent, 2);
            return;
        }
        // generic found, process and replace return type with record class if any
        extractGenericType(nonVoidMethodReturnTypeAsString).ifPresent(genericType -> {
            var parentCollectionType = parentCollectionInterface(processingEnv, nonVoidMethodReturnTypeAsString, this);
            if (isSubtype(processingEnv, extractCollectionType(nonVoidMethodReturnTypeAsString), MAP.qualifiedName())) {
                // only for maps
                var keyValueArray = genericType.split(COMMA); // the key/value entries in a Map genericType
                recordClassContent.append(format(
                        typeAndFieldNameDeclarationFormat,
                        buildReplacementStringForMapGeneric(extractCollectionType(parentCollectionType), keyValueArray[0].strip(), keyValueArray[1].strip()),
                        fieldName
                ));
            } else if (isSubtype(processingEnv, extractCollectionType(nonVoidMethodReturnTypeAsString), LIST.qualifiedName())
                    || isSubtype(processingEnv, extractCollectionType(nonVoidMethodReturnTypeAsString), SET.qualifiedName())) {
                recordClassContent.append(format(
                        typeAndFieldNameDeclarationFormat,
                        buildReplacementStringForListOrSetGeneric(extractCollectionType(parentCollectionType), genericType),
                        fieldName));
            }
            removeLastChars(recordClassContent, 2);
        });
    }

    private String buildReplacementStringForListOrSetGeneric(String collectionType,
                                                             String genericType) {
        var elementFromGenericTypeStringOpt = elementInstanceFromTypeString(processingEnv, genericType);
        return format(
                "%s<%s>",
                collectionType,
                elementFromGenericTypeStringOpt.isPresent() && isTypeAnnotatedAsRecordOrImmutable(processingEnv, allElementsTypesToConvertByAnnotation).test(genericType)
                        ? immutableQualifiedNameBasedOnElementType(elementFromGenericTypeStringOpt.get())
                        : genericType
        );
    }

    private String buildReplacementStringForMapGeneric(String collectionType, String keyType, String valueType) {
        return format("%s<%s, %s>",
                collectionType,
                isTypeAnnotatedAsRecordOrImmutable(processingEnv, allElementsTypesToConvertByAnnotation).test(keyType)
                        ? immutableQualifiedNameBasedOnElementType(elementInstanceValueFromTypeString(processingEnv, keyType))
                        : keyType,
                isTypeAnnotatedAsRecordOrImmutable(processingEnv, allElementsTypesToConvertByAnnotation).test(valueType)
                        ? immutableQualifiedNameBasedOnElementType(elementInstanceValueFromTypeString(processingEnv, valueType))
                        : valueType
        );
    }

    @Override
    public void generateCollectionFieldMappingIfGenericIsAnnotated(StringBuilder recordClassContent,
                                                                   String fieldName,
                                                                   String nonVoidMethodElementAsString,
                                                                   String nonVoidMethodReturnType) {
        var collectionType = extractCollectionType(nonVoidMethodReturnType);
        var genericTypeOpt = extractGenericType(nonVoidMethodReturnType);
        // List
        if (isSubtype(processingEnv, collectionType, LIST.qualifiedName())) {
            generateListFieldMapping(recordClassContent, fieldName, nonVoidMethodElementAsString, genericTypeOpt);
            return;
        }
        // Set
        if (isSubtype(processingEnv, collectionType, SET.qualifiedName())) {
            generateSetFieldMapping(recordClassContent, fieldName, nonVoidMethodElementAsString, genericTypeOpt);
            return;
        }
        // Map
        if (isSubtype(processingEnv, collectionType, MAP.qualifiedName())) {
            // only for maps
            generateMapFieldMapping(recordClassContent, fieldName, nonVoidMethodElementAsString, genericTypeOpt);
            return;
        }
        throw new UnsupportedOperationException(format("%s not supported as a collection type", collectionType));
    }

    private void generateListFieldMapping(StringBuilder recordClassContent, String fieldName, String nonVoidMethodElementAsString, Optional<String> genericTypeOpt) {
        var isTypeAnnotatedAsRecordOrImmutable = genericTypeOpt.isPresent()
                && isTypeAnnotatedAsRecordOrImmutable(processingEnv, allElementsTypesToConvertByAnnotation).test(genericTypeOpt.get());
        var genericTypeImmutableQualifiedNameOpt = genericTypeImmutableQualifiedName(processingEnv, genericTypeOpt);
        recordClassContent.append(format(
                LIST_FIELD_MAPPING_LOGIC_STRING_FORMAT,
                fieldName,
                nonVoidMethodElementAsString,
                fieldName,
                nonVoidMethodElementAsString,
                isTypeAnnotatedAsRecordOrImmutable && genericTypeImmutableQualifiedNameOpt.isPresent() ? NEW + SPACE + genericTypeImmutableQualifiedNameOpt.get() : EMPTY_STRING
        ));
    }

    private void generateSetFieldMapping(StringBuilder recordClassContent, String fieldName, String nonVoidMethodElementAsString, Optional<String> genericTypeOpt) {
        var isTypeAnnotatedAsRecordOrImmutable = genericTypeOpt.isPresent()
                && isTypeAnnotatedAsRecordOrImmutable(processingEnv, allElementsTypesToConvertByAnnotation).test(genericTypeOpt.get());
        var genericTypeImmutableQualifiedNameOpt = genericTypeImmutableQualifiedName(processingEnv, genericTypeOpt);
        recordClassContent.append(format(
                SET_FIELD_MAPPING_LOGIC_STRING_FORMAT,
                fieldName,
                nonVoidMethodElementAsString,
                fieldName,
                nonVoidMethodElementAsString,
                isTypeAnnotatedAsRecordOrImmutable && genericTypeImmutableQualifiedNameOpt.isPresent() ? NEW + SPACE + genericTypeImmutableQualifiedNameOpt.get() : EMPTY_STRING
        ));
    }

    private void generateMapFieldMapping(StringBuilder recordClassContent, String fieldName, String nonVoidMethodElementAsString, Optional<String> genericTypeOpt) {
        var keyValueArrayOpt = genericTypeOpt.map(genericType -> Optional.of(genericType.split(COMMA))).orElse(Optional.empty()); // <the key, value> entries in a Map genericType
        recordClassContent.append(format(
                MAP_FIELD_MAPPING_LOGIC_STRING_FORMAT,
                fieldName,
                nonVoidMethodElementAsString,
                fieldName,
                nonVoidMethodElementAsString,
                keyValueArrayOpt.isPresent() && isTypeAnnotatedAsRecordOrImmutable(processingEnv, allElementsTypesToConvertByAnnotation).test(keyValueArrayOpt.get()[0].strip())
                        ? NEW + SPACE + immutableQualifiedNameBasedOnElementType(elementInstanceValueFromTypeString(processingEnv, keyValueArrayOpt.get()[0].strip()))
                        : EMPTY_STRING,
                keyValueArrayOpt.isPresent() && isTypeAnnotatedAsRecordOrImmutable(processingEnv, allElementsTypesToConvertByAnnotation).test(keyValueArrayOpt.get()[1].strip())
                        ? NEW + SPACE + immutableQualifiedNameBasedOnElementType(elementInstanceValueFromTypeString(processingEnv, keyValueArrayOpt.get()[1].strip()))
                        : EMPTY_STRING
        ));
    }

    @Override
    public void generateCode(StringBuilder recordClassContent, Map<String, Object> params) {
        throw new UnsupportedOperationException("Call replaceGenericWithRecordClassNameIfAny or generateCollectionFieldMappingIfGenericIsAnnotated methods instead");
        // best would be to provide an additional parameter in the params map and based on its value call either replaceGenericWithRecordClassNameIfAny or generateCollectionFieldMappingIfGenericIsAnnotated
    }
}