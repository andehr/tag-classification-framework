package uk.ac.susx.tag.classificationframework.featureextraction.inference;

/*
 * #%L
 * FeatureInferrerFirstNameGender.java - classificationframework - CASM Consulting - 2,013
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

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * See the FeatureInferrer class for the function of FeatureInferrers.
 *
 * This class infers features which help to indicate whether a first name is male or female.
 *
 * Given a tokenised document, the first token is assumed to be the first name.
 *
 * The inferrer will produce NO features if any of the following are TRUE:
 *
 *  1. The first name is less than 2 characters
 *  2. The first name is entirely composed of punctuation
 *
 * User: Andrew D. Robertson
 * Date: 27/11/2013
 * Time: 13:26
 */
public class FeatureInferrerFirstNameGender extends FeatureInferrer{

    private static final long serialVersionUID = 0L;

    private static final String FEATURE_TYPE_FIRST_NAME = "firstName";
    private static final String FEATURE_TYPE_LAST_IS_VOWEL = "lastIsVowel";
    private static final String FEATURE_TYPE_LAST_LETTER = "lastLetter";
    private static final String FEATURE_TYPE_LAST_TWO_LETTERS = "lastTwoLetters";

    private static final Set<String> vowels = Sets.newHashSet("a", "e", "i", "o", "u", "y");
    private static final Pattern punctPattern = Pattern.compile("[!?\"#$%&'()*+,-./:;<=>@\\[\\]^_`{|}~]+");

    @Override
    public List<Feature> addInferredFeatures(Document document, List<Feature> featuresSoFar) {
        // If there are no words in the document, return now.
        if (document.size() == 0) return featuresSoFar;

        // Assume the first word in the document is the first name
        String firstName = document.get(0).get("form").trim();

        // If the first name is longer than 1 character and not entirely composed of punctuation, then do feature extraction.
        if (firstName.length() > 1 && !punctPattern.matcher(firstName).matches()) {
            // -- Feature extraction --

            // Whole name
            featuresSoFar.add(new Feature(firstName,
                    FEATURE_TYPE_FIRST_NAME));

            // Whether last letter is a vowel
            featuresSoFar.add(new Feature("lastIsVowel:" + Boolean.toString(vowels.contains(firstName.substring(firstName.length() - 1))),
                    FEATURE_TYPE_LAST_IS_VOWEL));

            // Last letter
            featuresSoFar.add(new Feature("lastLetter:" + firstName.substring(firstName.length() - 1),
                    FEATURE_TYPE_LAST_LETTER));

            // Last two letters
            featuresSoFar.add(new Feature("lastTwo:" + firstName.substring(firstName.length() - 2),
                    FEATURE_TYPE_LAST_TWO_LETTERS));
        }

        // *featuresSoFar* will be unchanged if the first word was less than 2 characters or entirely composed of punctuation.
        return featuresSoFar;
    }

    @Override
    public Set<String> getFeatureTypes() {
        return Sets.newHashSet(FEATURE_TYPE_FIRST_NAME, FEATURE_TYPE_LAST_IS_VOWEL, FEATURE_TYPE_LAST_LETTER, FEATURE_TYPE_LAST_TWO_LETTERS);
    }

    @Override
    public boolean isThreadSafe() {
        return true;
    }
}
