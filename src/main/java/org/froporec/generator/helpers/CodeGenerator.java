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
package org.froporec.generator.helpers;

import java.util.Map;

/**
 * Exposes contract for a CodeGenerator class to fulfill
 */
public sealed interface CodeGenerator permits JavaxGeneratedGenerator, FieldsGenerator, CustomConstructorGenerator, SupportedCollectionsGenerator {

    // List of the parameters expected in the params Map object of the generateCode method:

    /**
     * parameter name: "qualifiedClassName", expected type: String
     */
    String QUALIFIED_CLASS_NAME = "qualifiedClassName";

    /**
     * parameter name: "fieldName", expected type: String
     */
    String FIELD_NAME = "fieldName";

    /**
     * parameter name: "getterReturnType", expected type: String
     */
    String GETTER_RETURN_TYPE = "getterReturnType";

    /**
     * parameter name: "getterAsString", expected type: String
     */
    String GETTER_AS_STRING = "getterAsString";

    /**
     * parameter name: "gettersList", expected type: List&lt;? extends javax.lang.model.element.Element&gt;, ex:[getLastname(), getAge(), getMark(), getGrade(), getSchool()]
     */
    String GETTERS_LIST = "gettersList";

    /**
     * parameter name: "gettersMap", expected type: Map&lt;String, String&gt;, ex: {getAge=int, getSchool=org.froporec.data1.School, getLastname=java.lang.String}
     */
    String GETTERS_MAP = "gettersMap";

    /**
     * Generates piece of code requested, based on the parameters provided in the params object and appends it to the provided recordClassContent param
     * @param recordClassContent Stringbuilder object containing the record class code being generated
     * @param params expected parameters. restricted to what is expected by the implementing class. the expected parameters names are defined as constants in the CodeGenerator interface.
     */
    void generateCode(StringBuilder recordClassContent, Map<String, Object> params);
}

sealed interface SupportedCollectionsGenerator extends CodeGenerator {

    /**
     * Supported collections types, stored as a list of String values
     */
    enum SupportedCollectionTypes {
        LIST("List"),
        SET("Set"),
        MAP("Map");
        private final String type;
        SupportedCollectionTypes(final String type) {
            this.type = type;
        }
        public String getType() {
            return type;
        }
    }

    /**
     * Checks whether the provided type is a collection with a generic. The check is simply based on the string representation (qualified name) of the type
     * the check is based on the string representation. first checked is the presence of &lt;&gt; and then whether the name has "List", "Set" or "Map" in its name
     *
     * @param getterReturnType - the provided type qualified name
     * @return true or false
     */
    default boolean isCollectionWithGeneric(final String getterReturnType) {
        if (getterReturnType.indexOf('<') == -1 && getterReturnType.indexOf('>') == -1) {
            return false;
        }
        var collectionType = extractCollectionType(getterReturnType);
        return collectionType.contains(SupportedCollectionTypes.LIST.type)
                || collectionType.contains(SupportedCollectionTypes.SET.type)
                || collectionType.contains(SupportedCollectionTypes.MAP.type);
    }

    /**
     * extracts the type within the &lt;&gt;
     * @param getterReturnType getter return type
     * @return the generic type
     */
    default String extractGenericType(final String getterReturnType) {
        int idxFirstSign = getterReturnType.indexOf('<');
        int idxLastSign = getterReturnType.indexOf('>');
        return getterReturnType.substring(0, idxLastSign).substring(idxFirstSign + 1);
    }

    /**
     * extracts the collection type without the generic
     * @param getterReturnType getter return type
     * @return the collection type
     */
    default String extractCollectionType(final String getterReturnType) {
        int idxFirstSign = getterReturnType.indexOf('<');
        return getterReturnType.substring(0, idxFirstSign);
    }
}

sealed interface SupportedCollectionsFieldsGenerator extends SupportedCollectionsGenerator permits CollectionsGenerator {

    /**
     * Replaces every POJO class within a generic with its generated record class name. (only if Person was also annotated)
     * ex: if List&lt;Person&gt; is a member of an annotated POJO class, the generated record class of the POJO will have
     * a member of List&lt;PersonRecord&gt;
     *
     * @param recordClassContent content being built, containing the record source string
     * @param fieldName          record field being processed
     * @param getterReturnType   type of the POJO being processed
     * @return the string replacing the POJO with its generated record class name. In case it's not a generic no replacement is performed
     */
    void replaceGenericWithRecordClassNameIfAny(final StringBuilder recordClassContent, final String fieldName, final String getterReturnType);

    @Override
    default void generateCode(final StringBuilder recordClassContent, final Map<String, Object> params) {
        final String fieldName = (String) params.get(FIELD_NAME);
        final String getterReturnType = (String) params.get(GETTER_RETURN_TYPE);
        replaceGenericWithRecordClassNameIfAny(recordClassContent, fieldName, getterReturnType);
    }
}

sealed interface SupportedCollectionsMappingLogicGenerator extends SupportedCollectionsGenerator permits CollectionsGenerator {

    /**
     * Builds a Collection.stream()... logic to allow the mapping of a collection of POJO classes to a collection of the corresponding generated Record classes,
     * only if the POJOs defined as generic(s) is/are annotated
     * This happens inside the generated custom constructor inside which we call the canonical constructor of the Record class being generated
     *
     * @param fieldName        field name being processed
     * @param getterAsString   getter name
     * @param getterReturnType getter return type, also the type of the field being processed
     * @return return the mapping logic for the collection field being processed
     */
    void generateCollectionFieldMappingIfGenericIsAnnotated(final StringBuilder recordClassContent, final String fieldName, final String getterAsString, final String getterReturnType);

    @Override
    default void generateCode(final StringBuilder recordClassContent, final Map<String, Object> params) {
        final String fieldName = (String) params.get(FIELD_NAME);
        final String getterAsString = (String) params.get(GETTER_AS_STRING);
        final String getterReturnType = (String) params.get(GETTER_RETURN_TYPE);
        generateCollectionFieldMappingIfGenericIsAnnotated(recordClassContent, fieldName, getterAsString, getterReturnType);
    }
}