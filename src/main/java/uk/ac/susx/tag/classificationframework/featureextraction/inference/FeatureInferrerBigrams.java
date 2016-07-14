package uk.ac.susx.tag.classificationframework.featureextraction.inference;

/*
 * #%L
 * FeatureInferrerBigrams.java - classificationframework - CASM Consulting - 2,013
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
import uk.ac.susx.tag.classificationframework.datastructures.AnnotatedToken;
import uk.ac.susx.tag.classificationframework.datastructures.Document;

import java.io.Serializable;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * See the FeatureInferrer class for the function of FeatureInferrers.
 *
 * This inferrer adds bigrams.
 *
 * The inclusion of filtered tokens is optional and they are excluded by default.
 *
 * The inclusion of unfiltered punctuation is optional; they are excluded by default.
 * The mode by which a token is considered punctuation is also configurable by
 * supplying an object implementing PunctuationChecker.
 *
 * By default punctuation is identified by regex. The regex is equivalent to "\p{Punct}"
 *
 * All unfiltered punctuation can be included by supplying null for "puncChecker", or
 * a PunctuationChecker which always returns false for "isPunctuation".
 *
 * User: Andrew D. Robertson
 * Date: 08/08/2013
 * Time: 15:24
 */
public class FeatureInferrerBigrams extends FeatureInferrer{

    private static final long serialVersionUID = 0L;

    private static final String FEATURE_TYPE_BIGRAM = "bigram";

    private boolean includeFilteredTokens;
    private PunctuationChecker puncChecker;

    public FeatureInferrerBigrams(boolean includeFilteredTokens,
                                  PunctuationChecker puncChecker){
        this.includeFilteredTokens = includeFilteredTokens;
        this.puncChecker = puncChecker;
    }

    public FeatureInferrerBigrams(){
        this(false, new PunctuationCheckerByRegex("[!?\"#$%&'()*+,-./:;<=>@\\[\\]^_`{|}~]+"));
    }

    @Override
    public List<Feature> addInferredFeatures(Document document, List<Feature> featuresSoFar) {
        String previousToken = null;
        for (AnnotatedToken token : document) {
            if (!token.isFiltered() || includeFilteredTokens) {
                if (puncChecker == null || !puncChecker.isPunctuation(token)) {
                    if (previousToken != null) {
                        featuresSoFar.add(new Feature(previousToken + "_" + token.get("form"), FEATURE_TYPE_BIGRAM));
                    }
                    previousToken = token.get("form");
                }
            }
        }
        return featuresSoFar;
    }

    @Override
    public Set<String> getFeatureTypes() {
        return Sets.newHashSet(FEATURE_TYPE_BIGRAM);
    }

    /*************
     * Classes used to identify punctuation
     ************/

    public static interface PunctuationChecker extends Serializable {
        public boolean isPunctuation(AnnotatedToken token);
    }

    public static class PunctuationCheckerByPos implements PunctuationChecker{
        private static final long serialVersionUID = 0L;

        private String punctuationPos;

        public PunctuationCheckerByPos (String punctuationPos) { this.punctuationPos = punctuationPos; }

        @Override
        public boolean isPunctuation(AnnotatedToken token) {
            return token.get("pos").equals(punctuationPos);
        }
    }

    public static class PunctuationCheckerByRegex implements PunctuationChecker{
        private static final long serialVersionUID = 0L;

        private Pattern puncPattern;

        public PunctuationCheckerByRegex(String puncPattern) {
            this.puncPattern = Pattern.compile(puncPattern);
        }
        @Override
        public boolean isPunctuation(AnnotatedToken token) {
            return puncPattern.matcher(token.get("form")).matches();
        }
    }

    @Override
    public boolean isThreadSafe() {
        return true;
    }

}
