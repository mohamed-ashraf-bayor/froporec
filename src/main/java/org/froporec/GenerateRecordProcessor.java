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

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.FilerException;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ExecutableType;
import java.io.IOException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.lang.String.format;

/**
 * FroPoRec annotation processor class. Picks up and processes all elements (classes, fields and method params) annotated with @{@link GenerateRecord}<br>
 * The order of processing is: classes, then fields and then the method parameters<br>
 * For each element a Record class is generated. If the generated class already exists (in case the corresponding pojo has been annotated more than once),
 * the generation process will be skipped
 */
@SupportedAnnotationTypes("org.froporec.GenerateRecord")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
@AutoService(Processor.class)
public class GenerateRecordProcessor extends AbstractProcessor {

    private final Logger log = Logger.getLogger(GenerateRecordProcessor.class.getName());

    @Override
    public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnvironment) {
        for (var annotation : annotations) {
            if (!annotation.getSimpleName().toString().contains("GenerateRecord")) {
                continue;
            }
            var allAnnotatedElements = roundEnvironment.getElementsAnnotatedWith(annotation);
            processAnnotatedClasses(allAnnotatedElements); // process all annotated classes
            processAnnotatedFields(allAnnotatedElements); // process all annotated class fields
            processAnnotatedMethodParams(allAnnotatedElements); // process all annotated method parameters
        }
        return true;
    }

    private void processAnnotatedClasses(final Set<? extends Element> allAnnotatedElements) {
        var annotatedClasses = allAnnotatedElements.stream()
                .filter(element -> !element.getClass().isRecord())
                .filter(element -> !element.getClass().isEnum())
                .filter(element -> !element.getClass().isSealed())
                .filter(element -> ElementKind.CLASS.equals(element.getKind()))
                .collect(Collectors.toSet());
        annotatedClasses.forEach(annotatedClass -> processAnnotatedElement(annotatedClass, allAnnotatedElements));
    }

    private void processAnnotatedFields(final Set<? extends Element> allAnnotatedElements) {
        var annotatedFields = allAnnotatedElements.stream()
                .filter(element -> ElementKind.FIELD.equals(element.getKind()))
                .filter(element -> !ElementKind.ENUM_CONSTANT.equals(element.getKind()))
                .collect(Collectors.toSet());
        annotatedFields.forEach(annotatedMethod -> processAnnotatedElement(annotatedMethod, allAnnotatedElements));
    }

    private void processAnnotatedMethodParams(final Set<? extends Element> allAnnotatedElements) {
        var annotatedParams = allAnnotatedElements.stream()
                .filter(element -> ElementKind.PARAMETER.equals(element.getKind()))
                .collect(Collectors.toSet());
        annotatedParams.forEach(annotatedParam -> processAnnotatedElement(annotatedParam, allAnnotatedElements));
    }

    private void processAnnotatedElement(final Element annotatedElement, final Set<? extends Element> allAnnotatedElements) {
        var gettersList = processingEnv.getElementUtils().getAllMembers((TypeElement) processingEnv.getTypeUtils().asElement(annotatedElement.asType())).stream()
                .filter(element -> element.getSimpleName().toString().startsWith("get") || element.getSimpleName().toString().startsWith("is"))
                .filter(element -> !element.getSimpleName().toString().startsWith("getClass"))
                .toList();
        var qualifiedClassName = ((TypeElement) processingEnv.getTypeUtils().asElement(annotatedElement.asType())).getQualifiedName().toString();
        var gettersMap = gettersList.stream().collect(Collectors.toMap(getter -> getter.getSimpleName().toString(), getter -> ((ExecutableType) getter.asType()).getReturnType().toString()));
        try {
            new RecordSourceFileGenerator(processingEnv, allAnnotatedElements).writeRecordSourceFile(qualifiedClassName, gettersList, gettersMap);
            log.info(() -> "\t> Successfully generated " + qualifiedClassName + "Record");
        } catch (FilerException e) {
            // File was already generated - do nothing
        } catch (IOException e) {
            log.log(Level.SEVERE, format("Error generating %sRecord", qualifiedClassName), e);
        }
    }
}