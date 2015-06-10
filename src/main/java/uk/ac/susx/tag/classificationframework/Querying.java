package uk.ac.susx.tag.classificationframework;

/*
 * #%L
 * Querying.java - classificationframework - CASM Consulting - 2,013
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

import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import it.unimi.dsi.fastutil.ints.*;
import uk.ac.susx.tag.classificationframework.datastructures.ProcessedInstance;
import uk.ac.susx.tag.classificationframework.exceptions.FeatureExtractionException;
import uk.ac.susx.tag.classificationframework.exceptions.QueryingException;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.FeatureExtractionPipeline;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class provides ways of suggesting features for an annotator to label.
 *
 * It is composed of static methods divided into 2 categories:
 *
 *  1. Instance Querying
 *     - randomInstances() : randomly re-order a list of ProcessedInstances
 *     - queryInstances()  : sort list of ProcessedInstances by decreasing posterior class entropy (active learning)
 *
 *  2. Feature Querying
 *     - commonFeatures()  : propose for each class label a (the same) list of the overall most common features
 *     - queryFeatures()   : propose for each class label a list of features which are correlated with that class and
 *                           have the highest information gain.
 *     - getAlphaValue()   : Given the proportion of occurrence of a feature, propose an alpha value for it.
 *     - labelledFeatures2Strings() : Various solutions for converted indexed features to their string representations
 *
 * User: Andrew D. Robertson
 * Date: 02/09/2013
 * Time: 10:11
 */
public class Querying {

/**************
 * Instance Querying
 **************/

    /**
     * Randomly reorder instances IN-PLACE
     */
    public static List<ProcessedInstance> randomInstances(List<ProcessedInstance> instances){
        Collections.shuffle(instances);
        return instances;
    }

    /**
     * Sort instances IN-PLACE by decreasing posterior class entropy (element at index 0 has
     * highest entropy over labels given features).
     *
     * IMPORTANT: Function will use the "getLabelProbabilities()" function  on the instances
     * to get its label probabilities. So classify the instances first.
     */
    public static List<ProcessedInstance> queryInstances(List<ProcessedInstance> instances){
        Collections.sort(instances, new EntropyOrdering().reverse()); // Sort by most entropy to least.
        return instances;
    }

    /**
     * Calculate the posterior class entropy of an Instance. This can be used to rank
     * instances by an approximation of information gain (for active learning querying).
     */
    private static double labelEntropy(ProcessedInstance instance) {
        double entropy = 0;
        for (double probability : instance.getLabelProbabilities().values()){
            entropy -= (probability > 1e-7)? probability * Math.log(probability): 0;
        }
        return entropy;
    }

    private static class EntropyOrdering extends Ordering<ProcessedInstance> {
        @Override
        public int compare(ProcessedInstance instance1, ProcessedInstance instance2) {
            if (instance1==null || instance2==null) throw new NullPointerException("A ProcessedInstance is null");
            return Double.compare(labelEntropy(instance1), labelEntropy(instance2));
        }
    }

/***************
* Feature Querying
***************/

    // FEATURE QUERYING UTILITIES

    public static Map<String, Double> getAlphaValue(Set<String> features, Iterable<ProcessedInstance> documents, FeatureExtractionPipeline pipeline,
                                                    double min, double max, double beforeScaling, double saturation){

        Map<String, Double> featureCountFractions = Util.documentOccurrenceFractions(features, documents, pipeline);
        Map<String, Double> featureAlphas = new HashMap<>();
        for (Map.Entry<String, Double> entry : featureCountFractions.entrySet()){
            featureAlphas.put(entry.getKey(), getAlphaValue(entry.getValue(), min, max, beforeScaling, saturation));
        }
        return featureAlphas;
    }

