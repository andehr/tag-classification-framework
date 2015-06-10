package uk.ac.susx.tag.classificationframework.featureextraction.inference.featureselection;

/*
 * #%L
 * FeatureSelector.java - classificationframework - CASM Consulting - 2,013
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
import uk.ac.susx.tag.classificationframework.datastructures.Document;
import uk.ac.susx.tag.classificationframework.datastructures.Instance;
import uk.ac.susx.tag.classificationframework.featureextraction.inference.FeatureInferrer;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.FeatureExtractionPipeline;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A feature selector has some notion of what features are acceptable.
 *
 * So, once all FeatureInferrers that are before this selector in the pipeline
 * have produced their features, a FeatureSelector will trim that list according
 * to some internal feature vocabulary.
 *
 * When extending this class, you should provide functionality which adds features
 * to the topFeatures field. The selector will do the rest.
 *
 * TODO: Maybe add some ability to completely ignore features that have DF less than N (e.g. 3 like the WFO paper)
 *
 * User: Andrew D. Robertson
 * Date: 13/01/2014
 * Time: 10:36
 */
public abstract class FeatureSelector extends FeatureInferrer {

    private static final long serialVersionUID = 0L;

    protected Set<String> topFeatures = new HashSet<>();
    protected Set<String> additionalFeatures = new HashSet<>();
    protected Set<String> selectedFeatureTypes = new HashSet<>();

    public Set<String> getTopFeatures() { return topFeatures; }
    public Set<String> getSelectedFeatureTypes() { return selectedFeatureTypes; }

    public FeatureSelector() {

    }

    public FeatureSelector(Set<String> selectedFeatureTypes) {
        this.selectedFeatureTypes = selectedFeatureTypes==null? new HashSet<String>() : selectedFeatureTypes;
    }


    public void setAdditionalFeatures(Set<String> replacementAdditionalFeatures){
        additionalFeatures = new HashSet<>(replacementAdditionalFeatures);
    }
    public void addAdditionalFeatures(Set<String> supplementaryAdditionalFeatures){
        additionalFeatures.addAll(supplementaryAdditionalFeatures);
    }
    public void removeAdditionalFeatures(Set<String> featuresToBeRemoved){
        additionalFeatures.removeAll(featuresToBeRemoved);
    }

    @Override
    public Set<String> getFeatureTypes() {
        return new HashSet<>(); // Feature selectors simply prune features from those already created previously in the pipeline; they don't produce their own.
    }

    /**
     * All feature selectors must implement this method. It should look at
     * evidence in the form of feature/label counts, and fill its
     * *topFeatures* parameter.
     */
    public abstract void setTopFeatures(Iterable<Instance> documents, FeatureExtractionPipeline pipeline);

    /**
     * For each feature in *featuresSoFar*, a decision is made whether to keep said feature.
     * If *selectedFeatureTypes* is not empty, then any feature whose type is NOT in *selectedFeatureTypes*
     * will be allowed to pass through. Otherwise, if its type is present in *selectedFeatureTypes*
     * then it most also be one of the *topFeatures* in order to pass.
     *
     * If *selectedFeatureTypes* is empty, then ALL features must pass the *topFeatures* test.
     */
    public List<Feature> addInferredFeatures(Document document, List<Feature> featuresSoFar){
        return selectedFeatureTypes.isEmpty() ? selectFeaturesAllTypes(document, featuresSoFar) : selectFeaturesSpecificTypes(document, featuresSoFar);
    }

    private List<Feature> selectFeaturesAllTypes(Document document, List<Feature> featuresSoFar){
        return featuresSoFar.stream()
                .filter(feature -> topFeatures.contains(feature.value()) || additionalFeatures.contains(feature.value()))
                .collect(Collectors.toList());
    }

    private List<Feature> selectFeaturesSpecificTypes(Document document, List<Feature> featuresSoFar){
        return featuresSoFar.stream()
                .filter(feature -> !selectedFeatureTypes.contains(feature.type()) || topFeatures.contains(feature.value()) || additionalFeatures.contains(feature.value()))
                .collect(Collectors.toList());
    }

    public static Evidence collectEvidence(Iterable<Instance> documents, Set<String> selectedFeatureTypes, FeatureExtractionPipeline pipeline){
        Evidence e = new Evidence();
        for (Instance document : documents)
            e.addEvidence(document.label, pipeline.extractUnindexedFeatures(document), selectedFeatureTypes);
        return e;
    }

    /**
     * Class that can collect evidence about features occurring with particular class labels.
     *
     * An instance of this class is passed to a feature selector in order for it to calculate
     * which features to select.
     *
     * The addEvidence() method is used to add a document of evidence at a time. See the
     * FeatureSelector add() method in FeatureExtractionPipeline.
     *
     * The method names correspond to the variables outline in the feature selection framework
     * presented by Li et al in "A Framework of Feature Selection Method for Text Categorization"
     * who show that 6 popular feature selection methods can all be expressed with according
     * to the variables outlined by the method names.
     */
    public static class Evidence {

        private Object2IntOpenHashMap<String> featureCounts = new Object2IntOpenHashMap<>();
        private Object2IntOpenHashMap<String> labelCounts = new Object2IntOpenHashMap<>();
        private Map<String, Object2IntOpenHashMap<String>> jointCounts = new HashMap<>();
        private int totalDocuments;

        public void addEvidence(String classLabel, List<Feature> document, Set<String> featureTypes){
            if (featureTypes.isEmpty())
                addEvidenceAnyFeatureType(classLabel, document);
            else
                addEvidenceSpecificFeatureTypes(classLabel, document, featureTypes);
        }

        private void addEvidenceAnyFeatureType(String classLabel, List<Feature> document) {
            totalDocuments++;
            labelCounts.addTo(classLabel, 1);
            for (Feature feature : new HashSet<>(document)){
                featureCounts.addTo(feature.value(), 1);
                if (!jointCounts.containsKey(classLabel))
                    jointCounts.put(classLabel, new Object2IntOpenHashMap<String>());
                jointCounts.get(classLabel).addTo(feature.value(), 1);
            }
        }

        private void addEvidenceSpecificFeatureTypes(String classLabel, List<Feature> document, Set<String> featureTypes){
            totalDocuments++;
            labelCounts.addTo(classLabel, 1);
            for (Feature feature : new HashSet<>(document)){
                if (featureTypes.contains(feature.type())) {
                    featureCounts.addTo(feature.value(), 1);
                    if (!jointCounts.containsKey(classLabel))
                        jointCounts.put(classLabel, new Object2IntOpenHashMap<String>());
                    jointCounts.get(classLabel).addTo(feature.value(), 1);
                }
            }
        }

        /**
         * A(C, F) = the number of documents labelled C that contained feature F.
         */
        public int A(String classLabel, String feature) {
            return jointCounts.containsKey(classLabel)? jointCounts.get(classLabel).getInt(feature) : 0;
        }

        /**
         * B(C, F) = the number of documents NOT labelled C that contained feature F.
         */
        public int B(String classLabel, String feature) {
            return featureCounts.getInt(feature) - A(classLabel, feature);
        }

        /**
         * N(C) = the number of documents labelled C
         */
        public int N(String classLabel) {
            return labelCounts.getInt(classLabel);
        }

        /**
         * Nall = total number of documents
         */
        public int Nall() {
            return totalDocuments;
        }

        /**
         * Get the number of documents which contained a particular feature
         */
        public int getFeatureCount(String feature){
            return featureCounts.getInt(feature);
        }

        public Set<String> vocab(){
            return featureCounts.keySet();
        }

        public Set<String> classLabels() {
            return labelCounts.keySet();
        }
    }
}
