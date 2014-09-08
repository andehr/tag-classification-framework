package uk.ac.susx.tag.classificationframework.classifiers;

/*
 * #%L
 * AbstractNaiveBayesClassifier.java - classificationframework - CASM Consulting - 2,013
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

import it.unimi.dsi.fastutil.doubles.DoubleCollection;
import it.unimi.dsi.fastutil.doubles.DoubleIterator;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import uk.ac.susx.tag.classificationframework.datastructures.ProcessedInstance;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Class provides functionality for predicting labels of documents.
 *
 * One method must be implemented in subclasses, "logpriorPlusLoglikelihood".
 * For document D and each label L, this method should return a mapping:
 *
 *   L ==> Log(P(L)) + Log(P(D|L))
 *
 * All features and labels should be of type int (e.g. by externally indexing them).
 * The classifier is therefore  agnostic to type of features and labels.
 *
 * User: Andrew D. Robertson
 * Date: 25/07/2013
 * Time: 15:51
 */
abstract public class AbstractNaiveBayesClassifier implements Classifier, TrainableClassifier {

    protected boolean empiricalLabelPriors = true;

    protected IntSet labels = new IntOpenHashSet();
    protected IntSet vocab = new IntOpenHashSet();

    public AbstractNaiveBayesClassifier() { }

    /**
     * Any implementing NaiveBayesClassifier should implement this method.
     * For each label in labels:
     *   Calculate: log(P(label)) + log(P(features|label))
     * Place in mapping: label --> log(P(label)) + log(P(features|label))
     */
    public abstract Int2DoubleOpenHashMap logpriorPlusLoglikelihood(int[] features);

    @Override
    public IntSet getLabels() { return labels; }
    @Override
    public IntSet getVocab() { return vocab; }

    /**
     * @return a mapping from each class label to P(label|features)
     */
    @Override
    public Int2DoubleOpenHashMap predict(int[] features) {
        Int2DoubleOpenHashMap posteriorProbabilities = logpriorPlusLoglikelihood(features);
        double maxLogProbability = max(posteriorProbabilities.values());
        for (Int2DoubleMap.Entry entry : posteriorProbabilities.int2DoubleEntrySet())
            entry.setValue(Math.exp(entry.getDoubleValue()-maxLogProbability));
        double normalisation = 0;
        for (double probability : posteriorProbabilities.values())
            normalisation += probability;
        for (Int2DoubleMap.Entry entry : posteriorProbabilities.int2DoubleEntrySet())
            entry.setValue(entry.getDoubleValue()/normalisation);
        return posteriorProbabilities;
    }

    /**
     * @return the most probable class label
     */
    @Override
    public int bestLabel(int[] features) {
        Int2DoubleOpenHashMap labelScores = logpriorPlusLoglikelihood(features);
        return argMax(labelScores);
    }

    @Override
    public void train(Iterable<ProcessedInstance> labelledDocuments, Iterable<ProcessedInstance> unlabelledDocuments)
    { /* Alternatively, just train on the labelled docs */ }

    @Override
    public void train(Iterable<ProcessedInstance> labelledDocuments)
    {}

    /**
     * @return the max double in *doubles*
     */
    private double max(DoubleCollection doubles){
        DoubleIterator iterator = doubles.iterator();
        if (!iterator.hasNext()) throw new NoSuchElementException("Empty collection");
        double max = iterator.nextDouble();
        while (iterator.hasNext()) {
            double d = iterator.nextDouble();
            if (d > max) max = d;
        }
        return max;
    }

    /**
     * @return The int key which is mapped to the greatest double value.
     */
    private int argMax(Int2DoubleMap map) {
        Iterator<Int2DoubleMap.Entry> i = map.int2DoubleEntrySet().iterator();
        if (!i.hasNext()) throw new NoSuchElementException("Empty collection");
        Int2DoubleMap.Entry entry = i.next();
        int maxArg = entry.getIntKey();
        double maxValue = entry.getDoubleValue();
        while (i.hasNext()){
            entry = i.next();
            if (entry.getDoubleValue() > maxValue){
                maxArg = entry.getIntKey();
                maxValue = entry.getDoubleValue();
            }
        }
        return maxArg;
    }

    public static Int2DoubleOpenHashMap normaliseScoresByDocLength(Int2DoubleOpenHashMap scores, int docLength){
        for (Int2DoubleMap.Entry entry : scores.int2DoubleEntrySet())
            entry.setValue(entry.getDoubleValue() / docLength);
        return scores;
    }

    public boolean empiricalLabelPriors() {
        return empiricalLabelPriors;
    }

    public void empiricalLabelPriors(boolean empiricalLabelPriors) {
        this.empiricalLabelPriors = empiricalLabelPriors;
    }
}
