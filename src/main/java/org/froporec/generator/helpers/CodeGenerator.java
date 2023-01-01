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
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toMap;
import static org.froporec.generator.helpers.SupportedCollectionsGenerator.SupportedCollectionTypes;
import static org.froporec.generator.helpers.SupportedCollectionsGenerator.SupportedCollectionTypes.LIST;
import static org.froporec.generator.helpers.SupportedCollectionsGenerator.SupportedCollectionTypes.MAP;
import static org.froporec.generator.helpers.SupportedCollectionsGenerator.SupportedCollectionTypes.SET;

/**
 * Exposes contract for a CodeGenerator class to fulfill
 */
public sealed interface CodeGenerator extends StringGenerator permits CustomConstructorGenerator, FieldsGenerator, FieldsNamesConstantsGenerator,
        FactoryMethodsGenerator, JavaxGeneratedGenerator, SuperInterfacesGenerator, SupportedCollectionsGenerator {

    // List of the parameters expected in the params Map object of the generateCode method:

    /**
     * parameter name: "annotatedElement", expected type: {@link Element}
     */
    String ANNOTATED_ELEMENT = "annotatedElement";

    /**
     * parameter name: "fieldName", expected type: {@link String}
     */
    String FIELD_NAME = "fieldName";

    /**
     * parameter name: "nonVoidMethodReturnTypeAsString", expected type: {@link String}, return type of the method being processed
     */
    String NON_VOID_METHOD_RETURN_TYPE_AS_STRING = "nonVoidMethodReturnTypeAsString";

    /**
     * parameter name: "nonVoidMethodAsString", expected type: {@link String}, name of the method being processed
     */
    String NON_VOID_METHOD_AS_STRING = "nonVoidMethodAsString";

    /**
     * parameter name: "nonVoidMethodsElementsList", expected type: {@link List}&lt;? extends {@link Element}&gt;.<br>
     * toString representation ex: [getLastname(), getAge(), getMark(), getGrade(), getSchool()]
     */
    String NON_VOID_METHODS_ELEMENTS_LIST = "nonVoidMethodsElementsList";

    /**
     * parameter name: "isSuperRecord", expected type: {@link Boolean}, indicates whether the Element is being processed as a SuperRecord
     */
    String IS_SUPER_RECORD = "isSuperRecord";

    /**
     * Generates the requested code fragment, based on the parameters provided in the params object and appends it to the provided recordClassContent param
     *
     * @param recordClassContent {@link StringBuilder} object containing the record class code being generated
     * @param params             expected parameters. restricted to parameters and values expected by the implementing class.
     *                           the expected parameters names are defined as constants in the CodeGenerator interface.
     */
    void generateCode(StringBuilder recordClassContent, Map<String, Object> params);

    /**
     * Returns a {@link java.util.List} of {@link Element} instances representing each non-void non-args methods of the
     * provided annotated {@link Element} instance
     *
     * @param annotatedElement {@link Element} instance of the annotated Pojo or Record class
     * @param processingEnv    {@link ProcessingEnvironment} object, needed to access low-level information regarding the used annotations
     * @return {@link java.util.List} of {@link Element} instances representing each non-void non-args methods of the
     * provided annotated {@link Element} instance
     */
    static List<Element> nonVoidMethodsElementsList(Element annotatedElement, ProcessingEnvironment processingEnv) {
        // Array of methods to exclude while pulling the list of all methods of a Pojo or Record class
        String[] methodsToExclude = {"getClass", "wait", "notifyAll", "hashCode", "equals", "notify", "toString", "clone", "finalize"};
        return ElementKind.RECORD.equals((processingEnv.getTypeUtils().asElement(annotatedElement.asType())).getKind())
                ? processingEnv.getElementUtils().getAllMembers(
                        (TypeElement) processingEnv.getTypeUtils().asElement(annotatedElement.asType())
                ).stream()
                .map(Element.class::cast)
                .filter(element -> ElementKind.METHOD.equals(element.getKind()))
                .filter(element -> (!TypeKind.VOID.equals(((ExecutableElement) element).getReturnType().getKind())))
                .filter(element -> stream(methodsToExclude).noneMatch(excludedMeth ->
                        element.toString().contains(excludedMeth + OPENING_PARENTHESIS))) // exclude known Object methds
                .filter(element -> ((ExecutableElement) element).getParameters().isEmpty()) // only methods with no params
                .toList()
                : processingEnv.getElementUtils().getAllMembers(
                        (TypeElement) processingEnv.getTypeUtils().asElement(annotatedElement.asType())
                ).stream()
                .map(Element.class::cast)
                .filter(element -> ElementKind.METHOD.equals(element.getKind()))
                .filter(element -> element.getSimpleName().toString().startsWith(GET) || element.getSimpleName().toString().startsWith(IS))
                .filter(element -> stream(methodsToExclude).noneMatch(excludedMeth ->
                        element.toString().contains(excludedMeth + OPENING_PARENTHESIS)))
                .filter(element -> ((ExecutableElement) element).getParameters().isEmpty())
                .toList();
    }

    /**
     * Constructs the qualified name of the fully immutable record class being generated from an annotated Record class
     *
     * @param qualifiedClassName qualified name of the annotated class
     * @return the qualified name of the fully immutable record class being generated from an annotated Record class. ex: "Immutable"+RecordClassName
     */
    static String immutableRecordQualifiedName(String qualifiedClassName) {
        return qualifiedClassName.contains(DOT)
                ? qualifiedClassName.substring(0, qualifiedClassName.lastIndexOf(DOT)) + DOT + IMMUTABLE + qualifiedClassName.substring(qualifiedClassName.lastIndexOf(DOT) + 1)
                : IMMUTABLE + qualifiedClassName;
    }

    /**
     * Constructs the qualified name of the generated record class. Handles both cases of the class being processed as either a Pojo or a Record
     *
     * @param element {@link Element} instance of the annotated class
     * @return the qualified name of the fully immutable record class being generated from an annotated Pojo or Record class.<br>
     * ex: ..."Immutable" + RecordClassName or ...PojoClassName+"Record"
     */
    static String immutableQualifiedNameBasedOnElementType(Element element) {
        return ElementKind.RECORD.equals(element.getKind())
                ? immutableRecordQualifiedName(element.toString())
                : element + RECORD;
    }

    /**
     * Constructs the simple name of the generated record class. Handles both cases of the class being processed as either a Pojo or a Record
     *
     * @param element {@link Element} instance of the annotated class
     * @return the simple name of the fully immutable record class being generated from an annotated Pojo or Record class.<br>
     * ex: "Immutable" + RecordClassName or PojoClassName+"Record"
     */
    static String immutableSimpleNameBasedOnElementType(Element element) {
        var immutableQualifiedName = immutableQualifiedNameBasedOnElementType(element);
        return immutableQualifiedName.substring(immutableQualifiedName.lastIndexOf(DOT) + 1);
    }

    /**
     * Constructs the qualified name of the generated super record class.
     * Based on whether or not the qualified name of the passed in element ends with 'Record'
     *
     * @param element {@link Element} instance of the annotated class
     * @return the qualified name of the fully immutable super record class being generated from an annotated Pojo or Record class.<br>
     * ex: ...RecordClassName+"SuperRecord" or ...PojoClassName+"SuperRecord"
     */
    static String superRecordQualifiedNameBasedOnElementType(Element element) {
        var qualifiedName = element.toString();
        return qualifiedName.endsWith(RECORD)
                ? qualifiedName.substring(0, qualifiedName.lastIndexOf(RECORD)) + SUPER_RECORD
                : qualifiedName + SUPER_RECORD;
    }

    /**
     * Constructs the simple name of the generated super record class.
     * Based on whether or not the simple name of the passed in element ends with 'Record'
     *
     * @param element {@link Element} instance of the annotated class
     * @return the qualified name of the fully immutable super record class being generated from an annotated Pojo or Record class.<br>
     * ex: ...RecordClassName+"SuperRecord" or ...PojoClassName+"SuperRecord"
     */
    static String superRecordSimpleNameBasedOnElementType(Element element) {
        var simpleName = element.getSimpleName().toString();
        return simpleName.endsWith(RECORD)
                ? simpleName.substring(0, simpleName.lastIndexOf(RECORD)) + SUPER_RECORD
                : simpleName + SUPER_RECORD;
    }

    /**
     * Constructs the field name based on the {@link Element} instance representing the Pojo or Record's accessor method
     *
     * @param nonVoidMethodElement {@link Element} instance representing the Pojo or Record's accessor method
     * @return the field name as used in the Pojo or Record definition
     */
    static Optional<String> fieldName(Element nonVoidMethodElement) {
        return fieldName(nonVoidMethodElement, ElementKind.RECORD.equals(nonVoidMethodElement.getEnclosingElement().getKind()));
    }

    /**
     * Constructs the field name based on the {@link Element} instance representing the Pojo or Record's accessor method
     *
     * @param nonVoidMethodElement     {@link Element} instance representing the Pojo or Record's accessor method
     * @param enclosingElementIsRecord indicates whether the enclosing Element of the provided nonVoidMethodElement is a Record
     * @return the field name as used in the Pojo or Record definition
     */
    static Optional<String> fieldName(Element nonVoidMethodElement, boolean enclosingElementIsRecord) {
        if (enclosingElementIsRecord) {
            // Record class, handle all non-void methods
            var nonVoidMethodElementAsString = nonVoidMethodElement.toString();
            return Optional.of(nonVoidMethodElementAsString.substring(0, nonVoidMethodElementAsString.indexOf(OPENING_PARENTHESIS)));
        } else {
            // POJO class, handle only getters (only methods starting with get or is)
            var getterAsString = nonVoidMethodElement.toString();
            if (getterAsString.startsWith(GET)) {
                return Optional.of(getterAsString.substring(3, 4).toLowerCase() + getterAsString.substring(4, getterAsString.indexOf(OPENING_PARENTHESIS)));
            } else if (getterAsString.startsWith(IS)) {
                return Optional.of(getterAsString.substring(2, 3).toLowerCase() + getterAsString.substring(3, getterAsString.indexOf(OPENING_PARENTHESIS)));
            }
        }
        return Optional.empty();
    }

    /**
     * constructs a {@link Map} containing non-void methods names as keys and their corresponding return types as String values.<br>
     * toString representation ex: {getAge=int, getSchool=org.froporec.data1.School, getLastname=java.lang.String}
     *
     * @param nonVoidMethodsElementsList {@link List} of {@link Element} objects representing non-void methods
     * @return {@link Map} containing non-void methods names as keys and their corresponding return types as String values
     */
    default Map<Element, String> nonVoidMethodsElementsReturnTypesMapFromList(List<? extends Element> nonVoidMethodsElementsList) {
        return nonVoidMethodsElementsList.stream().collect(
                toMap(nonVoidMethodElement -> nonVoidMethodElement,
                        nonVoidMethodElement -> ((ExecutableType) nonVoidMethodElement.asType()).getReturnType().toString())
        );
    }

    /**
     * creates an Optional-wrapped {@link Element} instance for the provided qualified name
     *
     * @param processingEnv {@link ProcessingEnvironment} object, needed to access low-level information regarding the used annotations
     * @param qualifiedName qualified name of the provided type
     * @return an {@link Optional} object wrapping the corresponding {@link Element} instance if any
     */
    default Optional<Element> elementInstanceFromTypeString(ProcessingEnvironment processingEnv, String qualifiedName) {
        return Optional.ofNullable(processingEnv.getElementUtils().getTypeElement(qualifiedName)).isEmpty()
                ? Optional.empty()
                : Optional.ofNullable(processingEnv.getTypeUtils().asElement(processingEnv.getElementUtils().getTypeElement(qualifiedName).asType()));
    }

    /**
     * creates an {@link Element} instance for the provided qualified name
     *
     * @param processingEnv {@link ProcessingEnvironment} object, needed to access low-level information regarding the used annotations
     * @param qualifiedName qualified name of the provided type
     * @return the corresponding {@link Element} instance if the provided type is valid, null if not
     */
    default Element elementInstanceValueFromTypeString(ProcessingEnvironment processingEnv, String qualifiedName) {
        return processingEnv.getTypeUtils().asElement(processingEnv.getElementUtils().getTypeElement(qualifiedName).asType());
    }

    /**
     * Checks whether the provided {@link Element} instance is annotated with either &#64;{@link org.froporec.annotations.Record}
     * or &#64;{@link org.froporec.annotations.Immutable}
     *
     * @param allElementsTypesToConvertByAnnotation {@link Map} of annotated {@link Element} instances grouped by their
     *                                              respective annotation String representation
     * @return {@link java.util.function.Predicate} instance to apply on the {@link Element} instance to check
     */
    default Predicate<Element> isElementAnnotatedAsRecordOrImmutable(Map<String, Set<Element>> allElementsTypesToConvertByAnnotation) {
        return element -> allElementsTypesToConvertByAnnotation.get(ORG_FROPOREC_RECORD).contains(element)
                || allElementsTypesToConvertByAnnotation.get(ORG_FROPOREC_IMMUTABLE).contains(element);
    }

    /**
     * Checks whether the provided type String is annotated with either &#64;{@link org.froporec.annotations.Record}
     * or &#64;{@link org.froporec.annotations.Immutable}
     *
     * @param processingEnv                         {@link ProcessingEnvironment} object, needed to access low-level information
     *                                              regarding the used annotations
     * @param allElementsTypesToConvertByAnnotation {@link Map} of annotated {@link Element} instances grouped by their
     *                                              respective annotation String representation
     * @return {@link java.util.function.Predicate} instance to apply on the type String to check
     */
    default Predicate<String> isTypeAnnotatedAsRecordOrImmutable(ProcessingEnvironment processingEnv, Map<String, Set<Element>> allElementsTypesToConvertByAnnotation) {
        Function<String, Optional<Element>> convertToElement = typeString -> elementInstanceFromTypeString(processingEnv, typeString);
        return typeString -> convertToElement.apply(typeString).isPresent()
                && (allElementsTypesToConvertByAnnotation.get(ORG_FROPOREC_RECORD).contains(convertToElement.apply(typeString).get())
                || allElementsTypesToConvertByAnnotation.get(ORG_FROPOREC_IMMUTABLE).contains(convertToElement.apply(typeString).get()));
    }

    /**
     * Constructs a string containing the default value for the provided method's return type
     *
     * @param methodElement method {@link Element} instance
     * @return a string containing the default value for the provided method's return type
     */
    default String defaultReturnValueForMethod(Element methodElement) {
        return switch (((ExecutableType) methodElement.asType()).getReturnType().getKind()) {
            case BOOLEAN -> DEFAULT_BOOLEAN_VALUE;
            case VOID -> EMPTY_STRING;
            case LONG -> DEFAULT_LONG_VALUE;
            case FLOAT -> DEFAULT_FLOAT_VALUE;
            case DOUBLE -> DEFAULT_DOUBLE_VALUE;
            case BYTE, SHORT, INT, CHAR -> DEFAULT_NUMBER_VALUE;
            default -> DEFAULT_NULL_VALUE;
        };
    }

    /**
     * Constructs a {@link Map} containing a field name as key and field type as value.<br>
     * The field name is based on the provided {@link Element} instance representing the Pojo or Record's accessor method<br>
     * The field type is constructed considering whether the type was also annotated with either Record or Immutable
     *
     * @param nonVoidMethodElement                  {@link Element} instance representing the Pojo or Record's accessor method
     * @param nonVoidMethodsElementsReturnTypesMap  {@link Map} containing non-void methods names as keys and their corresponding
     *                                              return types as String values
     * @param allElementsTypesToConvertByAnnotation {@link Map} of annotated {@link Element} instances grouped by their
     *                                              respective annotation String representation
     * @param processingEnv                         {@link ProcessingEnvironment} object, needed to access low-level information
     *                                              regarding the used annotations
     * @param collectionsGenerator                  {@link SupportedCollectionsFieldsGenerator} instance needed to process collections
     * @return {@link Map} with the key being the field name and the value being the type of the field after conversion
     */
    default Map<String, String> fieldNameAndTypePair(Element nonVoidMethodElement,
                                                     Map<Element, String> nonVoidMethodsElementsReturnTypesMap,
                                                     Map<String, Set<Element>> allElementsTypesToConvertByAnnotation,
                                                     ProcessingEnvironment processingEnv,
                                                     SupportedCollectionsFieldsGenerator collectionsGenerator) {
        var nonVoidMethodReturnTypeAsString = nonVoidMethodsElementsReturnTypesMap.get(nonVoidMethodElement);
        var nonVoidMethodReturnTypeElementOpt = Optional.ofNullable(
                processingEnv.getTypeUtils().asElement(((ExecutableType) nonVoidMethodElement.asType()).getReturnType())
        );
        var fieldType = new StringBuilder();
        // Consumer to run in case of non-primitives i.e nonVoidMethodReturnTypeElementOpt.isPresent()
        Consumer<Element> consumer = nonVoidMethodReturnTypeElement ->
                buildFieldType(fieldType, nonVoidMethodElement, nonVoidMethodReturnTypeAsString,
                        isElementAnnotatedAsRecordOrImmutable(allElementsTypesToConvertByAnnotation).test(nonVoidMethodReturnTypeElement),
                        processingEnv, collectionsGenerator);
        // Runnable to execute in case of primitives i.e nonVoidMethodReturnTypeElementOpt.isEmpty()
        Runnable runnable = () -> buildFieldType(fieldType, nonVoidMethodElement, nonVoidMethodReturnTypeAsString, false, processingEnv, collectionsGenerator);
        nonVoidMethodReturnTypeElementOpt.ifPresentOrElse(consumer, runnable);
        return Map.of(fieldName(nonVoidMethodElement).orElseThrow(), fieldType.toString());
    }

    private void buildFieldType(StringBuilder fieldType,
                                Element nonVoidMethodElement,
                                String nonVoidMethodReturnTypeAsString,
                                boolean processAsImmutable,
                                ProcessingEnvironment processingEnv,
                                SupportedCollectionsFieldsGenerator collectionsGenerator) {
        fieldName(nonVoidMethodElement).ifPresent(fieldName -> {
            // if the type of the field being processed is a collection process it differently and return
            if (collectionsGenerator.isCollection(nonVoidMethodReturnTypeAsString, processingEnv)) {
                var typeAndNameSpaceSeparated = new StringBuilder();
                collectionsGenerator.replaceGenericWithRecordClassNameIfAny(typeAndNameSpaceSeparated, fieldName, nonVoidMethodReturnTypeAsString);
                fieldType.append(typeAndNameSpaceSeparated.substring(0, typeAndNameSpaceSeparated.lastIndexOf(SPACE)));
                return;
            }
            fieldType.append(
                    processAsImmutable
                            ? immutableQualifiedNameBasedOnElementType(elementInstanceValueFromTypeString(processingEnv, nonVoidMethodReturnTypeAsString))
                            : nonVoidMethodReturnTypeAsString
            );
        });
    }

    /**
     * Constructs the complete definition of a factory method including the signature and body
     *
     * @param methodReturnType               return type to be used in the generated method signature
     * @param methodParams                   String containing the method parameters ('type name,...' comma-separated list)
     *                                       just as seen in a typical java method signature
     * @param canonicalConstructorCallParams String containing the comma-separated values to pass to the call to the canonical constructor. ex:<br>
     *                                       return new ImmutableExamReport(examReport.candidateId(), examReport.fullName(), examReport.contactInfo());
     *                                       // here the value of canonicalConstructorCallParams is:
     *                                       'examReport.candidateId(), examReport.fullName(), examReport.contactInfo()'
     * @param isStatic                       indicates whether the method being generated is static or not
     * @return String containing the complete definition of a factory method including the signature and body
     */
    default String factoryMethodDeclarationAndBody(String methodReturnType, String methodParams, String canonicalConstructorCallParams, boolean isStatic) {
        return factoryMethodDeclarationAndBody(methodReturnType, methodParams, canonicalConstructorCallParams, isStatic, false);
    }

    /**
     * Constructs the complete definition of a factory method including the signature and body
     *
     * @param methodReturnType               return type to be used in the generated method signature
     * @param methodParams                   String containing the method parameters ('type name,...' comma-separated list)
     *                                       just as seen in a typical java method signature
     * @param canonicalConstructorCallParams String containing the comma-separated values to pass to the call to the canonical constructor. ex:<br>
     *                                       return new ImmutableExamReport(examReport.candidateId(), examReport.fullName(), examReport.contactInfo());
     *                                       // here the value of canonicalConstructorCallParams is:
     *                                       'examReport.candidateId(), examReport.fullName(), examReport.contactInfo()'
     * @param isStatic                       indicates whether the method being generated is static or not
     * @param hasGeneric                     indicates whether the method being generated has a generic type
     * @return String containing the complete definition of a factory method including the signature and body
     */
    default String factoryMethodDeclarationAndBody(String methodReturnType, String methodParams, String canonicalConstructorCallParams, boolean isStatic, boolean hasGeneric) {
        var methodCode = new StringBuilder();
        // "public (static) (<T>) <methodReturnType><SPACE>"
        methodCode.append(TAB + PUBLIC + SPACE + (isStatic ? STATIC + SPACE : EMPTY_STRING) + (hasGeneric ? GENERIC_T_SYMB + SPACE : EMPTY_STRING) + methodReturnType + SPACE);
        // "buildWith(<AnnotatedPojoOrRecordQualifiedName><SPACE><AnnotatedPojoOrRecordSimpleName>)"
        // "buildWith(<GeneratedRecordQualifiedName><SPACE><GeneratedRecordSimpleName>)"
        // "buildWith(java.util.Map<String, Object><SPACE>fieldsNameValuePairs)"
        // "buildWith(<AnnotatedPojoOrRecordQualifiedName><SPACE><AnnotatedPojoOrRecordSimpleName>,<SPACE>java.util.Map<String, Object><SPACE>fieldsNameValuePairs)"
        // "buildWith(<GeneratedRecordQualifiedName><SPACE><GeneratedRecordSimpleName>,<SPACE>java.util.Map<String, Object><SPACE>fieldsNameValuePairs)"
        // "with(java.util.Map<String, Object><SPACE>fieldsNameValuePairs)"
        // "with(String fieldName, T fieldValue)"
        methodCode.append((isStatic ? BUILD_WITH : WITH) + OPENING_PARENTHESIS + methodParams + CLOSING_PARENTHESIS + SPACE + OPENING_BRACE + NEW_LINE + TAB + TAB);
        // method body
        methodCode.append(RETURN + SPACE + NEW + SPACE + methodReturnType + OPENING_PARENTHESIS + canonicalConstructorCallParams + CLOSING_PARENTHESIS + SEMI_COLON + NEW_LINE);
        // closing
        methodCode.append(TAB + CLOSING_BRACE + NEW_LINE);
        return methodCode.toString();
    }

    /**
     * Checks whether the provided type1QualifiedName is a subtype of the provided type2QualifiedName
     *
     * @param processingEnv      {@link ProcessingEnvironment} object, needed to access low-level information
     *                           regarding the used annotations
     * @param type1QualifiedName qualified name of the provided type
     * @param type2QualifiedName qualified name of the provided type
     * @return true if the provided type1QualifiedName is a subtype of the provided type2QualifiedName
     */
    default boolean isSubtype(ProcessingEnvironment processingEnv, String type1QualifiedName, String type2QualifiedName) {
        var type1TypeElement = processingEnv.getElementUtils().getTypeElement(type1QualifiedName);
        var type2TypeElement = processingEnv.getElementUtils().getTypeElement(type2QualifiedName);
        if (type1TypeElement == null || type2TypeElement == null) {
            return false;
        }
        var typeUtils = processingEnv.getTypeUtils();
        return typeUtils.isSubtype(typeUtils.getDeclaredType(type1TypeElement), typeUtils.getDeclaredType(type2TypeElement));
    }

    /**
     * Constructs the qualified name of the parent interface of the provided {@link java.util.Collection} type name.<br>
     * ex: providing '{@link java.util.HashSet}' would return {@link Set}, '{@link java.util.ArrayList}' would return '{@link List }'
     *
     * @param processingEnv               {@link ProcessingEnvironment} object, needed to access low-level information
     *                                    regarding the used annotations
     * @param collectionTypeQualifiedName provided collection type name
     * @param collectionsGenerator        {@link SupportedCollectionsFieldsGenerator} instance needed to process collections
     * @return the qualified name of the parent interface of the provided Collection type name
     */
    default String parentCollectionInterface(ProcessingEnvironment processingEnv, String collectionTypeQualifiedName, SupportedCollectionsGenerator collectionsGenerator) {
        BiFunction<String, SupportedCollectionTypes, String> collectionTypeWithGenericIfAny = (collectionType, supportedCollectionType) ->
                supportedCollectionType.qualifiedName() +
                        (collectionsGenerator.hasGeneric(collectionType)
                                ? INFERIOR_SIGN + collectionsGenerator.extractGenericType(collectionType).get() + SUPERIOR_SIGN
                                : EMPTY_STRING);
        if (isSubtype(processingEnv, collectionsGenerator.extractCollectionType(collectionTypeQualifiedName), LIST.qualifiedName())) {
            return collectionTypeWithGenericIfAny.apply(collectionTypeQualifiedName, LIST);
        }
        if (isSubtype(processingEnv, collectionsGenerator.extractCollectionType(collectionTypeQualifiedName), SET.qualifiedName())) {
            return collectionTypeWithGenericIfAny.apply(collectionTypeQualifiedName, SET);
        }
        if (isSubtype(processingEnv, collectionsGenerator.extractCollectionType(collectionTypeQualifiedName), MAP.qualifiedName())) {
            return collectionTypeWithGenericIfAny.apply(collectionTypeQualifiedName, MAP);
        }
        return collectionTypeQualifiedName;
    }

    /**
     * Constructs the qualified name of the generated record class for the provided generic type if exists
     *
     * @param processingEnv  {@link ProcessingEnvironment} object, needed to access low-level information
     *                       regarding the used annotations
     * @param genericTypeOpt wraps a String containing the qualified name of a Collection generic type
     * @return the qualified name of the generated record class for the provided generic type if exists
     */
    default Optional<String> genericTypeImmutableQualifiedName(ProcessingEnvironment processingEnv, Optional<String> genericTypeOpt) {
        if (genericTypeOpt.isPresent()) {
            return elementInstanceFromTypeString(processingEnv, genericTypeOpt.get())
                    .map(element -> Optional.of(immutableQualifiedNameBasedOnElementType(element)))
                    .orElse(Optional.empty());
        }
        return Optional.empty();
    }
}

