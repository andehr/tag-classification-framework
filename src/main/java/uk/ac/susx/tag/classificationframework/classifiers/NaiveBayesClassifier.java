package uk.ac.susx.tag.classificationframework.classifiers;

/*
 * #%L
 * NaiveBayesClassifier.java - classificationframework - CASM Consulting - 2,013
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

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntIterable;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import org.apache.commons.math.util.MathUtils;
import uk.ac.susx.tag.classificationframework.datastructures.ModelState.ClassifierName;
import uk.ac.susx.tag.classificationframework.datastructures.ProcessedInstance;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.FeatureExtractionPipeline;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Naive bayes classifier which stores its data as counts.
 * Implements EM learning. Supports incremental training.
 *
 *  Example training:
 *    labels = set of class labels (String[])
 *    unlabelledDocs  = iterable over unlabelled documents
 *    labelledDocs    = iterable over labelled training documents
 *    pipeline        = instance of FeatureExtractionPipeline
 *
 *    Example 1: Dualist style
 *      finalNB = Util.initNBWithLabels(pipeline, labels) // Creates a new NB classifier which knows about the labels
 *      finalNB.train(labelledDocs)                       // Train on counts from labelled docs, we'll augment these with probabilistic ones
 *      NB = Util.initNBWithLabels(pipeline, labels)      // Make the classifier which is to be trained only on pseudo-counts
 *
 *      // use "nb.setLabelAlpha" and "nb.setFeatureAlpha" to add pseudo-counts to classes and features respectively.
 *      // then:
 *      finalNB.emTrain(unlabelledDocs, NB) // Notice that the classifier "NB" is used to classify the unlabelled. Then the finalNB is augmented with the resulting counts.
 *
 *    Example 2: Bootstrap style
 *      NB = Util.initNBWithLabels(pipeline, labels)
 *      // use "nb.setLabelAlpha" and "nb.setFeatureAlpha" to add pseudo-counts to classes and features.
 *      NB.train(labelledDocs)
 *      NB.emTrain(unlabelledDocs, NB)
 *
 * See NaiveBayesClassifierPreComputed for a classifier which cannot be incrementally trained but
 * is more efficient for prediction:
 *
 *   efficientNB = new NaiveBayesClassifierPreComputed(NB)
 *
 * User: Andrew D. Robertson
 * Date: 25/07/2013
 * Time: 16:13
 */
public class NaiveBayesClassifier extends AbstractNaiveBayesClassifier implements NaiveBayesPrecomputable, LowFrequencyFeatureTrimmable {

    private double labelSmoothing = 5;       // Smoothing applied to class labels
    private double featureSmoothing = 1;     // Smoothing applied to features

    protected Int2DoubleOpenHashMap labelMultipliers = new Int2DoubleOpenHashMap(); //Count multipliers for each label

    // Real counts
    protected Int2DoubleOpenHashMap docCounts = new Int2DoubleOpenHashMap();   // Number of documents per label
    protected Int2DoubleOpenHashMap labelCounts = new Int2DoubleOpenHashMap(); // Number of feature occurrences per label
    protected Int2ObjectMap<Int2DoubleOpenHashMap> jointCounts = new Int2ObjectOpenHashMap<>(); // For each label, joint counts of feature, label pairs

    // Pseudo counts
    protected Int2ObjectMap<Int2DoubleOpenHashMap> labelFeatureAlphas = new Int2ObjectOpenHashMap<>(); // For each label joint pseudo counts of feature, label pairs
    protected Int2DoubleOpenHashMap featureAlphaTotals = new Int2DoubleOpenHashMap(); // Total feature pseudo counts per label
    protected Int2DoubleOpenHashMap labelAlphas = new Int2DoubleOpenHashMap();        // Label pseudo counts per label

	private Map<String, Object> metadata = new HashMap<>();

	public static final ClassifierName CLASSIFIER_NAME = ClassifierName.NB;

    /**
     * See 1-parameter constructor for reasons why you might want to pre-specify your
     * class labels.
     */
    public NaiveBayesClassifier(){
		super();
		this.metadata.put("classifier_class_name", CLASSIFIER_NAME);
    }

