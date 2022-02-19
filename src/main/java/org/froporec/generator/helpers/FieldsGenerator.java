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
import javax.lang.model.element.ElementKind;
import javax.lang.model.type.ExecutableType;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.lang.String.format;

/**
 * Builds the list of fields of the record class being generated. ex: int a, String s, Person p.<br>
 * Considerations will be made for fields whose types have also been annotated or added as a .class value within the
 * "includeTypes" attribute of {@link GenerateRecord} or {@link GenerateImmutable}).<br>
 * The params {@link Map} parameter of the provided implementation of the generateCode() method (from {@link CodeGenerator}) MUST contain
 * the following parameters names:<br>
 * - {@link CodeGenerator#NON_VOID_METHODS_ELEMENTS_LIST}<br>
 */
public final class FieldsGenerator implements CodeGenerator {

    private final ProcessingEnvironment processingEnvironment;

    private final Set<String> allAnnotatedElementsTypes;

    private final SupportedCollectionsFieldsGenerator collectionsGenerator;

    /**
     * FieldsGenerationHelper constructor. Instantiates needed instance of {@link CollectionsGenerator}
     *
     * @param processingEnvironment     {@link ProcessingEnvironment} object, needed to access low-level information regarding the used annotations
     * @param allAnnotatedElementsTypes {@link Set} of all annotated elements types string representations
     */
    public FieldsGenerator(final ProcessingEnvironment processingEnvironment, final Set<String> allAnnotatedElementsTypes) {
        this.processingEnvironment = processingEnvironment;
        this.allAnnotatedElementsTypes = allAnnotatedElementsTypes;
        this.collectionsGenerator = new CollectionsGenerator(this.processingEnvironment, this.allAnnotatedElementsTypes);
    }

    private void buildRecordFieldsFromNonVoidMethodsList(
            final StringBuilder recordClassContent,
            final Map<? extends Element, String> nonVoidMethodsElementsReturnTypesMap,
            final List<? extends Element> nonVoidMethodsElementList) {
        nonVoidMethodsElementList.forEach(nonVoidMethodElement -> {
            var enclosingElementIsRecord = ElementKind.RECORD.equals(nonVoidMethodElement.getEnclosingElement().getKind());
            var nonVoidMethodReturnTypeAsString = nonVoidMethodsElementsReturnTypesMap.get(nonVoidMethodElement);
            var nonVoidMethodReturnTypeElementOpt = Optional.ofNullable(processingEnvironment.getTypeUtils().asElement(((ExecutableType) nonVoidMethodElement.asType()).getReturnType())); // for collections, Element.toString() will NOT return the generic part
            // TODO ifpresentorelse here ???
            if (nonVoidMethodReturnTypeElementOpt.isEmpty()) { // primitives
                buildSingleField(recordClassContent, nonVoidMethodElement, nonVoidMethodReturnTypeAsString, false, enclosingElementIsRecord);
            } else { // non-primitives
                // if the type has already been annotated somewhere else in the code, the field type is the corresponding generated record class
                // if not annotated and not a collection then keep the received type as is
                buildSingleField(recordClassContent, nonVoidMethodElement, nonVoidMethodReturnTypeAsString,
                        allAnnotatedElementsTypes.contains(nonVoidMethodReturnTypeElementOpt.get().toString()), enclosingElementIsRecord);
            }
        });
        recordClassContent.deleteCharAt(recordClassContent.length() - 1).deleteCharAt(recordClassContent.length() - 1);
    }

    private void buildSingleField(final StringBuilder recordClassContent, final Element nonVoidMethodElement,
                                  final String nonVoidMethodReturnTypeAsString, final boolean processAsImmutable,
                                  final boolean enclosingElementIsRecord) {
        var recordFieldsListFormat = "%s %s, "; // type fieldName,<SPACE>
        if (enclosingElementIsRecord) {
            // Record class, handle all non-void methods
            var nonVoidMethodElementAsString = nonVoidMethodElement.toString();
            var fieldName = nonVoidMethodElementAsString.substring(0, nonVoidMethodElementAsString.indexOf(OPENING_PARENTHESIS)); // fieldname or record component name
            // if the type of the field being processed is a collection process it differently and return
            if (collectionsGenerator.isCollectionWithGeneric(nonVoidMethodReturnTypeAsString)) {
                collectionsGenerator.replaceGenericWithRecordClassNameIfAny(recordClassContent, fieldName, nonVoidMethodReturnTypeAsString);
                return;
            }
            recordClassContent.append(format(
                    recordFieldsListFormat,
                    processAsImmutable
                            ? constructImmutableQualifiedNameBasedOnElementType(constructElementInstanceValueFromTypeString(processingEnvironment, nonVoidMethodReturnTypeAsString))
                            : nonVoidMethodReturnTypeAsString,
                    fieldName)
            );
        } else {
            // POJO class, handle only getters (only methods process methods starting with get or is)
            var getterAsString = nonVoidMethodElement.toString();
            var fieldNameNonBoolean = getterAsString.substring(3, 4).toLowerCase() + getterAsString.substring(4, getterAsString.indexOf(OPENING_PARENTHESIS));
            if (getterAsString.startsWith(GET)) {
                // if the type of the field being processed is a collection process it differently and return
                if (collectionsGenerator.isCollectionWithGeneric(nonVoidMethodReturnTypeAsString)) {
                    collectionsGenerator.replaceGenericWithRecordClassNameIfAny(recordClassContent, fieldNameNonBoolean, nonVoidMethodReturnTypeAsString);
                    return;
                }
                recordClassContent.append(format(
                        recordFieldsListFormat,
                        processAsImmutable
                                ? constructImmutableQualifiedNameBasedOnElementType(constructElementInstanceValueFromTypeString(processingEnvironment, nonVoidMethodReturnTypeAsString))
                                : nonVoidMethodReturnTypeAsString,
                        fieldNameNonBoolean)
                );
            } else if (getterAsString.startsWith(IS)) {
                var fieldNameBoolean = getterAsString.substring(2, 3).toLowerCase() + getterAsString.substring(3, getterAsString.indexOf(OPENING_PARENTHESIS));
                recordClassContent.append(format(recordFieldsListFormat, nonVoidMethodReturnTypeAsString, fieldNameBoolean));
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void generateCode(final StringBuilder recordClassContent, final Map<String, Object> params) {
        var nonVoidMethodsElementsList = (List<? extends Element>) params.get(CodeGenerator.NON_VOID_METHODS_ELEMENTS_LIST);
        buildRecordFieldsFromNonVoidMethodsList(recordClassContent, constructNonVoidMethodsElementsReturnTypesMapFromList(nonVoidMethodsElementsList), nonVoidMethodsElementsList);
    }
}