    /**
     * Acquire an alpha for a feature which is scaled by the feature's frequency of occurrence.
     * Provide a minimum alpha value, and a maximum alpha value. A saturation parameter allows
     * for the maximum alpha to be reached without requiring that the feature has 100% of all
     * the feature counts. The scale is linear from the minimum alpha to the maximum at point
     * of saturation. For the saturation to have no effect, set it to 1. For the beforeScaling
     * to have no effect, set it to 0.
     *
     * Example usage:
     *
     * I wish to get an alpha value between 0.1 and 50. I would like an alpha of 50 to be achieved if the
     * feature occurs in 10% or more of the data. Then for each feature 'f', I would do the following:
     *
     * featureCountFraction = count(f) / totalCountOfAllFeatures()
     * alpha = getAlphaValue(featureCountFraction, 0.1, 50, 0.1);
     *
     * @param featureCountFraction A value between 0 and 1 = (Count of feature we're interested in) / (total count of all features)
     * @param min The minimum alpha value.
     * @param max The maximum alpha value.
     * @param beforeScaling When the featureCountFraction is below this threshold, the alpha will always be equal to min (should be number between 0 and 1)
     * @param saturation When the featureCountFraction is above this threshold, the alpha will always be equal to max.
     * @return An alpha value scaled by the frequency of occurrence of a feature.
     */
    public static double getAlphaValue(double featureCountFraction, double min, double max, double beforeScaling, double saturation){
        max = Math.max(max, min); // Validation
        saturation = Math.max(saturation, beforeScaling+1E-4); //Validation
        return Math.max(
                Math.min(((max-min)/(saturation-beforeScaling))
                            *(featureCountFraction-beforeScaling)+min,
                         max),
                min);
    }

    /**
     * By default, this call starts scaling alpha from 0%, and the saturation is 10%.
     */
    public static double getAlphaValue(double featureCountFraction, double min, double max){
        return getAlphaValue(featureCountFraction, min, max, 0, 0.1);
    }

    public static double getAlphaValue(double featureCountFraction){
        return getAlphaValue(featureCountFraction, 0.01, 50);
    }

    public static double getAlphaValueScaledByIndicativeness(double featureCountFraction, double min, double max, double beforeScaling, double saturation, double indicativenessFraction){
        double alpha = getAlphaValue(featureCountFraction, min, max, beforeScaling, saturation);
        return ((alpha - min)*indicativenessFraction) + min;
    }

    public static double getAlphaValueScaledByIndicativeness(double featureCountFraction, double min, double max, double indicativenessFraction){
        return getAlphaValueScaledByIndicativeness(featureCountFraction, min, max, 0, 0.1, indicativenessFraction);
    }

    /**
     * Given the output of either commonFeatures() or queryFeatures(), de-index them
     * to their String values using the given pipeline.
     * Mapping: Label --> List of features
     */
    public static Map<String, List<String>> labelledFeatures2Strings(Int2ObjectMap<? extends IntCollection> labelledFeatures,
                                                                     FeatureExtractionPipeline pipeline){
        Map<String, List<String>> convertedLabelledFeatures = new HashMap<>();
        for (Int2ObjectMap.Entry<? extends IntCollection> entry : labelledFeatures.int2ObjectEntrySet()){
            LinkedList<String> features = new LinkedList<>();
            for (int feature : entry.getValue()) {
                features.add(pipeline.featureString(feature));
            }
            convertedLabelledFeatures.put(pipeline.labelString(entry.getIntKey()), features);
        }
        return convertedLabelledFeatures;
    }

    /**
     * Given a mapping from indexed features to doubles (e.g. alphas, probabilities, frequencies), converted to a
     * mapping from the actual string values of the features to their alphas. The use case in mind is when you've
     * passed the "featureCountFractions" arg to queryFeatures() and acquired a mapping from feature indexes to their
     * fraction of occurrence in the data.
     */
    public static Map<String, Double> labelledFeatures2Strings(Int2DoubleMap labelledFeatures,
                                                               FeatureExtractionPipeline pipeline){
        Map<String, Double> convertedFeatures = new HashMap<>();
        for (Int2DoubleMap.Entry entry : labelledFeatures.int2DoubleEntrySet()){
            convertedFeatures.put(pipeline.labelString(entry.getIntKey()), entry.getDoubleValue());
        }
        return convertedFeatures;
    }

