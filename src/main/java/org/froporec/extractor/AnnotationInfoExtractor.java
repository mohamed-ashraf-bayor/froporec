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
package org.froporec.extractor;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toSet;
import static org.froporec.generator.helpers.StringGenerator.ALSO_CONVERT_ATTRIBUTE;
import static org.froporec.generator.helpers.StringGenerator.COMMA_SEPARATOR;
import static org.froporec.generator.helpers.StringGenerator.DOT_CLASS;
import static org.froporec.generator.helpers.StringGenerator.EMPTY_STRING;
import static org.froporec.generator.helpers.StringGenerator.INCLUDE_TYPES_ATTRIBUTE;
import static org.froporec.generator.helpers.StringGenerator.MERGE_WITH_ATTRIBUTE;
import static org.froporec.generator.helpers.StringGenerator.ORG_FROPOREC_GENERATE_IMMUTABLE;
import static org.froporec.generator.helpers.StringGenerator.ORG_FROPOREC_GENERATE_RECORD;
import static org.froporec.generator.helpers.StringGenerator.ORG_FROPOREC_IMMUTABLE;
import static org.froporec.generator.helpers.StringGenerator.ORG_FROPOREC_RECORD;
import static org.froporec.generator.helpers.StringGenerator.ORG_FROPOREC_SUPER_RECORD;
import static org.froporec.generator.helpers.StringGenerator.SUPER_INTERFACES_ATTRIBUTE;

/**
 * Provides a bunch of methods allowing to extract different types of info from the annotated POJO or Record classes
 */
@FunctionalInterface
public interface AnnotationInfoExtractor {

    /**
     * Used to identify an annotated element as a POJO class
     */
    Predicate<Element> POJO_CLASSES_INFO_EXTRACTOR_PREDICATE = element ->
            !element.getClass().isRecord()
                    && !element.getClass().isEnum()
                    && !ElementKind.INTERFACE.equals(element.getKind())
                    && ElementKind.CLASS.equals(element.getKind());

    /**
     * Used to identify an annotated element as a Record class
     */
    Predicate<Element> RECORD_CLASSES_INFO_EXTRACTOR_PREDICATE = element -> ElementKind.RECORD.equals(element.getKind());

    /**
     * Used to identify an annotated element as a POJO class field or Record class component
     */
    Predicate<Element> FIELDS_INFO_EXTRACTOR_PREDICATE = element ->
            !ElementKind.ENUM_CONSTANT.equals(element.getKind())
                    && (ElementKind.FIELD.equals(element.getKind()) || ElementKind.RECORD_COMPONENT.equals(element.getKind()));

    /**
     * Used to identify an annotated element as a method parameter
     */
    Predicate<Element> METHOD_PARAMS_INFO_EXTRACTOR_PREDICATE = element -> ElementKind.PARAMETER.equals(element.getKind());

    /**
     * Extract info based on provided Predicate
     *
     * @param allAnnotatedElementsByAnnotation contains annotated elements organized by the annotation type value
     * @param filterPredicate                  used to identify annotated elements as either POJO classes, Record classes, fields, parameters,...
     * @return map organized according to the structure: Map&#60;String, Map&#60;Element, Map&#60;String, List&#60;Element&#62;&#62;&#62;&#62;, detailed below:<br>
     * - String : toString representation of the annotation being processed (org.froporec.annotations.Record, org.froporec.annotations.Immutable, ...)<br>
     * - Element : the annotated element (either a class, a field or parameter,...)<br>
     * - String : the attribute name of the annotation (includeTypes, alsoConvert, mergeWith, superInterfaces,...)<br>
     * - List&#60;Element&#62; : list of Element instances converted from the .class String values listed in the attributes names mentioned above
     */
    Map<String, Map<Element, Map<String, List<Element>>>> extractInfoBasedOnPredicate(Map<TypeElement, Set<? extends Element>> allAnnotatedElementsByAnnotation,
                                                                                      Predicate<Element> filterPredicate);

    /**
     * Extracts a {@link List} of the {@link Element} instances provided as attributes arrays values of the annotations
     *
     * @param attributeName    either 'alsoConvert', 'superInterfaces', or 'mergeWith'
     * @param annotatedElement {@link Element} instance of the annotated POJO or Record class
     * @param annotation       {@link TypeElement} instance of the annotation being processed
     * @param processingEnv    {@link ProcessingEnvironment} object, needed to access low-level information regarding the used annotations
     * @return {@link List} of {@link Element} instances
     */
    static List<Element> extractAttributeValuesList(String attributeName, Element annotatedElement, TypeElement annotation, ProcessingEnvironment processingEnv) {
        var extractedElementsList = new ArrayList<Element>();
        processingEnv.getElementUtils().getAllAnnotationMirrors(annotatedElement).stream()
                .filter(annotationMirror -> annotationMirror.toString().contains(annotation.toString()))
                .map(AnnotationMirror::getElementValues)
                .forEach(map -> map.forEach((executableElement, annotationValue) -> {
                    // toString() sample values: executableElement: "alsoConvert()", "superInterfaces()" , annotationValue: "{org.froporec...School.class, org.froporec...Student.class}"
                    if (executableElement.toString().contains(attributeName)) {
                        extractedElementsList.addAll(
                                asList(annotationValue.getValue().toString().split(COMMA_SEPARATOR)).stream() // maintain provided order
                                        .map(includedTypeDotClassString -> processingEnv.getTypeUtils().asElement(
                                                processingEnv.getElementUtils().getTypeElement(
                                                        includedTypeDotClassString.strip().replace(DOT_CLASS, EMPTY_STRING)
                                                ).asType()
                                        )).toList());
                    }
                }));
        return extractedElementsList;
    }

