/**
 * Copyright (c) 2021 Mohamed Ashraf Bayor
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.froporec.generator.helpers;

import org.froporec.processor.GenerateRecordProcessor;

import javax.annotation.processing.ProcessingEnvironment;

import java.time.LocalDateTime;

import static java.lang.String.format;

/**
 * Class dedicated to generating the value, date and comments data for the @javax.annotation.processing.Generated annotation
 */
public class JavaxGeneratedGenerationHelper {

    private final ProcessingEnvironment processingEnvironment;

    /**
     * Constructor for JavaxGeneratedBlockGenerationHelper
     * @param processingEnvironment needed to access sourceversion and other useful info
     */
    public JavaxGeneratedGenerationHelper(final ProcessingEnvironment processingEnvironment) {
        this.processingEnvironment = processingEnvironment;
    }

    /**
     * Generates the @javax.annotation.processing.Generated annotation section including the value, date and comments attributes for that annotation
     * @param recordClassContent content being built, containing the record source string
     */
    public void buildGeneratedAnnotationSection(final StringBuilder recordClassContent) {
        recordClassContent.append(format("""
                @javax.annotation.processing.Generated(value = "%s", date = "%s", comments = "version: %s")
                """
                , GenerateRecordProcessor.class.getName()
                , LocalDateTime.now()
                , getClass().getPackage().getImplementationVersion() + " >><< " + processingEnvironment.getSourceVersion()
        ));
    }
}