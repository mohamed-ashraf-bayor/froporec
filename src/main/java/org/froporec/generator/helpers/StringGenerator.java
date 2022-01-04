package org.froporec.generator.helpers;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;

public interface StringGenerator {

    String EMPTY_STRING = "";

    /**
     * "Record" string constant
     */
    String RECORD = "Record";

    String IMMUTABLE = "Immutable";

    String GENERATE_RECORD_QUALIFIED_NAME = "org.froporec.GenerateRecord";

    String GENERATE_IMMUTABLE_QUALIFIED_NAME = "org.froporec.GenerateImmutable";

    String INCLUDE_TYPES_ATTRIBUTE = "includeTypes";

    String COMMA_SEPARATOR = ",";
    /**
     * "("
     */
    String OPENING_PARENTHESIS = "(";
    /**
     * ")"
     */
    String CLOSING_PARENTHESIS = ")";

    String DOT_CLASS = ".class";

    String DOT = ".";

    String GENERATION_SUCCESS_MSG_FORMAT = "\t> Successfully generated %s";

    String GENERATION_FAILURE_MSG_FORMAT = "\t> Error generating %s";

    /**
     * Array of methods to exclude while pulling the list of all inherited methods of a class or interface
     */
    String[] METHODS_TO_EXCLUDE = {"getClass", "wait", "notifyAll", "hashCode", "equals", "notify", "toString", "clone", "finalize"};

    default String constructImmutableRecordQualifiedName(final String qualifiedClassName) {
        return qualifiedClassName.contains(DOT)
                ? qualifiedClassName.substring(0, qualifiedClassName.lastIndexOf(DOT)) + DOT + IMMUTABLE + qualifiedClassName.substring(qualifiedClassName.lastIndexOf(DOT) + 1)
                : IMMUTABLE + qualifiedClassName;
    }

    default String constructImmutableRecordSimpleName(final String qualifiedClassName) {
        return qualifiedClassName.contains(DOT)
                ? qualifiedClassName.substring(0, qualifiedClassName.lastIndexOf(DOT)) + DOT + IMMUTABLE + qualifiedClassName.substring(qualifiedClassName.lastIndexOf(DOT) + 1)
                : IMMUTABLE + qualifiedClassName;
    }

    default String constructImmutableQualifiedNameBasedOnElementType(final Element element) {
        return  ElementKind.RECORD.equals(element.getKind())
                ? constructImmutableRecordQualifiedName(element.toString())
                : element + RECORD;
    }

    default String constructImmutableSimpleNameBasedOnElementType(final Element element) {
        var immutableQualifiedName = constructImmutableQualifiedNameBasedOnElementType(element);
        return immutableQualifiedName.substring(immutableQualifiedName.lastIndexOf(DOT) + 1);
    }
}