/**
 * "Intermediate" interface providing an enum and a bunch of functions needed by classes handling code generation for collections fields instances
 */
sealed interface SupportedCollectionsGenerator extends CodeGenerator {

    /**
     * Supported collections types, stored as a list of String values
     */
    enum SupportedCollectionTypes {

        LIST(java.util.List.class),
        SET(java.util.Set.class),
        MAP(java.util.Map.class);

        private final Class<?> type;

        SupportedCollectionTypes(Class<?> type) {
            this.type = type;
        }

        /**
         * Qualified name of the Collection type
         *
         * @return the qualified name of the Collection type
         */
        public String qualifiedName() {
            return type.getName();
        }
    }

    /**
     * Checks whether the provided type's qualified name includes a generic type
     *
     * @param nonVoidMethodReturnTypeAsString qualified name of the method's return type
     * @return true if the provided type's qualified name includes a generic type
     */
    default boolean hasGeneric(String nonVoidMethodReturnTypeAsString) {
        return nonVoidMethodReturnTypeAsString.indexOf(INFERIOR_SIGN) > -1 && nonVoidMethodReturnTypeAsString.indexOf(SUPERIOR_SIGN) > -1;
    }

    /**
     * Checks whether the provided type is a collection with a generic.<br>
     * The check is simply based on the string representation (qualified name) of the type the check is based on the string representation.<br>
     * First checked is the presence of &lt;&gt; and then whether the qualified name has the String literals "List", "Set" or "Map" in its name
     *
     * @param nonVoidMethodReturnTypeAsString - qualified name of the method's return type
     * @param processingEnv                   {@link ProcessingEnvironment} object, needed to access low-level information
     *                                        regarding the used annotations
     * @return true if the provided type is a collection with a generic, false otherwise
     */
    default boolean isCollection(String nonVoidMethodReturnTypeAsString, ProcessingEnvironment processingEnv) {
        var collectionType = extractCollectionType(nonVoidMethodReturnTypeAsString);
        return isSubtype(processingEnv, collectionType, LIST.qualifiedName())
                || isSubtype(processingEnv, collectionType, SET.qualifiedName())
                || isSubtype(processingEnv, collectionType, MAP.qualifiedName());
    }

