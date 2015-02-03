package uk.ac.susx.tag.classificationframework.classifiers;

import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntSet;

/**
 * Created by thk22 on 03/02/2015.
 */
public class NaiveBayesClassifierSFEPrecomputed extends NaiveBayesClassifierPreComputed {

	private Int2DoubleOpenHashMap labelPriorTimesLikelihoodPerLabelSum;
	private Int2DoubleOpenHashMap unlabelledWordProbs;

	public NaiveBayesClassifierSFEPrecomputed(Int2DoubleMap labelPriors,
											  Int2ObjectMap<Int2DoubleMap> featureLikelihoods,
											  IntSet labels,
											  IntSet vocab,
											  Int2DoubleOpenHashMap labelPriorTimesLikelihoodPerLabelSum,
											  Int2DoubleOpenHashMap unlabelledWordProbs) {
		super(labelPriors, featureLikelihoods, labels, vocab);
		this.labelPriorTimesLikelihoodPerLabelSum = labelPriorTimesLikelihoodPerLabelSum;
		this.unlabelledWordProbs = unlabelledWordProbs;
	}

	public NaiveBayesClassifierSFEPrecomputed(NaiveBayesClassifierSFE nbSFE) {
		super();
		this.labelPriorTimesLikelihoodPerLabelSum = nbSFE.getLabelPriorTimesLikelihoodPerLabelSum();
		this.unlabelledWordProbs = nbSFE.getUnlabelledWordProbs();
		computeProbabilities(nbSFE);
	}

	private void computeProbabilities(NaiveBayesClassifierSFE nbSFE) {
		vocab.addAll(nbSFE.vocab);
		labels.addAll(nbSFE.labels);
		Int2DoubleMap rawLabelPriors = nbSFE.labelPriors();
		for (int label : labels) {
			labelPriors.put(label, Math.log(rawLabelPriors.get(label)));
			featureLikelihoods.put(label, new Int2DoubleOpenHashMap());
			for (int feature : vocab){
				double featureLikelihood = Math.log(nbSFE.likelihood(feature, label)) + ((unlabelledWordProbs.containsKey(feature)) ? Math.log(unlabelledWordProbs.get(feature)) : 0) - labelPriorTimesLikelihoodPerLabelSum.get(label);
				featureLikelihoods.get(label).put(feature, featureLikelihood);
			}
		}
	}
}
