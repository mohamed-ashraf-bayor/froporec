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
import org.froporec.extractors.FroporecAnnotationInfoExtractor;
import org.froporec.generator.RecordSourceFileGenerator;
import org.froporec.generator.helpers.StringGenerator;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.FilerException;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.froporec.generator.helpers.StringGenerator.ORG_FROPOREC_GENERATE_IMMUTABLE;
import static org.froporec.generator.helpers.StringGenerator.ORG_FROPOREC_GENERATE_RECORD;
import static org.froporec.generator.helpers.StringGenerator.ORG_FROPOREC_IMMUTABLE;
import static org.froporec.generator.helpers.StringGenerator.ORG_FROPOREC_RECORD;
import static org.froporec.generator.helpers.StringGenerator.ORG_FROPOREC_SUPER_RECORD;

/**
 * FroPoRec annotation processor class. Picks up and processes all elements (classes, fields and method params) annotated
 * with @{@link Record}, {@link Immutable} and {@link SuperRecord}.<br>
 * The order of processing is: classes, then fields and then the method parameters<br>
 * For each annotated element a fully immutable Record class is generated. If the generated class already exists (in case the
 * corresponding pojo or record has been annotated more than once), the generation process will be skipped
 */
@SupportedAnnotationTypes({ORG_FROPOREC_RECORD, ORG_FROPOREC_SUPER_RECORD, ORG_FROPOREC_IMMUTABLE, ORG_FROPOREC_GENERATE_RECORD, ORG_FROPOREC_GENERATE_IMMUTABLE})
@SupportedSourceVersion(SourceVersion.RELEASE_17)
@AutoService(Processor.class)
public class FroporecAnnotationProcessor extends AbstractProcessor implements StringGenerator {

    private final Logger log = Logger.getLogger(FroporecAnnotationProcessor.class.getName());

    @Override
    public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnvironment) {
        var allAnnotatedElements = new HashSet<Element>();
        for (var annotation : annotations) {
            if (!annotation.toString().contains(ORG_FROPOREC_GENERATE_RECORD) && !annotation.toString().contains(ORG_FROPOREC_GENERATE_IMMUTABLE)
                    && !annotation.toString().contains(ORG_FROPOREC_RECORD) && !annotation.toString().contains(ORG_FROPOREC_IMMUTABLE)
                    && !annotation.toString().contains(ORG_FROPOREC_SUPER_RECORD)) {
                continue;
            }
            allAnnotatedElements.addAll(roundEnvironment.getElementsAnnotatedWith(annotation));
        }
        var allAnnotatedElementsToProcess = new FroporecAnnotationInfoExtractor(processingEnv).extractAnnotatedElements(allAnnotatedElements);
        var recordSourceFileGenerator = new RecordSourceFileGenerator(processingEnv, allAnnotatedElementsToProcess);
        allAnnotatedElementsToProcess.forEach(annotatedElement -> processAnnotatedElement(annotatedElement, recordSourceFileGenerator));
        return true;
    }

    private void processAnnotatedElement(final Element annotatedElement, final RecordSourceFileGenerator recordSourceFileGenerator) {
        var nonVoidMethodsElementsList = ElementKind.RECORD.equals((processingEnv.getTypeUtils().asElement(annotatedElement.asType())).getKind())
                ? processingEnv.getElementUtils().getAllMembers((TypeElement) processingEnv.getTypeUtils().asElement(annotatedElement.asType())).stream()
                .filter(element -> ElementKind.METHOD.equals(element.getKind()))
                .filter(element -> (!TypeKind.VOID.equals(((ExecutableElement) element).getReturnType().getKind())))
                .filter(element -> asList(METHODS_TO_EXCLUDE).stream().noneMatch(excludedMeth -> element.toString().contains(excludedMeth + OPENING_PARENTHESIS)))
                .toList()
                : processingEnv.getElementUtils().getAllMembers((TypeElement) processingEnv.getTypeUtils().asElement(annotatedElement.asType())).stream()
                .filter(element -> ElementKind.METHOD.equals(element.getKind()))
                .filter(element -> element.getSimpleName().toString().startsWith(GET) || element.getSimpleName().toString().startsWith(IS))
                .filter(element -> asList(METHODS_TO_EXCLUDE).stream().noneMatch(excludedMeth -> element.toString().contains(excludedMeth + OPENING_PARENTHESIS)))
                .toList();
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