    /**
     * extracts the type within the &lt;&gt;
     *
     * @param nonVoidMethodReturnTypeAsString qualified name of the method's return type
     * @return the generic type as a String
     */
    default Optional<String> extractGenericType(String nonVoidMethodReturnTypeAsString) {
        if (!hasGeneric(nonVoidMethodReturnTypeAsString)) {
            return Optional.empty();
        }
        int idxFirstSign = nonVoidMethodReturnTypeAsString.indexOf(INFERIOR_SIGN);
        int idxLastSign = nonVoidMethodReturnTypeAsString.indexOf(SUPERIOR_SIGN);
        return Optional.of(nonVoidMethodReturnTypeAsString.substring(0, idxLastSign).substring(idxFirstSign + 1));
    }

    /**
     * extracts the collection type without the generic
     *
     * @param nonVoidMethodReturnTypeAsString qualified name of the method's return type
     * @return the collection type
     */
    default String extractCollectionType(String nonVoidMethodReturnTypeAsString) {
        if (hasGeneric(nonVoidMethodReturnTypeAsString)) {
            int idxFirstSign = nonVoidMethodReturnTypeAsString.indexOf(INFERIOR_SIGN);
            return nonVoidMethodReturnTypeAsString.substring(0, idxFirstSign);
        }
        return nonVoidMethodReturnTypeAsString;
    }
}