    // FEATURE QUERYING IN THE ABSENCE OF LABELLED FEATURE DATA

    /**
     * Find the K most common features in a list of documents. For each label in *labels* plus
     * any label also encountered in the documents, assign a mapping from the label to the list
     * of K most common features.
     */
    public static Int2ObjectMap<IntList> commonFeatures(Iterable<ProcessedInstance> documents,
                                                        Collection<Integer> labels,
                                                        int K) {
        // Count feature occurrences (multiple occurrences in same document count as 1)
        IntSet labelVocab = new IntOpenHashSet();
        if (labels!=null) labelVocab.addAll(labels);
        Int2IntOpenHashMap featureCounts = new Int2IntOpenHashMap();
        for (ProcessedInstance document : documents) {
            for (int feature : new IntOpenHashSet(document.features)){
                featureCounts.addTo(feature, 1);
            }
            if (document.getLabel()!=-1) labelVocab.add(document.getLabel());
        }

        // Find the top K most common features
        IntList topKFeatures = new IntArrayList();
        for (Int2IntMap.Entry entry : new FeatureCountOrdering().greatestOf(featureCounts.int2IntEntrySet(), K)){
            topKFeatures.add(entry.getIntKey());
        }

        // Assign them to each label
        Int2ObjectMap<IntList> topKFeaturesPerLabel = new Int2ObjectOpenHashMap<>();
        for (int label : labelVocab)  topKFeaturesPerLabel.put(label, topKFeatures);

        return topKFeaturesPerLabel;
    }

    // FEATURE QUERYING GIVEN PRIOR KNOWLEDGE

    public static Int2ObjectMap<IntList> queryFeatures(Collection<ProcessedInstance> documents,
                                                       Int2ObjectMap<Int2DoubleOpenHashMap> labelledFeatures,
                                                       int K){
        return queryFeatures(documents, labelledFeatures, K, 0.75, null, null);
    }

    public static Int2ObjectMap<IntList> queryFeatures(Collection<ProcessedInstance> documents,
                                                       Int2ObjectMap<Int2DoubleOpenHashMap> labelledFeatures,
                                                       int K,
                                                       IntSet exceptions){
        return queryFeatures(documents, labelledFeatures, K, 0.75, exceptions, null);
    }

