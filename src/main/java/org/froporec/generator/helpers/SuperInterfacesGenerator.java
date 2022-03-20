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
package org.froporec.generator.helpers;

import org.froporec.annotations.GenerateImmutable;
import org.froporec.annotations.GenerateRecord;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.joining;

/**
 * // TODO jdoc
 * Builds the list of fields of the record class being generated. ex: int a, String s, Person p.<br>
 * Considerations will be made for fields whose types have also been annotated or added as a .class value within the
 * "includeTypes" attribute of {@link GenerateRecord} or {@link GenerateImmutable}).<br>
 * The params {@link Map} parameter of the provided implementation of the generateCode() method (from {@link CodeGenerator}) MUST contain
 * the following parameters names:<br>
 * - {@link CodeGenerator#NON_VOID_METHODS_ELEMENTS_LIST}<br>
 */
public final class SuperInterfacesGenerator implements CodeGenerator {

    private final ProcessingEnvironment processingEnvironment;

    private final Map<String, Map<Element, List<Element>>> superInterfacesListByAnnotatedElementAndByAnnotation;

    /**
     * // TODO jdoc
     * FieldsGenerationHelper constructor. Instantiates needed instance of {@link CollectionsGenerator}
     *
     * @param processingEnvironment                 {@link ProcessingEnvironment} object, needed to access low-level information regarding the used annotations
     * @param allElementsTypesToConvertByAnnotation {@link Set} of all annotated elements types string representations
     */
    public SuperInterfacesGenerator(ProcessingEnvironment processingEnvironment, Map<String, Map<Element, List<Element>>> superInterfacesListByAnnotatedElementAndByAnnotation) {
        this.processingEnvironment = processingEnvironment;
        this.superInterfacesListByAnnotatedElementAndByAnnotation = superInterfacesListByAnnotatedElementAndByAnnotation;
    }

    private void buildSuperInterfacesList(StringBuilder recordClassContent, Element annotatedElement) {
        var recordAnnotatedElementsMap = superInterfacesListByAnnotatedElementAndByAnnotation.get(ORG_FROPOREC_RECORD);
        var immutableAnnotatedElementsMap = superInterfacesListByAnnotatedElementAndByAnnotation.get(ORG_FROPOREC_IMMUTABLE);
        var superRecordAnnotatedElementsMap = superInterfacesListByAnnotatedElementAndByAnnotation.get(ORG_FROPOREC_SUPER_RECORD);
        if (recordAnnotatedElementsMap.containsKey(annotatedElement)) {
            recordClassContent.append(buildCommaSeparatedList(recordAnnotatedElementsMap, annotatedElement));
            return;
        }
        if (immutableAnnotatedElementsMap.containsKey(annotatedElement)) {
            recordClassContent.append(buildCommaSeparatedList(immutableAnnotatedElementsMap, annotatedElement));
            return;
        }
        if (superRecordAnnotatedElementsMap.containsKey(annotatedElement)) {
            recordClassContent.append(buildCommaSeparatedList(superRecordAnnotatedElementsMap, annotatedElement));
        }
    }

    private String buildCommaSeparatedList(Map<Element, List<Element>> annotatedElementsMap, Element annotatedElement) {
        return annotatedElementsMap.get(annotatedElement).stream()
                .map(Object::toString) // TODO chck whthr we might need processingEnvironment here. if not, remove
                .map(StringGenerator::removeCommaSeparator)
                .collect(joining(COMMA_SEPARATOR + WHITESPACE));
    }

    @Override
    @SuppressWarnings("unchecked")
    public void generateCode(StringBuilder recordClassContent, Map<String, Object> params) {
        var annotatedElement = (Element) params.get(CodeGenerator.ANNOTATED_ELEMENT);
        buildSuperInterfacesList(recordClassContent, annotatedElement);
    }
}