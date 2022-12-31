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
import static org.froporec.generator.helpers.StringGenerator.IMMUTABLE;
import static org.froporec.generator.helpers.StringGenerator.MERGE_WITH_ATTRIBUTE;
import static org.froporec.generator.helpers.StringGenerator.POJO;
import static org.froporec.generator.helpers.StringGenerator.RECORD;
import static org.froporec.generator.helpers.StringGenerator.SUPER_RECORD;

/**
 * Exposes:<br>
 * - default (concrete) methods to be called from the Annotation Processor main class to perform the processing of annotated elements<br>
 * - abstract methods to be implemented by the Annotation Processor main class
 */
public sealed interface AnnotationProcessor permits FroporecAnnotationProcessor {

    /**
     * Warning message displayed during code compilation, indicating annotated elements skipped during generation process
     */
    String SKIPPED_ELEMENTS_WARNING_MSG_FORMAT = "\t> Skipped %s annotated elements (must be %s classes):%n\t\t%s";

    /**
     * Separator used while displaying each one of the generated or skipped filenames
     */
    String GENERATION_REPORT_ELEMENTS_SEPARATOR = "\n\t\t";

    /**
     * Message displayed during code compilation, along with the name of a successfully generated Record source file
     */
    String GENERATION_SUCCESS_MSG = "\t> Successfully generated";

    /**
     * Generation report info message format
     */
    String GENERATION_REPORT_MSG_FORMAT = "%s for %s:\n\t\t%s";

    /**
     * Message displayed during code compilation, in case an error occurred during a Record source file generation process
     */
    String GENERATION_FAILURE_MSG = "\t> Error generating";

    /**
     * Notifies the Annotation Processor main class with a message intended to be displayed as a Warning
     *
     * @param warningMsg a String object containing the warning message
     */
    void notifyWarning(String warningMsg);

    /**
     * Processes elements annotated with &#64;{@link org.froporec.annotations.Record}
     *
     * @param annotatedElementsMap      {@link Map} containing annotated {@link Element} instances along with the provided
     *                                  attributes with their respective values
     * @param recordSourceFileGenerator instance of {@link RecordSourceFileGenerator} used to perform the Record class generation
     * @param processingEnv             {@link ProcessingEnvironment} object, needed to access low-level information regarding the used annotations
     * @return {@link Map} containing 2 keys: SUCCESS and FAILURE, each one's values being the list of qualified names of the generated Record classes
     */
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
                });
        return recordSourceFileGenerator.generateForRecordAnnotatedElements(elementsListToProcess, processingEnv);
    }

    /**
     * Processes elements annotated with &#64;{@link org.froporec.annotations.Immutable}
     *
     * @param annotatedElementsMap      {@link Map} containing annotated {@link Element} instances along with the provided
     *                                  attributes with their respective values
     * @param recordSourceFileGenerator instance of {@link RecordSourceFileGenerator} used to perform the Record class generation
     * @param processingEnv             {@link ProcessingEnvironment} object, needed to access low-level information regarding the used annotations
     * @return {@link Map} containing 2 keys: SUCCESS and FAILURE, each one's values being the list of qualified names of the generated Record classes
     */
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
                });
        return recordSourceFileGenerator.generateForImmutableAnnotatedElements(elementsListToProcess, processingEnv);
    }

    /**
     * Processes elements annotated with &#64;{@link org.froporec.annotations.SuperRecord}
     *
     * @param annotatedElementsMap      {@link Map} containing annotated {@link Element} instances along with the provided
     *                                  attributes with their respective values
     * @param recordSourceFileGenerator instance of {@link RecordSourceFileGenerator} used to perform the Record class generation
     * @param processingEnv             {@link ProcessingEnvironment} object, needed to access low-level information regarding the used annotations
     * @return {@link Map} containing 2 keys: SUCCESS and FAILURE, each one's values being the list of qualified names of the generated Record classes
     */
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
                    skippedElements.stream().map(Object::toString).collect(joining(GENERATION_REPORT_ELEMENTS_SEPARATOR))
            ));
        }
        var elementsMapToProcess = annotatedElementsMap.keySet().stream()
                .filter(element -> !skippedElements.contains(element))
                .collect(toMap(element -> element, element -> annotatedElementsMap.get(element).get(MERGE_WITH_ATTRIBUTE)));
        return recordSourceFileGenerator.generateForSuperRecordAnnotatedElements(elementsMapToProcess, processingEnv);
    }
}