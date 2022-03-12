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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toSet;
import static org.froporec.generator.helpers.CodeGenerator.NON_VOID_METHODS_ELEMENTS_LIST;
import static org.froporec.generator.helpers.CodeGenerator.QUALIFIED_CLASS_NAME;

/**
 * Builds the record class string content and writes it to the generated record source file
 */
public class RecordSourceFileGenerator implements StringGenerator {

    private final ProcessingEnvironment processingEnvironment;

    private final Map<String, Set<String>> allElementsTypesToConvertByAnnotation;

    // TODO add 2 new fields here to store superInterfaces and mergeWith info

    private final CodeGenerator javaxGeneratedGenerator;

    private final CodeGenerator fieldsGenerator;

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
        // TODO add 2 new fields here to store superInterfaces and mergeWith info
        this.fieldsGenerator = new FieldsGenerator(this.processingEnvironment, this.allElementsTypesToConvertByAnnotation);
        this.customConstructorGenerator = new CustomConstructorGenerator(this.processingEnvironment, this.allElementsTypesToConvertByAnnotation);
        this.javaxGeneratedGenerator = new JavaxGeneratedGenerator();
    }

    // SuperRecord not considered in this prv methd
    private Map<String, Set<String>> buildAllElementsTypesToConvert(Map<String, Map<Element, Map<String, List<Element>>>> allAnnotatedElementsByAnnotation) {
        var allElementsTypesToConvert = new HashMap<String, Set<String>>();
        allAnnotatedElementsByAnnotation.forEach((annotationString, annotatedElementsMap) -> {
            var annotatedElementsWithAlsoConvertAndIncludeTypes = new HashSet<String>();
            annotatedElementsMap.forEach((annotatedElement, attributesMap) -> {
                annotatedElementsWithAlsoConvertAndIncludeTypes.add(processingEnvironment.getTypeUtils().asElement(annotatedElement.asType()).toString());
                annotatedElementsWithAlsoConvertAndIncludeTypes.addAll(attributesMap.get(ALSO_CONVERT_ATTRIBUTE).stream()
                        .map(element -> processingEnvironment.getTypeUtils().asElement(element.asType()).toString()).collect(toSet()));
                annotatedElementsWithAlsoConvertAndIncludeTypes.addAll(attributesMap.get(INCLUDE_TYPES_ATTRIBUTE).stream()
                        .map(element -> processingEnvironment.getTypeUtils().asElement(element.asType()).toString()).collect(toSet()));
            });
            allElementsTypesToConvert.put(annotationString, annotatedElementsWithAlsoConvertAndIncludeTypes);
        });
        return allElementsTypesToConvert;
    }

    public void generateRecord() {

    }

    public void generateImmutable() {

    }

    public void generateSuperRecord() {

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
    public void writeRecordSourceFile(String qualifiedClassName, String generatedQualifiedClassName, List<? extends Element> nonVoidMethodsElementsList) throws IOException {
        // TODO check superrecord case
        var recordClassFile = processingEnvironment.getFiler().createSourceFile(generatedQualifiedClassName); // if file already exists, this line throws a FilerException
        var recordClassString = buildRecordClassContent(qualifiedClassName, generatedQualifiedClassName, nonVoidMethodsElementsList);
        try (var out = new PrintWriter(recordClassFile.openWriter())) {
            out.println(recordClassString);
        }
    }

    private String buildRecordClassContent(String qualifiedClassName, String generatedQualifiedClassName, List<? extends Element> nonVoidMethodsElementsList) {
        var recordClassContent = new StringBuilder();
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
        recordClassContent.append(CLOSING_PARENTHESIS + SPACE + OPENING_BRACE + "\n"); // TODO around space and openeing-brace add super/parent-interfaces list if presnt
        // Custom 1 arg constructor statement
        customConstructorGenerator.generateCode(recordClassContent, Map.of(QUALIFIED_CLASS_NAME, qualifiedClassName, NON_VOID_METHODS_ELEMENTS_LIST, nonVoidMethodsElementsList));
        // no additional content: close the body of the class
        recordClassContent.append(CLOSING_BRACE);
        return recordClassContent.toString();
    }

    private void processAnnotatedElement(Element annotatedElement, RecordSourceFileGenerator recordSourceFileGenerator) {
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
            // TODO wghat abt SuperRecord ..and Pojo ?
            recordSourceFileGenerator.writeRecordSourceFile(qualifiedClassName, generatedQualifiedClassName, nonVoidMethodsElementsList);
            log.info(() -> format(GENERATION_SUCCESS_MSG_FORMAT, generatedQualifiedClassName));
        } catch (FilerException e) {
            // File was already generated - do nothing
        } catch (IOException e) {
            log.log(Level.SEVERE, format(GENERATION_FAILURE_MSG_FORMAT, generatedQualifiedClassName), e);
        }
    }
}