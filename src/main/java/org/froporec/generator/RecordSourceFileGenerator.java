/**
 * Copyright (c) 2021 Mohamed Ashraf Bayor
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

import org.froporec.generator.helpers.CustomConstructorGenerator;
import org.froporec.generator.helpers.FieldsGenerator;
import org.froporec.generator.helpers.JavaxGeneratedGenerator;
import org.froporec.generator.helpers.CodeGenerator;

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
import static org.froporec.generator.helpers.CodeGenerator.GETTERS_LIST;
import static org.froporec.generator.helpers.CodeGenerator.GETTERS_MAP;
import static org.froporec.generator.helpers.CodeGenerator.QUALIFIED_CLASS_NAME;

/**
 * Builds the record class string content and writes it to the generated record source file
 */
public class RecordSourceFileGenerator {

    /**
     * "Record" string constant
     */
    public static final String RECORD = "Record";

    private final ProcessingEnvironment processingEnvironment;

    private final Set<String> allAnnotatedElementsTypes;

    private final CodeGenerator javaxGeneratedGenerator;

    private final CodeGenerator fieldsGenerator;

    private final CodeGenerator customConstructorGenerator;

    /**
     * Constructor of RecordSourceFileGenerator
     *
     * @param processingEnvironment the processing environment needed to getTypeUtil() and getFile() methods
     * @param allAnnotatedElements  all annotated elements in the client program
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
     * Builds the content of the record class and writes it to the filesystem
     *
     * @param qualifiedClassName qualified name of the POJO class being processed. ex: org.froporec.data1.Person
     * @param gettersList        list of public getters of the POJO class being processed. ex:[getLastname(), getAge(), getMark(), getGrade(), getSchool()]
     * @param getterMap          map containing getters names as keys and their corresponding types as values. ex: {getAge=int, getSchool=org.froporec.data1.School, getLastname=java.lang.String}
     * @throws IOException only if a "severe" error happens while writing the file to the filesystem. Cases of already existing files are not treated as errors
     */
    public void writeRecordSourceFile(final String qualifiedClassName, final List<? extends Element> gettersList, final Map<String, String> getterMap) throws IOException {
        var recordClassFile = processingEnvironment.getFiler().createSourceFile(qualifiedClassName + RECORD); // if file already exists, this line throws a FilerException
        var recordClassString = buildRecordClassContent(qualifiedClassName, gettersList, getterMap);
        try (PrintWriter out = new PrintWriter(recordClassFile.openWriter())) {
            out.println(recordClassString);
        }
    }

    private String buildRecordClassContent(final String qualifiedClassName, final List<? extends Element> gettersList, final Map<String, String> getterMap) {
        var recordClassContent = new StringBuilder();
        int lastDot = qualifiedClassName.lastIndexOf('.');
        var recordSimpleClassName = (qualifiedClassName + RECORD).substring(lastDot + 1);
        // package statement
        String packageName = lastDot > 0 ? qualifiedClassName.substring(0, lastDot) : null;
        Optional.ofNullable(packageName).ifPresent(name -> recordClassContent.append(format("package %s;%n%n", name)));
        // javax.annotation.processing.Generated section
        javaxGeneratedGenerator.generateCode(recordClassContent, null);
        // record definition statement: public record ... with all attributes listed
        recordClassContent.append(format("public %s ", RECORD.toLowerCase()));
        recordClassContent.append(recordSimpleClassName);
        // listing all attributes next to the record name
        recordClassContent.append('(');
        fieldsGenerator.generateCode(recordClassContent, Map.of(GETTERS_MAP, getterMap, GETTERS_LIST, gettersList));
        recordClassContent.append(") {\n");
        // Custom 1 arg constructor statement
        customConstructorGenerator.generateCode(recordClassContent, Map.of(QUALIFIED_CLASS_NAME, qualifiedClassName, GETTERS_MAP, getterMap, GETTERS_LIST, gettersList));
        // no additional content: close the body of the class
        recordClassContent.append('}');
        return recordClassContent.toString();
    }
}