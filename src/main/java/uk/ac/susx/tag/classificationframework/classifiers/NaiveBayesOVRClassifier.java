package uk.ac.susx.tag.classificationframework.classifiers;

import it.unimi.dsi.fastutil.ints.*;
import uk.ac.susx.tag.classificationframework.datastructures.ProcessedInstance;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by thk22 on 03/10/2014.
 *
 * NaiveBayes type specific OVR Classifier, inherits from NaiveBayesClassifier rather than OVRClassifier, because it
 *      a) fulfils the contract of a NaiveBayesClassifier and
 *      b) makes life downstream a lot easier
 *
 * Essentially it takes n NaiveBayesClassifiers and wraps them into a single new classifier, which is able to pretend to
 * be a NaiveBayesClassifier and handles all the differences internally.
 *
 * It is a schizophrenic creature meant to cause confusion and havoc!
 *
 * On the downside of it inheriting from NaiveBayesClassifier rather than OVRClassifer is, it currently duplicates a fair bit of code already implemented in OVRClassifier which is identical to the stuff here.
 * Options for cleaning up that mess:
 *      a) Dress up in a black tie suit, grab a monocle, a pocket watch and a glass of Brandy, mix with some Enterprise Architect people and philosophise about Java Design Patterns and Best Practices and come up with a super-corporatistic-over-engineered solution.
 *      b) Delete OVRClassifier because its a prime example of pre-mature optimisation.
 *      c) Live with a badly written codebase which is a *dream* to maintain and where future developers (and myself) will curse me and my children and my childrens children, etc, for being an absolute useless and substandard code monkey.
 *      d) Something else.
 *
 * Note: Not all of the above statements are true anymore.
 *
 */
public class NaiveBayesOVRClassifier<T extends NaiveBayesClassifier> extends NaiveBayesClassifier {

    private static final int OTHER_LABEL = Integer.MAX_VALUE;

    private Int2ObjectMap<T> ovrLearners;
    private Class<T> learnerClass;

    public NaiveBayesOVRClassifier(IntSet labels, Class<T> learnerClass) {
        super(labels);
        this.ovrLearners = new Int2ObjectOpenHashMap<>();
        this.learnerClass = learnerClass;

        this.initOVRScheme();
    }

    public NaiveBayesOVRClassifier(IntSet labels, Class<T> learnerClass, Int2ObjectMap<T> ovrLearners) {
        super(labels);
        this.ovrLearners = ovrLearners;
        this.learnerClass = learnerClass;
    }

    @Override
    public void setLabelSmoothing(double smoothingValue)
    {
		for (int l : this.ovrLearners.keySet()) {
			this.ovrLearners.get(l).setLabelSmoothing(smoothingValue);
		}
    }

	@Override
	public void setFeatureSmoothing(double smoothingValue)
	{
		for (int l : this.ovrLearners.keySet()) {
			this.ovrLearners.get(l).setFeatureSmoothing(smoothingValue);
		}
	}

	@Override
	public void setLabelAlpha(int label, double alpha)
	{
		if (this.ovrLearners.keySet().size() > 1) {
			this.ovrLearners.get(label).setLabelAlpha(label, alpha);
		} else {
			this.ovrLearners.get(OTHER_LABEL).setLabelAlpha(label, alpha);
		}
	}

	@Override
	public Int2DoubleOpenHashMap getLabelAlphas()
	{
		Int2DoubleOpenHashMap labelAlphas = null;

		if (this.ovrLearners.keySet().size() > 1) {
			labelAlphas = new Int2DoubleOpenHashMap();
			for (int l : this.labels) {
				T ovrLearner = ovrLearners.get(l);
				labelAlphas.put(l, ovrLearner.labelAlphas.get(l));
			}
		} else {
			labelAlphas = this.ovrLearners.get(OTHER_LABEL).getLabelAlphas();
		}

		return labelAlphas;
	}