/**
 * Exposes contract to be fulfilled by any class dedicated to replacing every POJO or Record class within a generic with its generated record class name
 */
sealed interface SupportedCollectionsFieldsGenerator extends SupportedCollectionsGenerator permits CollectionsGenerator {

    /**
     * Replaces every POJO or Record class within a generic with its generated record class name, only if the POJO or Record within
     * the generic was also annotated or added as a .class value within the "alsoConvert" attribute of {@link org.froporec.annotations.Record} or {@link org.froporec.annotations.Immutable}).<br>
     * ex: if List&lt;Person&gt; is a member of an annotated POJO class, the generated record class of the POJO will have a member of List&lt;PersonRecord&gt;
     *
     * @param recordClassContent              content being built, containing the record source string
     * @param fieldName                       record field being processed
     * @param nonVoidMethodReturnTypeAsString qualified name of the method's return type
     */
    void replaceGenericWithRecordClassNameIfAny(StringBuilder recordClassContent, String fieldName, String nonVoidMethodReturnTypeAsString);

    @Override
    default void generateCode(StringBuilder recordClassContent, Map<String, Object> params) {
        var fieldName = (String) params.get(FIELD_NAME);
        var nonVoidMethodReturnTypeAsString = (String) params.get(NON_VOID_METHOD_RETURN_TYPE_AS_STRING);
        replaceGenericWithRecordClassNameIfAny(recordClassContent, fieldName, nonVoidMethodReturnTypeAsString);
    }
}