    /**
     * Create an source of NaiveBayesClassifier and specify the class labels.
     * This is necessary if creating an empty classifier to pass to EM:
     *
     * Imagine you want to use a classifier with only some features or
     * instances labeled; it may not have encountered all possible labels.
     * So it is impossible for it use certain labels. Whereas if you pre-specify
     * the labels, the class priors will initially be uniform for all possible labels.
     */
    public NaiveBayesClassifier(IntSet labels){
        super();
        this.labels = labels;
		this.metadata.put("classifier_class_name", CLASSIFIER_NAME);
    }

	public ClassifierName getClassifierName() {
		return CLASSIFIER_NAME;
	}

	public Map<String, Object> getMetadata() {
		return this.metadata;
	}

	public void setLabelSmoothing(double smoothingValue) {labelSmoothing = smoothingValue;}
	public double getLabelSmoothing() {return labelSmoothing;}
    public void setFeatureSmoothing(double smoothingValue) {featureSmoothing = smoothingValue;}
	public double getFeatureSmoothing() {return featureSmoothing;}

    /**
     * Add pseudo-counts to a particular class label.
     */
    public void setLabelAlpha(int label, double alpha){
        labels.add(label);
        labelAlphas.addTo(label, alpha);
    }
    public Int2DoubleOpenHashMap getLabelAlphas(){ return labelAlphas; }

    /**
     * Add pseudo-counts to a particular feature under a particular label.
     */
    public void setFeatureAlpha(int feature, int label, double alpha){
        labels.add(label);
        vocab.add(feature);
        featureAlphaTotals.addTo(label, alpha - getFromMap(label, labelFeatureAlphas).get(feature));
        getFromMap(label, labelFeatureAlphas).put(feature, alpha);
    }
    public Int2ObjectMap<Int2DoubleOpenHashMap> getLabelledFeatures(){ return labelFeatureAlphas; }

    public void unlabelFeature(int feature, int label){
        setFeatureAlpha(feature, label, 0);
        labelFeatureAlphas.get(label).remove(feature);
        boolean seen = false;
        for (int l : labels) {
            if (getFromMap(l, jointCounts).containsKey(feature)) seen = true;
            if (getFromMap(l, labelFeatureAlphas).containsKey(feature)) seen = true;
        }
        // If the feature was only in vocab because it was labelled explicitly with this label, then remove it from vocab
        if (!seen) vocab.remove(feature);
    }

    /**
     * Delete all features with frequency less than *frequencyCutoff* including pseudo-counts.
     */
    public IntSet getInfrequentFeatures(double frequencyCutoff){
        return vocab.stream()
                .filter(feature -> featureCount(feature) < frequencyCutoff)
                .collect(Collectors.toCollection(IntOpenHashSet::new));
    }

    @Override
    public IntSet trimInfrequentFeature(double frequencyCutoff) {
        IntSet features = getInfrequentFeatures(frequencyCutoff);
        features.stream()
                .forEach(this::deleteFeature);
        return features;
    }

    public void deleteFeature(int feature){
        for (int label : getLabels()){
            if (hasPseudoCounts(feature, label))
                unlabelFeature(feature, label);
            if (hasRealCounts(feature, label))
                getFromMap(label, labelFeatureAlphas).remove(feature);
        } vocab.remove(feature);
    }

    public boolean hasPseudoCounts(int feature, int label){
        return getFromMap(label, labelFeatureAlphas).get(feature) > 0;
    }

    public boolean hasRealCounts(int feature, int label){
        return getFromMap(label, jointCounts).get(feature) > 0;
    }

    public void setLabelMultiplier(int label, double multiplier){  labelMultipliers.put(label, multiplier); }
    public Int2DoubleOpenHashMap getLabelMultipliers() { return labelMultipliers; }


    /**
     * Train on labelled documents.
     * See class documentation for training examples.
     * @param documents Iterable over TrainingInstances (which are (label, feature-list) pairs.
     * @param weight The weighting applied to the counts obtained.
     */
    public void train(Iterable<ProcessedInstance> documents, double weight){
        for (ProcessedInstance doc : documents){
            trainOnInstance(doc.getLabel(), doc.features, 1, weight);
        }
    }