    /**
     * Extracts a {@link Set} of annotated {@link Element} instances grouped by annotation String representations
     *
     * @param processingEnvironment            {@link ProcessingEnvironment} object, needed to access low-level information regarding the used annotations
     * @param allAnnotatedElementsByAnnotation map organized according to the structure: Map&#60;String, Map&#60;Element, Map&#60;String, List&#60;Element&#62;&#62;&#62;&#62;, detailed below:<br>
     *                                         - String : toString representation of the annotation being processed (org.froporec.annotations.Record, org.froporec.annotations.Immutable, ...)<br>
     *                                         - Element : the annotated element (either a class, a field or parameter,...)<br>
     *                                         - String : the attribute name of the annotation (includeTypes, alsoConvert, mergeWith, superInterfaces,...)<br>
     *                                         - List&#60;Element&#62; : list of Element instances converted from the .class String values listed in the attributes names mentioned above
     * @return {@link Set} of annotated {@link Element} instances grouped by annotation String representations
     */
    static Map<String, Set<Element>> extractAllElementsTypesToConvert(ProcessingEnvironment processingEnvironment,
                                                                      Map<String, Map<Element, Map<String, List<Element>>>> allAnnotatedElementsByAnnotation) {
        var allElementsTypesToConvert = new HashMap<String, Set<Element>>(); // String = annotation toString value , Element = Element instance of the annotated element type
        allAnnotatedElementsByAnnotation.forEach((annotationString, annotatedElementsMap) -> {
            var annotatedElementsWithAlsoConvertAndIncludeTypes = new HashSet<Element>();
            annotatedElementsMap.forEach((annotatedElement, attributesMap) -> {
                if (isAnnotatedAsExpected(processingEnvironment, annotationString, annotatedElement)) {
                    annotatedElementsWithAlsoConvertAndIncludeTypes.add(processingEnvironment.getTypeUtils().asElement(annotatedElement.asType()));
                    annotatedElementsWithAlsoConvertAndIncludeTypes.addAll(attributesMap.get(ALSO_CONVERT_ATTRIBUTE).stream()
                            .map(element -> processingEnvironment.getTypeUtils().asElement(element.asType())).collect(toSet()));
                    annotatedElementsWithAlsoConvertAndIncludeTypes.addAll(attributesMap.get(INCLUDE_TYPES_ATTRIBUTE).stream()
                            .map(element -> processingEnvironment.getTypeUtils().asElement(element.asType())).collect(toSet()));
                }
            });
            allElementsTypesToConvert.put(annotationString, annotatedElementsWithAlsoConvertAndIncludeTypes);
        });
        return allElementsTypesToConvert;
    }

    private static boolean isAnnotatedAsExpected(ProcessingEnvironment processingEnvironment, String annotationString, Element annotatedElement) {
        boolean isAClass = ElementKind.CLASS.equals(processingEnvironment.getTypeUtils().asElement(annotatedElement.asType()).getKind());
        boolean isARecord = ElementKind.RECORD.equals(processingEnvironment.getTypeUtils().asElement(annotatedElement.asType()).getKind());
        return (ORG_FROPOREC_RECORD.equals(annotationString) && isAClass)
                || (ORG_FROPOREC_GENERATE_RECORD.equals(annotationString) && isAClass)
                || (ORG_FROPOREC_IMMUTABLE.equals(annotationString) && isARecord)
                || (ORG_FROPOREC_GENERATE_IMMUTABLE.equals(annotationString) && isARecord)
                || (ORG_FROPOREC_SUPER_RECORD.equals(annotationString) && (isAClass || isARecord));
    }

