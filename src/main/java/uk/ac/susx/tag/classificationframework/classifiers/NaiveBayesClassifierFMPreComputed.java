package uk.ac.susx.tag.classificationframework.classifiers;

import it.unimi.dsi.fastutil.ints.*;

/**
 * Created by thk22 on 02/02/2015.
 */
public class NaiveBayesClassifierFMPreComputed extends NaiveBayesClassifierPreComputed  {

	private Int2ObjectMap<Int2DoubleOpenHashMap> optClassCondFMProbs = new Int2ObjectOpenHashMap<>();

	public NaiveBayesClassifierFMPreComputed(Int2DoubleMap labelPriors,
											 Int2ObjectMap<Int2DoubleMap> featureLikelihoods,
										   	 IntSet labels,
										     IntSet vocab,
										     Int2ObjectMap<Int2DoubleOpenHashMap> optClassCondFMProbs){
		super(labelPriors, featureLikelihoods, labels, vocab);
		this.optClassCondFMProbs = optClassCondFMProbs;
	}

	public NaiveBayesClassifierFMPreComputed(NaiveBayesClassifierFeatureMarginals nbFM) {
		super();
		this.optClassCondFMProbs = nbFM.getOptClassCondFMProbs();
		computeProbabilities(nbFM);
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
}