    public void train(Iterable<ProcessedInstance> documents){
        train(documents, 1);
    }

    public void train(Iterable<ProcessedInstance> documents, IntIterable weights){
        IntIterator w = weights.iterator();
        for (ProcessedInstance doc : documents){
            trainOnInstance(doc.getLabel(), doc.features, 1, w.nextInt());
        }
    }

    /**
     * Train on unlabelled documents (Expectation-Maximisation) using a Classifier to assign label
     * probabilities to the unlabelled documents. Weight the obtained counts by:
     *  1. The weight parameter, and
     *  2. The label probabilities assigned to the unlabelled documents
     *
     * See class documentation for examples of training.
     *
     * @param documents Iterable over collections of features (where each collection is the features
     *                  obtained from a single document).
     * @param weight Weighting applied to the counts (in addition to the probabilistic weighting).
     * @param classifier Classifier used to assign probabilities to the labels (can just be "this").
     */
    public void emTrain(Iterable<ProcessedInstance> documents, double weight, Classifier classifier) {
        mStep(eStep(documents, weight, classifier));
    }

    public void emTrain(Iterable<ProcessedInstance> documents, double weight){
        emTrain(documents, weight, this);
    }

    public void emTrain(Iterable<ProcessedInstance> documents, Classifier classifier){
        emTrain(documents, 0.1, classifier);
    }

    public void emTrain(Iterable<ProcessedInstance> documents) {
        emTrain(documents, 0.1, this);
    }

    /**
     * Train on a single document.
     * @param label Label of the document
     * @param features Features extracted from the document
     * @param labelProbability Probability of the label (1 if the label was annotated by a human)
     * @param weight Weighting applied to the counts (in addition to probabilistic weighting).
     */
    public void trainOnInstance(int label, int[] features, double labelProbability, double weight){
        if (features.length == 0) return; // Skip documents with no features
        if (label < 0) return; // Skip documents which bear no label
        labels.add(label);
        docCounts.addTo(label, labelProbability * weight);
        for (int feature : features) {
            vocab.add(feature);
            labelCounts.addTo(label, labelProbability * weight);
            getFromMap(label,jointCounts).addTo(feature, labelProbability * weight);
        }
    }

    /**
     * Numerator: Dirichlet prior + Count of *feature* occurring in documents labelled with *label*
     * Denominator: Total pseudocounts added under *label* + total smoothing assigned to features + total features with *label*
     * @return P(feature|label)
     */
    public double likelihood(int feature, int label){
        return (featureDirichletPrior(feature, label) + getFromMap(label, jointCounts).get(feature)) /
               (featureAlphaTotals.get(label) + featureSmoothing*vocab.size() + labelCounts.get(label));
    }

    /**
     * For each label:
     *  Numerator: label multiplier * (Dirichlet prior + count of documents labelled as *label*)
     *  Denominator: Total document count + total label pseudo-counts + total label smoothing
     *  NOTE: label multiplier defaults to 1
     * @return A mapping:  label ==> P(label)
     */
    public Int2DoubleMap labelPriors(){
        Int2DoubleMap priors = new Int2DoubleOpenHashMap();
        double sum = 0;
        for (int label : labels) {
            double labelMultiplier = labelMultipliers.containsKey(label)? labelMultipliers.get(label) : 1;
            double empiricalCount = empiricalLabelPriors ? docCounts.get(label) : 1;
            priors.put(label, labelMultiplier * (labelDirichletPrior(label) + empiricalCount));
            sum += priors.get(label);
        }
        for (Int2DoubleMap.Entry entry : priors.int2DoubleEntrySet()){
            entry.setValue(entry.getDoubleValue()/sum);
        }
        return priors;
    }

