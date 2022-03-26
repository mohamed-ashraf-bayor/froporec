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
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.toSet;
import static org.froporec.extractor.AnnotationInfoExtractor.FIELDS_INFO_EXTRACTOR_PREDICATE;
import static org.froporec.extractor.AnnotationInfoExtractor.METHOD_PARAMS_INFO_EXTRACTOR_PREDICATE;
import static org.froporec.extractor.AnnotationInfoExtractor.POJO_CLASSES_INFO_EXTRACTOR_PREDICATE;
import static org.froporec.extractor.AnnotationInfoExtractor.RECORD_CLASSES_INFO_EXTRACTOR_PREDICATE;
import static org.froporec.extractor.AnnotationInfoExtractor.extractAttributeValuesList;
import static org.froporec.generator.helpers.StringGenerator.ALSO_CONVERT_ATTRIBUTE;
import static org.froporec.generator.helpers.StringGenerator.INCLUDE_TYPES_ATTRIBUTE;
import static org.froporec.generator.helpers.StringGenerator.MERGE_WITH_ATTRIBUTE;
import static org.froporec.generator.helpers.StringGenerator.SUPER_INTERFACES_ATTRIBUTE;

/**
 * Extracts info about the annotated POJO or Record classes and applies grouping by annotated elements or annotations strings
 */
public final class FroporecAnnotationInfoExtractor {

    private final ProcessingEnvironment processingEnv;

    public FroporecAnnotationInfoExtractor(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
    }

    /**
     * Extracts {@link Map} of annotated POJO or Record classes, grouping them by annotated element and annotation string
     *
     * @param allAnnotatedElementsByAnnotation contains annotated elements organized by the annotation type value
     * @return map organized according to the structure: Map&#60;String, Map&#60;Element, Map&#60;String, List&#60;Element&#62;&#62;&#62;&#62;, detailed below:<br>
     * - String : toString representation of the annotation being processed (org.froporec.annotations.Record, org.froporec.annotations.Immutable, ...)<br>
     * - Element : the annotated element (either a class, a field or parameter,...)<br>
     * - String : the attribute name of the annotation (includeTypes, alsoConvert, mergeWith, superInterfaces,...)<br>
     * - List&#60;Element&#62; : list of Element instances converted from the .class String values listed in the attributes names mentioned above
     */
    public Map<String, Map<Element, Map<String, List<Element>>>> extractAnnotatedElementsByAnnotation(Map<TypeElement, Set<? extends Element>> allAnnotatedElementsByAnnotation) {

        // implementation of the AnnotationInfoExtractor functional interface
        final AnnotationInfoExtractor annotationInfoExtractor = (annotatedElementsByAnnotation, filterPredicate) -> {
            final Map<String, Map<Element, Map<String, List<Element>>>> allFilteredAnnotatedElementsToProcess = new HashMap<>();
            annotatedElementsByAnnotation.forEach((annotation, annotatedElements) -> {
                // prcss each annot in here
                var filteredAnnotatedElements = annotatedElements.stream().filter(filterPredicate).collect(toSet());
                Map<Element, Map<String, List<Element>>> internalMap = new HashMap<>();
                filteredAnnotatedElements.forEach(element -> internalMap.put(
                        element,
                        Map.of(INCLUDE_TYPES_ATTRIBUTE, extractAttributeValuesList(INCLUDE_TYPES_ATTRIBUTE, element, annotation, processingEnv),
                                ALSO_CONVERT_ATTRIBUTE, extractAttributeValuesList(ALSO_CONVERT_ATTRIBUTE, element, annotation, processingEnv),
                                MERGE_WITH_ATTRIBUTE, extractAttributeValuesList(MERGE_WITH_ATTRIBUTE, element, annotation, processingEnv),
                                SUPER_INTERFACES_ATTRIBUTE, extractAttributeValuesList(SUPER_INTERFACES_ATTRIBUTE, element, annotation, processingEnv))
                ));
                allFilteredAnnotatedElementsToProcess.put(annotation.toString(), internalMap);
            });
            return allFilteredAnnotatedElementsToProcess;
        };

        var allAnnotatedPojosElementsInfosByAnnotation = annotationInfoExtractor.extractInfoBasedOnPredicate(allAnnotatedElementsByAnnotation, POJO_CLASSES_INFO_EXTRACTOR_PREDICATE);
        var annotatedRecordsElementsInfosByAnnotation = annotationInfoExtractor.extractInfoBasedOnPredicate(allAnnotatedElementsByAnnotation, RECORD_CLASSES_INFO_EXTRACTOR_PREDICATE);
        var annotatedFieldsElementsInfosByAnnotation = annotationInfoExtractor.extractInfoBasedOnPredicate(allAnnotatedElementsByAnnotation, FIELDS_INFO_EXTRACTOR_PREDICATE);
        var annotatedParamsElementsInfosByAnnotation = annotationInfoExtractor.extractInfoBasedOnPredicate(allAnnotatedElementsByAnnotation, METHOD_PARAMS_INFO_EXTRACTOR_PREDICATE);

        final Map<String, Map<Element, Map<String, List<Element>>>> allAnnotatedElementsInfosByAnnotation = new HashMap<>();
        allAnnotatedElementsByAnnotation.keySet().forEach(annotation -> {
            var annotatedElementsMap = allAnnotatedPojosElementsInfosByAnnotation.get(annotation.toString());
            annotatedElementsMap.putAll(annotatedRecordsElementsInfosByAnnotation.get(annotation.toString()));
            annotatedElementsMap.putAll(annotatedFieldsElementsInfosByAnnotation.get(annotation.toString()));
            annotatedElementsMap.putAll(annotatedParamsElementsInfosByAnnotation.get(annotation.toString()));
            allAnnotatedElementsInfosByAnnotation.put(annotation.toString(), annotatedElementsMap);
        });
        return allAnnotatedElementsInfosByAnnotation;
    }
}