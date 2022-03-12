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

import com.google.auto.service.AutoService;
import org.froporec.annotations.Immutable;
import org.froporec.annotations.Record;
import org.froporec.annotations.SuperRecord;
import org.froporec.extractor.FroporecAnnotationInfoExtractor;
import org.froporec.generator.RecordSourceFileGenerator;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.concat;
import static org.froporec.generator.helpers.StringGenerator.ALSO_CONVERT_ATTRIBUTE;
import static org.froporec.generator.helpers.StringGenerator.AT_SIGN;
import static org.froporec.generator.helpers.StringGenerator.IMMUTABLE;
import static org.froporec.generator.helpers.StringGenerator.INCLUDE_TYPES_ATTRIBUTE;
import static org.froporec.generator.helpers.StringGenerator.MERGE_WITH_ATTRIBUTE;
import static org.froporec.generator.helpers.StringGenerator.ORG_FROPOREC_GENERATE_IMMUTABLE;
import static org.froporec.generator.helpers.StringGenerator.ORG_FROPOREC_GENERATE_RECORD;
import static org.froporec.generator.helpers.StringGenerator.ORG_FROPOREC_IMMUTABLE;
import static org.froporec.generator.helpers.StringGenerator.ORG_FROPOREC_RECORD;
import static org.froporec.generator.helpers.StringGenerator.ORG_FROPOREC_SUPER_RECORD;
import static org.froporec.generator.helpers.StringGenerator.POJO;
import static org.froporec.generator.helpers.StringGenerator.RECORD;
import static org.froporec.generator.helpers.StringGenerator.SKIPPED_ELEMENTS_WARNING_MSG_FORMAT;
import static org.froporec.generator.helpers.StringGenerator.SUPER_RECORD;

/**
 * FroPoRec annotation processor class. Picks up and processes all elements (classes, fields and method params) annotated
 * with @{@link Record}, {@link Immutable} and {@link SuperRecord}.<br>
 * The order of processing is: classes, then fields and then the method parameters<br>
 * For each annotated element a fully immutable Record class is generated. If the generated class already exists (in case the
 * corresponding pojo or record has been annotated more than once), the generation process will be skipped
 */
@SupportedAnnotationTypes({ORG_FROPOREC_RECORD, ORG_FROPOREC_SUPER_RECORD, ORG_FROPOREC_IMMUTABLE, ORG_FROPOREC_GENERATE_RECORD,
        ORG_FROPOREC_GENERATE_IMMUTABLE})
@SupportedSourceVersion(SourceVersion.RELEASE_17)
@AutoService(Processor.class)
public class FroporecAnnotationProcessor extends AbstractProcessor {

