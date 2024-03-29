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
package org.froporec.generator;

import org.froporec.generator.helpers.StringGenerator;

import javax.annotation.processing.FilerException;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.froporec.generator.helpers.CodeGenerator.immutableQualifiedNameBasedOnElementType;
import static org.froporec.generator.helpers.CodeGenerator.nonVoidMethodsElementsList;
import static org.froporec.generator.helpers.CodeGenerator.superRecordQualifiedNameBasedOnElementType;

/**
 * Exposes:<br>
 * - default (concrete) methods to be called from the Annotation Processor main class to perform the generation of Record
 * classes based on the annotated elements<br>
 * - abstract methods to be implemented by the Record Source File Generator main class
 */
public sealed interface RecordSourceFileGenerator extends StringGenerator permits FroporecRecordSourceFileGenerator {

    /**
     * Generates String content of a Record class
     *
     * @param annotatedElement            {@link Element} instance of the annotated Pojo or Record class
     * @param generatedQualifiedClassName Qualified name of the Record class being generated
     * @param nonVoidMethodsElementsList  Non-void methods list of the Record class being generated
     * @param isSuperRecord               indicates whether the Pojo or Record class being processed was annotated
     *                                    with @{@link org.froporec.annotations.SuperRecord}
     * @return the Record class content
     */
    String generateRecordClassContent(Element annotatedElement,
                                      String generatedQualifiedClassName,
                                      List<? extends Element> nonVoidMethodsElementsList,
                                      boolean isSuperRecord);

    /**
     * Performs generation process for elements annotated with &#64;{@link org.froporec.annotations.Record}
     *
     * @param elementsListToProcess {@link List} of elements annotated with &#64;{@link org.froporec.annotations.Record}
     * @param processingEnv         {@link ProcessingEnvironment} object, needed to access low-level information regarding the used annotations
     * @return {@link Map} containing 2 keys: SUCCESS and FAILURE, each one's values being the list of qualified names of the generated Record classes
     */
    default Map<String, List<String>> generateForRecordAnnotatedElements(List<Element> elementsListToProcess,
                                                                         ProcessingEnvironment processingEnv) {
        Map<String, List<String>> generationReport = Map.of(SUCCESS, new ArrayList<>(), FAILURE, new ArrayList<>());
        elementsListToProcess.forEach(annotatedElement -> {
            var individualReport = performRecordSourceFileGeneration(
                    annotatedElement,
                    nonVoidMethodsElementsList(annotatedElement, processingEnv),
                    processingEnv,
                    false);
            mergeIndividualReportInMainReport(individualReport, generationReport);
        });
        return generationReport;
    }

    /**
     * Performs generation process for elements annotated with &#64;{@link org.froporec.annotations.Immutable}
     *
     * @param elementsListToProcess {@link List} of elements annotated with &#64;{@link org.froporec.annotations.Immutable}
     * @param processingEnv         {@link ProcessingEnvironment} object, needed to access low-level information regarding the used annotations
     * @return {@link Map} containing 2 keys: SUCCESS and FAILURE, each one's values being the list of qualified names of the generated Record classes
     */
    default Map<String, List<String>> generateForImmutableAnnotatedElements(List<Element> elementsListToProcess,
                                                                            ProcessingEnvironment processingEnv) {
        // redirect to generateForRecordAnnotatedElements which already handles both pojo and record classes
        return generateForRecordAnnotatedElements(elementsListToProcess, processingEnv);
    }

    /**
     * Performs generation process for elements annotated with &#64;{@link org.froporec.annotations.SuperRecord}
     *
     * @param annotatedElementsWithMergeWithInfo {@link Map} containing elements annotated with &#64;{@link org.froporec.annotations.SuperRecord},
     *                                           along with their respective {@link List} of {@link Element} instances provided
     *                                           as 'mergeWith' attribute value
     * @param processingEnv                      {@link ProcessingEnvironment} object, needed to access low-level information regarding the used annotations
     * @return {@link Map} containing 2 keys: SUCCESS and FAILURE, each one's values being the list of qualified names of the generated Record classes
     */
    default Map<String, List<String>> generateForSuperRecordAnnotatedElements(Map<Element, List<Element>> annotatedElementsWithMergeWithInfo,
                                                                              ProcessingEnvironment processingEnv) {
        Map<String, List<String>> generationReport = Map.of(SUCCESS, new ArrayList<>(), FAILURE, new ArrayList<>());
        annotatedElementsWithMergeWithInfo.forEach((annotatedElement, mergeWithElementsList) -> {
            var nonVoidMethodsElements = nonVoidMethodsElementsList(annotatedElement, processingEnv);
            // ...skipped processing mergeWithElementsList here, left it to FieldsGenerator and CustomConstructorGenerator
            var individualReport = performRecordSourceFileGeneration(annotatedElement, nonVoidMethodsElements, processingEnv, true);
            mergeIndividualReportInMainReport(individualReport, generationReport);
        });
        return generationReport;
    }

    private Map<String, String> performRecordSourceFileGeneration(Element annotatedElement,
                                                                  List<? extends Element> nonVoidMethodsElementsList,
                                                                  ProcessingEnvironment processingEnv,
                                                                  boolean isSuperRecord) {
        var generationReport = new HashMap<String, String>();
        var annotatedTypeElement = (TypeElement) processingEnv.getTypeUtils().asElement(annotatedElement.asType());
        var generatedQualifiedClassName = isSuperRecord
                ? superRecordQualifiedNameBasedOnElementType(annotatedTypeElement)
                : immutableQualifiedNameBasedOnElementType(annotatedTypeElement);
        try {
            writeRecordSourceFile(annotatedElement, generatedQualifiedClassName, nonVoidMethodsElementsList, processingEnv, isSuperRecord);
            generationReport.put(SUCCESS, generatedQualifiedClassName);
        } catch (FilerException e) {
            // File was already generated - do nothing
        } catch (IOException e) {
            generationReport.put(FAILURE, generatedQualifiedClassName);
        }
        return generationReport;
    }

    private void writeRecordSourceFile(Element annotatedElement,
                                       String generatedQualifiedClassName,
                                       List<? extends Element> nonVoidMethodsElementsList,
                                       ProcessingEnvironment processingEnv,
                                       boolean isSuperRecord) throws IOException {
        var recordClassFile = processingEnv.getFiler().createSourceFile(generatedQualifiedClassName); // if file already exists, this line throws a FilerException
        try (var out = new PrintWriter(recordClassFile.openWriter())) {
            out.print(generateRecordClassContent(annotatedElement, generatedQualifiedClassName, nonVoidMethodsElementsList, isSuperRecord));
        }
    }

    private void mergeIndividualReportInMainReport(Map<String, String> individualReport, Map<String, List<String>> mainReport) {
        if (individualReport.containsKey(SUCCESS)) {
            mainReport.get(SUCCESS).add(individualReport.get(SUCCESS));
        }
        if (individualReport.containsKey(FAILURE)) {
            mainReport.get(FAILURE).add(individualReport.get(FAILURE));
        }
    }
}