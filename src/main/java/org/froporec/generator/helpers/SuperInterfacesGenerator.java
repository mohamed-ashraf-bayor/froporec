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
package org.froporec.generator.helpers;

import javax.lang.model.element.Element;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.stream.Collectors.joining;

/**
 * Builds the list of super interfaces implemented by the Record class being generated. Based on the array provided as
 * value of the 'superInterfaces' attribute.<br>
 * The params {@link Map} parameter of the provided implementation of the generateCode() method (from {@link CodeGenerator}) MUST contain
 * the following parameters names:<br>
 * - {@link CodeGenerator#ANNOTATED_ELEMENT}<br>
 */
public final class SuperInterfacesGenerator implements CodeGenerator {

    private final Map<String, Map<Element, List<Element>>> superInterfacesListByAnnotatedElementAndByAnnotation;

    /**
     * SuperInterfacesGenerator constructor
     *
     * @param superInterfacesListByAnnotatedElementAndByAnnotation {@link List} of provided super interfaces .class values
     *                                                             grouped by their respective annotated {@link Element} instances
     *                                                             and by their respective annotation String representation
     */
    public SuperInterfacesGenerator(Map<String, Map<Element, List<Element>>> superInterfacesListByAnnotatedElementAndByAnnotation) {
        this.superInterfacesListByAnnotatedElementAndByAnnotation = superInterfacesListByAnnotatedElementAndByAnnotation;
    }

    private void buildSuperInterfacesList(StringBuilder recordClassContent, Element annotatedElement, boolean isSuperRecord) {
        // @SuperRecord annotated elmnt being processed, we only pull its superinterf and exit
        if (isSuperRecord) {
            var interfacesListByAnnotatedElementSR = superInterfacesListByAnnotatedElementAndByAnnotation.get(ORG_FROPOREC_SUPER_RECORD);
            buildInterfacesList(recordClassContent, annotatedElement, interfacesListByAnnotatedElementSR);
            return;
        }
        // @Record or @Immutable: only 1 of follwng scenarios possible as 1 of the annotations will be skipped if both used
        var interfacesListByAnnotatedElementREC = superInterfacesListByAnnotatedElementAndByAnnotation.get(ORG_FROPOREC_RECORD);
        if (interfacesListByAnnotatedElementREC.containsKey(annotatedElement)) {
            buildInterfacesList(recordClassContent, annotatedElement, interfacesListByAnnotatedElementREC);
            return;
        }
        var interfacesListByAnnotatedElementIMM = superInterfacesListByAnnotatedElementAndByAnnotation.get(ORG_FROPOREC_IMMUTABLE);
        if (interfacesListByAnnotatedElementIMM.containsKey(annotatedElement)) {
            buildInterfacesList(recordClassContent, annotatedElement, interfacesListByAnnotatedElementIMM);
        }
    }

    private void buildInterfacesList(StringBuilder recordClassContent, Element annotatedElement, Map<Element, List<Element>> superInterfacesListByAnnotatedElement) {
        var superInterfacesListString = buildCommaSeparatedList(superInterfacesListByAnnotatedElement, annotatedElement);
        if (!superInterfacesListString.isBlank()) {
            recordClassContent.append(IMPLEMENTS + SPACE + superInterfacesListString);
        }
    }

    private String buildCommaSeparatedList(Map<Element, List<Element>> superInterfacesListByAnnotatedElement, Element annotatedElement) {
        return superInterfacesListByAnnotatedElement.getOrDefault(annotatedElement, List.of()).stream()
                .map(Object::toString)
                .map(StringGenerator::removeCommaSeparator)
                .collect(joining(COMMA + SPACE));
    }

    @Override
    @SuppressWarnings("unchecked")
    public void generateCode(StringBuilder recordClassContent, Map<String, Object> params) {
        var annotatedElement = (Element) params.get(CodeGenerator.ANNOTATED_ELEMENT);
        var isSuperRecord = Optional.ofNullable((Boolean) params.get(CodeGenerator.IS_SUPER_RECORD)).orElse(false).booleanValue();
        buildSuperInterfacesList(recordClassContent, annotatedElement, isSuperRecord);
    }
}