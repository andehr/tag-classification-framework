package uk.ac.susx.tag.classificationframework;

/*
 * #%L
 * Evaluation.java - classificationframework - CASM Consulting - 2,013
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

import com.google.common.collect.Lists;
import uk.ac.susx.tag.classificationframework.classifiers.Classifier;
import uk.ac.susx.tag.classificationframework.datastructures.ProcessedInstance;
import uk.ac.susx.tag.classificationframework.exceptions.EvaluationException;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.FeatureExtractionPipeline;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An Evaluation instance represents an evaluation of a classifier over a set of documents.
 *
 * The evaluation includes the following data:
 *
 *  1. Precision, recall, and FB1 per class label
 *  2. Total documents labelled correctly
 *  3. Total documents seen
 *  4. Overall accuracy (= 2. / 3.)
 *  5. Full confusion matrix of each label versus each label
 *
 * Usage:
 *
 *  Evaluate e = new Evaluation(classifier, pipeline, goldStandardDocs);
 *
 *  Where "goldStandardDocs" is an iterable over ProcessedInstances.
 *  If you don't have ProcessedInstances, then use the new FeatureExtractionPipeline
 *  extractFeatures() method which accepts an iterable of Instances.
 *
 * User: Andrew D. Robertson
 * Date: 10/02/2014
 * Time: 11:43
 */
public class Evaluation {

    // Label -> [Precision, Recall, FB1]. See precision(), recall(), fb1() convenience methods.
    public Map<String, double[]> measures = new HashMap<>();

    // Accuracy = totalCorrect / totalDocuments
    public double accuracy = 0;

    // Total number of documents seen
    public int totalDocuments = 0;

    // Total number of documents given the correct classification
    public int totalCorrect = 0;

    // Actual Label --> [Predicted Label --> [Count]]. See getConfusionMatrixValue() method.
    public Map<String, Map<String, Integer>> confusionMatrix = new HashMap<>();


    /**
     * Use this to create a new evaluation. Note that this method (unlike the old Util.evaluate())
     * expects an iterable over ProcessedInstances. This is an effort to encourage more efficiency,
     * since you've probably already processed the Instances elsewhere.
     *
     * However, if you haven't already done so, then there is now an extractFeatures() method on
     * the FeatureExtractionPipeline which accepts an iterable of Instances, and returns an
     * iterable over ProcessedInstances (lazily processed).
     */
    public Evaluation(Classifier classifier, FeatureExtractionPipeline pipeline, Iterable<ProcessedInstance> goldStandardDocs){
        Set<String> labels = new HashSet<>();
        for (int label : classifier.getLabels())
            labels.add(pipeline.labelString(label));

        // Initialise data structures
        for (String label : labels) {
            confusionMatrix.put(label, new HashMap<String, Integer>());
            measures.put(label, new double[3]);

            for (String l : labels)
                confusionMatrix.get(label).put(l, 0);
        }

        // Obtain confusion counts and totals
        for (ProcessedInstance doc : goldStandardDocs) {
            String systemLabel = pipeline.labelString(classifier.bestLabel(doc.features));
            String goldLabel = pipeline.labelString(doc.getLabel());

            if (!labels.contains(goldLabel)) throw new EvaluationException("The Gold standard contains labels that the classifier is unaware of.");

            incConfusionMatrixValue(goldLabel, systemLabel);

            if (systemLabel.equals(goldLabel))
                totalCorrect++;
            totalDocuments++;
        }

        // Calculate precision, recall, fb1, and accuracy.
        calculateMeasures();
    }

    /**
     * Accessors for various counts and measures.
     */
    public Set<String> getLabels()  { return measures.keySet(); }
    public double precision(String label) { return measures.get(label)[0];}
    public double recall(String label)    { return measures.get(label)[1];}
    public double fb1(String label)       { return measures.get(label)[2];}

    /**
     * The best way to construct a full confusion matrix is probably:
     *
     * Evaluation e = new Evaluation(classifier, pipeline, goldStandardDocs);
     * for (String actualLabel : e.getLabels()){
     *     for (String predictedLabel: e.getLabels()) {
     *         int count = getConfusionMatrixValue(actualLabel, predictedLabel);
     *     }
     * }
     * @param actualLabel The label that should have been picked. The gold standard label.
     * @param predictedLabel The label that the classifier predicted.
     */
    public int getConfusionMatrixValue(String actualLabel, String predictedLabel) {
        return confusionMatrix.get(actualLabel).get(predictedLabel);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        DecimalFormat df = new DecimalFormat("#.###");
        for (String label : getLabels()) {
            sb.append(label); sb.append("\n\n");
            sb.append("  Precision : "); sb.append(df.format(precision(label))); sb.append("\n");
            sb.append("  Recall    : "); sb.append(df.format(recall(label))); sb.append("\n");
            sb.append("  FB1       : "); sb.append(df.format(fb1(label))); sb.append("\n\n");
        }   sb.append("Accuracy    : "); sb.append(df.format(accuracy)); sb.append("\n\n");

        sb.append("Confusion Matrix (rows = actual label, columns = predicted label)\n\n");

        List<String> labels = Lists.newArrayList(confusionMatrix.keySet());

        sb.append("  X\t\t");
        for (String label : labels) {sb.append(label.substring(0, 3)); sb.append(" "); }

        sb.append("\n");
        for (String actualLabel : labels) {
            sb.append("  ");
            sb.append(actualLabel.substring(0, 3)); sb.append("\t");
            for (String predictedLabel : labels) {
                sb.append(getConfusionMatrixValue(actualLabel, predictedLabel)); sb.append("\t");
            } sb.append("\n");
        } return sb.toString();
    }

    private void incConfusionMatrixValue(String actualLabel, String predictedLabel){
        confusionMatrix.get(actualLabel).put(predictedLabel, confusionMatrix.get(actualLabel).get(predictedLabel) + 1);
    }

    private void calculateMeasures() {

        for (String label : measures.keySet()) {

            int truePositives = getConfusionMatrixValue(label, label);
            int falsePositives = 0;
            int falseNegatives = 0;
            for (String l : measures.keySet()){
                if (!label.equals(l)) {
                    falsePositives += getConfusionMatrixValue(l, label);
                    falseNegatives += getConfusionMatrixValue(label, l);
                }
            }

            double TP_plus_FP = truePositives + falsePositives;
            measures.get(label)[0] = TP_plus_FP==0? 1 : truePositives / TP_plus_FP; // Precision

            double TP_plus_FN = truePositives + falseNegatives;
            measures.get(label)[1] = TP_plus_FN==0? 1 : truePositives / TP_plus_FN; // Recall

            double PREC_plus_REC = precision(label) + recall(label);
            measures.get(label)[2] = PREC_plus_REC==0? 0 : (2*precision(label)*recall(label)) / PREC_plus_REC; // FB1
        }
        accuracy = ((double)totalCorrect) / totalDocuments;
    }

}
