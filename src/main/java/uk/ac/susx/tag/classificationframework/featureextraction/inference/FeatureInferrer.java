package uk.ac.susx.tag.classificationframework.featureextraction.inference;

/*
 * #%L
 * FeatureInferrer.java - classificationframework - CASM Consulting - 2,013
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

import uk.ac.susx.tag.classificationframework.datastructures.Document;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.PipelineComponent;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A Feature inferrer uses properties of the tokens in a document
 * to infer features (which are Strings). It is the only type of
 * PipelineComponent which actually produces features.
 *
 * It is also possible to do additional document processing if to do so
 * beforehand would be inconvenient. But it's better to modularise this
 * into a DocProcessor.
 *
 * Any document processing done here though will only be available to
 * subsequent FeatureInferrers in the pipeline.
 *
 * User: Andrew D. Robertson
 * Date: 08/08/2013
 * Time: 14:57
 */
public abstract class FeatureInferrer extends PipelineComponent {

    private static final long serialVersionUID = 0L;

    /**
     * Given an entire document of tokens, and a collection of features so far extracted,
     * extend the collection with new features which can be inferred from those tokens.
     * The implementing class is responsible for returning the modified feature collection.
     *
     * @param featuresSoFar A collection of features extracted from preceding inferrers in the pipeline
     */
    public abstract List<Feature> addInferredFeatures(Document document, List<Feature> featuresSoFar);

    public List<List<Feature>> addInferredFeaturesFromBatch(List<Document> documents, List<List<Feature>> featuresSoFarPerDocument){
        throw new UnsupportedOperationException();
    }

    /**
     * Every instance of Feature returned by the addInferredFeatures function should have a type, indicating what
     * type of feature it is (e.g. bigram, dependency-ngram, etc.). This set should be a set of all the possible
     * feature types that this inferrer can produce. This allows feature selectors to ask an inferrer to tell it
     * which features it should select on if it wants to select on features from that inferrer.
     */
    public abstract Set<String> getFeatureTypes();


    public static class Feature {

        private String value;  // What the classifier sees
        private String type;   // Identifier for the type of the feature (e.g. "unigram")
        public Map<String, Object> attributes = null;

        public Feature(String value, String type){
            this.value = value;
            this.type = type;
        }

        public String toString() {return value;}

        public String value() { return value; }
        public String type() { return type; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Feature feature = (Feature) o;

            if (type != null ? !type.equals(feature.type) : feature.type != null) return false;
            if (value != null ? !value.equals(feature.value) : feature.value != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = value != null ? value.hashCode() : 0;
            result = 31 * result + (type != null ? type.hashCode() : 0);
            return result;
        }
    }
}
