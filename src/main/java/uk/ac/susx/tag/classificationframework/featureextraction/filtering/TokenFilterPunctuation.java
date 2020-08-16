package uk.ac.susx.tag.classificationframework.featureextraction.filtering;

/*
 * #%L
 * TokenFilterPunctuation.java - classificationframework - CASM Consulting - 2,013
 * %%
 * Copyright (C) 2013 - 2014 CASM Consulting
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.google.common.collect.Sets;
import uk.ac.susx.tag.classificationframework.datastructures.Document;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * This is a TokenFilter, see the TokenFilter abstract class for details of function and purpose of
 * token filters.
 *
 * This filter filters out punctuation according to a regex of what constitutes punctuation.
 * It's an alternative to using the PoS to filter punctuation.
 *
 * User: Andrew D. Robertson
 * Date: 19/08/2013
 * Time: 12:11
 */
public class TokenFilterPunctuation extends TokenFilter {

    private static final long serialVersionUID = 0L;

    private Pattern puncPattern;
    private boolean filterExclamationAndQuestionMarks;

    public TokenFilterPunctuation(boolean filterExclamationAndQuestionMarks){
        this.filterExclamationAndQuestionMarks = filterExclamationAndQuestionMarks;
        puncPattern = getPunctuationPattern();
    }

    private Pattern getPunctuationPattern() {
       /* Ahmed Younes: This is a conditional operator where it could be stated as if a then b otherwise c
                If filterExclamationAndQuestionMarks is true then the expression before the colon is evaluated if false then the one after the colon evaluated
                note that the whole expression is in the return statement which means that if the flag is filtering exclamation mark then the first pattern will be compiled
                else the second pattern will be compiled and returned*/
        return filterExclamationAndQuestionMarks?
//                Pattern.compile("[!?؟؛"#$%&'()*+,-./::;><،ـ<=>@\\[\\]^_`{|}~]+"):
//                Pattern.compile("[\"#$%&'()*+,-./::;<=>@\\[\\]^_`{|}~]+");
                // Ahmed Younes: expression before the colon, i added ؛،؟«»
                Pattern.compile("[!！؟?？\"“”#$%&'()（）「」【】、*+،，,-.。/:：؛；;<=>»«@\\[\\]^_`{|}~]+"):
                // Ahmed Younes expression after the colon, i added ؛،«»
                Pattern.compile("[\"“”#$%&'()（）「」【】、*+،，,-.。/:：؛；;<=>»«@\\[\\]^_`{|}~]+");
    }

    public TokenFilterPunctuation() {
        this(false);
    }

    public boolean filter(int index, Document tokens) {
        return puncPattern.matcher(tokens.get(index).get("form")).matches();
    }

    @Override
    public boolean isThreadSafe() {
        return true;
    }
}