	@Override
	public void setFeatureAlpha(int feature, int label, double alpha)
	{
		if (this.ovrLearners.keySet().size() > 1) {
			for (int l : this.labels) {
				int targetLabel = (l == label) ? label : OTHER_LABEL;
				this.ovrLearners.get(l).setFeatureAlpha(feature, targetLabel, alpha);
			}
		} else {
			this.ovrLearners.get(OTHER_LABEL).setFeatureAlpha(feature, label, alpha);
		}
	}

	@Override
	public Int2DoubleOpenHashMap getLabelMultipliers()
	{
		if (this.ovrLearners.keySet().size() > 1) {
			Int2DoubleOpenHashMap labelMultipliers = new Int2DoubleOpenHashMap();
			for (int l : this.labels) {
				labelMultipliers.putAll(this.ovrLearners.get(l).getLabelMultipliers());
			}
			// Remove OTHER_LABEL
			labelMultipliers.remove(OTHER_LABEL);

			return labelMultipliers;
		} else {
			return this.ovrLearners.get(OTHER_LABEL).getLabelMultipliers();
		}
	}

	@Override
	public Int2ObjectMap<Int2DoubleOpenHashMap> getLabelledFeatures()
	{
		// TODO: Check if the statement below is true
		// As all classifiers have the same labelledFeatures (might be with different labels, but the actual features are the same)
		// we can just return the labelled features from *any* of the classifiers

		// TODO: This looks weird and ugly, can we do it differently?
		Iterator<T> iter = this.ovrLearners.values().iterator();
		return iter.next().getLabelledFeatures();
	}

	@Override
	public void trainOnInstance(int label, int[] features, double labelProbability, double weight)
	{
		if (this.ovrLearners.keySet().size() > 1) {
			/*
			 * The specialist classifier for the given label needs to be positively trained on that instance
			 *
			 * The other classifier need to be negatively trained on that instance (that is with label=OTHER_LABEL)
			 */
			for (int l : this.labels) {
				int targetLabel = (l == label) ? label : OTHER_LABEL;
				this.ovrLearners.get(l).trainOnInstance(targetLabel, features, labelProbability, weight);
			}
		} else {
			this.ovrLearners.get(OTHER_LABEL).trainOnInstance(label, features, labelProbability, weight);
		}
	}

	@Override
	public void unlabelFeature(int feature, int label)
	{
		if (this.ovrLearners.keySet().size() > 1) {
			for (int l : this.labels) {
				int targetLabel = (l == label) ? label : OTHER_LABEL;
				this.ovrLearners.get(l).unlabelFeature(feature, targetLabel);
			}
		} else {
			this.ovrLearners.get(OTHER_LABEL).unlabelFeature(feature, label);
		}
	}

	@Override
	public void setLabelMultiplier(int label, double multiplier)
	{
		int targetLabel = (this.ovrLearners.keySet().size() > 1) ? label : OTHER_LABEL;
		this.ovrLearners.get(targetLabel).setLabelMultiplier(label, multiplier);
	}

	@Override
	public double likelihood(int feature, int label)
	{
		return (this.ovrLearners.keySet().size() > 1) ? this.ovrLearners.get(label).likelihood(feature, label) : this.ovrLearners.get(OTHER_LABEL).likelihood(feature, label);
	}

    @Override
    public void train(Iterable<ProcessedInstance> labelledDocuments, Iterable<ProcessedInstance> unlabelledDocuments)
    {
        if (this.labels.size() > 2) {
            this.trainOVRSemiSupervised(labelledDocuments, unlabelledDocuments);
        } else {
            this.trainBinarySemiSupervised(labelledDocuments, unlabelledDocuments);
        }
    }

    @Override
    public void train(Iterable<ProcessedInstance> labelledDocuments)
    {
            if (this.labels.size() > 2) {
                this.trainOVRSupervised(labelledDocuments);
            } else {
                this.trainBinarySupervised(labelledDocuments);
            }
    }

