package uk.ac.susx.tag.classificationframework.datastructures;

/*
 * #%L
 * AnnotatedToken.java - classificationframework - CASM Consulting - 2,013
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

import cmu.arktweetnlp.Tagger;
import uk.ac.susx.tag.classificationframework.exceptions.FeatureExtractionException;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * An AnnotatedToken represents a token during the feature extraction phase.
 * It is capable of receiving String properties which can then be used when extracting features.
 * It is usually present in a list of tokens as part of a Document object.
 *
 * A token must be instantiated with at least a "form" attribute, which is the actual token text.
 *
 * During feature extraction, the "filtered" property will be set if upon seeing the token a TokenFilter
 * asserts that the token should be filtered. Subsequent feature extractors can choose whether or not to ignore
 * this property by accessing the "isFiltered" method. See FeatureExtractionPipeline class and FeatureInferrer class.
 *
 * Using the "get()" with an attribute type will return that attribute, or throw a FeatureExtractionException if the attribute ain't present.
 *
 * Alternatively the "getWithNullFeature()" will do the same thing, if the attribute is present, other it'll return
 * the contents of the nullFeature field.
 *
 * User: Andrew D. Robertson
 * Date: 27/07/2013
 * Time: 12:38
 */
public class AnnotatedToken implements Serializable {

    private static final long serialVersionUID = 0L;

    private Map<String, String> attributes;
    public static final String nullFeature = "FeatureNotPresent";
    private boolean filtered = false;
    private int start;
    private int end;

    public AnnotatedToken(){
        attributes = new HashMap<>();
    }

    public AnnotatedToken(String form) {
        attributes = new HashMap<>();
        attributes.put("form", form);
    }

    public AnnotatedToken(String form, Map<String, String> attributes) {
        this.attributes = attributes;
        attributes.put("form", form);
    }

    public AnnotatedToken(Map<String, String> attributes) throws FeatureExtractionException {
        if (!attributes.containsKey("form"))
            throw new FeatureExtractionException("'form' must be present in attributes of AnnotatedToken");
        this.attributes = attributes;
    }

    /**
     * Convenience constructor. Creates a token from a CMU tagger
     * TaggedToken object.
     * @param taggedToken
     */
    public AnnotatedToken(Tagger.TaggedToken taggedToken){
        attributes = new HashMap<>();
        attributes.put("form", taggedToken.token);
        attributes.put("pos", taggedToken.tag);
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void put(String featureType, String feature){
        attributes.put(featureType, feature);
    }

    public String get(String featureType) throws FeatureExtractionException {
        if (attributes.containsKey(featureType)) return attributes.get(featureType);
        else throw new FeatureExtractionException("Feature type '" + featureType + "' not present on token.");
    }

    public String getOrNull(String featureType) {
        if (attributes.containsKey(featureType))
            return attributes.get(featureType);
        else {
            return null;
        }
    }

    public String getWithNullFeature(String featureType){
        return attributes.containsKey(featureType)? attributes.get(featureType) : nullFeature;
    }

    public boolean has(String featureType){
        return attributes.containsKey(featureType);
    }

    public boolean isFiltered() {
        return filtered;
    }

    public void setFiltered(boolean filtered) {
        this.filtered = filtered;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(attributes.get("form"));
        if (attributes.size()>1){
            sb.append(" (");
            for (Map.Entry<String, String> entrySet : attributes.entrySet()) {
                if (!entrySet.getKey().equals("form")) {
                    sb.append(entrySet.getKey());
                    sb.append(": \"");
                    sb.append(entrySet.getValue());
                    sb.append("\", ");
                }
            }
            sb.replace(sb.length()-3, sb.length(), "\")");
        }
        return sb.toString();
    }

    public int start() {
        return start;
    }

    public void start(int start) {
        this.start = start;
    }

    public int end() {
        return end;
    }

    public void end(int end) {
        this.end = end;
    }
}