    /**
     * For each class, find the K features with the highest per-class information gain.
     * Then for each label, for each of those identified features, keep only those which are
     * correlated with that label, where "correlation" means that the feature occurred in a
     * document with said label at least 75% as much as with the label with which it occurred most.
     * @param documents The documents from which to calculate stats
     * @param labelledFeatures The features already labelled by the user
     * @param K The top K high IG features will be chosen before filtering by correlation
     * @param correlationThreshold This is the fraction of joint occurrences of a feature and class that is considered correlation, of the most correlated class
     * @param exceptions Set of indexed features to be ignored (e.g. stopwords, streaming keywords)
     * @param labelledFeatureData A data structure which if not null will be reset and filled with information pertinent to labelled features (e.g. used by alpha weighting)
     */
    public static Int2ObjectMap<IntList> queryFeatures(Collection<ProcessedInstance> documents,
                                                       Int2ObjectMap<Int2DoubleOpenHashMap> labelledFeatures,
                                                       int K,
                                                       double correlationThreshold,
                                                       IntSet exceptions,
                                                       LabelledFeatureData labelledFeatureData){

        // Initialisation
        Int2IntOpenHashMap featureCounts = new Int2IntOpenHashMap(); // Frequency of each feature
        Int2IntOpenHashMap labelCounts = new Int2IntOpenHashMap();   // Frequency of each label
        Int2ObjectMap<Int2IntOpenHashMap> jointCounts = new Int2ObjectOpenHashMap<>(); // Frequency of each feature per label
        Int2ObjectMap<Int2DoubleOpenHashMap> perLabelInfoGain = new Int2ObjectOpenHashMap<>();
        int numDocuments = documents.size();

        // Obtain counts (non-probabilistic)
        for (ProcessedInstance document : documents) {

            IntSet features = new IntOpenHashSet(document.features); // Only count features once per document for IG calc.
            int label = document.getLabel();
            if (label == -1) throw new FeatureExtractionException("A ProcessedInstance is unlabelled: " + document);

            for (int feature : features) {
                if (exceptions != null && exceptions.contains(feature)) continue; // Don't collect information on features that we want to ignore
                featureCounts.addTo(feature, 1);
                try {
                    jointCounts.get(label).addTo(feature, 1);
                } catch (NullPointerException e){ // If label wasn't in joint counts (will only happen once on the first time a label is seen)
                    jointCounts.put(label, new Int2IntOpenHashMap());
                    jointCounts.get(label).addTo(feature, 1);
                }
            }
            labelCounts.addTo(label, 1);
        }

        // Handle edge-case in which a feature is labelled with a label that doesn't appear in the data.
        for(int label : labelledFeatures.keySet()) {
            if(!jointCounts.containsKey(label)) {
                jointCounts.put(label, new Int2IntOpenHashMap());
                labelCounts.addTo(label, 0);
            }
        }

        /*
        Per-label entropy.

        The entropy of each label is calculated one-versus-the-rest. So given labels L = {l1, l2, l3}, the entropy
        of label l1 is:

            H(l1) =  -(  P(l1) * log(P(l1)) + 1-P(l1) * log(1-P(l1))  )

        This produces an entropy for each label. Instead of simply calculating a single entropy value over all
        labels:

            H(L) = - SUM(li in L)[  P(li) * log(P(li)) ]

        Essentially, this implies that when considering the entropy of l1, we have no concern with level of uniformity
        of the probabilities of the other labels, our only concern is the uniformity of the probabilities P(l1) and 1-P(l1).

        This means that the closer P(l1) is to 0.5, the greater the entropy.

        Using this we will be able to calculate a per-label information gain also.
        */
        Int2DoubleMap labelEntropies = new Int2DoubleOpenHashMap();
        for (Int2IntMap.Entry entry : labelCounts.int2IntEntrySet()){
            double pLabel = (double)entry.getValue()/numDocuments;   // P(label)
            double pNotLabel = ((double)numDocuments-entry.getIntValue())/numDocuments; // 1 - P(label)
            labelEntropies.put(entry.getIntKey(), entropy(pLabel, pNotLabel)); // Per-label entropy
        }

        // Per-label information gain
        for (int label : labelCounts.keySet()) {perLabelInfoGain.put(label, new Int2DoubleOpenHashMap());}
        for (int feature : featureCounts.keySet()) {
            // Probability of feature independent of label.
            double pFeature = (double)featureCounts.get(feature) / numDocuments;
            double pNotFeature = ((double)numDocuments-featureCounts.get(feature))/numDocuments;

            for (int label : labelCounts.keySet()) {
                // If the feature didn't occur at all, information gain is zero; skip.
                if (featureCounts.get(feature)==0){
                    perLabelInfoGain.get(label).put(feature, 0.0); continue;
                }
                double P, notP;

                // Specific conditional entropy of L given F occurred = H(L|F=present)
                P    = ((double)jointCounts.get(label).get(feature))/featureCounts.get(feature);
                notP = ((double)featureCounts.get(feature)-jointCounts.get(label).get(feature))/featureCounts.get(feature);
                double entropyLGivenF = entropy(P, notP);

                // Specific conditional entropy of L given F did not occur = H(L|F=absent)
                P    = ((double)labelCounts.get(label)-jointCounts.get(label).get(feature))/(numDocuments-featureCounts.get(feature));
                notP = ((double)(numDocuments-featureCounts.get(feature))-(labelCounts.get(label)-jointCounts.get(label).get(feature))) / (numDocuments-featureCounts.get(feature));
                double entropyLGivenNotF = entropy(P, notP);

                /*
                Conditional entropy of L given F = H(L|F)
                This is simply a weighted average of the specific conditional entropies:
                    SUM(f in F)[ P(f) * H(L|F=f) ]
                */
                double conditionalEntropyLGivenF = pFeature*entropyLGivenF + pNotFeature*entropyLGivenNotF;

                // IG(L|F) = H(L) - H(L|F)
                perLabelInfoGain.get(label).put(feature, labelEntropies.get(label) - conditionalEntropyLGivenF);
            }
        }
        return selectTopKCorrelated(perLabelInfoGain, documents, labelledFeatures, K, correlationThreshold, labelledFeatureData);
    }

