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

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.froporec.generator.helpers.CodeGenerator.NON_VOID_METHODS_ELEMENTS_LIST;
import static org.froporec.generator.helpers.CodeGenerator.QUALIFIED_CLASS_NAME;

/**
 * Builds the record class string content and writes it to the generated record source file
 */
public class RecordSourceFileGenerator implements StringGenerator {

    private final ProcessingEnvironment processingEnvironment;

    private final Set<String> allAnnotatedElementsTypes;

    private final CodeGenerator javaxGeneratedGenerator;

    private final CodeGenerator fieldsGenerator;

    private final CodeGenerator customConstructorGenerator;

    /**
     * RecordSourceFileGenerator constructor. Instantiates needed instances of {@link FieldsGenerator}, {@link CustomConstructorGenerator} and {@link JavaxGeneratedGenerator}
     *
     * @param processingEnvironment {@link ProcessingEnvironment} object, needed to access low-level information regarding the used annotations
     * @param allAnnotatedElements  {@link Set} of all annotated {@link Element} instances
     */
    public RecordSourceFileGenerator(final ProcessingEnvironment processingEnvironment, final Set<? extends Element> allAnnotatedElements) {
        this.processingEnvironment = processingEnvironment;
        this.allAnnotatedElementsTypes = buildAllAnnotatedElementsTypes(allAnnotatedElements);
        this.fieldsGenerator = new FieldsGenerator(this.processingEnvironment, this.allAnnotatedElementsTypes);
        this.customConstructorGenerator = new CustomConstructorGenerator(this.processingEnvironment, this.allAnnotatedElementsTypes);
        this.javaxGeneratedGenerator = new JavaxGeneratedGenerator();
    }

    private Set<String> buildAllAnnotatedElementsTypes(Set<? extends Element> allAnnotatedElements) {
        return allAnnotatedElements.stream()
                .map(element -> processingEnvironment.getTypeUtils().asElement(element.asType()).toString())
                .collect(Collectors.toSet());
    }

    /**
     * Builds the content of the record class to be generated and writes it to the filesystem
     *
     * @param qualifiedClassName          qualified name of the pojo or record class being processed
     * @param generatedQualifiedClassName qualified name of the record class to be generated
     * @param nonVoidMethodsElementsList  {@link List} of {@link Element} instances of public getters of the POJO class, or public methods of
     *                                    the Record class being processed.<br>
     *                                    ex:[getLastname(), getAge(), getMark(), getGrade(), getSchool()]
     * @throws IOException only if a "severe" error happens while writing the file to the filesystem. Cases of already existing files are not treated as errors
     */
    public void writeRecordSourceFile(final String qualifiedClassName, final String generatedQualifiedClassName, final List<? extends Element> nonVoidMethodsElementsList) throws IOException {
        var recordClassFile = processingEnvironment.getFiler().createSourceFile(generatedQualifiedClassName); // if file already exists, this line throws a FilerException
        var recordClassString = buildRecordClassContent(qualifiedClassName, generatedQualifiedClassName, nonVoidMethodsElementsList);
        try (var out = new PrintWriter(recordClassFile.openWriter())) {
            out.println(recordClassString);
        }
    }

    private String buildRecordClassContent(final String qualifiedClassName, final String generatedQualifiedClassName, final List<? extends Element> nonVoidMethodsElementsList) {
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
        recordClassContent.append(CLOSING_PARENTHESIS + SPACE + OPENING_BRACE + "\n");
        // Custom 1 arg constructor statement
        customConstructorGenerator.generateCode(recordClassContent, Map.of(QUALIFIED_CLASS_NAME, qualifiedClassName, NON_VOID_METHODS_ELEMENTS_LIST, nonVoidMethodsElementsList));
        // no additional content: close the body of the class
        recordClassContent.append(CLOSING_BRACE);
        return recordClassContent.toString();
    }
}