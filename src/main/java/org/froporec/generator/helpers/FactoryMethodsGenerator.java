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

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.froporec.generator.helpers.StringGenerator.constructImmutableSimpleNameBasedOnElementType;
import static org.froporec.generator.helpers.StringGenerator.lowerCase1stChar;

/**
 * // TODO chnge jdoc
 * Builds the list of fields of the record class being generated. ex: int a, String s, Person p.<br>
 * Considerations will be made for fields whose types have also been annotated or added as a .class value within the
 * "alsoConvert" attribute of {@link org.froporec.annotations.Record} or {@link org.froporec.annotations.Immutable}).<br>
 * The params {@link Map} parameter of the provided implementation of the generateCode() method (from {@link CodeGenerator}) MUST contain
 * the following parameters names:<br>
 * - {@link CodeGenerator#ANNOTATED_ELEMENT}<br>
 * - {@link CodeGenerator#NON_VOID_METHODS_ELEMENTS_LIST}<br>
 * - {@link CodeGenerator#IS_SUPER_RECORD}<br>
 */
public final class FactoryMethodsGenerator implements CodeGenerator {

    private final ProcessingEnvironment processingEnvironment;

    private final Map<String, Set<Element>> allElementsTypesToConvertByAnnotation;

    private final CodeGenerator customConstructorGenerator;
//    private final CodeGenerator fieldsGenerator;

    /**
     * // TODO chnge jdoc
     * FieldsGenerationHelper constructor. Instantiates needed instance of {@link CollectionsGenerator}
     *
     * @param processingEnvironment                 {@link ProcessingEnvironment} object, needed to access low-level information regarding the used annotations
     * @param allElementsTypesToConvertByAnnotation {@link Set} of {@link Element} instances grouped by the annotation String representation
     */
    public FactoryMethodsGenerator(ProcessingEnvironment processingEnvironment,
                                   Map<String, Set<Element>> allElementsTypesToConvertByAnnotation) {
        this.processingEnvironment = processingEnvironment;
        this.allElementsTypesToConvertByAnnotation = allElementsTypesToConvertByAnnotation;
        this.customConstructorGenerator = new CustomConstructorGenerator(this.processingEnvironment, this.allElementsTypesToConvertByAnnotation, null);
//        this.fieldsGenerator = new FieldsGenerator(this.processingEnvironment, this.allElementsTypesToConvertByAnnotation, this.mergeWithListByAnnotatedElementAndByAnnotation);
    }

    private void buildStaticFactoryMethods(StringBuilder recordClassContent, Element annotatedElement, List<Element> nonVoidMethodsElementsList) {
        var annotatedTypeElement = (TypeElement) processingEnvironment.getTypeUtils().asElement(annotatedElement.asType());
        var annotatedElementQualifiedName = annotatedTypeElement.getQualifiedName().toString();
        var annotatedElementFieldName = lowerCase1stChar(annotatedTypeElement.getSimpleName().toString());
        recordClassContent.append(NEW_LINE + TAB);
        // "public static <GeneratedRecordSimpleName><SPACE>"
        recordClassContent.append(PUBLIC + SPACE + STATIC + SPACE + constructImmutableSimpleNameBasedOnElementType(annotatedTypeElement) + SPACE);
        // "buildWith(<AnnotatedPojoOrRecordQualifiedName><SPACE><AnnotatedPojoOrRecordSimpleName>)"
        recordClassContent.append(BUILD_WITH + OPENING_PARENTHESIS + annotatedElementQualifiedName + SPACE + annotatedElementFieldName + CLOSING_PARENTHESIS + SPACE + OPENING_BRACE + NEW_LINE + TAB + TAB);
        // method body
//        recordClassContent.append(RETURN + SPACE + NEW + SPACE + constructImmutableSimpleNameBasedOnElementType(annotatedTypeElement) + OPENING_PARENTHESIS + extract + OPENING_PARENTHESIS + SEMI_COLON + NEW_LINE);
        recordClassContent.append(RETURN + SPACE + NULL + SEMI_COLON + NEW_LINE);
        // closing
        recordClassContent.append(TAB + CLOSING_BRACE + NEW_LINE);
        recordClassContent.append(NEW_LINE);

        // "buildWith(<AnnotatedPojoOrRecordQualifiedName><SPACE><AnnotatedPojoOrRecordSimpleName>,<SPACE>java.util.Map<String, Object><SPACE>fieldsNameValuePairs)"

        // "buildWith(<AnnotatedPojoOrRecordQualifiedName><SPACE><AnnotatedPojoOrRecordSimpleName>)"

    }

    @Override
    @SuppressWarnings("unchecked")
    public void generateCode(StringBuilder recordClassContent, Map<String, Object> params) {
        var annotatedElement = (Element) params.get(CodeGenerator.ANNOTATED_ELEMENT);
        var nonVoidMethodsElementsList = new ArrayList<Element>((List<? extends Element>) params.get(CodeGenerator.NON_VOID_METHODS_ELEMENTS_LIST));
        var isSuperRecord = Optional.ofNullable((Boolean) params.get(CodeGenerator.IS_SUPER_RECORD)).orElse(false).booleanValue();
        if (isSuperRecord) {
            return; // static factory mthds generation NOT YET supported for SuperRecord classes
        }
        buildStaticFactoryMethods(recordClassContent, annotatedElement, nonVoidMethodsElementsList);
    }
}