    @Override
    public Int2DoubleOpenHashMap logpriorPlusLoglikelihood(int[] features){
        Int2DoubleOpenHashMap labelScores = new Int2DoubleOpenHashMap();
        Int2DoubleMap labelPriors = labelPriors();
        for (int label : labels) {
            double loglikelihood = 0.0;
            for (int feature : features) {
                if (vocab.contains(feature)){
                    loglikelihood += Math.log(likelihood(feature, label));
                }
            }
            labelScores.put(label, Math.log(labelPriors.get(label)) + loglikelihood);
        }
        return labelScores;
    }

    /**
     * Get the count of a feature across all labels (including pseudocounts).
     */
    public double featureCount(int feature){
        double sum = 0;
        for (int label : labels)
            sum += getFromMap(label, labelFeatureAlphas).get(feature) + getFromMap(label, jointCounts).get(feature);
        return sum;
    }

    /**
     * Perform the Expectation step of EM. See emTrain().
     *  1. Use *classifier* to assign probabilistic labels to unlabelled documents
     *  2. Build new classifier based on probabilistic counts.
     */
    private NaiveBayesClassifier eStep(Iterable<ProcessedInstance> documents, double weight, Classifier classifier){
        NaiveBayesClassifier nb = new NaiveBayesClassifier();
        for (ProcessedInstance document : documents){
            Int2DoubleMap posteriors = classifier.predict(document.features);
            for (Int2DoubleMap.Entry entry : posteriors.int2DoubleEntrySet()){
                nb.trainOnInstance(entry.getIntKey(), document.features, entry.getDoubleValue(), weight);
            }
        }
        return nb;
    }

    /**
     * Perform the Maximisation step of EM. See emTrain().
     * Add to this NaiveBayesClassifier the counts of another
     * (usually acquired during the Expectation step of EM).
     */
    private void mStep(NaiveBayesClassifier nb) {
        labels.addAll(nb.labels);
        vocab.addAll(nb.vocab);
        // Update real counts
        addTo(docCounts, nb.docCounts);
        addTo(labelCounts, nb.labelCounts);
        for (Int2ObjectMap.Entry<Int2DoubleOpenHashMap> entry : nb.jointCounts.int2ObjectEntrySet()){
            addTo(getFromMap(entry.getIntKey(), jointCounts), entry.getValue());
        }
    }

    /**
     * The dirichlet prior of a feature is the feature smoothing plus any
     * pseudo-counts associated with said feature and label.
     */
    private double featureDirichletPrior(int feature, int label){
        return featureSmoothing + getFromMap(label, labelFeatureAlphas).get(feature);
    }

    /**
     * The dirichlet prior of a label is the label smoothing plus any pseudo-counts
     * associated with said label.
     */
    private double labelDirichletPrior(int label){
        return labelSmoothing + labelAlphas.get(label);
    }

    /**
     * Get the Int2DoubleOpenHashMap of the corresponding corresponding int key from a Int2ObjectMap.
     * If the int is not present in the map, then put a new Int2DoubleOpenHashMap in the map, and return it.
     */
    protected Int2DoubleOpenHashMap getFromMap(int label, Int2ObjectMap<Int2DoubleOpenHashMap> map){
        if (!map.containsKey(label)) {
            map.put(label, new Int2DoubleOpenHashMap());
        }
        return map.get(label);
    }

    /**
     * Add the entries of *toBeAdded* to *map*. If any keys are already present in *map*, then the new value
     * will be the sum of the original value in *map* and the value in *toBeAdded*.
     */
    protected void addTo(Int2DoubleOpenHashMap map, Int2DoubleOpenHashMap toBeAdded){
        ObjectIterator<Int2DoubleMap.Entry> i = toBeAdded.int2DoubleEntrySet().fastIterator();
        while (i.hasNext()) {
            Int2DoubleMap.Entry entry = i.next();
            map.addTo(entry.getIntKey(), entry.getDoubleValue());
        }
    }

    /**
     * Write classifier to file in JSON representation. Convert all features and labels to their string representation.
     */
    public void writeJson(File out, FeatureExtractionPipeline pipeline) throws IOException {
        try (JsonWriter writer = new JsonWriter(new OutputStreamWriter(new FileOutputStream(out), "UTF-8"))){
            writer.beginObject();
			writeModelBasics(writer, out, pipeline);
            writer.endObject();
        }
    }

