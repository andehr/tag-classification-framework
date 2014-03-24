package uk.ac.susx.tag.classificationframework.datastructures;

/*
 * #%L
 * ProcessedInstance.java - classificationframework - CASM Consulting - 2,013
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

import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;

import java.util.Map;
import java.util.Random;

/**
 * Represents a processed (passed through feature extraction) Instance.
 * An instance is generally acquired by passing an Instance object through
 * the "extractFeatures" function of a FeatureExtractionPipeline instance.
 *
 * Maintains a reference to the source Instance. Has its own notion of
 * a label, which if re-assigned does NOT write back to the source Instance.
 *
 * The label can be set using one of the setLabelling() methods. Either:
 *
 *  1. A single label can be passed. in which case it is assumed the probability of this
 *     label is 1 and all others zero. Or alternatively,
 *  2. A probability distribution over labels can be passed in. In which case the label field is set to
 *     be the most probable.
 *
 *  The most probable label can be retrieved by calling getLabels().
 *  The probability distribution can be retrieved by calling getLabelProbabilities().
 *
 *  ProcessedInstances represent features and labels with their int indexed form.
 *
 * User: Andrew D. Robertson
 * Date: 07/08/2013
 * Time: 15:39
 */
public class ProcessedInstance {

    public int[] features;
    public Instance source;

    private int label;
    private Int2DoubleOpenHashMap labelProbabilities;

    /**
     * @param label Should be -1 if no label can be assigned.
     */
    public ProcessedInstance(int label, int[] features, Instance source) {
        this.label = label;
        this.features = features;
        this.source = source;
        this.labelProbabilities = new Int2DoubleOpenHashMap();
        if (label >= 0) this.labelProbabilities.addTo(label, 1.0);
    }

    /**
     * @return the most probable label.
     */
    public int getLabel(){ return label; }

    public boolean hasFeatures() {
        return !(features.length == 0);
    }

    /**
     * Given a label, return the probability P(label|instance)
     */
    public double getLabelProbability(int label){ return labelProbabilities.get(label); }

    public void resetLabeling(){
        label = -1;
        labelProbabilities = new Int2DoubleOpenHashMap();
    }

    /**
     * @return the probability distribution over labels.
     */
    public Int2DoubleOpenHashMap getLabelProbabilities(){ return labelProbabilities; }

    /**
     * Set the probability of *label* to 1 and all others to zero. Technically,
     * No other labels are represented, so a "getLabelProbabilities()" will return
     * a map with only an entry containing *label*. However if that particular type
     * of map is queried with a label that isn't present, a probability of 0 is returned.
     */
    public void setLabeling(int label){
        if (label >= 0) {
            this.label = label;
            this.labelProbabilities = new Int2DoubleOpenHashMap();
            this.labelProbabilities.addTo(label, 1.0);
        }
    }

    /**
     * Set the probability distribution of the labels. The label field returned
     * by getLabel() is updated to contain the most probable label in the probability
     * distribution.
     *
     * NOTE: Assumes all probabilities in labelProbabilities add to 1.
     *       Specifically, if any probability p is within 1E-6 of 1/labelProbabilities.size()
     *       Then the probabilities are assumed to be uniform, and a random label is
     *       to be assigned as the most likely label for this instance.
     */
    public void setLabeling(Int2DoubleOpenHashMap labelProbabilities){
        this.labelProbabilities = labelProbabilities;

        if (labelProbabilities.isEmpty()){
            label = -1;
        } else {
            boolean uniformProbabilities = true; // Set to false if the probabilities are not uniform
            double uniformProbability = 1 / (double)labelProbabilities.size();
            int maxLabel = -1;   // Set to the highest probability label
            double maxProbability = 0;

            for (Int2DoubleMap.Entry entry : labelProbabilities.int2DoubleEntrySet()){
                double p = entry.getDoubleValue();
                if (p > maxProbability) {
                    maxLabel = entry.getIntKey();
                    maxProbability = p;
                }
                if (uniformProbabilities && Math.abs(p-uniformProbability) > 1E-6) uniformProbabilities = false;
            }

            if (uniformProbabilities){ // If the probabilities are uniform, the best label is chosen by random
                int[] labels = labelProbabilities.keySet().toIntArray();
                label = labels[new Random().nextInt(labels.length)];
            } else label = maxLabel;
        }
    }

    /**
     * If you can call the above version of this method which takes a
     * Int2DoubleOpenHashMap, then all the better, because the Map will
     * just be converted to an Int2DoubleOpenHashMap anyway.
     */
    public void setLabeling(Map<Integer,Double> labelProbabilities){
        setLabeling(new Int2DoubleOpenHashMap(labelProbabilities));
    }

    /**
     * NOTE: the implementations of "setLabeling" all assign a random
     * best label if the probabilities are uniform. This method just
     * uses the first label it sees.
     * Set the probability distribution of the labels. The label field returned
     * by getLabel() is updated to contain the most probable label in the probability
     * distribution.
     */
    public void setLabelingNonRandom(Int2DoubleOpenHashMap labelProbabilities) {
        this.labelProbabilities = labelProbabilities;
        int maxLabel = -1;
        double maxProbability = 0;
        for (Int2DoubleMap.Entry entry : labelProbabilities.int2DoubleEntrySet()){
            if (entry.getDoubleValue() > maxProbability) {
                maxLabel = entry.getIntKey();
                maxProbability = entry.getDoubleValue();
            }
        }
        label = maxLabel;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Instance instance = (Instance) o;

        if (source.id != null ? !source.id.equals(instance.id) : instance.id != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return source.id != null ? source.id.hashCode() : 0;
    }
}
