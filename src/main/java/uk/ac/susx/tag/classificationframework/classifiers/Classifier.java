package uk.ac.susx.tag.classificationframework.classifiers;

/*
 * #%L
 * Classifier.java - classificationframework - CASM Consulting - 2,013
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

import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntSet;
import uk.ac.susx.tag.classificationframework.datastructures.ProcessedInstance;

/**
 * Interface defining the behaviour of a classifier.
 *
 * See methods for details.
 *
 * All features and labels should be of type int (e.g. by externally indexing them).
 * The classifier is therefore  agnostic to type of features and labels.
 *
 * The classifiers should be implemented making use of fastutil's collections.
 *
 * User: Andrew D. Robertson
 * Date: 25/07/2013
 * Time: 18:28
 */
public interface Classifier {

    /**
     * @return Set of all class labels
     */
    public IntSet getLabels();

    /**
     * @return Set of all feature vocabulary
     */
    public IntSet getVocab();

    /**
     * @param features A document as a collection of features.
     * @return A mapping from each label to its probability. Returning a map of
     * type "Int2DoubleOpenHashMap" is useful because if it is queried for the
     * probability of a label that the  classifier did not know about, the
     * probability returned will be 0.
     */
    public Int2DoubleOpenHashMap predict(int[] features);

    /**
     * @param features A document as a collection of features.
     * @return The most probable label.
     */
    public int bestLabel(int[] features);

    // TODO: Add all of weight parameterised variants as well?
    public void train(Iterable<ProcessedInstance> labelledDocuments);
    public void train(Iterable<ProcessedInstance> labelledDocuments, Iterable<ProcessedInstance> unlabelledDocuments);

}
