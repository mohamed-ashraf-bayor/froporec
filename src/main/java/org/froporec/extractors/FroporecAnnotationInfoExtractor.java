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
import javax.lang.model.element.Element;
import java.util.HashSet;
import java.util.Set;

import static java.util.stream.Collectors.toSet;
import static org.froporec.extractors.AnnotationInfoExtractor.FIELDS_INFO_EXTRACTOR_PREDICATE;
import static org.froporec.extractors.AnnotationInfoExtractor.METHOD_PARAMS_INFO_EXTRACTOR_PREDICATE;
import static org.froporec.extractors.AnnotationInfoExtractor.POJO_CLASSES_INFO_EXTRACTOR_PREDICATE;
import static org.froporec.extractors.AnnotationInfoExtractor.RECORD_CLASSES_INFO_EXTRACTOR_PREDICATE;
import static org.froporec.extractors.AnnotationInfoExtractor.extractIncludedTypes;

public final class FroporecAnnotationInfoExtractor {

    private final ProcessingEnvironment processingEnv;

    public FroporecAnnotationInfoExtractor(final ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
    }

    public Set<Element> extractAnnotatedElements(final Set<Element> allAnnotatedElements) {
        final AnnotationInfoExtractor annotationInfoExtractor = (annotatedElements, filterPredicate) -> {
            var filteredAnnotatedElements = allAnnotatedElements.stream().filter(filterPredicate).collect(toSet());
            filteredAnnotatedElements.addAll(extractIncludedTypes(filteredAnnotatedElements, processingEnv));
            return filteredAnnotatedElements;
        };
        var allAnnotatedElementsToProcess = new HashSet<>(annotationInfoExtractor.extract(allAnnotatedElements, POJO_CLASSES_INFO_EXTRACTOR_PREDICATE));
        allAnnotatedElementsToProcess.addAll(annotationInfoExtractor.extract(allAnnotatedElements, RECORD_CLASSES_INFO_EXTRACTOR_PREDICATE));
        allAnnotatedElementsToProcess.addAll(annotationInfoExtractor.extract(allAnnotatedElements, FIELDS_INFO_EXTRACTOR_PREDICATE));
        allAnnotatedElementsToProcess.addAll(annotationInfoExtractor.extract(allAnnotatedElements, METHOD_PARAMS_INFO_EXTRACTOR_PREDICATE));
        return allAnnotatedElementsToProcess;
    }
}