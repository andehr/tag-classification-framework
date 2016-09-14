package uk.ac.susx.tag.classificationframework.datastructures;

/*
 * #%L
 * StringIndexer.java - classificationframework - CASM Consulting - 2,013
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

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Class providing a two-way mapping between Strings and indices for those Strings.
 * Fills a similar role as the Mallet "Alphabet" class. But there are some key differences.
 *
 * Currently, managing StringIndexers is left to the user; the user must ensure that the
 * correct indexer is being used.
 *
 * Classifiers DO NOT make use of StringIndexers, they know nothing about them. They perform
 * their calculations on ints - already indexed features and labels. They maintain their own
 * vocabulary set. ProcessedInstances also know nothing of the indexers, simply holding an
 * array of already converted ints. This avoids many "Alphabets Don't Match"-esque issues;
 *
 * The FeatureExtractionPipeline holds the indexers, and after extracting features uses them
 * to convert features to ints, and the Instance labels to ints. The pipeline also provides
 * methods for converting features and labels from and to strings if required.
 *
 * The StringIndexers are serialised with the pipeline (not true as of version 5.8), but they can be replaced with mutator
 * methods if necessary. See FeatureExtractionPipeline class. All classifiers when saved to disk
 * should have their features de-indexed, so that when loading them from disk, a new pipeline
 * could simply be used. This enables us to throw away the string indexers when serialising pipelines.
 *
 * User: Andrew D. Robertson
 * Date: 04/09/2013
 * Time: 12:13
 */
public class StringIndexer implements Serializable {

    private static final long serialVersionUID = 0L;

    private Object2IntOpenHashMap<String> stringIndices = new Object2IntOpenHashMap<>();
    private List<String> strings = new ArrayList<>();

    public StringIndexer() { }

    /**
     * Get the index of a string. If the String is not present in the mapping,
     * then return a new index. If "addIfNotPresent" is true, then add the new
     * index to the mapping for next time.
     */
    public int getIndex(String item, boolean addIfNotPresent){
        int index = -1;
        if (stringIndices.containsKey(item)){
            index = stringIndices.getInt(item);
        } else if (addIfNotPresent) {
            index = strings.size();
            stringIndices.put(item, index);
            strings.add(item);
        }
        return index;
    }
    public int getIndex(String item) {
        return getIndex(item, true);
    }

    /**
     * Convenience method: populate int[] with result of getIndex() on each
     * String in a list of strings. See getIndex().
     */
    public int[] getIndexList(List<String> items, boolean addIfNotPresent){
        int[] indexList = new int[items.size()];
        for(int i=0; i<items.size(); i++){
            indexList[i] = getIndex(items.get(i), addIfNotPresent);
        }
        return indexList;
    }
    public int[] getIndexList(List<String> items){
        return getIndexList(items, true);
    }

    public int[] getIndices()
    {
        return this.stringIndices.values().toIntArray();
    }

    public List<String> getStrings() { return strings; }

    /**
     * Get the String value of an index or null if index not present.
     */
    public String getValue(int index){
        if (index >= 0 && index < strings.size()) {
            return strings.get(index);
        } else {
            return null;
        }
    }

    public String getValue(int index, String indexNotPresentValue){
        if (index >= 0 && index < strings.size()) {
            return strings.get(index);
        } else {
            return indexNotPresentValue;
        }
    }

    /**
     * Check to see whether a String value is present in the mapping.
     */
    public boolean contains(String value) {
        return stringIndices.containsKey(value);
    }

    public int size() {
        return strings.size();
    }

    public String toString() {
        return "IndexMap: " + stringIndices + "\n" + "ItemList: " + strings;
    }
}