    private static Int2ObjectMap<IntList> selectTopKCorrelated(Int2ObjectMap<Int2DoubleOpenHashMap> perLabelInfoGain,
                                                               Collection<ProcessedInstance> documents,
                                                               Int2ObjectMap<Int2DoubleOpenHashMap> labelledFeatures,
                                                               int K,
                                                               double correlationThreshold,
                                                               LabelledFeatureData labelledFeatureData){
        // Initialise
        Int2DoubleOpenHashMap labelCounts = new Int2DoubleOpenHashMap();
        Int2ObjectMap<Int2DoubleOpenHashMap> jointCounts = new Int2ObjectOpenHashMap<>();
        for (int label : perLabelInfoGain.keySet()) jointCounts.put(label, new Int2DoubleOpenHashMap());

        if (labelledFeatureData != null) labelledFeatureData.resetData();

        // Acquire probabilistic counts
        for (ProcessedInstance document : documents) {

            // Counts for correlation purposes
            for (Int2DoubleMap.Entry entry : document.getLabelProbabilities().int2DoubleEntrySet()){
                int label = entry.getIntKey();
                double P = entry.getDoubleValue();
                labelCounts.addTo(label, P);
                for (int feature : document.features) jointCounts.get(label).addTo(feature, P);
            }

            // Counts for LabelledFeatureData purposes
            if (labelledFeatureData != null) {
                labelledFeatureData.totalFeatureCount += document.features.length;
                for (int feature : document.features) {
                    if (labelledFeatureData.featureCounts.containsKey(feature)){
                        labelledFeatureData.featureCounts.addTo(feature, 1);
                    }


                    labelledFeatureData.addDocumentToIndex(document, feature);
                }
            }
        }

        // Normalise counts
        for (Int2DoubleMap.Entry labelCount : labelCounts.int2DoubleEntrySet()){
            Int2DoubleOpenHashMap featureCounts = jointCounts.get(labelCount.getIntKey());
            for (Int2DoubleMap.Entry entry : featureCounts.int2DoubleEntrySet()){
                entry.setValue(entry.getDoubleValue()/labelCount.getDoubleValue());
            }
        }

        // Get top K features for each label
        Int2ObjectMap<List<Int2DoubleMap.Entry>> topKFeaturesPerLabel = new Int2ObjectOpenHashMap<>();
        for (Int2ObjectMap.Entry<Int2DoubleOpenHashMap> entry : perLabelInfoGain.int2ObjectEntrySet()){
            List<Int2DoubleMap.Entry> topEntries = new InformationGainOrdering().greatestOf(entry.getValue().int2DoubleEntrySet(), K);
            topKFeaturesPerLabel.put(entry.getIntKey(), topEntries);
        }

        // From top K, keep only those highly correlated (in frequency) with each label
        Int2ObjectMap<IntList> correlatedFeatures = new Int2ObjectOpenHashMap<>();
        for (Int2ObjectMap.Entry<List<Int2DoubleMap.Entry>> topKFeaturesEntry : topKFeaturesPerLabel.int2ObjectEntrySet()){
            int label = topKFeaturesEntry.getIntKey();
            correlatedFeatures.put(label, new IntArrayList());
            for (Int2DoubleMap.Entry featureEntry : topKFeaturesEntry.getValue()){
                int feature = featureEntry.getIntKey();
                if (labelledFeatures==null || !labelledFeatures.containsKey(label) || !labelledFeatures.get(label).containsKey(feature)){ //If isn't already listed as a labelled feature
                    double highestCount = 0;
                    for (Int2DoubleOpenHashMap countsPerLabel : jointCounts.values()){
                        double count = countsPerLabel.get(feature);
                        if (count > highestCount) highestCount = count;
                    }
                    if (jointCounts.get(label).get(feature) > highestCount * correlationThreshold)  //If feature correlates with label
                        correlatedFeatures.get(label).add(feature);
                }
            }
        }
        return correlatedFeatures;
    }