/**
 * Exposes contract to be fulfilled by any class dedicated to building a Collection.stream()... logic to allow the mapping of a
 * collection of POJO or Record classes to a collection of the corresponding generated Record classes
 */
sealed interface SupportedCollectionsMappingLogicGenerator extends SupportedCollectionsGenerator permits CollectionsGenerator {

    /**
     * Builds a Collection.stream()... logic to allow the mapping of a collection of POJO or Record classes to a collection of the
     * corresponding generated Record classes, only if the POJOs or Records defined as generics were also annotated or
     * added as .class values within the "alsoConvert" attribute of {@link org.froporec.annotations.Record} or {@link org.froporec.annotations.Immutable}).<br>
     * This happens inside the generated custom constructor inside which we call the canonical constructor of the Record class being generated
     *
     * @param recordClassContent              content being built, containing the record source string
     * @param fieldName                       field name being processed
     * @param nonVoidMethodElementAsString    non-void method name
     * @param nonVoidMethodReturnTypeAsString qualified name of the method's return type, also the type of the field being processed
     */
    void generateCollectionFieldMappingIfGenericIsAnnotated(StringBuilder recordClassContent,
                                                            String fieldName,
                                                            String nonVoidMethodElementAsString,
                                                            String nonVoidMethodReturnTypeAsString);

    @Override
    default void generateCode(StringBuilder recordClassContent, Map<String, Object> params) {
        var fieldName = (String) params.get(FIELD_NAME);
        var nonVoidMethodElementAsString = (String) params.get(NON_VOID_METHOD_AS_STRING);
        var nonVoidMethodReturnTypeAsString = (String) params.get(NON_VOID_METHOD_RETURN_TYPE_AS_STRING);
        generateCollectionFieldMappingIfGenericIsAnnotated(recordClassContent, fieldName, nonVoidMethodElementAsString, nonVoidMethodReturnTypeAsString);
    }
}