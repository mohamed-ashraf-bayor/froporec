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
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.froporec.generator.helpers.StringGenerator.constructImmutableQualifiedNameBasedOnElementType;

public sealed interface SourceFileGenerator extends StringGenerator permits RecordSourceFileGenerator {

    String buildRecordClassContent(String qualifiedClassName, String generatedQualifiedClassName, List<? extends Element> nonVoidMethodsElementsList);

    default Map<String, List<String>> generateForRecordAnnotatedElements(List<Element> elementsListToProcess,
                                                                         ProcessingEnvironment processingEnv) {
        Map<String, List<String>> generationReport = Map.of(SUCCESS, new ArrayList<>(), FAILURE, new ArrayList<>());
        elementsListToProcess.forEach(annotatedElement ->
                performGeneration(generationReport, annotatedElement, buildNonVoidMethodsElementsList(annotatedElement, processingEnv), processingEnv));
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
            var nonVoidMethodsElements = new ArrayList<Element>(buildNonVoidMethodsElementsList(annotatedElement, processingEnv)); // TODO make this a Map<theAnnotatedElmnt, theMEthodsLists> instead
            // TODO do renameing by element name not type -> will reduce issue while calling canonical cnstructor
            nonVoidMethodsElements.addAll(mergeWithElementsList.stream()
                    .flatMap(element -> buildNonVoidMethodsElementsList(element, processingEnv).stream())
                    .toList());
            // TODO do the duplicate check/rename process here // NOT HERE DO THAT IN FIELDS & CNSTRCTORS GEN...
            performGeneration(generationReport, annotatedElement, nonVoidMethodsElements, processingEnv);
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
                        element.toString().contains(excludedMeth + OPENING_PARENTHESIS)))
                .toList()
                : processingEnv.getElementUtils().getAllMembers(
                        (TypeElement) processingEnv.getTypeUtils().asElement(annotatedElement.asType())
                ).stream()
                .filter(element -> ElementKind.METHOD.equals(element.getKind()))
                .filter(element -> element.getSimpleName().toString().startsWith(GET) || element.getSimpleName().toString().startsWith(IS))
                .filter(element -> asList(METHODS_TO_EXCLUDE).stream().noneMatch(excludedMeth ->
                        element.toString().contains(excludedMeth + OPENING_PARENTHESIS)))
                .toList();
    }

    private void performGeneration(Map<String, List<String>> generationReport,
                                   Element annotatedElement,
                                   List<? extends Element> nonVoidMethodsElementsList,
                                   ProcessingEnvironment processingEnv) {
        var annotatedTypeElement = (TypeElement) processingEnv.getTypeUtils().asElement(annotatedElement.asType());
        var qualifiedClassName = annotatedTypeElement.getQualifiedName().toString();
        var generatedQualifiedClassName = constructImmutableQualifiedNameBasedOnElementType(annotatedTypeElement);
        try {
            writeRecordSourceFile(qualifiedClassName, generatedQualifiedClassName, nonVoidMethodsElementsList, processingEnv);
            generationReport.get(SUCCESS).add(generatedQualifiedClassName);
        } catch (FilerException e) {
            // File was already generated - do nothing
        } catch (IOException e) {
            generationReport.get(FAILURE).add(generatedQualifiedClassName);
        }
    }

    /**
     * Builds the content of the record class to be generated and writes it to the filesystem
     *
     * @param qualifiedClassName          qualified name of the pojo or record class being processed
     * @param generatedQualifiedClassName qualified name of the record class to be generated
     * @param nonVoidMethodsElementsList  {@link List} of {@link Element} instances of public getters of the POJO class, or public methods of
     *                                    the Record class being processed.
     *                                    toString representation ex for a POJO: [getLastname(), getAge(), getMark(), getGrade(), getSchool()]
     * @throws IOException only if a "severe" error happens while writing the file to the filesystem. Cases of already existing files are not treated as errors
     */
    private void writeRecordSourceFile(String qualifiedClassName,
                                       String generatedQualifiedClassName,
                                       List<? extends Element> nonVoidMethodsElementsList,
                                       ProcessingEnvironment processingEnv) throws IOException {
        // TODO check superrecord case
        var recordClassFile = processingEnv.getFiler().createSourceFile(generatedQualifiedClassName); // if file already exists, this line throws a FilerException
        var recordClassString = buildRecordClassContent(qualifiedClassName, generatedQualifiedClassName, nonVoidMethodsElementsList);
        try (var out = new PrintWriter(recordClassFile.openWriter())) {
            out.println(recordClassString);
        }
    }
}