	protected void writeModelBasics(JsonWriter writer, File out, FeatureExtractionPipeline pipeline) throws IOException {
		writer.name("labelSmoothing").value(labelSmoothing);
		writer.name("featureSmoothing").value(featureSmoothing);
		writer.name("labelMultipliers"); writeJsonInt2DoubleMap(writer, pipeline, labelMultipliers, false);
		writer.name("labels"); writeJsonIntSet(writer, pipeline, labels, false);
		writer.name("vocab");  writeJsonIntSet(writer, pipeline, vocab, true);
		writer.name("docCounts");   writeJsonInt2DoubleMap(writer, pipeline, docCounts, false);
		writer.name("labelCounts"); writeJsonInt2DoubleMap(writer, pipeline, labelCounts, false);
		writer.name("jointCounts"); writeJsonInt2ObjectMap(writer, pipeline, jointCounts);
		writer.name("labelFeatureAlphas"); writeJsonInt2ObjectMap(writer, pipeline, labelFeatureAlphas);
		writer.name("featureAlphaTotals"); writeJsonInt2DoubleMap(writer, pipeline, featureAlphaTotals, false);
		writer.name("labelAlphas"); writeJsonInt2DoubleMap(writer, pipeline, labelAlphas, false);
		writer.name("empiricalLabelPriors").value(empiricalLabelPriors);
	}

    protected void writeJsonIntSet(JsonWriter writer, FeatureExtractionPipeline pipeline, IntSet set, boolean areFeatures) throws IOException{
        writer.beginArray();
        for (int i : set)
            writer.value(areFeatures? pipeline.featureString(i) : pipeline.labelString(i));
        writer.endArray();
    }
    protected void writeJsonInt2DoubleMap(JsonWriter writer, FeatureExtractionPipeline pipeline, Int2DoubleOpenHashMap map, boolean areFeatures) throws IOException{
        writer.beginObject();
        ObjectIterator<Int2DoubleMap.Entry> i = map.int2DoubleEntrySet().fastIterator();
        while (i.hasNext()) {
            Int2DoubleMap.Entry entry = i.next();
            writer.name(areFeatures? pipeline.featureString(entry.getIntKey()) : pipeline.labelString(entry.getIntKey()));
            writer.value(entry.getDoubleValue());
        }
        writer.endObject();
    }
    protected void writeJsonInt2ObjectMap(JsonWriter writer, FeatureExtractionPipeline pipeline, Int2ObjectMap<Int2DoubleOpenHashMap> map) throws IOException{
        writer.beginObject();
        for(Int2ObjectMap.Entry<Int2DoubleOpenHashMap> entry : map.int2ObjectEntrySet()){
            writer.name(pipeline.labelString(entry.getIntKey()));
            writeJsonInt2DoubleMap(writer, pipeline, entry.getValue(), true);
        }
        writer.endObject();
    }

    /**
     * Read classifier from file in JSON representation. Convert all features and labels from their string representation.
     */
    public static NaiveBayesClassifier readJson(File in, FeatureExtractionPipeline pipeline) throws IOException {
        NaiveBayesClassifier nb = null;
        try (JsonReader reader = new JsonReader(new InputStreamReader(new FileInputStream(in), "UTF-8"))){
            reader.beginObject();
            nb = readJson(reader, pipeline);
            reader.endObject();
        }
        return nb;
    }

