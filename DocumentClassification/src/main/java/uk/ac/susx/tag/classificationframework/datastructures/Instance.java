package uk.ac.susx.tag.classificationframework.datastructures;

/*
 * #%L
 * Instance.java - classificationframework - CASM Consulting - 2,013
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

import java.io.Serializable;

/**
 * Class representing a tweet from the database.
 * - its label may be the empty string.
 * - the id should be the id of the tweet, and
 * - the text should be the tweet's text.
 *
 * In order to be used in training or prediction, the Instance will need to be
 * passed through a FeatureExtractionPipeline to produce a ProcessedInstance object.
 *
 * The "ProcessedInstance" class in this package is a wrapper for a training label and
 * collection of features, and can be passed to a classifier for training/prediction.
 * Well, during the prediction phase, the classifier only needs the "features" field
 * of the ProcessedInstance, which is an int[].
 *
 * User: Andrew D. Robertson
 * Date: 06/08/2013
 * Time: 12:21
 */
public class Instance implements Serializable{

    private static final long serialVersionUID = 0L;

    public String label;    // Class label
    public String text;     // Original text of tweet
    public final String id; // Tweet ID

    public Instance(String label, String text, String id) {
        this.label = label;
        this.text = text;
        this.id = id;
    }

    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append("{id="); sb.append(id); sb.append(", ");
        sb.append("label="); sb.append(label); sb.append(", ");
        sb.append("text="); sb.append(text); sb.append("}");
        return sb.toString();
    }
}
