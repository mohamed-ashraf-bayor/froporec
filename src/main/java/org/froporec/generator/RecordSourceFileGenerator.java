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
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.lang.String.format;
import static java.util.stream.Collectors.toSet;
import static org.froporec.generator.helpers.CodeGenerator.NON_VOID_METHODS_ELEMENTS_LIST;
import static org.froporec.generator.helpers.CodeGenerator.ANNOTATED_ELEMENT;

/**
 * Builds the record class string content and writes it to the generated record source file
 */
public final class RecordSourceFileGenerator implements SourceFileGenerator {

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
    public RecordSourceFileGenerator(ProcessingEnvironment processingEnvironment, Map<String, Map<Element, Map<String, List<Element>>>> allAnnotatedElementsByAnnotation) {
        this.processingEnvironment = processingEnvironment;
        this.allElementsTypesToConvertByAnnotation = buildAllElementsTypesToConvert(allAnnotatedElementsByAnnotation);
        this.superInterfacesListByAnnotatedElementAndByAnnotation = buildSuperInterfacesListByAnnotatedElement(this.allElementsTypesToConvertByAnnotation, allAnnotatedElementsByAnnotation);
        this.mergeWithListByAnnotatedElementAndByAnnotation = buildMergeWithElementsListByAnnotatedElement(this.allElementsTypesToConvertByAnnotation, allAnnotatedElementsByAnnotation);
        this.fieldsGenerator = new FieldsGenerator(this.processingEnvironment, this.allElementsTypesToConvertByAnnotation, this.mergeWithListByAnnotatedElementAndByAnnotation);
        this.superInterfacesGenerator = new SuperInterfacesGenerator(this.processingEnvironment, this.superInterfacesListByAnnotatedElementAndByAnnotation);
        this.customConstructorGenerator = new CustomConstructorGenerator(this.processingEnvironment, this.allElementsTypesToConvertByAnnotation, this.mergeWithListByAnnotatedElementAndByAnnotation);
        this.javaxGeneratedGenerator = new JavaxGeneratedGenerator();
    }

    private Map<String, Set<Element>> buildAllElementsTypesToConvert(Map<String, Map<Element, Map<String, List<Element>>>> allAnnotatedElementsByAnnotation) {
        var allElementsTypesToConvert = new HashMap<String, Set<Element>>(); // String = annotation toString value , Element = Element instance of the annotated element type
        allAnnotatedElementsByAnnotation.forEach((annotationString, annotatedElementsMap) -> {
            var annotatedElementsWithAlsoConvertAndIncludeTypes = new HashSet<Element>();
            annotatedElementsMap.forEach((annotatedElement, attributesMap) -> {
                if (isAnnotatedAsExpected(annotationString, annotatedElement)) {
                    annotatedElementsWithAlsoConvertAndIncludeTypes.add(processingEnvironment.getTypeUtils().asElement(annotatedElement.asType())); // TODO ADD SKIPPIN LOGIC HERE - ONLY 1LINE
                    annotatedElementsWithAlsoConvertAndIncludeTypes.addAll(attributesMap.get(ALSO_CONVERT_ATTRIBUTE).stream()
                            .map(element -> processingEnvironment.getTypeUtils().asElement(element.asType())).collect(toSet()));
                    annotatedElementsWithAlsoConvertAndIncludeTypes.addAll(attributesMap.get(INCLUDE_TYPES_ATTRIBUTE).stream()
                            .map(element -> processingEnvironment.getTypeUtils().asElement(element.asType())).collect(toSet()));
                }
            });
            allElementsTypesToConvert.put(annotationString, annotatedElementsWithAlsoConvertAndIncludeTypes);
        });
        return allElementsTypesToConvert;
    }

    private boolean isAnnotatedAsExpected(String annotationString, Element annotatedElement) {
        boolean isAClass = ElementKind.CLASS.equals(processingEnvironment.getTypeUtils().asElement(annotatedElement.asType()).getKind());
        boolean isARecord = ElementKind.RECORD.equals(processingEnvironment.getTypeUtils().asElement(annotatedElement.asType()).getKind());
        return (ORG_FROPOREC_RECORD.equals(annotationString) && isAClass)
                || (ORG_FROPOREC_GENERATE_RECORD.equals(annotationString) && isAClass)
                || (ORG_FROPOREC_IMMUTABLE.equals(annotationString) && isARecord)
                || (ORG_FROPOREC_GENERATE_IMMUTABLE.equals(annotationString) && isARecord)
                || (ORG_FROPOREC_SUPER_RECORD.equals(annotationString) && (isAClass || isARecord));
    }

