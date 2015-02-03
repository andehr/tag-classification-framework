package uk.ac.susx.tag.classificationframework.classifiers;

/*
 * #%L
 * NaiveBayesClassifierPreComputed.java - classificationframework - CASM Consulting - 2,013
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
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntSet;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.FeatureExtractionPipeline;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Naive bayes which stores data as pre-computed log probabilities.
 * It should perform faster than a NaiveBayesClassifier because
 * log priors and log likelihoods are already calculated. However,
 * it cannot be further trained. It must always be created from a
 * NaiveBayesClassifier source.
 *
 * User: Andrew D. Robertson
 * Date: 26/07/2013
 * Time: 10:23
 */
public class NaiveBayesClassifierPreComputed extends AbstractNaiveBayesClassifier {

    protected Int2DoubleMap labelPriors = new Int2DoubleOpenHashMap();
    protected Int2ObjectMap<Int2DoubleMap> featureLikelihoods = new Int2ObjectOpenHashMap<>();

	private Int2ObjectMap<Int2DoubleOpenHashMap> optClassCondFMProbs = new Int2ObjectOpenHashMap<>();

    public NaiveBayesClassifierPreComputed(NaiveBayesClassifier nb){
        super();
        computeProbabilities(nb);
    }

    public Int2DoubleMap getLabelPriors() { return labelPriors; }
    public void setLabelPriors(Int2DoubleMap labelPriors) { this.labelPriors = labelPriors; }

    public Int2ObjectMap<Int2DoubleMap> getFeatureLikelihoods() { return featureLikelihoods; }
    public void setFeatureLikelihoods(Int2ObjectMap<Int2DoubleMap> featureLikelihoods) { this.featureLikelihoods = featureLikelihoods; }

    public NaiveBayesClassifierPreComputed(Int2DoubleMap labelPriors,
                                           Int2ObjectMap<Int2DoubleMap> featureLikelihoods) {
        super();
        this.labelPriors = labelPriors;
        this.featureLikelihoods = featureLikelihoods;
        labels.addAll(labelPriors.keySet());
        for (Int2DoubleMap featureMap : featureLikelihoods.values()){
            vocab.addAll(featureMap.keySet());
        }
    }

    public NaiveBayesClassifierPreComputed(Int2DoubleMap labelPriors,
                                           Int2ObjectMap<Int2DoubleMap> featureLikelihoods,
                                           IntSet labels,
                                           IntSet vocab){
        super();
        this.labelPriors = labelPriors;
        this.featureLikelihoods = featureLikelihoods;
        this.labels = labels;
        this.vocab = vocab;
    }

	public NaiveBayesClassifierPreComputed(Int2DoubleMap labelPriors,
											 Int2ObjectMap<Int2DoubleMap> featureLikelihoods,
											 IntSet labels,
											 IntSet vocab,
											 Int2ObjectMap<Int2DoubleOpenHashMap> optClassCondFMProbs){
		this(labelPriors, featureLikelihoods, labels, vocab);
		this.optClassCondFMProbs = optClassCondFMProbs;
	}

	public NaiveBayesClassifierPreComputed(NaiveBayesClassifierFeatureMarginals nbFM) {
		super();
		this.optClassCondFMProbs = nbFM.getOptClassCondFMProbs();
		computeProbabilities(nbFM);
	}

    protected NaiveBayesClassifierPreComputed() {}

    @Override
    public Int2DoubleOpenHashMap logpriorPlusLoglikelihood(int[] features){
        Int2DoubleOpenHashMap labelScores = new Int2DoubleOpenHashMap();
        for (int label : labels){
            double loglikelihood = 0;
            for (int feature : features) {
                if (vocab.contains(feature)) loglikelihood += featureLikelihoods.get(label).get(feature);
            }
            double labelPrior = empiricalLabelPriors ? labelPriors.get(label) : 0;
            labelScores.put(label, labelPrior + loglikelihood);
        }
        return labelScores;
    }

    /**
     * Pre-compute likelihoods and priors based on counts obtained from NaiveBayesClassifier instance.
     */
    private void computeProbabilities(NaiveBayesClassifier nb){
        vocab.addAll(nb.vocab);
        labels.addAll(nb.labels);
        Int2DoubleMap rawLabelPriors = nb.labelPriors();
        for (int label : labels) {
            labelPriors.put(label, Math.log(rawLabelPriors.get(label)));
            featureLikelihoods.put(label, new Int2DoubleOpenHashMap());
            for (int feature : vocab){
                featureLikelihoods.get(label).put(feature, Math.log(nb.likelihood(feature, label)));
            }
        }
    }

	private void computeProbabilities(NaiveBayesClassifierFeatureMarginals nbFM) {
		vocab.addAll(nbFM.vocab);
		labels.addAll(nbFM.labels);
		Int2DoubleMap rawLabelPriors = nbFM.labelPriors();
		for (int label : labels) {
			labelPriors.put(label, Math.log(rawLabelPriors.get(label)));
			featureLikelihoods.put(label, new Int2DoubleOpenHashMap());
			for (int feature : vocab){
				double featureLikelihood = (nbFM.getFromMap(label, optClassCondFMProbs).containsKey(feature)) ? Math.log(nbFM.getFromMap(label, optClassCondFMProbs).get(feature)) : Math.log(nbFM.likelihood(feature, label));
				featureLikelihoods.get(label).put(feature, featureLikelihood);
			}
		}
	}

    public static NaiveBayesClassifierPreComputed readJson(File jsonFile, FeatureExtractionPipeline pipeline) throws IOException {
        NaiveBayesClassifierPreComputed nb = new NaiveBayesClassifierPreComputed();
        try (JsonReader reader = new JsonReader(new InputStreamReader(new FileInputStream(jsonFile), "UTF-8"))){
            reader.beginObject(); // Begin classifier object
            while (reader.hasNext()){
                String name = reader.nextName();
                switch(name) {
                    case "labelPriors": nb.labelPriors = readJsonInt2DoubleMap(reader, pipeline, false, nb.labels); break;
                    case "featureLikelihoods": nb.featureLikelihoods = readJsonInt2ObjectMap(reader, pipeline, nb.labels, nb.vocab); break;
                    case "empiricalLabelPriors": nb.empiricalLabelPriors = reader.nextBoolean(); break;
                }
            } reader.endObject(); // End classifier
        }
        return nb;
    }

    private static Int2ObjectMap<Int2DoubleMap> readJsonInt2ObjectMap(JsonReader reader, FeatureExtractionPipeline pipeline, IntSet labelVocab, IntSet featureVocab) throws IOException {
        Int2ObjectMap<Int2DoubleMap> map = new Int2ObjectOpenHashMap<>();
        reader.beginObject();
        while(reader.hasNext()){
            int labelIndex = pipeline.labelIndex(reader.nextName());
            labelVocab.add(labelIndex);
            map.put(labelIndex, readJsonInt2DoubleMap(reader, pipeline, true, featureVocab));
        } reader.endObject();
        return map;
    }

    private static Int2DoubleMap readJsonInt2DoubleMap(JsonReader reader, FeatureExtractionPipeline pipeline, boolean areFeatures, IntSet vocab) throws IOException {
        Int2DoubleMap map = new Int2DoubleOpenHashMap();
        reader.beginObject();
        while (reader.hasNext()){
            String name = reader.nextName();
            int index = areFeatures? pipeline.featureIndex(name) : pipeline.labelIndex(name);
            map.put(index, reader.nextDouble());
            vocab.add(index);
        } reader.endObject();
        return map;
    }
}
