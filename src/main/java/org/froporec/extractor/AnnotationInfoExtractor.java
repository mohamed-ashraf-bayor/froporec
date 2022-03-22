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

@FunctionalInterface
public interface AnnotationInfoExtractor {

    Predicate<Element> POJO_CLASSES_INFO_EXTRACTOR_PREDICATE = element ->
            !element.getClass().isRecord()
                    && !element.getClass().isEnum()
                    && !ElementKind.INTERFACE.equals(element.getKind())
                    && ElementKind.CLASS.equals(element.getKind());

    Predicate<Element> RECORD_CLASSES_INFO_EXTRACTOR_PREDICATE = element -> ElementKind.RECORD.equals(element.getKind());

    Predicate<Element> FIELDS_INFO_EXTRACTOR_PREDICATE = element ->
            !ElementKind.ENUM_CONSTANT.equals(element.getKind())
                    && (ElementKind.FIELD.equals(element.getKind()) || ElementKind.RECORD_COMPONENT.equals(element.getKind()));

    Predicate<Element> METHOD_PARAMS_INFO_EXTRACTOR_PREDICATE = element -> ElementKind.PARAMETER.equals(element.getKind());

    /**
     * // TODO jdoc
     * @param allAnnotatedElementsByAnnotation
     * @param filterPredicate
     * @return map organized according to the structure: Map<String, Map<Element, Map<String, List<Element>>>>, detailed below:
     * String : toString representation of the annotation being processed (org.froporec.annotations.Record, org.froporec.annotations.Immutable, ...)
     * Element : the annotated element (either a class, a field or parameter,...)
     * String : the attribute name of the annotation (includeTypes, alsoConvert, mergeWith, superInterfaces,...)
     * List<Element> : list of Element instances converted from the .class String values listed in the attributes names mentioned above
     */
    Map<String, Map<Element, Map<String, List<Element>>>> extractInfoBasedOnPredicate(Map<TypeElement, Set<? extends Element>> allAnnotatedElementsByAnnotation, Predicate<Element> filterPredicate);

    static List<Element> extractAttributeValuesList(String attributeName, Element annotatedElement, TypeElement annotation, ProcessingEnvironment processingEnv) {
        var extractedElementsList = new ArrayList<Element>();
        processingEnv.getElementUtils().getAllAnnotationMirrors(annotatedElement).stream()
                .filter(annotationMirror -> annotationMirror.toString().contains(annotation.toString()))
                .map(AnnotationMirror::getElementValues)
                .forEach(map -> map.forEach((executableElement, annotationValue) -> {
                    // toString() sample values: executableElement: "includeTypes()", "superInterfaces()" , annotationValue: "{org.froporec...School.class, org.froporec...Student.class}"
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