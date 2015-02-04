package uk.ac.susx.tag.classificationframework.classifiers;

import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntSet;

/**
 * Created by thk22 on 02/02/2015.
 */
public class NaiveBayesClassifierOVRPreComputed extends NaiveBayesClassifierPreComputed {
	private Int2ObjectMap<Int2DoubleOpenHashMap> optClassCondFMProbs = new Int2ObjectOpenHashMap<>();
	private Int2ObjectMap<AbstractNaiveBayesClassifier> ovrLearners = new Int2ObjectOpenHashMap<>();

	public NaiveBayesClassifierOVRPreComputed(Int2DoubleMap labelPriors,
											 Int2ObjectMap<Int2DoubleMap> featureLikelihoods,
											 IntSet labels,
											 IntSet vocab){
		super(labelPriors, featureLikelihoods, labels, vocab);
	}

	public NaiveBayesClassifierOVRPreComputed(Int2DoubleMap labelPriors,
											  Int2ObjectMap<Int2DoubleMap> featureLikelihoods,
											  IntSet labels,
											  IntSet vocab,
											  Int2ObjectMap<Int2DoubleOpenHashMap> optClassCondFMProbs){
		super(labelPriors, featureLikelihoods, labels, vocab);
		this.optClassCondFMProbs = optClassCondFMProbs;
	}

	public NaiveBayesClassifierOVRPreComputed(NaiveBayesOVRClassifier<? extends NaiveBayesClassifier> nbOVR) {
		super();
		for (int l : nbOVR.getOvrLearners().keySet()) {
			ovrLearners.put(l, nbOVR.getOvrLearners().get(l).getPrecomputedClassifier());
		}
		vocab.addAll(nbOVR.getVocab());
		labels.addAll(nbOVR.getLabels());
	}

	@Override
	public Int2DoubleOpenHashMap logpriorPlusLoglikelihood(int[] features)
	{
		Int2DoubleOpenHashMap jll = new Int2DoubleOpenHashMap();
		for (AbstractNaiveBayesClassifier learner : this.ovrLearners.values()) {
			jll.putAll(learner.logpriorPlusLoglikelihood(features));
		}

		// Remove other label again
		jll.remove(NaiveBayesOVRClassifier.OTHER_LABEL);

		return jll;
	}

}
