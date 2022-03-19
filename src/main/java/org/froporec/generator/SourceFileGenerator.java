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

    String buildRecordClassContent(String qualifiedClassName, String generatedQualifiedClassName, List<? extends Element> nonVoidMethodsElementsList);

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

    private Map<String, String> performGeneration(Element annotatedElement,
                                                  List<? extends Element> nonVoidMethodsElementsList,
                                                  ProcessingEnvironment processingEnv,
                                                  boolean isSuperRecord) {
        var generationReport = new HashMap<String, String>();
        var annotatedTypeElement = (TypeElement) processingEnv.getTypeUtils().asElement(annotatedElement.asType());
        var qualifiedClassName = annotatedTypeElement.getQualifiedName().toString();
        var generatedQualifiedClassName = isSuperRecord
                ? constructSuperRecordQualifiedNameBasedOnElementType(annotatedTypeElement)
                : constructImmutableQualifiedNameBasedOnElementType(annotatedTypeElement);
        try {
            writeRecordSourceFile(qualifiedClassName, generatedQualifiedClassName, nonVoidMethodsElementsList, processingEnv);
            generationReport.put(SUCCESS, generatedQualifiedClassName);
        } catch (FilerException e) {
            // File was already generated - do nothing
        } catch (IOException e) {
            generationReport.put(FAILURE, generatedQualifiedClassName);
        }
        return generationReport;
    }

    private void writeRecordSourceFile(String qualifiedClassName,
                                       String generatedQualifiedClassName,
                                       List<? extends Element> nonVoidMethodsElementsList,
                                       ProcessingEnvironment processingEnv) throws IOException {
        var recordClassFile = processingEnv.getFiler().createSourceFile(generatedQualifiedClassName); // if file already exists, this line throws a FilerException
        var recordClassString = buildRecordClassContent(qualifiedClassName, generatedQualifiedClassName, nonVoidMethodsElementsList);
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