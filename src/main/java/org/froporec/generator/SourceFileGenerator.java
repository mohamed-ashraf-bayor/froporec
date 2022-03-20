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
package org.froporec.generator;

import org.froporec.generator.helpers.StringGenerator;

import javax.annotation.processing.FilerException;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.froporec.generator.helpers.StringGenerator.constructImmutableQualifiedNameBasedOnElementType;
import static org.froporec.generator.helpers.StringGenerator.constructSuperRecordQualifiedNameBasedOnElementType;

public sealed interface SourceFileGenerator extends StringGenerator permits RecordSourceFileGenerator {

    String buildRecordClassContent(Element annotatedElement, String generatedQualifiedClassName, List<? extends Element> nonVoidMethodsElementsList);

    default Map<String, List<String>> generateForRecordAnnotatedElements(List<Element> elementsListToProcess,
                                                                         ProcessingEnvironment processingEnv) {
        Map<String, List<String>> generationReport = Map.of(SUCCESS, new ArrayList<>(), FAILURE, new ArrayList<>());
        elementsListToProcess.forEach(annotatedElement -> {
            var individualReport = performGeneration(
                    annotatedElement,
                    buildNonVoidMethodsElementsList(annotatedElement, processingEnv),
                    processingEnv,
                    false);
            mergeIndividualReportInMainReport(individualReport, generationReport);
        });
        return generationReport;
    }

    default Map<String, List<String>> generateForImmutableAnnotatedElements(List<Element> elementsListToProcess,
                                                                            ProcessingEnvironment processingEnv) {
        // redirect to generateForRecordAnnotatedElements which already handles both pojo and record classes
        return generateForRecordAnnotatedElements(elementsListToProcess, processingEnv);
    }

    default Map<String, List<String>> generateForSuperRecordAnnotatedElements(Map<Element, List<Element>> annotatedElementsWithMergeWithInfo,
                                                                              ProcessingEnvironment processingEnv) {
        Map<String, List<String>> generationReport = Map.of(SUCCESS, new ArrayList<>(), FAILURE, new ArrayList<>());
        annotatedElementsWithMergeWithInfo.forEach((annotatedElement, mergeWithElementsList) -> {
            var nonVoidMethodsElements = new ArrayList<Element>(buildNonVoidMethodsElementsList(annotatedElement, processingEnv));
            nonVoidMethodsElements.addAll(mergeWithElementsList.stream()
                    .flatMap(element -> buildNonVoidMethodsElementsList(element, processingEnv).stream())
                    .toList());
            var individualReport = performGeneration(annotatedElement, nonVoidMethodsElements, processingEnv, true);
            mergeIndividualReportInMainReport(individualReport, generationReport);
        });
        return generationReport;
    }

    private List<? extends Element> buildNonVoidMethodsElementsList(Element annotatedElement,
                                                                    ProcessingEnvironment processingEnv) {
        return ElementKind.RECORD.equals((processingEnv.getTypeUtils().asElement(annotatedElement.asType())).getKind())
                ? processingEnv.getElementUtils().getAllMembers(
                        (TypeElement) processingEnv.getTypeUtils().asElement(annotatedElement.asType())
                ).stream()
                .filter(element -> ElementKind.METHOD.equals(element.getKind()))
                .filter(element -> (!TypeKind.VOID.equals(((ExecutableElement) element).getReturnType().getKind())))
                .filter(element -> asList(METHODS_TO_EXCLUDE).stream().noneMatch(excludedMeth ->
                        element.toString().contains(excludedMeth + OPENING_PARENTHESIS))) // exclude known Object methds
                .filter(element -> ((ExecutableElement) element).getParameters().isEmpty()) // only methods with no params
                .toList()
                : processingEnv.getElementUtils().getAllMembers(
                        (TypeElement) processingEnv.getTypeUtils().asElement(annotatedElement.asType())
                ).stream()
                .filter(element -> ElementKind.METHOD.equals(element.getKind()))
                .filter(element -> element.getSimpleName().toString().startsWith(GET) || element.getSimpleName().toString().startsWith(IS))
                .filter(element -> asList(METHODS_TO_EXCLUDE).stream().noneMatch(excludedMeth ->
                        element.toString().contains(excludedMeth + OPENING_PARENTHESIS)))
                .filter(element -> ((ExecutableElement) element).getParameters().isEmpty())
                .toList();
    }

    // TODO add the annot string/name of the annotation being processed
    private Map<String, String> performGeneration(Element annotatedElement,
                                                  List<? extends Element> nonVoidMethodsElementsList,
                                                  ProcessingEnvironment processingEnv,
                                                  boolean isSuperRecord) { // TODO check abv, might no longer be needed
        var generationReport = new HashMap<String, String>();
        var annotatedTypeElement = (TypeElement) processingEnv.getTypeUtils().asElement(annotatedElement.asType());
        var generatedQualifiedClassName = isSuperRecord
                ? constructSuperRecordQualifiedNameBasedOnElementType(annotatedTypeElement)
                : constructImmutableQualifiedNameBasedOnElementType(annotatedTypeElement);
        try {
            writeRecordSourceFile(annotatedElement, generatedQualifiedClassName, nonVoidMethodsElementsList, processingEnv);
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
                                       ProcessingEnvironment processingEnv) throws IOException {
        var recordClassFile = processingEnv.getFiler().createSourceFile(generatedQualifiedClassName); // if file already exists, this line throws a FilerException
        var recordClassString = buildRecordClassContent(annotatedElement, generatedQualifiedClassName, nonVoidMethodsElementsList);
        try (var out = new PrintWriter(recordClassFile.openWriter())) {
            out.println(recordClassString);
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