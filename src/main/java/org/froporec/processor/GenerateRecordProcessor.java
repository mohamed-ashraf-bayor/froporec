package org.froporec.processor;

import com.google.auto.service.AutoService;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.FilerException;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ExecutableType;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.lang.String.format;

/**
 * FroPoRec annotation processor class . Picks up and processes all elements (classes, fields and method params) annotated with @GenerateRecord
 * The order of processing is: classes, then fields (or classes attributes) and then the method params
 * For each element a Record class is generated. If generated class already exists (in case the corresponding pojo has been annotated more than once, the generation process will be skipped
 * @author Mohamed Ashraf Bayor
 */
@SupportedAnnotationTypes("org.froporec.GenerateRecord")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
@AutoService(Processor.class)
public class GenerateRecordProcessor extends AbstractProcessor {

    private static final String RECORD = "Record";

    Logger log = Logger.getLogger(GenerateRecordProcessor.class.getName());

    @Override
    public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {
        for (TypeElement annotation : annotations) {
            if (!annotation.getSimpleName().toString().contains("GenerateRecord")) {
                continue;
            }
            var allAnnotatedElements = roundEnv.getElementsAnnotatedWith(annotation);
            processAnnotatedClasses(allAnnotatedElements); // process all annotated classes
            processAnnotatedClassAttributes(allAnnotatedElements); // process all annotated class attributes (fields)
            processAnnotatedMethodParams(allAnnotatedElements); // process all annotated method parameters
        }
        return true;
    }

    private void processAnnotatedClasses(final Set<? extends Element> allAnnotatedElements) {
        var annotatedClasses = allAnnotatedElements.stream()
                .filter(element -> !element.getClass().isRecord())
                .filter(element -> ElementKind.CLASS.equals(element.getKind()))
                .collect(Collectors.toSet());
        annotatedClasses.forEach(annotatedClass -> processAnnotatedElement(annotatedClass, allAnnotatedElements));
    }

    private void processAnnotatedClassAttributes(final Set<? extends Element> allAnnotatedElements) {
        var annotatedClassAttributes = allAnnotatedElements.stream()
                .filter(element -> ElementKind.FIELD.equals(element.getKind()))
                .map(element -> processingEnv.getTypeUtils().asElement(element.asType()))
                .collect(Collectors.toSet());
        annotatedClassAttributes.forEach(annotatedMethod -> processAnnotatedElement(annotatedMethod, allAnnotatedElements));
    }

    private void processAnnotatedMethodParams(final Set<? extends Element> allAnnotatedElements) {
        var annotatedParams = allAnnotatedElements.stream()
                .filter(element -> ElementKind.PARAMETER.equals(element.getKind()))
                .map(element -> processingEnv.getTypeUtils().asElement(element.asType()))
                .collect(Collectors.toSet());
        annotatedParams.forEach(annotatedParam -> processAnnotatedElement(annotatedParam, allAnnotatedElements));
    }

    /**
     * processes the annotated element and generates the record class if the record was not already generated. if already generated then the process is skipped
     * @param annotatedElement
     * @param allAnnotatedElements
     */
    private void processAnnotatedElement(final Element annotatedElement, final Set<? extends Element> allAnnotatedElements) {
        var gettersList = processingEnv.getElementUtils().getAllMembers((TypeElement) processingEnv.getTypeUtils().asElement(annotatedElement.asType())).stream()
                .filter(element -> element.getSimpleName().toString().startsWith("get") || element.getSimpleName().toString().startsWith("is"))
                .filter(element -> !element.getSimpleName().toString().startsWith("getClass"))
                .toList();
        var className = ((TypeElement) processingEnv.getTypeUtils().asElement(annotatedElement.asType())).getQualifiedName().toString();
        var gettersMap = gettersList.stream().collect(Collectors.toMap(getter -> getter.getSimpleName().toString(), getter -> ((ExecutableType) getter.asType()).getReturnType().toString()));
        try {
            new RecordClassGenerator().writeRecordClassFile(className, gettersList, gettersMap, allAnnotatedElements);
            log.info("\t> Successfully generated " + className + "Record");
        } catch (FilerException e) {
            // log.info("Skipped generating " + className + "Record - file already exists");
        } catch (IOException e) {
            log.log(Level.SEVERE, "Error generating " + className + "Record", e);
        }
    }

    /**
     * class in charge of building the record class string content and write it to the file
     */
    private class RecordClassGenerator {

        void writeRecordClassFile(final String className, final List<? extends Element> gettersList, final Map<String, String> getterMap,
                                          final Set<? extends Element> allAnnotatedElements) throws IOException {
            var recordClassFile = processingEnv.getFiler().createSourceFile(className + RECORD); // if file already exists, this line throws IOException
            var recordClassString = buildRecordClassContent(className, gettersList, getterMap, allAnnotatedElements);
            try (PrintWriter out = new PrintWriter(recordClassFile.openWriter())) {
                out.println(recordClassString);
            }
        }