	public static NaiveBayesClassifier readJson(JsonReader reader, FeatureExtractionPipeline pipeline) throws IOException {
		NaiveBayesClassifier nb = new NaiveBayesClassifier();
		while (reader.hasNext()){
			String name = reader.nextName();
			switch (name) { // Don't worry; this is okay in Java 7 onwards
				case "labelSmoothing":   nb.labelSmoothing = reader.nextDouble(); break;
				case "featureSmoothing": nb.featureSmoothing = reader.nextDouble(); break;
				case "labelMultipliers": nb.labelMultipliers = readJsonInt2DoubleMap(reader, pipeline, false); break;
				case "labels": nb.labels = readJsonIntSet(reader, pipeline, false); break;
				case "vocab": nb.vocab = readJsonIntSet(reader, pipeline, true);  break;
				case "docCounts": nb.docCounts = readJsonInt2DoubleMap(reader, pipeline, false); break;
				case "labelCounts": nb.labelCounts = readJsonInt2DoubleMap(reader, pipeline, false); break;
				case "jointCounts": nb.jointCounts = readJsonInt2ObjectMap(reader, pipeline); break;
				case "labelFeatureAlphas": nb.labelFeatureAlphas = readJsonInt2ObjectMap(reader, pipeline); break;
				case "featureAlphaTotals": nb.featureAlphaTotals = readJsonInt2DoubleMap(reader, pipeline, false); break;
				case "labelAlphas": nb.labelAlphas = readJsonInt2DoubleMap(reader, pipeline, false); break;
				case "empiricalLabelPriors": nb.empiricalLabelPriors = reader.nextBoolean(); break;
			}
		}

		return nb;
	}

    protected static IntSet readJsonIntSet(JsonReader reader, FeatureExtractionPipeline pipeline, boolean areFeatures) throws IOException {
        IntSet set = new IntOpenHashSet();
        reader.beginArray();
        while (reader.hasNext()){
            set.add(areFeatures? pipeline.featureIndex(reader.nextString()) : pipeline.labelIndex(reader.nextString()));
        }
        reader.endArray();
        return set;
    }
	protected static Int2DoubleOpenHashMap readJsonInt2DoubleMap(JsonReader reader, FeatureExtractionPipeline pipeline, boolean areFeatures) throws IOException {
        Int2DoubleOpenHashMap map = new Int2DoubleOpenHashMap();
        reader.beginObject();
        while (reader.hasNext()){
            map.put(areFeatures? pipeline.featureIndex(reader.nextName()) : pipeline.labelIndex(reader.nextName()), reader.nextDouble());
        }
        reader.endObject();
        return map;
    }
	protected static Int2ObjectMap<Int2DoubleOpenHashMap> readJsonInt2ObjectMap(JsonReader reader, FeatureExtractionPipeline pipeline) throws IOException {
        Int2ObjectMap<Int2DoubleOpenHashMap> map = new Int2ObjectOpenHashMap<>();
        reader.beginObject();
        while (reader.hasNext()){
            map.put(pipeline.labelIndex(reader.nextName()), readJsonInt2DoubleMap(reader,pipeline, true));
        }
        reader.endObject();
        return map;
    }


    /**
     * calculates the pair wise Kullback-Leibler divergence of the language models pf each label
     *
     * doesn't account for pseudo-counts / dirichlet priors
     *
     * ref: http://mathworld.wolfram.com/RelativeEntropy.html
     *
     * @param classifier
     * @return
     */
    public static double[][] pairWiseLabelKLD(NaiveBayesClassifier classifier) {

        IntSet labels = classifier.getLabels();

        int n = labels.size();

        double[][] dpq = new double[n][n];
        for (double[] row : dpq) {
            Arrays.fill(row, 0.0);
        }

        for( int p : labels) {
            for( int q : labels) {
                if( p == q) {
                    dpq[p][q] = 0;
                    continue;
                }
                for(int k : classifier.vocab) {

//                    double pk = classifier.jointCounts.get(p).get(k) / classifier.labelCounts.get(p);
//                    double qk = classifier.jointCounts.get(q).get(k) / classifier.labelCounts.get(q);
                    double pk = classifier.likelihood(k,p);
                    double qk = classifier.likelihood(k,q);

                    if(pk == 0 || qk == 0 || pk == qk)
                        continue;


                    dpq[p][q] += pk * MathUtils.log(2, pk/qk);
                }
            }
        }
        return dpq;
    }

    public Int2ObjectMap<Int2DoubleOpenHashMap> getJointCounts() {

        return jointCounts;
    }

	@Override
	public AbstractNaiveBayesClassifier getPrecomputedClassifier() {
		return new NaiveBayesClassifierPreComputed(this);
	}
}
