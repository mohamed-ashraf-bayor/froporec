package org.froporec;

import org.froporec.generator.RecordSourceFileGenerator;
import org.froporec.generator.SourceFileGenerator;

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
                                                                     SourceFileGenerator sourceFileGenerator,
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
        return sourceFileGenerator.generateForRecordAnnotatedElements(elementsListToProcess, processingEnv);
    }

    default Map<String, List<String>> processImmutableAnnotatedElements(Map<Element, Map<String, List<Element>>> annotatedElementsMap,
                                                                        SourceFileGenerator sourceFileGenerator,
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
        return sourceFileGenerator.generateForImmutableAnnotatedElements(elementsListToProcess, processingEnv);
    }

    default Map<String, List<String>> processSuperRecordAnnotatedElements(Map<Element, Map<String, List<Element>>> annotatedElementsMap,
                                                                          SourceFileGenerator sourceFileGenerator,
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
        return sourceFileGenerator.generateForSuperRecordAnnotatedElements(elementsMapToProcess, processingEnv);
    }
}