    @Override
    public IntSet getLabels() {
        return this.labels;
    }

    @Override
    public IntSet getVocab() {
        // All learners have the same vocab, so we just return the vocab of the first one
        return (this.ovrLearners.size() > 0 ? this.ovrLearners.get(0).getVocab() : null);
    }

    public Int2ObjectMap<T> getOvrLearners()
    {
        return this.ovrLearners;
    }

	@Override
	public Int2DoubleOpenHashMap logpriorPlusLoglikelihood(int[] features)
	{
		Int2DoubleOpenHashMap jll = new Int2DoubleOpenHashMap();
		for (T learner : this.ovrLearners.values()) {
			jll.putAll(learner.logpriorPlusLoglikelihood(features));
		}

		// Remove other label again
		jll.remove(OTHER_LABEL);

		return jll;
	}

	@Override
	public Int2DoubleMap labelPriors()
	{
		Int2DoubleMap priors = new Int2DoubleOpenHashMap();

		for (T learner : this.ovrLearners.values()) {
			priors.putAll(learner.labelPriors());
		}

		// Remove OTHER_LABEL
		priors.remove(OTHER_LABEL);

		return priors;
	}

    public Int2DoubleOpenHashMap predict(int[] features)
    {
        Int2DoubleOpenHashMap prediction = new Int2DoubleOpenHashMap();
        for (T learner : this.ovrLearners.values()) {
            prediction.putAll(learner.predict(features));
        }

        // Remove other label, so all that remains are the predictions for the existing labels
        prediction.remove(OTHER_LABEL);

        return prediction;
    }

    public int bestLabel(int[] features)
    {
        Int2DoubleMap prediction = this.predict(features);

        double maxPrediction = Double.MIN_VALUE;
        int bestLabel = -1;

        for (int key : prediction.keySet()) {
            if (prediction.get(key) > maxPrediction) {
                maxPrediction = prediction.get(key);
                bestLabel = key;
            }
        }

        return bestLabel;
    }

    private void trainBinarySupervised(Iterable<ProcessedInstance> labelledDocs)
    {
        this.ovrLearners.get(OTHER_LABEL).train(labelledDocs);
    }

    private void trainBinarySemiSupervised(Iterable<ProcessedInstance> labelledDocs, Iterable<ProcessedInstance> unlabelledDocuments)
    {
        this.ovrLearners.get(OTHER_LABEL).train(labelledDocs, unlabelledDocuments);
    }

    private void trainOVRSupervised(Iterable<ProcessedInstance> labelledDocs)
    {
        for (int l : this.labels) {
            T currLearner = this.ovrLearners.get(l);
            currLearner.train(this.binariseLabelledDocuments(labelledDocs, l));
        }
    }

    private void trainOVRSemiSupervised(Iterable<ProcessedInstance> labelledDocs, Iterable<ProcessedInstance> unlabelledDocs)
    {
        for (int l : this.labels) {
            T currLearner = this.ovrLearners.get(l);
            currLearner.train(this.binariseLabelledDocuments(labelledDocs, l), unlabelledDocs);
        }
    }

    private Iterable<ProcessedInstance> binariseLabelledDocuments(Iterable<ProcessedInstance> labelledDocs, int currLabel)
    {
        List<ProcessedInstance> binarisedDocs = new ArrayList<>();

        for (ProcessedInstance p : labelledDocs) {
            binarisedDocs.add(new ProcessedInstance((p.getLabel() == currLabel ? p.getLabel() : OTHER_LABEL), p.features, p.source));
        }

        return binarisedDocs;
    }

    private void initOVRScheme() {
        try {
            if (this.labels.size() > 2) {
                for (int l : this.labels) {
                    this.ovrLearners.put(l, this.learnerClass.newInstance());
                }
            } else {
                this.ovrLearners.put(OTHER_LABEL, this.learnerClass.newInstance());
            }
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}
