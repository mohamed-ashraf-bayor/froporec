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
import org.froporec.generator.FroporecRecordSourceFileGenerator;
import org.froporec.generator.RecordSourceFileGenerator;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.concat;
import static org.froporec.generator.helpers.StringGenerator.ALL_ANNOTATIONS_QUALIFIED_NAMES;
import static org.froporec.generator.helpers.StringGenerator.AT_SIGN;
import static org.froporec.generator.helpers.StringGenerator.FAILURE;
import static org.froporec.generator.helpers.StringGenerator.GENERATE_IMMUTABLE;
import static org.froporec.generator.helpers.StringGenerator.GENERATE_RECORD;
import static org.froporec.generator.helpers.StringGenerator.GENERATION_FAILURE_MSG;
import static org.froporec.generator.helpers.StringGenerator.GENERATION_REPORT_ELEMENTS_SEPARATOR;
import static org.froporec.generator.helpers.StringGenerator.GENERATION_REPORT_MSG_FORMAT;
import static org.froporec.generator.helpers.StringGenerator.GENERATION_SUCCESS_MSG;
import static org.froporec.generator.helpers.StringGenerator.IMMUTABLE;
import static org.froporec.generator.helpers.StringGenerator.ORG_FROPOREC_GENERATE_IMMUTABLE;
import static org.froporec.generator.helpers.StringGenerator.ORG_FROPOREC_GENERATE_RECORD;
import static org.froporec.generator.helpers.StringGenerator.ORG_FROPOREC_IMMUTABLE;
import static org.froporec.generator.helpers.StringGenerator.ORG_FROPOREC_RECORD;
import static org.froporec.generator.helpers.StringGenerator.ORG_FROPOREC_SUPER_RECORD;
import static org.froporec.generator.helpers.StringGenerator.RECORD;
import static org.froporec.generator.helpers.StringGenerator.SUCCESS;
import static org.froporec.generator.helpers.StringGenerator.SUPER_RECORD;

/**
 * FroPoRec annotation processor class. Picks up and processes all elements (classes, fields and method params) annotated
 * with @{@link Record}, {@link Immutable} and {@link SuperRecord}.<br>
 * For each annotated element a fully immutable Record class is generated. If the generated class already exists (in case the
 * corresponding pojo or record has been annotated more than once), the generation process will be skipped
 */
@SupportedAnnotationTypes({ORG_FROPOREC_RECORD, ORG_FROPOREC_SUPER_RECORD, ORG_FROPOREC_IMMUTABLE, ORG_FROPOREC_GENERATE_RECORD,
        ORG_FROPOREC_GENERATE_IMMUTABLE})
@SupportedSourceVersion(SourceVersion.RELEASE_17)
@AutoService(Processor.class)
public final class FroporecAnnotationProcessor extends AbstractProcessor implements AnnotationProcessor {

    private final Logger log = Logger.getLogger(FroporecAnnotationProcessor.class.getName());

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnvironment) {
        var allAnnotatedElementsByAnnotation = new HashMap<TypeElement, Set<? extends Element>>();
        annotations.forEach(annotation -> {
            if (ALL_ANNOTATIONS_QUALIFIED_NAMES.stream().anyMatch(annotation.toString()::contains)) {
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
            // add empty maps for non-provided annotations before returning
            ALL_ANNOTATIONS_QUALIFIED_NAMES.stream()
                    .filter(annotationString -> !allAnnotatedElementsToProcessByAnnotation.keySet().contains(annotationString))
                    .forEach(annotationString -> allAnnotatedElementsToProcessByAnnotation.put(annotationString, Map.of()));
            RecordSourceFileGenerator recordSourceFileGenerator = new FroporecRecordSourceFileGenerator(processingEnv, allAnnotatedElementsToProcessByAnnotation);
            // process annotated elements and display reports
            displayReport(
                    AT_SIGN + RECORD,
                    processRecordAnnotatedElements(allAnnotatedElementsToProcessByAnnotation.get(ORG_FROPOREC_RECORD), recordSourceFileGenerator, processingEnv)
            );
            displayReport(
                    AT_SIGN + GENERATE_RECORD,
                    processRecordAnnotatedElements(allAnnotatedElementsToProcessByAnnotation.get(ORG_FROPOREC_GENERATE_RECORD), recordSourceFileGenerator, processingEnv)
            );
            displayReport(
                    AT_SIGN + IMMUTABLE,
                    processImmutableAnnotatedElements(allAnnotatedElementsToProcessByAnnotation.get(ORG_FROPOREC_IMMUTABLE), recordSourceFileGenerator, processingEnv)
            );
            displayReport(
                    AT_SIGN + GENERATE_IMMUTABLE,
                    processImmutableAnnotatedElements(allAnnotatedElementsToProcessByAnnotation.get(ORG_FROPOREC_GENERATE_IMMUTABLE), recordSourceFileGenerator, processingEnv)
            );
            displayReport(
                    AT_SIGN + SUPER_RECORD,
                    processSuperRecordAnnotatedElements(allAnnotatedElementsToProcessByAnnotation.get(ORG_FROPOREC_SUPER_RECORD), recordSourceFileGenerator, processingEnv)
            );
        }
        return true;
    }

    private void displayReport(String processedAnnotation, Map<String, List<String>> generatedClassesMap) {
        if (!generatedClassesMap.get(SUCCESS).isEmpty()) {
            log.info(() -> format(
                    GENERATION_REPORT_MSG_FORMAT,
                    GENERATION_SUCCESS_MSG,
                    processedAnnotation,
                    generatedClassesMap.get(SUCCESS).stream().collect(joining(format(GENERATION_REPORT_ELEMENTS_SEPARATOR)))
            ));
        }
        if (!generatedClassesMap.get(FAILURE).isEmpty()) {
            log.log(Level.SEVERE, format(
                    GENERATION_REPORT_MSG_FORMAT,
                    GENERATION_FAILURE_MSG,
                    AT_SIGN + processedAnnotation,
                    generatedClassesMap.get(FAILURE).stream().collect(joining(format(GENERATION_REPORT_ELEMENTS_SEPARATOR)))
            ));
        }
    }

    @Override
    public void notifyWarning(String warningMsg) {
        log.warning(() -> warningMsg);
    }
}