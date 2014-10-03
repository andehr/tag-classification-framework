package uk.ac.susx.tag.classificationframework.classifiers;

import it.unimi.dsi.fastutil.ints.*;
import uk.ac.susx.tag.classificationframework.datastructures.ProcessedInstance;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by thk22 on 03/10/2014.
 *
 * NaiveBayes type specific OVR Classifier, inherits from NaiveBayesClassifier rather than OVRClassifier, because it
 *      a) fulfils the contract of a NaiveBayesClassifier and
 *      b) makes life downstream a lot easier
 *
 * On the downside, it currently duplicates a fair bit of code already implemented in OVRClassifier which is identical to the stuff here.
 * Options:
 *      a) Dress up in a black tie suit, grab a monocle, a pocket watch and a glass of Brandy, mix with some Enterprise Architect people and philosophise about Java Design Patterns and Best Practices and come up with a super-corporatistic-over-engineered solution.
 *      b) Delete OVRClassifier because its a prime example of pre-mature optimisation.
 *      c) Live with a badly written codebase which is a *dream* to maintain and where future developers (and myself) will curse me and my children and my childrens children, etc, for being an absolute useless and substandard code monkey.
 *      d) Something else.
 */
public class NaiveBayesOVRClassifier<T extends AbstractNaiveBayesClassifier> extends NaiveBayesClassifier {

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
