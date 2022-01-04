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
import org.froporec.generator.RecordSourceFileGenerator;
import org.froporec.generator.helpers.StringGenerator;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.FilerException;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toSet;
import static org.froporec.generator.helpers.StringGenerator.GENERATE_IMMUTABLE_QUALIFIED_NAME;
import static org.froporec.generator.helpers.StringGenerator.GENERATE_RECORD_QUALIFIED_NAME;

/**
 * // TODO MODIFY JAVADOC
 * FroPoRec annotation processor class. Picks up and processes all elements (classes, fields and method params) annotated with @{@link GenerateRecord}<br>
 * The order of processing is: classes, then fields and then the method parameters<br>
 * For each element a Record class is generated. If the generated class already exists (in case the corresponding pojo has been annotated more than once),
 * the generation process will be skipped
 */
@SupportedAnnotationTypes({GENERATE_RECORD_QUALIFIED_NAME, GENERATE_IMMUTABLE_QUALIFIED_NAME})
@SupportedSourceVersion(SourceVersion.RELEASE_17)
@AutoService(Processor.class)
public class FroporecAnnotationProcessor extends AbstractProcessor implements StringGenerator {

    private final Logger log = Logger.getLogger(FroporecAnnotationProcessor.class.getName());

    @Override
    public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnvironment) {
        var allAnnotatedElements = new HashSet<Element>();
        for (var annotation : annotations) {
            if (!annotation.toString().contains(GENERATE_RECORD_QUALIFIED_NAME) && !annotation.toString().contains(GENERATE_IMMUTABLE_QUALIFIED_NAME)) {
                continue;
            }
            allAnnotatedElements.addAll(roundEnvironment.getElementsAnnotatedWith(annotation));
        }
        // System.out.println(">>>>>>>allAnnotatedElements: " + allAnnotatedElements);
        var allAnnotatedElementsToProcess = new HashSet<Element>();
        extractAnnotatedPojoClassesWithIncludedTypes(allAnnotatedElements, allAnnotatedElementsToProcess);
        System.out.println(">>>>>>>allAnnotatedElementsToProcess extractAnnotatedPojoClassesWithIncludedTypes: " + allAnnotatedElementsToProcess);
        extractAnnotatedRecordClassesWithIncludedTypes(allAnnotatedElements, allAnnotatedElementsToProcess);
        System.out.println(">>>>>>>allAnnotatedElementsToProcess extractAnnotatedRecordClassesWithIncludedTypes: " + allAnnotatedElementsToProcess);
        extractAnnotatedFieldsWithIncludedTypes(allAnnotatedElements, allAnnotatedElementsToProcess);
        System.out.println(">>>>>>>allAnnotatedElementsToProcess extractAnnotatedFieldsWithIncludedTypes: " + allAnnotatedElementsToProcess);
        extractAnnotatedMethodParamsWithIncludedTypes(allAnnotatedElements, allAnnotatedElementsToProcess);
        System.out.println(">>>>>>>allAnnotatedElementsToProcess extractAnnotatedMethodParamsWithIncludedTypes: " + allAnnotatedElementsToProcess);
        var recordSourceFileGenerator = new RecordSourceFileGenerator(processingEnv, allAnnotatedElementsToProcess);
        // TODO loop thru each Map<String, List<? extnds Elements>> and call recordSourceFileGenerator.generate... // chck call: processAnnotatedElement cmmntd blw
        allAnnotatedElementsToProcess.forEach(annotatedElement -> processAnnotatedElement(annotatedElement, recordSourceFileGenerator));
        return true;
    }

    private void extractAnnotatedPojoClassesWithIncludedTypes(final Set<? extends Element> allAnnotatedElements, final Set<Element> allAnnotatedElementsToProcess) {
        var annotatedPojoClasses = allAnnotatedElements.stream()
                .filter(element -> !allAnnotatedElementsToProcess.contains(element))
                .filter(element -> !element.getClass().isRecord())
                .filter(element -> !element.getClass().isEnum())
                .filter(element -> !element.getClass().isSealed())
                .filter(element -> ElementKind.CLASS.equals(element.getKind()))
                .collect(toSet());
        allAnnotatedElementsToProcess.addAll(annotatedPojoClasses);
        allAnnotatedElementsToProcess.addAll(extractIncludedTypes(annotatedPojoClasses));
    }

    private void extractAnnotatedRecordClassesWithIncludedTypes(final Set<? extends Element> allAnnotatedElements, final Set<Element> allAnnotatedElementsToProcess) {
        var annotatedRecordClasses = allAnnotatedElements.stream()
                .filter(element -> !allAnnotatedElementsToProcess.contains(element))
                .filter(element -> ElementKind.RECORD.equals(element.getKind()))
                .collect(toSet());
        allAnnotatedElementsToProcess.addAll(annotatedRecordClasses);
        allAnnotatedElementsToProcess.addAll(extractIncludedTypes(annotatedRecordClasses));
    }

    private void extractAnnotatedFieldsWithIncludedTypes(final Set<? extends Element> allAnnotatedElements, final Set<Element> allAnnotatedElementsToProcess) {
        var annotatedFields = allAnnotatedElements.stream()
                .filter(element -> !allAnnotatedElementsToProcess.contains(element))
                .filter(element -> ElementKind.FIELD.equals(element.getKind()))
                .filter(element -> !ElementKind.ENUM_CONSTANT.equals(element.getKind()))
                .collect(toSet());
        allAnnotatedElementsToProcess.addAll(annotatedFields);
        allAnnotatedElementsToProcess.addAll(extractIncludedTypes(annotatedFields));
    }

    private void extractAnnotatedMethodParamsWithIncludedTypes(final Set<? extends Element> allAnnotatedElements, final Set<Element> allAnnotatedElementsToProcess) {
        var annotatedParams = allAnnotatedElements.stream()
                .filter(element -> !allAnnotatedElementsToProcess.contains(element))
                .filter(element -> ElementKind.PARAMETER.equals(element.getKind()))
                .collect(toSet());
        allAnnotatedElementsToProcess.addAll(annotatedParams);
        allAnnotatedElementsToProcess.addAll(extractIncludedTypes(annotatedParams));
    }

    private Set<Element> extractIncludedTypes(Set<? extends Element> annotatedElement) {
        var includedTypesAsElements = new HashSet<Element>();
        annotatedElement.forEach(element -> processingEnv.getElementUtils().getAllAnnotationMirrors(element).stream()
                .filter(annotationMirror -> annotationMirror.toString().contains(GENERATE_IMMUTABLE_QUALIFIED_NAME)
                        || annotationMirror.toString().contains(GENERATE_RECORD_QUALIFIED_NAME))
                .map(AnnotationMirror::getElementValues)
                .forEach(map -> map.forEach((executableElement, annotationValue) -> { // annotationValue.getValue() sample value: com.bayor...School.class,com.bayor...Person.class
                    if (executableElement.toString().contains(INCLUDE_TYPES_ATTRIBUTE)) {
                        includedTypesAsElements.addAll(asList(annotationValue.getValue().toString().split(COMMA_SEPARATOR)).stream()
                                .map(includedTypeDotClassString -> processingEnv.getTypeUtils().asElement(processingEnv.getElementUtils().getTypeElement(includedTypeDotClassString.strip().replace(DOT_CLASS, EMPTY_STRING)).asType()))
                                .collect(toSet()));
                    }
                })));
        return includedTypesAsElements;
    }

    private void processAnnotatedElement(final Element annotatedElement, final RecordSourceFileGenerator recordSourceFileGenerator) {
        var nonVoidMethodsElementsList = ElementKind.RECORD.equals((processingEnv.getTypeUtils().asElement(annotatedElement.asType())).getKind())
                ? processingEnv.getElementUtils().getAllMembers((TypeElement) processingEnv.getTypeUtils().asElement(annotatedElement.asType())).stream()
                .filter(element -> ElementKind.METHOD.equals(element.getKind()))
                .filter(element -> asList(METHODS_TO_EXCLUDE).stream().noneMatch(excludedMeth -> element.toString().contains(excludedMeth + OPENING_PARENTHESIS)))
                .toList()
                : processingEnv.getElementUtils().getAllMembers((TypeElement) processingEnv.getTypeUtils().asElement(annotatedElement.asType())).stream()
                .filter(element -> ElementKind.METHOD.equals(element.getKind()))
                .filter(element -> element.getSimpleName().toString().startsWith("get") || element.getSimpleName().toString().startsWith("is"))
                .filter(element -> asList(METHODS_TO_EXCLUDE).stream().noneMatch(excludedMeth -> element.toString().contains(excludedMeth + OPENING_PARENTHESIS)))
                .toList();
        System.out.println("@@@@@@@@@@@@@@@@@@@@@@ nonVoidMethodsElementsList: " + nonVoidMethodsElementsList);
        var annotatedTypeElement = (TypeElement) processingEnv.getTypeUtils().asElement(annotatedElement.asType());
        var qualifiedClassName = annotatedTypeElement.getQualifiedName().toString();
        var generatedQualifiedClassName = constructImmutableQualifiedNameBasedOnElementType(annotatedTypeElement);
        try {
            recordSourceFileGenerator.writeRecordSourceFile(qualifiedClassName, generatedQualifiedClassName, nonVoidMethodsElementsList);
            log.info(() -> format(GENERATION_SUCCESS_MSG_FORMAT, generatedQualifiedClassName));
        } catch (FilerException e) {
            // File was already generated - do nothing
        } catch (IOException e) {
            log.log(Level.SEVERE, format(GENERATION_FAILURE_MSG_FORMAT, generatedQualifiedClassName), e);
        }
    }
}