        private String buildRecordClassContent(final String className, final List<? extends Element> gettersList,
                                               final Map<String, String> getterMap, final Set<? extends Element> allAnnotatedElements) {
            var recordClassContent = new StringBuilder();
            String packageName = null;
            int lastDot = className.lastIndexOf('.');
            if (lastDot > 0) {
                packageName = className.substring(0, lastDot);
            }
            var simpleClassName = className.substring(lastDot + 1);
            var recordClassName = className + RECORD;
            var recordSimpleClassName = recordClassName.substring(lastDot + 1);
            if (packageName != null) {
                recordClassContent.append("package ");
                recordClassContent.append(packageName);
                recordClassContent.append(';');
                recordClassContent.append("\n\n");
            }
            recordClassContent.append(format("public %s ", RECORD.toLowerCase()));
            recordClassContent.append(recordSimpleClassName);
            recordClassContent.append('(');
            buildRecordAttributesFromGettersList(recordClassContent, getterMap, gettersList, allAnnotatedElements);
            recordClassContent.append(") {\n\n");
            buildRecordCustom1ArgConstructor(recordClassContent, className, simpleClassName, gettersList, allAnnotatedElements);
            recordClassContent.append('}');
            return recordClassContent.toString();
        }

        private void buildRecordAttributesFromGettersList(final StringBuilder recordClassContent, final Map<String, String> gettersMap,
                                                          final List<? extends Element> gettersList, final Set<? extends Element> allAnnotatedElements) {
            gettersList.forEach(getter -> {
                var getterAsString = getter.toString();
                var getterNameWithoutParenthesis = getterAsString.substring(0, getterAsString.indexOf('('));
                var getterAttributeNonBoolean1stLetter = getterAsString.substring(3, 4).toLowerCase();
                var getterAttributeBoolean1stLetter = getterAsString.substring(2, 3).toLowerCase();
                var getterAttributeNonBooleanAfter1stLetter = getterAsString.substring(4, getterAsString.indexOf('('));
                var getterAttributeBooleanAfter1stLetter = getterAsString.substring(3, getterAsString.indexOf('('));
                if (processingEnv.getTypeUtils().asElement(((ExecutableType) getter.asType()).getReturnType()) == null) {
                    // primitives
                    if (getterAsString.startsWith("get")) {
                        // %s = attribute type qualified name , %s = 1st letter of attribute name (lowercase) , %s = rest of the attribute name after 1st letter
                        recordClassContent.append(format("%s %s%s, ", gettersMap.get(getterNameWithoutParenthesis), getterAttributeNonBoolean1stLetter, getterAttributeNonBooleanAfter1stLetter));
                    } else if (getterAsString.startsWith("is")) {
                        recordClassContent.append(format("%s %s%s, ", gettersMap.get(getterNameWithoutParenthesis), getterAttributeBoolean1stLetter, getterAttributeBooleanAfter1stLetter));
                    }
                } else {
                    // non-primitives
                    if (allAnnotatedElements.contains(processingEnv.getTypeUtils().asElement(((ExecutableType) getter.asType()).getReturnType()))) {
                        // if the type has already been annotated somewhere else in the code, the attribute type is the corresponding generated record class
                        if (getterAsString.startsWith("get")) {
                            recordClassContent.append(format("%s %s%s, ", gettersMap.get(getterNameWithoutParenthesis) + RECORD, getterAttributeNonBoolean1stLetter, getterAttributeNonBooleanAfter1stLetter));
                        } else if (getterAsString.startsWith("is")) {
                            recordClassContent.append(format("%s %s%s, ", gettersMap.get(getterNameWithoutParenthesis) + RECORD, getterAttributeBoolean1stLetter, getterAttributeBooleanAfter1stLetter));
                        }
                    } else {
                        // if not annotated then keep the received type as is
                        if (getterAsString.startsWith("get")) {
                            recordClassContent.append(format("%s %s%s, ", gettersMap.get(getterNameWithoutParenthesis), getterAttributeNonBoolean1stLetter, getterAttributeNonBooleanAfter1stLetter));
                        } else if (getterAsString.startsWith("is")) {
                            recordClassContent.append(format("%s %s%s, ", gettersMap.get(getterNameWithoutParenthesis), getterAttributeBoolean1stLetter, getterAttributeBooleanAfter1stLetter));
                        }
                    }
                }
            });
            recordClassContent.deleteCharAt(recordClassContent.length() - 1).deleteCharAt(recordClassContent.length() - 1);
        }

        private void buildRecordCustom1ArgConstructor(StringBuilder recordClassContent, String className, String simpleClassName, List<? extends Element> gettersList, Set<? extends Element> allAnnotatedElements) {
            // %s = simple class name , %s = "Record" , %s = pojo class fully qualified name , %s = 1st letter of the param name in lowercase , %s = rest of the param name
            var paramName1stLetter = simpleClassName.substring(0, 1).toLowerCase();
            recordClassContent.append(format("\tpublic %s%s(%s %s%s) {\n", simpleClassName, RECORD, className, paramName1stLetter, simpleClassName.substring(1)));
            recordClassContent.append("\t\tthis("); // calling canonical constructor
            gettersList.forEach(getter -> {
                Element getterReturnTypeElement = processingEnv.getTypeUtils().asElement(((ExecutableType) getter.asType()).getReturnType());
                // if the pojo constructor param is another pojo check if it's been annotated. if yes use the corresponding generated record class
                if (allAnnotatedElements.contains(getterReturnTypeElement)) {
                    recordClassContent.append(format("new %s%s(%s%s.%s), ", getterReturnTypeElement, RECORD, paramName1stLetter, simpleClassName.substring(1), getter));
                } else {
                    // if not just call the getter as is
                    recordClassContent.append(format("%s%s.%s, ", paramName1stLetter, simpleClassName.substring(1), getter));
                }
            });
            recordClassContent.deleteCharAt(recordClassContent.length() - 1).deleteCharAt(recordClassContent.length() - 1);
            recordClassContent.append(");\n");
            recordClassContent.append("\t}\n");
        }
    }
}