    private final Logger log = Logger.getLogger(FroporecAnnotationProcessor.class.getName());

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnvironment) {
        var allAnnotatedElementsByAnnotation = new HashMap<TypeElement, Set<? extends Element>>();
        var allAnnotationsQualifiedNames = List.of(ORG_FROPOREC_GENERATE_RECORD, ORG_FROPOREC_GENERATE_IMMUTABLE,
                ORG_FROPOREC_RECORD, ORG_FROPOREC_IMMUTABLE, ORG_FROPOREC_SUPER_RECORD);
        annotations.forEach(annotation -> {
            if (allAnnotationsQualifiedNames.stream().anyMatch(annotation.toString()::contains)) {
                allAnnotatedElementsByAnnotation.merge(
                        annotation,
                        roundEnvironment.getElementsAnnotatedWith(annotation),
                        (currentSet, newSet) -> concat(currentSet.stream(), newSet.stream()).collect(toSet())
                );
            }
        });
        if (!allAnnotatedElementsByAnnotation.isEmpty()) {
            var allAnnotatedElementsToProcessByAnnotation =
                    new FroporecAnnotationInfoExtractor(processingEnv).extractAnnotatedElementsByAnnotation(allAnnotatedElementsByAnnotation);
            var recordSourceFileGenerator = new RecordSourceFileGenerator(processingEnv, allAnnotatedElementsToProcessByAnnotation);
            processRecordAnnotatedElements(allAnnotatedElementsToProcessByAnnotation.get(ORG_FROPOREC_RECORD), recordSourceFileGenerator);
            processImmutableAnnotatedElements(allAnnotatedElementsToProcessByAnnotation.get(ORG_FROPOREC_IMMUTABLE), recordSourceFileGenerator);
            processSuperRecordAnnotatedElements(allAnnotatedElementsToProcessByAnnotation.get(ORG_FROPOREC_SUPER_RECORD), recordSourceFileGenerator);
        }
        return true;
    }

    private void processRecordAnnotatedElements(Map<Element, Map<String, List<Element>>> annotatedElementsMap, RecordSourceFileGenerator recordSourceFileGenerator) {
        var skippedElements = annotatedElementsMap.keySet().stream()
                .filter(element -> !ElementKind.CLASS.equals(processingEnv.getTypeUtils().asElement(element.asType()).getKind()))
                .toList();
        if (!skippedElements.isEmpty()) {
            log.warning(() -> format(
                    SKIPPED_ELEMENTS_WARNING_MSG_FORMAT,
                    AT_SIGN + RECORD,
                    POJO,
                    skippedElements.stream().map(Object::toString).collect(joining(format("%n\t\t")))
            ));
        }
        annotatedElementsMap.keySet().stream()
                .filter(element -> !skippedElements.contains(element))
                .forEach(element -> {
                    recordSourceFileGenerator.generateRecord(List.of(element));
                    recordSourceFileGenerator.generateRecord(annotatedElementsMap.get(element).get(ALSO_CONVERT_ATTRIBUTE));
                    recordSourceFileGenerator.generateRecord(annotatedElementsMap.get(element).get(INCLUDE_TYPES_ATTRIBUTE));
                });
    }

    private void processImmutableAnnotatedElements(Map<Element, Map<String, List<Element>>> annotatedElementsMap, RecordSourceFileGenerator recordSourceFileGenerator) {
        var skippedElements = annotatedElementsMap.keySet().stream()
                .filter(element -> !ElementKind.RECORD.equals(processingEnv.getTypeUtils().asElement(element.asType()).getKind()))
                .toList();
        if (!skippedElements.isEmpty()) {
            log.warning(() -> format(
                    SKIPPED_ELEMENTS_WARNING_MSG_FORMAT,
                    AT_SIGN + IMMUTABLE,
                    RECORD,
                    skippedElements.stream().map(Object::toString).collect(joining(format("%n\t\t")))
            ));
        }
        annotatedElementsMap.keySet().stream()
                .filter(element -> !skippedElements.contains(element))
                .forEach(element -> {
                    recordSourceFileGenerator.generateImmutable(List.of(element));
                    recordSourceFileGenerator.generateImmutable(annotatedElementsMap.get(element).get(ALSO_CONVERT_ATTRIBUTE));
                    recordSourceFileGenerator.generateImmutable(annotatedElementsMap.get(element).get(INCLUDE_TYPES_ATTRIBUTE));
                });
    }

    private void processSuperRecordAnnotatedElements(Map<Element, Map<String, List<Element>>> annotatedElementsMap, RecordSourceFileGenerator recordSourceFileGenerator) {
        var skippedElements = annotatedElementsMap.keySet().stream()
                .filter(element -> !ElementKind.RECORD.equals(processingEnv.getTypeUtils().asElement(element.asType()).getKind()))
                .filter(element -> !ElementKind.CLASS.equals(processingEnv.getTypeUtils().asElement(element.asType()).getKind()))
                .toList();
        if (!skippedElements.isEmpty()) {
            log.warning(() -> format(
                    SKIPPED_ELEMENTS_WARNING_MSG_FORMAT,
                    AT_SIGN + SUPER_RECORD,
                    POJO + " or " + RECORD,
                    skippedElements.stream().map(Object::toString).collect(joining(format("%n\t\t")))
            ));
        }
        annotatedElementsMap.keySet().stream()
                .filter(element -> !skippedElements.contains(element))
                .forEach(element ->
                        recordSourceFileGenerator.generateSuperRecord(List.of(element), annotatedElementsMap.get(element).get(MERGE_WITH_ATTRIBUTE)));
    }
}