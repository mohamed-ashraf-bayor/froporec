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
import javax.lang.model.element.ElementKind;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.froporec.generator.helpers.StringGenerator.javaConstantNamingConvention;

/**
 * // TODO chnge jdoc
 * Builds the list of fields of the record class being generated. ex: int a, String s, Person p.<br>
 * Considerations will be made for fields whose types have also been annotated or added as a .class value within the
 * "alsoConvert" attribute of {@link org.froporec.annotations.Record} or {@link org.froporec.annotations.Immutable}).<br>
 * The params {@link Map} parameter of the provided implementation of the generateCode() method (from {@link CodeGenerator}) MUST contain
 * the following parameters names:<br>
 * - {@link CodeGenerator#NON_VOID_METHODS_ELEMENTS_LIST}<br>
 * - {@link CodeGenerator#IS_SUPER_RECORD}<br>
 */
public final class FieldsNamesConstantsGenerator implements CodeGenerator {

    private void buildFieldsConstantsFromNonVoidMethodsList(StringBuilder recordClassContent, List<Element> nonVoidMethodsElementsList) {
        recordClassContent.append(NEW_LINE + TAB);
        nonVoidMethodsElementsList.forEach(nonVoidMethodElement -> {
                    var enclosingElementIsRecord = ElementKind.RECORD.equals(nonVoidMethodElement.getEnclosingElement().getKind());
                    buildFieldConstantDeclaration(nonVoidMethodElement, enclosingElementIsRecord).ifPresent(fieldName ->
                            recordClassContent.append(
                                    PUBLIC + SPACE + STATIC + SPACE + STRING + SPACE
                                            + javaConstantNamingConvention(fieldName)
                                            + SPACE + EQUALS_STR + SPACE
                                            + DOUBLE_QUOTES + fieldName + DOUBLE_QUOTES
                                            + SEMI_COLON + NEW_LINE + TAB
                            )
                    );
                }
        );
    }

    private Optional<String> buildFieldConstantDeclaration(Element nonVoidMethodElement, boolean enclosingElementIsRecord) {
        if (enclosingElementIsRecord) {
            // Record class, handle all non-void methods
            var nonVoidMethodElementAsString = nonVoidMethodElement.toString();
            return Optional.of(nonVoidMethodElementAsString.substring(0, nonVoidMethodElementAsString.indexOf(OPENING_PARENTHESIS)));
        } else {
            // POJO class, handle only getters (only methods starting with get or is)
            var getterAsString = nonVoidMethodElement.toString();
            if (getterAsString.startsWith(GET)) {
                return Optional.of(getterAsString.substring(3, 4).toLowerCase() + getterAsString.substring(4, getterAsString.indexOf(OPENING_PARENTHESIS)));
            } else if (getterAsString.startsWith(IS)) {
                return Optional.of(getterAsString.substring(2, 3).toLowerCase() + getterAsString.substring(3, getterAsString.indexOf(OPENING_PARENTHESIS)));
            }
        }
        return Optional.empty();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void generateCode(StringBuilder recordClassContent, Map<String, Object> params) {
        var nonVoidMethodsElementsList = new ArrayList<Element>((List<? extends Element>) params.get(CodeGenerator.NON_VOID_METHODS_ELEMENTS_LIST));
        var isSuperRecord = Optional.ofNullable((Boolean) params.get(CodeGenerator.IS_SUPER_RECORD)).orElse(false).booleanValue();
        if (isSuperRecord) {
            return; // fields names constants generation NOT YET supported for SuperRecord classes
        }
        buildFieldsConstantsFromNonVoidMethodsList(recordClassContent, nonVoidMethodsElementsList);
    }
}