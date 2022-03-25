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
package org.froporec;

import org.froporec.generator.RecordSourceFileGenerator;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;
import static org.froporec.generator.helpers.StringGenerator.ALSO_CONVERT_ATTRIBUTE;
import static org.froporec.generator.helpers.StringGenerator.AT_SIGN;
import static org.froporec.generator.helpers.StringGenerator.GENERATION_REPORT_ELEMENTS_SEPARATOR;
import static org.froporec.generator.helpers.StringGenerator.IMMUTABLE;
import static org.froporec.generator.helpers.StringGenerator.INCLUDE_TYPES_ATTRIBUTE;
import static org.froporec.generator.helpers.StringGenerator.MERGE_WITH_ATTRIBUTE;
import static org.froporec.generator.helpers.StringGenerator.POJO;
import static org.froporec.generator.helpers.StringGenerator.RECORD;
import static org.froporec.generator.helpers.StringGenerator.SKIPPED_ELEMENTS_WARNING_MSG_FORMAT;
import static org.froporec.generator.helpers.StringGenerator.SUPER_RECORD;

public sealed interface AnnotationProcessor permits FroporecAnnotationProcessor {

    void notifyWarning(String warningMsg);

    default Map<String, List<String>> processRecordAnnotatedElements(Map<Element, Map<String, List<Element>>> annotatedElementsMap,
                                                                     RecordSourceFileGenerator recordSourceFileGenerator,
                                                                     ProcessingEnvironment processingEnv) {
        var skippedElements = annotatedElementsMap.keySet().stream()
                .filter(element -> !ElementKind.CLASS.equals(processingEnv.getTypeUtils().asElement(element.asType()).getKind()))
                .toList();
        if (!skippedElements.isEmpty()) {
            notifyWarning(format(
                    SKIPPED_ELEMENTS_WARNING_MSG_FORMAT,
                    AT_SIGN + RECORD,
                    POJO,
                    skippedElements.stream().map(Object::toString).collect(joining(format(GENERATION_REPORT_ELEMENTS_SEPARATOR)))
            ));
        }
        var elementsListToProcess = new ArrayList<Element>();
        annotatedElementsMap.keySet().stream()
                .filter(element -> !skippedElements.contains(element))
                .forEach(element -> {
                    elementsListToProcess.add(element);
                    elementsListToProcess.addAll(annotatedElementsMap.get(element).get(ALSO_CONVERT_ATTRIBUTE));
                    elementsListToProcess.addAll(annotatedElementsMap.get(element).get(INCLUDE_TYPES_ATTRIBUTE));
                });
        return recordSourceFileGenerator.generateForRecordAnnotatedElements(elementsListToProcess, processingEnv);
    }

    default Map<String, List<String>> processImmutableAnnotatedElements(Map<Element, Map<String, List<Element>>> annotatedElementsMap,
                                                                        RecordSourceFileGenerator recordSourceFileGenerator,
                                                                        ProcessingEnvironment processingEnv) {
        var skippedElements = annotatedElementsMap.keySet().stream()
                .filter(element -> !ElementKind.RECORD.equals(processingEnv.getTypeUtils().asElement(element.asType()).getKind()))
                .toList();
        if (!skippedElements.isEmpty()) {
            notifyWarning(format(
                    SKIPPED_ELEMENTS_WARNING_MSG_FORMAT,
                    AT_SIGN + IMMUTABLE,
                    RECORD,
                    skippedElements.stream().map(Object::toString).collect(joining(format(GENERATION_REPORT_ELEMENTS_SEPARATOR)))
            ));
        }
        var elementsListToProcess = new ArrayList<Element>();
        annotatedElementsMap.keySet().stream()
                .filter(element -> !skippedElements.contains(element))
                .forEach(element -> {
                    elementsListToProcess.add(element);
                    elementsListToProcess.addAll(annotatedElementsMap.get(element).get(ALSO_CONVERT_ATTRIBUTE));
                    elementsListToProcess.addAll(annotatedElementsMap.get(element).get(INCLUDE_TYPES_ATTRIBUTE));
                });
        return recordSourceFileGenerator.generateForImmutableAnnotatedElements(elementsListToProcess, processingEnv);
    }

    default Map<String, List<String>> processSuperRecordAnnotatedElements(Map<Element, Map<String, List<Element>>> annotatedElementsMap,
                                                                          RecordSourceFileGenerator recordSourceFileGenerator,
                                                                          ProcessingEnvironment processingEnv) {
        var skippedElements = annotatedElementsMap.keySet().stream()
                .filter(element -> !ElementKind.RECORD.equals(processingEnv.getTypeUtils().asElement(element.asType()).getKind()))
                .filter(element -> !ElementKind.CLASS.equals(processingEnv.getTypeUtils().asElement(element.asType()).getKind()))
                .toList();
        if (!skippedElements.isEmpty()) {
            notifyWarning(format(
                    SKIPPED_ELEMENTS_WARNING_MSG_FORMAT,
                    AT_SIGN + SUPER_RECORD,
                    POJO + " or " + RECORD,
                    skippedElements.stream().map(Object::toString).collect(joining(format(GENERATION_REPORT_ELEMENTS_SEPARATOR)))
            ));
        }
        var elementsMapToProcess = annotatedElementsMap.keySet().stream()
                .filter(element -> !skippedElements.contains(element))
                .collect(toMap(element -> element, element -> annotatedElementsMap.get(element).get(MERGE_WITH_ATTRIBUTE)));
        return recordSourceFileGenerator.generateForSuperRecordAnnotatedElements(elementsMapToProcess, processingEnv);
    }
}