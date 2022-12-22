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
import java.util.function.Function;
import java.util.function.Predicate;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toMap;

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
     * Array of methods to exclude while pulling the list of all methods of a Pojo or Record class
     */
    String[] METHODS_TO_EXCLUDE = {"getClass", "wait", "notifyAll", "hashCode", "equals", "notify", "toString", "clone", "finalize"};

    /**
     * Default value to use for boolean returned values
     */
    String DEFAULT_BOOLEAN_VALUE = FALSE;

    /**
     * Default value to use for numeric returned values (int, long, float, double,...)
     */
    String DEFAULT_NUMBER_VALUE = "0";

    /**
     * Default value to use for Object returned values
     */
    String DEFAULT_NULL_VALUE = "null";

    /**
     * Generates the requested code fragment, based on the parameters provided in the params object and appends it to the provided recordClassContent param
     *
     * @param recordClassContent {@link StringBuilder} object containing the record class code being generated
     * @param params             expected parameters. restricted to parameters and values expected by the implementing class.
     *                           the expected parameters names are defined as constants in the CodeGenerator interface.
     */
    void generateCode(StringBuilder recordClassContent, Map<String, Object> params);

    /**
     * constructs a {@link Map} containing non-void methods names as keys and their corresponding return types as String values.<br>
     * toString representation ex: {getAge=int, getSchool=org.froporec.data1.School, getLastname=java.lang.String}
     *
     * @param nonVoidMethodsElementsList {@link List} of {@link Element} objects representing non-void methods
     * @return {@link Map} containing non-void methods names as keys and their corresponding return types as String values
     */
    default Map<Element, String> constructNonVoidMethodsElementsReturnTypesMapFromList(List<? extends Element> nonVoidMethodsElementsList) {
        return nonVoidMethodsElementsList.stream().collect(
                toMap(nonVoidMethodElement -> nonVoidMethodElement,
                        nonVoidMethodElement -> ((ExecutableType) nonVoidMethodElement.asType()).getReturnType().toString())
        );
    }

    /**
     * creates an Optional-wrapped {@link Element} instance for the provided qualified name
     *
     * @param processingEnvironment {@link ProcessingEnvironment} object, needed to access low-level information regarding the used annotations
     * @param qualifiedName         qualified name of the provided type
     * @return an {@link Optional} object wrapping the corresponding {@link Element} instance if any
     */
    default Optional<Element> constructElementInstanceFromTypeString(ProcessingEnvironment processingEnvironment, String qualifiedName) {
        return Optional.ofNullable(processingEnvironment.getElementUtils().getTypeElement(qualifiedName)).isEmpty()
                ? Optional.empty()
                : Optional.ofNullable(processingEnvironment.getTypeUtils().asElement(processingEnvironment.getElementUtils().getTypeElement(qualifiedName).asType()));
    }

    /**
     * creates an {@link Element} instance for the provided qualified name
     *
     * @param processingEnvironment {@link ProcessingEnvironment} object, needed to access low-level information regarding the used annotations
     * @param qualifiedName         qualified name of the provided type
     * @return the corresponding {@link Element} instance if the provided type is valid, null if not
     */
    default Element constructElementInstanceValueFromTypeString(ProcessingEnvironment processingEnvironment, String qualifiedName) {
        return processingEnvironment.getTypeUtils().asElement(processingEnvironment.getElementUtils().getTypeElement(qualifiedName).asType());
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
     * @param processingEnvironment                 {@link ProcessingEnvironment} object, needed to access low-level information regarding the used annotations
     * @param allElementsTypesToConvertByAnnotation {@link Map} of annotated {@link Element} instances grouped by their
     *                                              respective annotation String representation
     * @return {@link java.util.function.Predicate} instance to apply on the type String to check
     */
    default Predicate<String> isTypeAnnotatedAsRecordOrImmutable(ProcessingEnvironment processingEnvironment, Map<String, Set<Element>> allElementsTypesToConvertByAnnotation) {
        Function<String, Element> convertToElement = typeString -> constructElementInstanceValueFromTypeString(processingEnvironment, typeString);
        return typeString -> allElementsTypesToConvertByAnnotation.get(ORG_FROPOREC_RECORD).contains(convertToElement.apply(typeString))
                || allElementsTypesToConvertByAnnotation.get(ORG_FROPOREC_IMMUTABLE).contains(convertToElement.apply(typeString));
    }

    /**
     * Returns a {@link java.util.List} of {@link Element} instances representing each non-void non-args methods of the
     * provided annotated {@link Element} instance
     *
     * @param annotatedElement {@link Element} instance of the annotated Pojo or Record class
     * @param processingEnv    {@link ProcessingEnvironment} object, needed to access low-level information regarding the used annotations
     * @return {@link java.util.List} of {@link Element} instances representing each non-void non-args methods of the
     * provided annotated {@link Element} instance
     */
    static List<Element> buildNonVoidMethodsElementsList(Element annotatedElement, ProcessingEnvironment processingEnv) {
        return ElementKind.RECORD.equals((processingEnv.getTypeUtils().asElement(annotatedElement.asType())).getKind())
                ? processingEnv.getElementUtils().getAllMembers(
                        (TypeElement) processingEnv.getTypeUtils().asElement(annotatedElement.asType())
                ).stream()
                .map(Element.class::cast)
                .filter(element -> ElementKind.METHOD.equals(element.getKind()))
                .filter(element -> (!TypeKind.VOID.equals(((ExecutableElement) element).getReturnType().getKind())))
                .filter(element -> stream(METHODS_TO_EXCLUDE).noneMatch(excludedMeth ->
                        element.toString().contains(excludedMeth + OPENING_PARENTHESIS))) // exclude known Object methds
                .filter(element -> ((ExecutableElement) element).getParameters().isEmpty()) // only methods with no params
                .toList()
                : processingEnv.getElementUtils().getAllMembers(
                        (TypeElement) processingEnv.getTypeUtils().asElement(annotatedElement.asType())
                ).stream()
                .map(Element.class::cast)
                .filter(element -> ElementKind.METHOD.equals(element.getKind()))
                .filter(element -> element.getSimpleName().toString().startsWith(GET) || element.getSimpleName().toString().startsWith(IS))
                .filter(element -> stream(METHODS_TO_EXCLUDE).noneMatch(excludedMeth ->
                        element.toString().contains(excludedMeth + OPENING_PARENTHESIS)))
                .filter(element -> ((ExecutableElement) element).getParameters().isEmpty())
                .toList();
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
            case BYTE, SHORT, INT, LONG, FLOAT, DOUBLE, CHAR -> DEFAULT_NUMBER_VALUE;
            default -> DEFAULT_NULL_VALUE;
        };
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

        LIST("List"),
        SET("Set"),
        MAP("Map");

        private final String type;

        SupportedCollectionTypes(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }
    }

    /**
     * Checks whether the provided type is a collection with a generic.<br>
     * The check is simply based on the string representation (qualified name) of the type the check is based on the string representation.<br>
     * First checked is the presence of &lt;&gt; and then whether the qualified name has the String literals "List", "Set" or "Map" in its name
     *
     * @param nonVoidMethodReturnTypeAsString - qualified name of the method's return type
     * @return true if the provided type is a collection with a generic, false otherwise
     */
    default boolean isCollectionWithGeneric(String nonVoidMethodReturnTypeAsString) {
        // TODO chnge the chck here +use sbclss is intceof
        if (nonVoidMethodReturnTypeAsString.indexOf(INFERIOR_SIGN) == -1 && nonVoidMethodReturnTypeAsString.indexOf(SUPERIOR_SIGN) == -1) {
            return false;
        }
        var collectionType = extractCollectionType(nonVoidMethodReturnTypeAsString);
        return collectionType.contains(SupportedCollectionTypes.LIST.type)
                || collectionType.contains(SupportedCollectionTypes.SET.type)
                || collectionType.contains(SupportedCollectionTypes.MAP.type);
    }

    /**
     * extracts the type within the &lt;&gt;
     *
     * @param nonVoidMethodReturnTypeAsString qualified name of the method's return type
     * @return the generic type as a String
     */
    default String extractGenericType(String nonVoidMethodReturnTypeAsString) {
        int idxFirstSign = nonVoidMethodReturnTypeAsString.indexOf(INFERIOR_SIGN);
        int idxLastSign = nonVoidMethodReturnTypeAsString.indexOf(SUPERIOR_SIGN);
        return nonVoidMethodReturnTypeAsString.substring(0, idxLastSign).substring(idxFirstSign + 1);
    }

    /**
     * extracts the collection type without the generic
     *
     * @param nonVoidMethodReturnTypeAsString qualified name of the method's return type
     * @return the collection type
     */
    default String extractCollectionType(String nonVoidMethodReturnTypeAsString) {
        int idxFirstSign = nonVoidMethodReturnTypeAsString.indexOf(INFERIOR_SIGN);
        return nonVoidMethodReturnTypeAsString.substring(0, idxFirstSign);
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
     * @return the string replacing the POJO or Record with its generated record class name. In case it's not a generic no replacement is performed
     */
    void replaceGenericWithRecordClassNameIfAny(final StringBuilder recordClassContent, final String fieldName, final String nonVoidMethodReturnTypeAsString);

    @Override
    default void generateCode(final StringBuilder recordClassContent, final Map<String, Object> params) {
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
     * @return return the mapping logic for the collection field being processed
     */
    void generateCollectionFieldMappingIfGenericIsAnnotated(final StringBuilder recordClassContent,
                                                            final String fieldName,
                                                            final String nonVoidMethodElementAsString,
                                                            final String nonVoidMethodReturnTypeAsString);

    @Override
    default void generateCode(final StringBuilder recordClassContent, final Map<String, Object> params) {
        var fieldName = (String) params.get(FIELD_NAME);
        var nonVoidMethodElementAsString = (String) params.get(NON_VOID_METHOD_AS_STRING);
        var nonVoidMethodReturnTypeAsString = (String) params.get(NON_VOID_METHOD_RETURN_TYPE_AS_STRING);
        generateCollectionFieldMappingIfGenericIsAnnotated(recordClassContent, fieldName, nonVoidMethodElementAsString, nonVoidMethodReturnTypeAsString);
    }
}