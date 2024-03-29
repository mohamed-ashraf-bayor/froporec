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

import org.froporec.generator.helpers.CodeGenerator;
import org.froporec.generator.helpers.CustomConstructorGenerator;
import org.froporec.generator.helpers.FactoryMethodsGenerator;
import org.froporec.generator.helpers.FieldsGenerator;
import org.froporec.generator.helpers.FieldsNamesConstantsGenerator;
import org.froporec.generator.helpers.JavaxGeneratedGenerator;
import org.froporec.generator.helpers.SuperInterfacesGenerator;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.lang.String.format;
import static org.froporec.extractor.AnnotationInfoExtractor.extractAllElementsTypesToConvert;
import static org.froporec.extractor.AnnotationInfoExtractor.extractMergeWithElementsListByAnnotatedElement;
import static org.froporec.extractor.AnnotationInfoExtractor.extractSuperInterfacesListByAnnotatedElement;
import static org.froporec.generator.helpers.CodeGenerator.ANNOTATED_ELEMENT;
import static org.froporec.generator.helpers.CodeGenerator.IS_SUPER_RECORD;
import static org.froporec.generator.helpers.CodeGenerator.NON_VOID_METHODS_ELEMENTS_LIST;

/**
 * Builds the record class string content and writes it to the generated record source file
 */
public final class FroporecRecordSourceFileGenerator implements RecordSourceFileGenerator {

    private final ProcessingEnvironment processingEnv;

    private final Map<String, Set<Element>> allElementsTypesToConvertByAnnotation;

    private final Map<String, Map<Element, List<Element>>> superInterfacesListByAnnotatedElementAndByAnnotation;

    private final Map<String, Map<Element, List<Element>>> mergeWithListByAnnotatedElementAndByAnnotation;

    private final CodeGenerator javaxGeneratedGenerator;

    private final CodeGenerator fieldsGenerator;

    private final CodeGenerator superInterfacesGenerator;

    private final CodeGenerator customConstructorGenerator;

    private final CodeGenerator fieldsNamesConstantsGenerator;

    private final CodeGenerator factoryMethodsGenerator;

    /**
     * RecordSourceFileGenerator constructor. Instantiates needed instances of {@link FieldsGenerator}, {@link SuperInterfacesGenerator},
     * {@link CustomConstructorGenerator} and {@link JavaxGeneratedGenerator}
     *
     * @param processingEnv                    {@link ProcessingEnvironment} object, needed to access low-level information regarding the used annotations
     * @param allAnnotatedElementsByAnnotation {@link Map} of all annotated elements. The Map content (key/value) structure is organized as:<br>
     *                                         String = annotation toString representation,<br>
     *                                         Element = the annotated class or record,<br>
     *                                         String = the attribute name,<br>
     *                                         List&#60;Element&#62; = list of all elements specified as values of: alsoConvert, superInterfaces,...
     */
    public FroporecRecordSourceFileGenerator(ProcessingEnvironment processingEnv, Map<String, Map<Element, Map<String, List<Element>>>> allAnnotatedElementsByAnnotation) {
        this.processingEnv = processingEnv;
        this.allElementsTypesToConvertByAnnotation = extractAllElementsTypesToConvert(this.processingEnv, allAnnotatedElementsByAnnotation);
        this.superInterfacesListByAnnotatedElementAndByAnnotation = extractSuperInterfacesListByAnnotatedElement(this.processingEnv, this.allElementsTypesToConvertByAnnotation, allAnnotatedElementsByAnnotation);
        this.mergeWithListByAnnotatedElementAndByAnnotation = extractMergeWithElementsListByAnnotatedElement(this.processingEnv, this.allElementsTypesToConvertByAnnotation, allAnnotatedElementsByAnnotation);
        this.javaxGeneratedGenerator = new JavaxGeneratedGenerator();
        this.fieldsGenerator = new FieldsGenerator(this.processingEnv, this.allElementsTypesToConvertByAnnotation, this.mergeWithListByAnnotatedElementAndByAnnotation);
        this.superInterfacesGenerator = new SuperInterfacesGenerator(this.superInterfacesListByAnnotatedElementAndByAnnotation);
        this.customConstructorGenerator = new CustomConstructorGenerator(this.processingEnv, this.allElementsTypesToConvertByAnnotation, this.mergeWithListByAnnotatedElementAndByAnnotation);
        this.fieldsNamesConstantsGenerator = new FieldsNamesConstantsGenerator(this.processingEnv, this.allElementsTypesToConvertByAnnotation);
        this.factoryMethodsGenerator = new FactoryMethodsGenerator(this.processingEnv, this.allElementsTypesToConvertByAnnotation);
    }

    @Override
    public String generateRecordClassContent(Element annotatedElement,
                                             String generatedQualifiedClassName,
                                             List<? extends Element> nonVoidMethodsElementsList,
                                             boolean isSuperRecord) {
        var recordClassContent = new StringBuilder();
        var annotatedTypeElement = (TypeElement) processingEnv.getTypeUtils().asElement(annotatedElement.asType());
        var qualifiedClassName = annotatedTypeElement.getQualifiedName().toString();
        int lastDot = qualifiedClassName.lastIndexOf(DOT);
        var recordSimpleClassName = generatedQualifiedClassName.substring(lastDot + 1);
        // package statement
        var packageName = lastDot > 0 ? qualifiedClassName.substring(0, lastDot) : null;
        Optional.ofNullable(packageName).ifPresent(name -> recordClassContent.append(format("%s %s;%n%n", PACKAGE, name)));
        // javax.annotation.processing.Generated section
        javaxGeneratedGenerator.generateCode(recordClassContent, Map.of());
        // record definition statement: public record ... with all attributes listed
        recordClassContent.append(format("%s %s ", PUBLIC, RECORD.toLowerCase()));
        recordClassContent.append(recordSimpleClassName);
        // list all attributes next to the record name
        recordClassContent.append(OPENING_PARENTHESIS);
        fieldsGenerator.generateCode(recordClassContent, Map.of(ANNOTATED_ELEMENT, annotatedElement, NON_VOID_METHODS_ELEMENTS_LIST, nonVoidMethodsElementsList, IS_SUPER_RECORD, isSuperRecord));
        recordClassContent.append(CLOSING_PARENTHESIS + SPACE);
        // list all provided superinterfaces
        superInterfacesGenerator.generateCode(recordClassContent, Map.of(ANNOTATED_ELEMENT, annotatedElement, IS_SUPER_RECORD, isSuperRecord));
        recordClassContent.append(SPACE + OPENING_BRACE + NEW_LINE);
        // fields names constants declarations
        fieldsNamesConstantsGenerator.generateCode(recordClassContent, Map.of(NON_VOID_METHODS_ELEMENTS_LIST, nonVoidMethodsElementsList, IS_SUPER_RECORD, isSuperRecord));
        // custom 1-arg constructor statement
        customConstructorGenerator.generateCode(recordClassContent, Map.of(ANNOTATED_ELEMENT, annotatedElement, NON_VOID_METHODS_ELEMENTS_LIST, nonVoidMethodsElementsList, IS_SUPER_RECORD, isSuperRecord));
        // static & instance factory methods
        factoryMethodsGenerator.generateCode(recordClassContent, Map.of(ANNOTATED_ELEMENT, annotatedElement, NON_VOID_METHODS_ELEMENTS_LIST, nonVoidMethodsElementsList, IS_SUPER_RECORD, isSuperRecord));
        // no additional content: close the body of the class
        recordClassContent.append(CLOSING_BRACE);
        return recordClassContent.toString();
    }
}