package uk.ac.susx.tag.classificationframework.featureextraction.inference;

/*
 * #%L
 * FeatureInferrerUnigrams.java - classificationframework - CASM Consulting - 2,013
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

import java.util.List;
import java.util.Set;

/**
 * See the FeatureInferrer class for the function of FeatureInferrers.
 *
 * This inferrer adds unigrams as features.
 *
 * User: Andrew D. Robertson
 * Date: 21/08/2013
 * Time: 13:44
 */
public class FeatureInferrerUnigrams extends FeatureInferrer {

    private static final long serialVersionUID = 0L;

    private static final String FEATURE_TYPE_UNIGRAM = "unigram";

    @Override
    public List<Feature> addInferredFeatures(Document document, List<Feature> featuresSoFar) {
        for (AnnotatedToken token : document){
            if (!token.isFiltered() && !token.get("form").isEmpty()) {
                featuresSoFar.add(new Feature(token.get("form"), FEATURE_TYPE_UNIGRAM));
            }
        }
        return featuresSoFar;
    }

    @Override
    public Set<String> getFeatureTypes() {
        return Sets.newHashSet(FEATURE_TYPE_UNIGRAM);
    }

    @Override
    public boolean isThreadSafe() {
        return true;
    }
}
