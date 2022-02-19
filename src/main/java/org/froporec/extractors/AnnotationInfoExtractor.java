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
package org.froporec.extractors;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toSet;
import static org.froporec.generator.helpers.StringGenerator.ALSO_CONVERT_ATTRIBUTES;
import static org.froporec.generator.helpers.StringGenerator.COMMA_SEPARATOR;
import static org.froporec.generator.helpers.StringGenerator.DOT_CLASS;
import static org.froporec.generator.helpers.StringGenerator.EMPTY_STRING;
import static org.froporec.generator.helpers.StringGenerator.INCLUDE_TYPES_ATTRIBUTE;
import static org.froporec.generator.helpers.StringGenerator.ORG_FROPOREC_GENERATE_IMMUTABLE;
import static org.froporec.generator.helpers.StringGenerator.ORG_FROPOREC_GENERATE_RECORD;
import static org.froporec.generator.helpers.StringGenerator.ORG_FROPOREC_IMMUTABLE;
import static org.froporec.generator.helpers.StringGenerator.ORG_FROPOREC_RECORD;
import static org.froporec.generator.helpers.StringGenerator.ORG_FROPOREC_SUPER_RECORD;

public interface AnnotationInfoExtractor {

    Predicate<Element> POJO_CLASSES_INFO_EXTRACTOR_PREDICATE = element ->
            !element.getClass().isRecord()
                    && !element.getClass().isEnum()
                    && !element.getClass().isSealed()
                    && ElementKind.CLASS.equals(element.getKind());

    Predicate<Element> RECORD_CLASSES_INFO_EXTRACTOR_PREDICATE = element ->
            ElementKind.RECORD.equals(element.getKind());

    Predicate<Element> FIELDS_INFO_EXTRACTOR_PREDICATE = element ->
            !ElementKind.ENUM_CONSTANT.equals(element.getKind()) &&
                    (ElementKind.FIELD.equals(element.getKind())
                            || ElementKind.RECORD_COMPONENT.equals(element.getKind()));

    Predicate<Element> METHOD_PARAMS_INFO_EXTRACTOR_PREDICATE = element ->
            ElementKind.PARAMETER.equals(element.getKind());

    Set<Element> extract(Set<Element> allAnnotatedElements, Predicate<Element> filterPredicate);

    // TODO HUGE REFACTRNG NDED TO CNSDR includeTypes / alsoConvert
    //parentInterfaces
    //
    //mergeWith
    static Set<Element> extractIncludedTypes(final Set<? extends Element> annotatedElements, final ProcessingEnvironment processingEnv) {
        var includedTypesAsElements = new HashSet<Element>();
        annotatedElements.forEach(element -> processingEnv.getElementUtils().getAllAnnotationMirrors(element).stream()
                .filter(annotationMirror -> annotationMirror.toString().contains(ORG_FROPOREC_GENERATE_IMMUTABLE)
                        || annotationMirror.toString().contains(ORG_FROPOREC_GENERATE_RECORD)
                        || annotationMirror.toString().contains(ORG_FROPOREC_RECORD)
                        || annotationMirror.toString().contains(ORG_FROPOREC_IMMUTABLE)
                        || annotationMirror.toString().contains(ORG_FROPOREC_SUPER_RECORD))
                .map(AnnotationMirror::getElementValues)
                .forEach(map -> map.forEach((executableElement, annotationValue) -> {
                    // annotationValue.getValue() sample value: org.froporec...School.class,org.froporec...Person.class
                    if (executableElement.toString().contains(INCLUDE_TYPES_ATTRIBUTE)
                            || executableElement.toString().contains(ALSO_CONVERT_ATTRIBUTES)) {
                        includedTypesAsElements.addAll(
                                asList(annotationValue.getValue().toString().split(COMMA_SEPARATOR)).stream()
                                        .map(includedTypeDotClassString -> processingEnv.getTypeUtils().asElement(
                                                processingEnv.getElementUtils().getTypeElement(
                                                        includedTypeDotClassString.strip().replace(DOT_CLASS, EMPTY_STRING)
                                                ).asType()
                                        )).collect(toSet()));
                    }
                })));
        return includedTypesAsElements;
    }
}