    private Map<String, Map<Element, List<Element>>> buildSuperInterfacesListByAnnotatedElement(Map<String, Set<Element>> allElementsTypesToConvertByAnnotation,
                                                                                                Map<String, Map<Element, Map<String, List<Element>>>> allAnnotatedElementsByAnnotation) {
        var superInterfacesListByAnnotatedElementAndByAnnotation = new HashMap<String, Map<Element, List<Element>>>();
        // (generics above) String = annotation toString value , Element = Element instance of the annotated element type, List<Element> = list of provided superinterfaces Element instances
        allAnnotatedElementsByAnnotation.forEach((annotationString, annotatedElementsMap) -> { // TODO if annttd elmnt belongs to allElementsTypesToConvertByAnnotation

            var annotatedElementsWithSuperInterfacesMap = new HashMap<Element, List<Element>>();
            annotatedElementsMap.forEach((annotatedElement, attributesMap) ->
                    annotatedElementsWithSuperInterfacesMap.put(annotatedElement, attributesMap.get(SUPER_INTERFACES_ATTRIBUTE).stream()
                            .map(element -> processingEnvironment.getTypeUtils().asElement(element.asType())).toList()));
            superInterfacesListByAnnotatedElementAndByAnnotation.put(annotationString, annotatedElementsWithSuperInterfacesMap);
        });
        return superInterfacesListByAnnotatedElementAndByAnnotation;
    }

    private Map<String, Map<Element, List<Element>>> buildMergeWithElementsListByAnnotatedElement(Map<String, Set<Element>> allElementsTypesToConvertByAnnotation,
                                                                                                  Map<String, Map<Element, Map<String, List<Element>>>> allAnnotatedElementsByAnnotation) {
        var mergeWithListByAnnotatedElementAndByAnnotation = new HashMap<String, Map<Element, List<Element>>>();
        // (generics above) String = annotation toString value , Element = Element instance of the annotated element type, List<Element> = list of provided mergeWith Element instances
        allAnnotatedElementsByAnnotation.forEach((annotationString, annotatedElementsMap) -> { // TODO if annttd elmnt belongs to allElementsTypesToConvertByAnnotation
            var annotatedElementsWithMergeWithElementsMap = new HashMap<Element, List<Element>>();
            annotatedElementsMap.forEach((annotatedElement, attributesMap) ->
                    annotatedElementsWithMergeWithElementsMap.put(annotatedElement, attributesMap.get(MERGE_WITH_ATTRIBUTE).stream()
                            .map(element -> processingEnvironment.getTypeUtils().asElement(element.asType())).toList()));
            mergeWithListByAnnotatedElementAndByAnnotation.put(annotationString, annotatedElementsWithMergeWithElementsMap);
        });
        return mergeWithListByAnnotatedElementAndByAnnotation;
    }

    @Override
    public String buildRecordClassContent(Element annotatedElement,
                                          String generatedQualifiedClassName,
                                          List<? extends Element> nonVoidMethodsElementsList) {
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
        fieldsGenerator.generateCode(recordClassContent, Map.of(ANNOTATED_ELEMENT, annotatedElement, NON_VOID_METHODS_ELEMENTS_LIST, nonVoidMethodsElementsList));
        recordClassContent.append(CLOSING_PARENTHESIS + SPACE);
        superInterfacesGenerator.generateCode(recordClassContent, Map.of(ANNOTATED_ELEMENT, annotatedElement));
        recordClassContent.append(OPENING_BRACE + NEW_LINE);
        // Custom 1 arg constructor statement
        customConstructorGenerator.generateCode(recordClassContent, Map.of(ANNOTATED_ELEMENT, annotatedElement, NON_VOID_METHODS_ELEMENTS_LIST, nonVoidMethodsElementsList));
        // no additional content: close the body of the class
        recordClassContent.append(CLOSING_BRACE);
        return recordClassContent.toString();
    }
}