    /**
     * Extracts a {@link List} of annotated {@link Element} instances representing the provided superInterfaces and grouped by
     * the corresponding annotated {@link Element} instance and the annotation String representation
     *
     * @param processingEnvironment                 {@link ProcessingEnvironment} object, needed to access low-level information regarding the used annotations
     * @param allElementsTypesToConvertByAnnotation {@link Set} of {@link Element} instances grouped by the annotation String representation
     * @param allAnnotatedElementsByAnnotation      map organized according to the structure: Map&#60;String, Map&#60;Element, Map&#60;String, List&#60;Element&#62;&#62;&#62;&#62;, detailed below:<br>
     *                                              - String : toString representation of the annotation being processed (org.froporec.annotations.Record, org.froporec.annotations.Immutable, ...)<br>
     *                                              - Element : the annotated element (either a class, a field or parameter,...)<br>
     *                                              - String : the attribute name of the annotation (includeTypes, alsoConvert, mergeWith, superInterfaces,...)<br>
     *                                              - List&#60;Element&#62; : list of Element instances converted from the .class String values listed in the attributes names mentioned above
     * @return {@link List} of annotated {@link Element} instances representing the provided superInterfaces and grouped by
     * the corresponding annotated {@link Element} instance and the annotation String representation
     */
    static Map<String, Map<Element, List<Element>>> extractSuperInterfacesListByAnnotatedElement(ProcessingEnvironment processingEnvironment,
                                                                                                 Map<String, Set<Element>> allElementsTypesToConvertByAnnotation,
                                                                                                 Map<String, Map<Element, Map<String, List<Element>>>> allAnnotatedElementsByAnnotation) {
        var superInterfacesListByAnnotatedElementAndByAnnotation = new HashMap<String, Map<Element, List<Element>>>();
        // (generics above) String = annotation toString value , Element = Element instance of the annotated element type, List<Element> = list of provided superinterfaces Element instances
        allAnnotatedElementsByAnnotation.forEach((annotationString, annotatedElementsMap) -> {
            var annotatedElementsWithSuperInterfacesMap = new HashMap<Element, List<Element>>();
            annotatedElementsMap.forEach((annotatedElement, attributesMap) -> {
                if (allElementsTypesToConvertByAnnotation.get(annotationString).contains(annotatedElement)) {
                    annotatedElementsWithSuperInterfacesMap.put(annotatedElement, attributesMap.get(SUPER_INTERFACES_ATTRIBUTE).stream()
                            .map(element -> processingEnvironment.getTypeUtils().asElement(element.asType())).toList());
                }
            });
            superInterfacesListByAnnotatedElementAndByAnnotation.put(annotationString, annotatedElementsWithSuperInterfacesMap);
        });
        return superInterfacesListByAnnotatedElementAndByAnnotation;
    }

    /**
     * Extracts a {@link List} of annotated {@link Element} instances representing the provided 'mergeWith' array and grouped by
     * the corresponding annotated {@link Element} instance and the annotation String representation
     *
     * @param processingEnvironment                 {@link ProcessingEnvironment} object, needed to access low-level information regarding the used annotations
     * @param allElementsTypesToConvertByAnnotation {@link Set} of {@link Element} instances grouped by the annotation String representation
     * @param allAnnotatedElementsByAnnotation      map organized according to the structure: Map&#60;String, Map&#60;Element, Map&#60;String, List&#60;Element&#62;&#62;&#62;&#62;, detailed below:<br>
     *                                              - String : toString representation of the annotation being processed (org.froporec.annotations.Record, org.froporec.annotations.Immutable, ...)<br>
     *                                              - Element : the annotated element (either a class, a field or parameter,...)<br>
     *                                              - String : the attribute name of the annotation (includeTypes, alsoConvert, mergeWith, superInterfaces,...)<br>
     *                                              - List&#60;Element&#62; : list of Element instances converted from the .class String values listed in the attributes names mentioned above
     * @return {@link List} of annotated {@link Element} instances representing the provided 'mergeWith' array elements and grouped by
     * the corresponding annotated {@link Element} instance and the annotation String representation
     */
    static Map<String, Map<Element, List<Element>>> extractMergeWithElementsListByAnnotatedElement(ProcessingEnvironment processingEnvironment,
                                                                                                   Map<String, Set<Element>> allElementsTypesToConvertByAnnotation,
                                                                                                   Map<String, Map<Element, Map<String, List<Element>>>> allAnnotatedElementsByAnnotation) {
        var mergeWithListByAnnotatedElementAndByAnnotation = new HashMap<String, Map<Element, List<Element>>>();
        // (generics above) String = annotation toString value , Element = Element instance of the annotated element type, List<Element> = list of provided mergeWith Element instances
        allAnnotatedElementsByAnnotation.forEach((annotationString, annotatedElementsMap) -> {
            var annotatedElementsWithMergeWithElementsMap = new HashMap<Element, List<Element>>();
            annotatedElementsMap.forEach((annotatedElement, attributesMap) -> {
                if (allElementsTypesToConvertByAnnotation.get(annotationString).contains(annotatedElement)) {
                    annotatedElementsWithMergeWithElementsMap.put(annotatedElement, attributesMap.get(MERGE_WITH_ATTRIBUTE).stream()
                            .map(element -> processingEnvironment.getTypeUtils().asElement(element.asType())).toList());
                }
            });
            mergeWithListByAnnotatedElementAndByAnnotation.put(annotationString, annotatedElementsWithMergeWithElementsMap);
        });
        return mergeWithListByAnnotatedElementAndByAnnotation;
    }
}