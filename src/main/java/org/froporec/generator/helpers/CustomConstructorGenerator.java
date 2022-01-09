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

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.type.ExecutableType;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.lang.String.format;

/**
 * Builds the custom 1-arg constructor section for the record class being generated.<br>
 * Starts with "public RecordName(list of fields)" and includes a call to the canonical constructor inside the body of the custom constructor.<br>
 * The params {@link Map} parameter of the provided implementation of the generateCode() method (from {@link CodeGenerator}) MUST contain
 * the following parameters names:<br>
 * - {@link CodeGenerator#QUALIFIED_CLASS_NAME}<br>
 * - {@link CodeGenerator#NON_VOID_METHODS_ELEMENTS_LIST}<br>
 */
public final class CustomConstructorGenerator implements CodeGenerator {

    private final ProcessingEnvironment processingEnvironment;

    private final Set<String> allAnnotatedElementsTypes;

    private final SupportedCollectionsMappingLogicGenerator collectionsGenerator;

    /**
     * CustomConstructorGenerationHelper constructor. Instantiates needed instances of {@link ProcessingEnvironment} and {@link CollectionsGenerator}
     *
     * @param processingEnvironment     {@link ProcessingEnvironment} object, needed to access low-level information regarding the used annotations
     * @param allAnnotatedElementsTypes {@link Set} of all annotated elements types
     */
    public CustomConstructorGenerator(final ProcessingEnvironment processingEnvironment, final Set<String> allAnnotatedElementsTypes) {
        this.processingEnvironment = processingEnvironment;
        this.allAnnotatedElementsTypes = allAnnotatedElementsTypes;
        this.collectionsGenerator = new CollectionsGenerator(this.processingEnvironment, this.allAnnotatedElementsTypes);
    }

    private void buildRecordCustom1ArgConstructor(
            final StringBuilder recordClassContent,
            final String qualifiedClassName,
            final Map<? extends Element, String> nonVoidMethodsElementsReturnTypesMap,
            final List<? extends Element> nonVoidMethodsElementsList) {
        var simpleClassName = qualifiedClassName.substring(qualifiedClassName.lastIndexOf(DOT) + 1);
        var fieldName = simpleClassName.substring(0, 1).toLowerCase() + simpleClassName.substring(1); // simpleClassName starting with lowercase char
        // %s = generated simple class name, %s = pojo class qualified name , %s = field name
        recordClassContent.append(format(
                "\tpublic %s(%s %s) {%n",
                constructImmutableSimpleNameBasedOnElementType(constructElementInstanceValueFromTypeString(processingEnvironment, qualifiedClassName)),
                qualifiedClassName,
                fieldName
        )); // line declaring the constructor
        recordClassContent.append("\t\tthis("); // calling canonical constructor
        // building canonical constructor content
        nonVoidMethodsElementsList.forEach(nonVoidMethodElement -> {
            var enclosingElementIsRecord = ElementKind.RECORD.equals(nonVoidMethodElement.getEnclosingElement().getKind());
            var nonVoidMethodReturnTypeAsString = nonVoidMethodsElementsReturnTypesMap.get(nonVoidMethodElement);
            var nonVoidMethodReturnTypeElementOpt = Optional.ofNullable(processingEnvironment.getTypeUtils().asElement(((ExecutableType) nonVoidMethodElement.asType()).getReturnType())); // for collections, Element.toString() will NOT return the generic part
            // if the pojo constructor param is another pojo, check if it's been annotated. if yes, use the corresponding generated record class
            if (nonVoidMethodReturnTypeElementOpt.isEmpty()) {
                // primitives
                buildCanonicalConstructorCallSingleParameter(recordClassContent, fieldName, nonVoidMethodElement, nonVoidMethodReturnTypeAsString, false, enclosingElementIsRecord);
            } else {
                // non-primitives
                buildCanonicalConstructorCallSingleParameter(recordClassContent, fieldName, nonVoidMethodElement, nonVoidMethodReturnTypeAsString, allAnnotatedElementsTypes.contains(nonVoidMethodReturnTypeElementOpt.get().toString()), enclosingElementIsRecord);
            }
        });
        recordClassContent.deleteCharAt(recordClassContent.length() - 1).deleteCharAt(recordClassContent.length() - 1);
        // finished building canonical constructor content
        recordClassContent.append(");\n");
        recordClassContent.append("\t}\n");
    }

    private void buildCanonicalConstructorCallSingleParameter(
            final StringBuilder recordClassContent,
            final String fieldName,
            final Element nonVoidMethodElement,
            final String nonVoidMethodReturnTypeAsString,
            final boolean processAsImmutable,
            final boolean enclosingElementIsRecord) {
        var nonVoidMethodElementAsString = nonVoidMethodElement.toString();
        if (nonVoidMethodElementAsString.startsWith(GET) || enclosingElementIsRecord) {
            if (collectionsGenerator.isCollectionWithGeneric(nonVoidMethodReturnTypeAsString)) {
                collectionsGenerator.generateCollectionFieldMappingIfGenericIsAnnotated(recordClassContent, fieldName, nonVoidMethodElementAsString, nonVoidMethodReturnTypeAsString);
            } else {
                if (processAsImmutable) {
                    recordClassContent.append(format(
                            "new %s(%s.%s), ",
                            constructImmutableQualifiedNameBasedOnElementType(constructElementInstanceValueFromTypeString(processingEnvironment, nonVoidMethodReturnTypeAsString)),
                            fieldName,
                            nonVoidMethodElementAsString
                    ));
                } else {
                    // applies to primitives and any pojo or record which hasn't been annotated
                    // %s.%s = fieldname.nonVoidMethodElementAsString
                    recordClassContent.append(format("%s.%s, ", fieldName, nonVoidMethodElementAsString));
                }
            }
        } else if (nonVoidMethodElementAsString.startsWith(IS)) {
            recordClassContent.append(format("%s.%s, ", fieldName, nonVoidMethodElementAsString));
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void generateCode(StringBuilder recordClassContent, Map<String, Object> params) {
        var qualifiedClassName = (String) params.get(CodeGenerator.QUALIFIED_CLASS_NAME);
        var nonVoidMethodsElementsList = (List<? extends Element>) params.get(CodeGenerator.NON_VOID_METHODS_ELEMENTS_LIST);
        buildRecordCustom1ArgConstructor(recordClassContent, qualifiedClassName, constructNonVoidMethodsElementsReturnTypesMapFromList(nonVoidMethodsElementsList), nonVoidMethodsElementsList);
    }
}