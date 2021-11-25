/**
 * Copyright (c) 2021 Mohamed Ashraf Bayor
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.froporec.processor;

import com.google.auto.service.AutoService;

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

/**
 * FroPoRec annotation processor class . Picks up and processes all elements (classes, fields, generic types and method params) annotated with @GenerateRecord
 * The order of processing is: classes, then fields, generic types and then the method parameters
 * For each element a Record class is generated. If the generated class already exists (in case the corresponding pojo has been annotated more than once), the generation process will be skipped
 *
 * @author Mohamed Ashraf Bayor
 */
@SupportedAnnotationTypes("org.froporec.GenerateRecord")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
@AutoService(Processor.class)
public class GenerateRecordProcessor extends AbstractProcessor {

    private final Logger log = Logger.getLogger(GenerateRecordProcessor.class.getName());

    @Override
    public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {
        for (TypeElement annotation : annotations) {
            if (!annotation.getSimpleName().toString().contains("GenerateRecord")) {
                continue;
            }
            Set<? extends Element> allAnnotatedElements = roundEnv.getElementsAnnotatedWith(annotation);
            System.out.println(">>>>>>>> annotatedElements >>>>>>>>>>>" + allAnnotatedElements);
            processAnnotatedClasses(allAnnotatedElements); // process all annotated classes
            processAnnotatedClassAttributes(allAnnotatedElements); // process all annotated class attributes (fields)
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
        System.out.println(">>>>>>>> annotatedClasses >>>>>>>>>>>" + annotatedClasses);
        annotatedClasses.forEach(annotatedClass -> processAnnotatedElement(annotatedClass, allAnnotatedElements));
    }

    private void processAnnotatedClassAttributes(final Set<? extends Element> allAnnotatedElements) {
        var annotatedClassAttributes = allAnnotatedElements.stream()
                .filter(element -> ElementKind.FIELD.equals(element.getKind()))
                .filter(element -> !ElementKind.ENUM_CONSTANT.equals(element.getKind()))
                .map(element -> processingEnv.getTypeUtils().asElement(element.asType()))
                .collect(Collectors.toSet());
        System.out.println(">>>>>>>> annotatedClassAttributes >>>>>>>>>>>" + annotatedClassAttributes);
        annotatedClassAttributes.forEach(annotatedMethod -> processAnnotatedElement(annotatedMethod, allAnnotatedElements));
    }

    private void processAnnotatedMethodParams(final Set<? extends Element> allAnnotatedElements) {
        var annotatedParams = allAnnotatedElements.stream()
                .filter(element -> ElementKind.PARAMETER.equals(element.getKind()))
                .map(element -> processingEnv.getTypeUtils().asElement(element.asType()))
                .collect(Collectors.toSet());
        System.out.println(">>>>>>>> annotatedParams >>>>>>>>>>>" + annotatedParams);
        annotatedParams.forEach(annotatedParam -> processAnnotatedElement(annotatedParam, allAnnotatedElements));
    }

    /**
     * processes the annotated element and generates the record class if the record was not already generated. if already generated then the process is skipped
     * @param annotatedElement
     * @param allAnnotatedElements
     */
    private void processAnnotatedElement(final Element annotatedElement, final Set<? extends Element> allAnnotatedElements) {
        var gettersList = processingEnv.getElementUtils().getAllMembers((TypeElement) processingEnv.getTypeUtils().asElement(annotatedElement.asType())).stream()
                .filter(element -> element.getSimpleName().toString().startsWith("get") || element.getSimpleName().toString().startsWith("is"))
                .filter(element -> !element.getSimpleName().toString().startsWith("getClass"))
                .toList();
        System.out.println(">>>>>>>>gettersList>>>>>>>>>>>" + gettersList);
        var className = ((TypeElement) processingEnv.getTypeUtils().asElement(annotatedElement.asType())).getQualifiedName().toString();
        var gettersMap = gettersList.stream().collect(Collectors.toMap(getter -> getter.getSimpleName().toString(), getter -> ((ExecutableType) getter.asType()).getReturnType().toString()));
        System.out.println(">>>>>>>>gettersMap>>>>>>>>>>>" + gettersMap);
        try {
            new RecordClassGenerator(processingEnv, allAnnotatedElements).writeRecordClassFile(className, gettersList, gettersMap);
            log.info("\t> Successfully generated " + className + "Record");
        } catch (FilerException e) {
            // log.info("Skipped generating " + className + "Record - file already exists");
        } catch (IOException e) {
            log.log(Level.SEVERE, "Error generating " + className + "Record", e);
        }
    }
}