package uk.ac.susx.tag.classificationframework.classifiers;

import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import uk.ac.susx.tag.classificationframework.datastructures.ProcessedInstance;
import uk.ac.susx.tag.classificationframework.Util;

/**
 * Created by thomas on 2/23/14.
 */
public class NaiveBayesClassifierSFE extends NaiveBayesClassifier
{
    private Int2DoubleOpenHashMap labelPriorTimesLikelihoodPerLabelSum;
    private Int2DoubleOpenHashMap unlabelledWordProbs;

    /**
     * See 1-parameter constructor for reasons why you might want to pre-specify your
     * class labels.
     */
    public NaiveBayesClassifierSFE() {
        super();
        this.labelPriorTimesLikelihoodPerLabelSum = new Int2DoubleOpenHashMap();
        this.labelPriorTimesLikelihoodPerLabelSum.defaultReturnValue(0.);
    }

    /**
     * Train on labelled documents.
     * See class documentation for training examples.
     * @param labelledDocs Iterable over TrainingInstances (which are (label, feature-list) pairs.
     * @param unlabelledDocs Iterable over unlabelled Instances, used for Feature Marginals
     * @param weight The weighting applied to the counts obtained.
     */
    public void train(Iterable<ProcessedInstance> labelledDocs, Iterable<ProcessedInstance>unlabelledDocs, double weight)
    {
        // Train on labelled data
        super.train(labelledDocs, weight);

        // Calculate P(w) over the unlabelled data -> (Formula 10, P(wi)u, enumerator)
        this.unlabelledWordProbs = Util.calculateWordProbabilities(unlabelledDocs);

        // Get P(c) * P(w|c) (= log(P(c)) + log(P(w|c))) from labelled data
        this.labelPriorTimesLikelihoodPerLabelSum.clear();

        // Sum over all P(c) * P(w|c) of the labelled data (= likelihood * prior) -> (Formula 10, denominator)
        for (ProcessedInstance labelledDoc : labelledDocs) {
            Int2DoubleOpenHashMap labelScores = new Int2DoubleOpenHashMap();
            Int2DoubleMap labelPriors = super.labelPriors();
            for (int label : super.getLabels()) {
                double likelihood = 1.;
                for (int feature : labelledDoc.features) {
                    if (vocab.contains(feature)) {
                        likelihood *= super.likelihood(feature, label);
                    }
                }
                this.labelPriorTimesLikelihoodPerLabelSum.addTo(label, (labelPriors.get(label) * likelihood));
            }

        }
    }

    public void train(Iterable<ProcessedInstance> labelledDocs, Iterable<ProcessedInstance> unlabelledDocs)
    {
        this.train(labelledDocs, unlabelledDocs, 1);
    }

    @Override
    public Int2DoubleOpenHashMap logpriorPlusLoglikelihood(int[] features){
        Int2DoubleOpenHashMap labelScores = new Int2DoubleOpenHashMap();
        Int2DoubleMap labelPriors = labelPriors();
        for (int label : labels) {
            double loglikelihood = 0.0;
            for (int feature : features) {
                if (vocab.contains(feature)){
                    loglikelihood += this.sfeLogLikelihood(feature, label);
                }
            }
            labelScores.put(label, Math.log(labelPriors.get(label)) + loglikelihood);
        }
        return labelScores;
    }

    private double sfeLogLikelihood(int feature, int label)
    {
        /**
         * Apply Formula 10, but with our smoothing and adding business taken into account
         *
         * Note that the terms P(wj)u in the denominator can be dropped because it sums to 1
         */

        // Basic log(likelihood)
        double sfeLogLikelihood = Math.log(super.likelihood(feature, label));

        // Add log(P(w)u)
        sfeLogLikelihood += Math.log(this.unlabelledWordProbs.get(feature));

        // Subtract sum of labelPriorsTimesLikelihood per class
        sfeLogLikelihood -= this.labelPriorTimesLikelihoodPerLabelSum.get(label);

        return sfeLogLikelihood;
    }
}