    private static class InformationGainOrdering extends Ordering<Int2DoubleMap.Entry> {
        @Override
        public int compare(Int2DoubleMap.Entry entry1, Int2DoubleMap.Entry entry2) {
            return Double.compare(entry1.getDoubleValue(), entry2.getDoubleValue());
        }
    }

    private static class FeatureCountOrdering extends Ordering<Int2IntMap.Entry> {
        @Override
        public int compare(Int2IntMap.Entry entry1, Int2IntMap.Entry entry2) {
            return Double.compare(entry1.getIntValue(), entry2.getIntValue());
        }
    }

    private static double entropy(double P, double notP){
        if (P == 0 || notP == 0) return (float)0;
        else return  -P*Math.log(P) -notP*Math.log(notP);
    }

    /**
     * An instance of this class can be passed to the feature querying method, so that the user
     * may retain useful information pertinent to the labelled features. If the passed instance
     * already contains any information, it will be removed before adding new information.
     *
     * Currently, the instance holds the following information:
     *
     *  1. Field name "featureCounts":
     *      A mapping from each feature ID to the number of times that it occurs in the data.
     *      Multiple occurrences within the same document ARE taken into account.
     *
     *  2. Field name "totalFeatureCount":
     *      The total number of times that any feature occurs in the data
     *      Multiple occurrences within the same document ARE taken into account.
     */
    public static class LabelledFeatureData {

        public Int2IntOpenHashMap featureCounts = new Int2IntOpenHashMap();
        public int totalFeatureCount = 0;
        public Int2ObjectOpenHashMap<Set<ProcessedInstance>> featureDocumentIndex = new Int2ObjectOpenHashMap<>();

        public LabelledFeatureData() {}

        public void resetData() {
            featureCounts = new Int2IntOpenHashMap();
            totalFeatureCount = 0;
            featureDocumentIndex = new Int2ObjectOpenHashMap<>();
        }

        public void addDocumentToIndex(ProcessedInstance document, int feature) {
            if (!featureDocumentIndex.containsKey(feature))
                featureDocumentIndex.put(feature, new HashSet<ProcessedInstance>());
            featureDocumentIndex.get(feature).add(document);
        }

        /**
         * Given a feature, return the fraction of the times that it occurs in the data.
         *  = feature count / total feature count
         */
        public double getFeatureCountFraction(int feature){
            if (totalFeatureCount == 0) return 0;
            if (!featureCounts.containsKey(feature)) throw new QueryingException("No information on the feature specified. " +
                    "This would happen if you've acquired a LabelledFeatureData instance during querying with a set of " +
                    "labelled features, then altered your set without querying again (and thus collecting data on your new " +
                    "labelled features), so then trying to use the same unchanged LabelledFeatureData instance to get information " +
                    "on a feature that it's never heard of.");
            return ((double)featureCounts.get(feature)) / totalFeatureCount;
        }
    }

}
