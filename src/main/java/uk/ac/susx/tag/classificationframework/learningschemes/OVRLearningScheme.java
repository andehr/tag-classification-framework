package uk.ac.susx.tag.classificationframework.learningschemes;

import it.unimi.dsi.fastutil.ints.*;
import uk.ac.susx.tag.classificationframework.classifiers.*;
import uk.ac.susx.tag.classificationframework.datastructures.ProcessedInstance;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by thomas on 8/27/14.
 */
public class OVRLearningScheme<T extends TrainableClassifier> implements TrainableClassifier {
    private static final int OTHER_LABEL = 666;

    private List<T> ovrLearners;
    private Int2ObjectMap<Iterable<ProcessedInstance>> binarisedTrainingSets;
    private Class<T> learnerClass;
    private IntSet labels;

    public OVRLearningScheme(IntSet labels, Class<T> learnerClass)
    {
        super();
        this.ovrLearners = new ArrayList<>();
        this.binarisedTrainingSets = new Int2ObjectOpenHashMap<>();
        this.learnerClass = learnerClass;
        this.labels = labels;
    }

    public void train(Iterable<ProcessedInstance> labelledDocuments, Iterable<ProcessedInstance> unlabelledDocuments)
    {
        // Sanitise: If the unlabelled documents are null, then simply execute the purely supervised training regime
        if (unlabelledDocuments == null) {
            this.train(labelledDocuments);
        } else {
            try {
                if (this.labels.size() > 2) {
                    this.trainOVRSemiSupervised(labelledDocuments, unlabelledDocuments);
                } else {
                    this.trainBinarySemiSupervised(labelledDocuments, unlabelledDocuments);
                }
            } catch (IllegalAccessException ex) {
                ex.printStackTrace();
            } catch (InstantiationException ex) {
                ex.printStackTrace();
            }
        }
    }

    public void train(Iterable<ProcessedInstance> labelledDocuments)
    {

        try {
            if (this.labels.size() > 2) {
                this.trainOVRSupervised(labelledDocuments);
            } else {
                this.trainBinarySupervised(labelledDocuments);
            }
        } catch (IllegalAccessException ex) {
            ex.printStackTrace();
        } catch (InstantiationException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public IntSet getLabels() {
        return this.labels;
    }

    @Override
    public IntSet getVocab() {
        return null; //FIXME
    }

    public Int2DoubleOpenHashMap predict(int[] features)
    {
        Int2DoubleOpenHashMap prediction = new Int2DoubleOpenHashMap();
        for (T learner : this.ovrLearners) {
            prediction.putAll(learner.predict(features));
        }

        // Remove other label
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

    private void trainBinarySupervised(Iterable<ProcessedInstance> labelledDocs) throws IllegalAccessException, InstantiationException
    {
        this.ovrLearners.add(this.learnerClass.newInstance());
        this.binarisedTrainingSets.put(OTHER_LABEL, labelledDocs);

        this.ovrLearners.get(0).train(labelledDocs);
    }

    private void trainBinarySemiSupervised(Iterable<ProcessedInstance> labelledDocs, Iterable<ProcessedInstance> unlabelledDocuments) throws InstantiationException, IllegalAccessException
    {
        this.ovrLearners.add(this.learnerClass.newInstance());
        this.binarisedTrainingSets.put(OTHER_LABEL, labelledDocs);

        this.ovrLearners.get(0).train(labelledDocs, unlabelledDocuments);
    }

    private void trainOVRSupervised(Iterable<ProcessedInstance> labelledDocs) throws IllegalAccessException, InstantiationException
    {
        for (int l : this.labels) {
            this.binarisedTrainingSets.put(l, this.binariseLabelledDocuments(labelledDocs, l));
            T ovrLearner = this.learnerClass.newInstance();
            ovrLearner.train(this.binarisedTrainingSets.get(l));

            this.ovrLearners.add(ovrLearner);
        }
    }

    private void trainOVRSemiSupervised(Iterable<ProcessedInstance> labelledDocs, Iterable<ProcessedInstance> unlabelledDocs) throws IllegalAccessException, InstantiationException
    {
        for (int l : this.labels) {
            this.binarisedTrainingSets.put(l, this.binariseLabelledDocuments(labelledDocs, l));
            T ovrLearner = this.learnerClass.newInstance();
            ovrLearner.train(this.binarisedTrainingSets.get(l), unlabelledDocs);

            this.ovrLearners.add(ovrLearner);
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
}
