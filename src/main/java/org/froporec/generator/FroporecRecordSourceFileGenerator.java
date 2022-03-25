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

import org.froporec.generator.helpers.CodeGenerator;
import org.froporec.generator.helpers.CustomConstructorGenerator;
import org.froporec.generator.helpers.FieldsGenerator;
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

    private final ProcessingEnvironment processingEnvironment;

    private final Map<String, Set<Element>> allElementsTypesToConvertByAnnotation;

    private final Map<String, Map<Element, List<Element>>> superInterfacesListByAnnotatedElementAndByAnnotation;

    private final Map<String, Map<Element, List<Element>>> mergeWithListByAnnotatedElementAndByAnnotation;

    private final CodeGenerator javaxGeneratedGenerator;

    private final CodeGenerator fieldsGenerator;

    private final CodeGenerator superInterfacesGenerator;

    private final CodeGenerator customConstructorGenerator;

    /**
     * RecordSourceFileGenerator constructor. Instantiates needed instances of {@link FieldsGenerator}, {@link CustomConstructorGenerator} and {@link JavaxGeneratedGenerator}
     *
     * @param processingEnvironment            {@link ProcessingEnvironment} object, needed to access low-level information regarding the used annotations
     * @param allAnnotatedElementsByAnnotation @{@link Map} of all annotated elements. The Map content (key/value) structure is organized as:
     *                                         String = annotation toString representation,
     *                                         Element = the annotated class or record,
     *                                         String = the attribute name,
     *                                         List<Element> = list of all elements specified as values of: alsoConvert, includeTypes,...
     */
    public FroporecRecordSourceFileGenerator(ProcessingEnvironment processingEnvironment, Map<String, Map<Element, Map<String, List<Element>>>> allAnnotatedElementsByAnnotation) {
        this.processingEnvironment = processingEnvironment;
        this.allElementsTypesToConvertByAnnotation = extractAllElementsTypesToConvert(this.processingEnvironment, allAnnotatedElementsByAnnotation);
        this.superInterfacesListByAnnotatedElementAndByAnnotation = extractSuperInterfacesListByAnnotatedElement(this.processingEnvironment, this.allElementsTypesToConvertByAnnotation, allAnnotatedElementsByAnnotation);
        this.mergeWithListByAnnotatedElementAndByAnnotation = extractMergeWithElementsListByAnnotatedElement(this.processingEnvironment, this.allElementsTypesToConvertByAnnotation, allAnnotatedElementsByAnnotation);
        this.fieldsGenerator = new FieldsGenerator(this.processingEnvironment, this.allElementsTypesToConvertByAnnotation);
        this.superInterfacesGenerator = new SuperInterfacesGenerator(this.superInterfacesListByAnnotatedElementAndByAnnotation);
        this.customConstructorGenerator = new CustomConstructorGenerator(this.processingEnvironment, this.allElementsTypesToConvertByAnnotation, this.mergeWithListByAnnotatedElementAndByAnnotation);
        this.javaxGeneratedGenerator = new JavaxGeneratedGenerator();
    }

    @Override
    public String generateRecordClassContent(Element annotatedElement,
                                             String generatedQualifiedClassName,
                                             List<? extends Element> nonVoidMethodsElementsList,
                                             boolean isSuperRecord) {
        var recordClassContent = new StringBuilder();
        var annotatedTypeElement = (TypeElement) processingEnvironment.getTypeUtils().asElement(annotatedElement.asType());
        var qualifiedClassName = annotatedTypeElement.getQualifiedName().toString();
        int lastDot = qualifiedClassName.lastIndexOf(DOT);
        var recordSimpleClassName = generatedQualifiedClassName.substring(lastDot + 1);
        // package statement
        var packageName = lastDot > 0 ? qualifiedClassName.substring(0, lastDot) : null;
        Optional.ofNullable(packageName).ifPresent(name -> recordClassContent.append(format("package %s;%n%n", name)));
        // javax.annotation.processing.Generated section
        javaxGeneratedGenerator.generateCode(recordClassContent, Map.of());
        // record definition statement: public record ... with all attributes listed
        recordClassContent.append(format("public %s ", RECORD.toLowerCase()));
        recordClassContent.append(recordSimpleClassName);
        // list all attributes next to the record name
        recordClassContent.append(OPENING_PARENTHESIS);
        fieldsGenerator.generateCode(recordClassContent, Map.of(NON_VOID_METHODS_ELEMENTS_LIST, nonVoidMethodsElementsList));
        recordClassContent.append(CLOSING_PARENTHESIS + SPACE);
        // list all provided superinterfaces
        superInterfacesGenerator.generateCode(recordClassContent, Map.of(ANNOTATED_ELEMENT, annotatedElement));
        recordClassContent.append(SPACE + OPENING_BRACE + NEW_LINE);
        // Custom 1 arg constructor statement
        customConstructorGenerator.generateCode(recordClassContent, Map.of(ANNOTATED_ELEMENT, annotatedElement, NON_VOID_METHODS_ELEMENTS_LIST, nonVoidMethodsElementsList, IS_SUPER_RECORD, isSuperRecord));
        // no additional content: close the body of the class
        recordClassContent.append(CLOSING_BRACE);
        return recordClassContent.toString();
    }
}