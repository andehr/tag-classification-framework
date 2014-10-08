package uk.ac.susx.tag.classificationframework;

/*
 * #%L
 * Util.java - classificationframework - CASM Consulting - 2,013
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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import uk.ac.susx.tag.classificationframework.classifiers.Classifier;
import uk.ac.susx.tag.classificationframework.classifiers.NaiveBayesClassifier;
import uk.ac.susx.tag.classificationframework.classifiers.NaiveBayesOVRClassifier;
import uk.ac.susx.tag.classificationframework.datastructures.Instance;
import uk.ac.susx.tag.classificationframework.datastructures.ProcessedInstance;
import uk.ac.susx.tag.classificationframework.datastructures.StringIndexer;
import uk.ac.susx.tag.classificationframework.exceptions.FeatureExtractionException;
import uk.ac.susx.tag.classificationframework.featureextraction.inference.FeatureInferrer;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.FeatureExtractionPipeline;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.PipelineBuilder;
import uk.ac.susx.tag.classificationframework.jsonhandling.JsonInstanceListStreamWriter;
import uk.ac.susx.tag.classificationframework.jsonhandling.JsonListStreamReader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;

/**
 * Util class providing convenience functions. This comment should maintain a directory of
 * what functions are contained in this class and a short line description of their purpose.
 * Furthermore, use comment dividers between functions in order to group functionality by
 * category.
 *
 * Classification convenience methods:
 *  - setFeatureAlpha()   : Set the pseudo-counts of a NB classifier, passing String features instead of ints.
 *  - initNBWithLabels()  : Create a new NB classifier initialised with particular class labels, specified with strings.
 *  - classifyInstances() : Use NB to set the labelling of a collection of ProcessedInstances.
 *  - classifyInstancesIterable() : Get iterable over ProcessedInstances, each being classified lazily on call to .next()
 *  - inferVocabulary()   : Infer the vocabulary of a collection of ProcessedInstances
 *
 * Classifier performance:
 *  - evaluate() : Obtain PerformanceEvaluation instance for classifier on data, detailing precision, recall, fb1, acc.
 *
 * Json:
 *  - getGson()     : Obtain a reference to a singleton Gson instance.
 *  - convertJson() : Convert a file containing JSON object on each line, to a proper JSON array.
 *
 * Gender Identification:
 *  - createTrainingData()     : Given a newly downloaded "names" folder from http://www.ssa.gov/OACT/babynames/limits.html, create appropriate training data.
 *  - trainGenderClassifier()  : Train a naive bayes classifier on training data obtained from "createTrainignData()"
 *  - genderClassifierTester() : Interactive testing of the gender classifier
 *
 * User: Andrew D. Robertson
 * Date: 08/08/2013
 * Time: 16:13
 */
public class Util {

/********************************
 * Feature extraction convenience methods
 *******************************/

    /**
     * Given a set of documents and a feature to look for, find those documents in which said feature is found, and
     * return a list of the text fields of those documents (there are alternative methods below which return the
     * whole document instead, or which take an unindexed feature as input).
     */
    public static List<String> getOriginalContextStrings(int feature, Iterable<ProcessedInstance> documents){
        List<String> originalContexts = new ArrayList<>();
        for (ProcessedInstance document : documents) {
            if (document.hasFeature(feature))
                originalContexts.add(document.source.text + " ID:" + document.source.id);
        }
        return originalContexts;
    }

    public static List<String> getOriginalContextStrings(String feature, Iterable<ProcessedInstance> documents, FeatureExtractionPipeline pipeline){
        return getOriginalContextStrings(pipeline.featureIndex(feature), documents);
    }

    public static List<ProcessedInstance> getOriginalContextDocuments(int feature, Iterable<ProcessedInstance> documents){
        List<ProcessedInstance> originalContexts = new ArrayList<>();
        for (ProcessedInstance document: documents) {
            if (document.hasFeature(feature))
                originalContexts.add(document);
        }
        return originalContexts;
    }

    public static List<ProcessedInstance> getOriginalContextDocuments(String feature, Iterable<ProcessedInstance> documents, FeatureExtractionPipeline pipeline){
        return getOriginalContextDocuments(pipeline.featureIndex(feature), documents);
    }

    /**
     * Given a document, retrieve a human-readable set of the features extracted for said document.
     */
    public static Set<String> getFeatureSet(ProcessedInstance document, FeatureExtractionPipeline pipeline){
        Set<String> features = new HashSet<>();
        StringIndexer indexer = pipeline.getFeatureIndexer();
        for (int feature : document.features) {
            features.add(indexer.getValue(feature));
        } return features;
    }

    public static Set<String> getFeatureSet(Instance document, FeatureExtractionPipeline pipeline) {
        Set<String> features = new HashSet<>();
        StringIndexer indexer = pipeline.getFeatureIndexer();
        for (int feature : pipeline.extractFeatures(document).features) {
            features.add(indexer.getValue(feature));
        } return features;
    }

    public static Set<FeatureInferrer.Feature> getTypedFeatureSet(Instance document, FeatureExtractionPipeline pipeline){
        return Sets.newHashSet(pipeline.extractUnindexedFeatures(document));
    }

    public static double occurrenceFraction(int feature, Iterable<ProcessedInstance> documents){
        int count = 0;
        int total = 0;
        for(ProcessedInstance document: documents){
            total++;
            if (document.hasFeature(feature))
                count++;
        }
        return ((double)count) / total;
    }

    public static double occurrenceFraction(String feature, Iterable<ProcessedInstance> documents, FeatureExtractionPipeline pipeline){
        return occurrenceFraction(pipeline.featureIndex(feature), documents);
    }

    /**
     * Given a set of features and a bunch of documents containing those features, produce a mapping from each feature
     * to the fraction of the number of documents in which that feature occurs.
     */
    public static Map<String, Double> documentOccurrenceFractions(Set<String> features, Iterable<ProcessedInstance> documents, FeatureExtractionPipeline pipeline){
        Int2IntOpenHashMap indexedFeatureCounts = new Int2IntOpenHashMap();
        int total = 0;
        for (String feature : features){
            indexedFeatureCounts.put(pipeline.featureIndex(feature), 0);
        }
        for (ProcessedInstance document : documents){
            total++;
            for (int docFeature : new IntOpenHashSet(document.features)) {
                if (indexedFeatureCounts.containsKey(docFeature)) {
                    indexedFeatureCounts.addTo(docFeature, 1);
                }
            }
        }
        Map<String, Double> fractions = new HashMap<>();
        for (Int2IntMap.Entry e : indexedFeatureCounts.int2IntEntrySet()){
            fractions.put(pipeline.featureString(e.getIntKey()), ((double)e.getIntValue())/total);
        }
        return fractions;
    }


    /**
     * Create an index that maps a feature to the set of all documents which contain said feature.
     */
    public static Int2ObjectOpenHashMap<Set<ProcessedInstance>> feature2DocumentIndex(Iterable<ProcessedInstance> documents){
        Int2ObjectOpenHashMap<Set<ProcessedInstance>> index = new Int2ObjectOpenHashMap<>();
        for (ProcessedInstance document : documents){
            for (int feature : document.features){
                if (!index.containsKey(feature)){
                    index.put(feature, new HashSet<ProcessedInstance>());
                }
                index.get(feature).add(document);
            }
        }
        return index;
    }

    public static Map<FeatureInferrer.Feature, Set<String>> typedFeature2DocumentIndex(Iterable<Instance> documents, FeatureExtractionPipeline pipeline){
        Map<FeatureInferrer.Feature, Set<String>> index = new HashMap<>();
        for (Instance document : documents){
            List<FeatureInferrer.Feature> features = pipeline.extractUnindexedFeatures(document);
            for (FeatureInferrer.Feature feature : features){
                if (!index.containsKey(feature)){
                    index.put(feature, new HashSet<String>());
                }
                index.get(feature).add(document.text);
            }
        }
        return index;
    }



/**********************
 * Classification convenience methods
 **********************/

    /**
     * Set the pseudo-counts of a NB classifier, passing String features instead of ints.
     * @param alpha The amount of pseudo-counts to be added.
     */
    public static void setFeatureAlpha(String feature, String label, double alpha, NaiveBayesClassifier nb, FeatureExtractionPipeline pipeline){
        nb.setFeatureAlpha(pipeline.featureIndex(feature), pipeline.labelIndex(label), alpha);
    }

    public static void setFeatureAlphas(Collection<String> features, String label, double alpha,NaiveBayesClassifier nb, FeatureExtractionPipeline pipeline ){
        for (String feature : features) setFeatureAlpha(feature, label, alpha, nb, pipeline);
    }

    /**
     * Create a new NB classifier initialised with particular class labels, specified with strings.
     * Generally, if you've already got a NB and you want to create a new one initialised with the same labels, you
     * can just do the following, where "nbClassifier" is a pre-existing trained classifier:
     *
     * NaiveBayesClassifier newNB = new NaiveBayesClassifier(nbClassifier.getLabels());
     *
     * So this method is primarily for testing purposes.
     */
    public static NaiveBayesClassifier initNBWithLabels(FeatureExtractionPipeline pipeline, String... labels){
        IntSet convertedLabels = new IntOpenHashSet();
        for (String label : labels){
            convertedLabels.add(pipeline.labelIndex(label));
        }
        return new NaiveBayesClassifier(convertedLabels);
    }

	/**
	 * Create a new NB OVR classifier initialised with particular class labels, specified with strings.
	 * This method is the generalisation of initNBWithLabels, as it returns an NaiveBayesOVRClassifier,
	 * which could wrap any kind of NaiveBayesClassifier (NaiveBayesClassifier, NaiveBayesClassifierSFE,
	 * NaiveBayesClassifierFeatureMarginals, etc).
	 */
	//public static NaiveBayesOVRClassifier<? extends  NaiveBayesClassifier> initNBOVRWithLabels(Class<? extends NaiveBayesClassifier> learnerClass, FeatureExtractionPipeline pipeline, String... labels) {
	//	IntSet convertedLabels = new IntOpenHashSet();
	//	for (String label : labels) {
	//		convertedLabels.add(pipeline.labelIndex(label));
	//	}
	//	return new NaiveBayesOVRClassifier<T>()
	//}

    /**
     * Given a collection of ProcessedInstances, get the set of unique features used in the document.
     */
    public static IntSet inferVocabulary(Collection<ProcessedInstance> instances) {
        IntSet vocab = new IntOpenHashSet();
        for (ProcessedInstance instance : instances){
            for (int feature : instance.features){
                vocab.add(feature);
            }
        } return vocab;
    }

    /**
     * Use NB to set the labelling of a collection of ProcessedInstances.
     */
    public static void classifyInstances(Collection<ProcessedInstance> instances, Classifier classifier){
        for (ProcessedInstance instance : instances) {
            instance.setLabeling(classifier.predict(instance.features));
        }
    }

    public static Iterable<ProcessedInstance> classifyInstancesIterable(final Iterable<ProcessedInstance> instances,
                                                                        final Classifier classifier) {

        final Iterator<ProcessedInstance> instanceIterator = instances.iterator();
        return new Iterable<ProcessedInstance>() {

            @Override
            public Iterator<ProcessedInstance> iterator() {
                return new Iterator<ProcessedInstance>() {
                    @Override
                    public boolean hasNext() {
                        return instanceIterator.hasNext();
                    }

                    @Override
                    public ProcessedInstance next() {
                        if (instanceIterator.hasNext()){
                            ProcessedInstance instance = instanceIterator.next();
                            instance.setLabeling(classifier.predict(instance.features));
                            return instance;
                        } else {
                            throw new NoSuchElementException();
                        }
                    }

                    @Override
                    public void remove() { throw new UnsupportedOperationException(); }
                };
            }
        };
    }

/************************
 * Classifier performance
 ************************/

    /**
     * Class representing the performance of a classifier on a collection of gold standard data.
     * Set of evaluated labels can be obtained with "getLabels()"
     * Convenience methods for precision, recall and fb1 return the statistic for a specific label.
     * The accuracy field holds the overall accuracy.
     */
    @Deprecated
    public static class PerformanceEvaluation {

        public Map<String, double[]> measures = new HashMap<>();  // Label -> [Precision, Recall, FB1]
        public double accuracy;

        public Set<String> getLabels()        { return measures.keySet(); }
        public double precision(String label) { return measures.get(label)[0];}
        public double recall(String label)    { return measures.get(label)[1];}
        public double fb1(String label)       { return measures.get(label)[2];}

        public PerformanceEvaluation(Collection<String> labels) {
            for (String label : labels)  measures.put(label, new double[3]);
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            DecimalFormat df = new DecimalFormat("#.###");
            for (String label : getLabels()) {
                sb.append(label); sb.append("\n\n");
                sb.append("  Precision : "); sb.append(df.format(precision(label))); sb.append("\n");
                sb.append("  Recall    : "); sb.append(df.format(recall(label))); sb.append("\n");
                sb.append("  FB1       : "); sb.append(df.format(fb1(label))); sb.append("\n\n");
            }   sb.append("Accuracy    : "); sb.append(df.format(accuracy));
            return sb.toString();
        }
    }

    /**
     * Evaluate classifier, see PerformanceEvaluation class above.
     */
    @Deprecated
    public static PerformanceEvaluation evaluate(Classifier classifier,
                                                 FeatureExtractionPipeline pipeline,
                                                 Iterable<Instance> goldStandardDocs) {
        int total = 0;
        int totalCorrect = 0;

        Map<String, int[]> stats = new HashMap<>();
        for (int label : classifier.getLabels()) {
            stats.put(pipeline.labelString(label), new int[3]); //  Label -> [TP, FP, FN]
        }

        for (Instance doc : goldStandardDocs) {
            String systemOutput = pipeline.labelString(classifier.bestLabel(pipeline.extractFeatures(doc).features));
            if (systemOutput.equals(doc.label)) {
                stats.get(systemOutput)[0] += 1;
                totalCorrect += 1;
            } else {
                stats.get(systemOutput)[1] += 1;
                stats.get(doc.label)[2] += 1;
            }
            total += 1;
        }
        return calcMeasures(stats, totalCorrect, total);
    }

    @Deprecated
    public static PerformanceEvaluation evaluateProcessed(Classifier classifier,
                                                          FeatureExtractionPipeline pipeline,
                                                          Iterable<ProcessedInstance> goldStandardDocs) {
        int total = 0;
        int totalCorrect = 0;

        Map<String, int[]> stats = new HashMap<>();
        for (int label : classifier.getLabels()) {
            stats.put(pipeline.labelString(label), new int[3]); //  Label -> [TP, FP, FN]
        }

        for (ProcessedInstance doc : goldStandardDocs) {
            String systemOutput = pipeline.labelString(classifier.bestLabel(doc.features));
            String goldLabel = pipeline.labelString(doc.getLabel());
            if (systemOutput.equals(goldLabel)) {
                stats.get(systemOutput)[0] += 1;
                totalCorrect += 1;
            } else {
                stats.get(systemOutput)[1] += 1;
                stats.get(goldLabel)[2] += 1;
            }
            total += 1;
        }
        return calcMeasures(stats, totalCorrect, total);
    }

    @Deprecated
    private static PerformanceEvaluation calcMeasures(Map<String, int[]> stats, int totalCorrect, int total){
        PerformanceEvaluation eval = new PerformanceEvaluation(stats.keySet());
        for (Map.Entry<String, int[]> entry : stats.entrySet()) {
            String label = entry.getKey();
            int[] labelStats = entry.getValue();

            double tp_plus_fp = labelStats[0]+labelStats[1];  // TP + FP
            eval.measures.get(label)[0] = tp_plus_fp==0? 1 : labelStats[0]/tp_plus_fp;

            double tp_plus_fn = labelStats[0]+labelStats[2]; // TP + FN
            eval.measures.get(label)[1] = tp_plus_fn==0? 1 : labelStats[0]/tp_plus_fn;

            double prec_plus_rec = eval.precision(label) + eval.recall(label);  // Precision + Recall
            eval.measures.get(label)[2] = prec_plus_rec==0? 0 : (2*eval.precision(label)*eval.recall(label)) / prec_plus_rec;
        }
        eval.accuracy = ((double)totalCorrect) / total;
        return eval;
    }


/*********************
 * Json
 *********************/

    private static Gson gson = null;
    public static Gson getGson() {
        if (gson == null) gson = getNewGson();
        return gson;
    }
    private static Gson getNewGson() {
        GsonBuilder gsonBuilder =
                new GsonBuilder()
                     .setPrettyPrinting();
        return gsonBuilder.create();
    }

    /**
     * Reads a file where each line is a JSON object, and creates a converted file
     * where each object is an element in a proper json list.
     * @return File object representing converted file.
     */
    public static File convertJson(File file) throws IOException{
        try (BufferedReader br = new BufferedReader(new FileReader(file));
             BufferedWriter bw = new BufferedWriter(new FileWriter(new File(file.getAbsolutePath()+".converted")))){
            String line = br.readLine();
            if (line==null) throw new IOException("File empty");
            bw.write("["); bw.write(line);
            while ((line=br.readLine())!=null) {  bw.write(","); bw.write(line); }
            bw.write("]");
        }
        return new File(file.getAbsolutePath()+".converted");
    }

/***********************
 * Gender Identification
 ***********************/

    /**
     * Same as method defined below, with a default feature extraction pipeline for gender classification.
     */
    public static NaiveBayesClassifier trainGenderClassifier(File genderDataFile) throws IOException {
        return trainGenderClassifier(buildGenderPipeline(), genderDataFile);
    }

    /**
     * Given a training file created from U.S. census data (see "createTrainingData" method).
     * Train a naive bayes classifier to be able to classify the gender of first names.
     *
     * The pipeline should be able to find the first name within a document, then extract
     * features which are telling of gender.
     */
    public static NaiveBayesClassifier trainGenderClassifier(FeatureExtractionPipeline pipeline,
                                                             File genderDataFile) throws IOException {

        // Get the training instances
        final List<Instance> instances;
        try( JsonListStreamReader sr = new JsonListStreamReader(genderDataFile, new Gson()) ) {
            instances = Lists.newLinkedList(sr.iterableOverInstances());
        }
        if (instances.isEmpty()) throw new FeatureExtractionException("No training data found...");

        // Run 'em through the pipeline to get ProcessedInstances
        List<ProcessedInstance> training = new LinkedList<>();
        for (Instance i : instances){
            training.add(pipeline.extractFeaturesWithoutCache(i));
        }

        // The weighting of each name is stored as the ID of the Instances (shh.. it was convenient)
        IntList weights = new IntArrayList(training.size());
        for (Instance i : instances){
            weights.add(Integer.parseInt(i.id));
        }

        // Create a NaiveBayes using the weights of each instance
        NaiveBayesClassifier nb = new NaiveBayesClassifier();
        nb.train(training, weights);
        return nb;
    }


    /**
     * Go to:  http://www.ssa.gov/OACT/babynames/limits.html
     *
     * There you can find census data from the U.S. from the 1800s to 2013. When you download this
     * information, it will be in a folder called "names".
     *
     * The purpose of this function is to take that names folder and convert it into a single
     * file that the "trainGenderClassifier" method above can make use of.
     *
     * @param inputDir the "names" directory.
     * @param output the output file
     */
    public static void createTrainingData(File inputDir, File output) throws IOException {
        Map<String, Map<String, Integer>> data = new HashMap<>();
        data.put("M", new HashMap<String, Integer>());
        data.put("F", new HashMap<String, Integer>());

        for (File input_file : inputDir.listFiles(new FilenameFilter() { public boolean accept(File dir, String name) { return name.startsWith("yob"); } })) {

            try (BufferedReader br = new BufferedReader(new FileReader(input_file))){
                String line;
                while ((line = br.readLine()) != null) {

                    line = line.trim();
                    String[] items = line.split(",");

                    if (data.get(items[1]).containsKey(items[0].toLowerCase())){
                        int current = data.get(items[1]).get(items[0].toLowerCase());
                        data.get(items[1]).put(items[0].toLowerCase(), current + Integer.parseInt(items[2]));
                    } else {
                        data.get(items[1]).put(items[0].toLowerCase(), Integer.parseInt(items[2]));
                    }
                }
            }
        }

        List<Instance> instanceList = new LinkedList<>();
        for (Map.Entry<String, Map<String, Integer>> perGenderNames : data.entrySet()){
            for (Map.Entry<String, Integer> entry : perGenderNames.getValue().entrySet()){
                instanceList.add(new Instance(perGenderNames.getKey(), entry.getKey(), entry.getValue().toString()));
            }
        }
        try (JsonInstanceListStreamWriter sw = new JsonInstanceListStreamWriter(output)){
             sw.write(instanceList);
        }
    }

    /**
     * Enter "q" for a name to quit.
     */
    public static void genderClassifierTester(File genderDataFile) throws IOException {

        FeatureExtractionPipeline pipeline = buildGenderPipeline();
        NaiveBayesClassifier nb = trainGenderClassifier(pipeline, genderDataFile);

        String name;
        boolean quit = false;
        Scanner scanIn = new Scanner(System.in);

        System.out.print("Enter name here : ");
        while (!quit){
            name = scanIn.nextLine();
            if (name.equals("q")) quit=true;
            else{
                System.out.println("Gender: " + pipeline.labelString(nb.bestLabel(pipeline.extractFeatures(new Instance("", name, "")).features)));
                Int2DoubleOpenHashMap predictions = nb.predict(pipeline.extractFeatures(new Instance("", name, "")).features);
                System.out.println("Predictions: ");
                for (Map.Entry<Integer, Double> entry : predictions.entrySet()) {
                    System.out.println("  " + pipeline.labelString(entry.getKey()) + " : " + entry.getValue());
                }
                System.out.print("Enter name here : ");
            }
        }
        scanIn.close();
    }

/*******************************
 * Pipeline building convenience methods
 *******************************/

    public static FeatureExtractionPipeline buildParsingPipeline(boolean removeStopwords, boolean normaliseURLs) {
    // It may look like there is an inconsistency about where boolean and String types are used,
    // But this is just a test of how flexible the framework is. Anywhere where you see a boolean,
    // a String would do. Where you see a map, a JSON string representing a map would do.

    // TODO: make consistent, and place documentation to demonstration flexibility inside the PipelineBuilder class.

    PipelineBuilder pb = new PipelineBuilder();
    List<PipelineBuilder.Option> options = Lists.newArrayList(
            new PipelineBuilder.Option("tokeniser", ImmutableMap.of("type", "cmu",
                    "filter_punctuation", "true",
                    "normalise_urls", normaliseURLs,
                    "lower_case", "false")),
            new PipelineBuilder.Option("remove_stopwords", removeStopwords),
            new PipelineBuilder.Option("unigrams", true),
            new PipelineBuilder.Option("bigrams", true),
            new PipelineBuilder.Option("dependency_parser", true));
    return pb.build(options);
}


    public static FeatureExtractionPipeline buildCMUPipeline(boolean removeStopwords, boolean normaliseURLs) {
        PipelineBuilder pb = new PipelineBuilder();
        List<PipelineBuilder.Option> options = Lists.newArrayList(
                new PipelineBuilder.Option("tokeniser", ImmutableMap.of("type", "cmu",
                        "filter_punctuation", "true",
                        "normalise_urls", normaliseURLs,
                        "lower_case", "true")),
                new PipelineBuilder.Option("remove_stopwords", removeStopwords),
                new PipelineBuilder.Option("unigrams", true),
                new PipelineBuilder.Option("bigrams", true));
        return pb.build(options);
    }

    public static FeatureExtractionPipeline buildBasicPipeline(boolean removeStopwords, boolean normaliseURLs){
        PipelineBuilder pb = new PipelineBuilder();
        List<PipelineBuilder.Option> options = Lists.newArrayList(
                new PipelineBuilder.Option("tokeniser", ImmutableMap.of("type", "basic",
                                            "normalise_urls", normaliseURLs,
                                            "lower_case", "true")),
                new PipelineBuilder.Option("remove_stopwords", removeStopwords),
                new PipelineBuilder.Option("normalise_repeated_qe_marks", true),
                new PipelineBuilder.Option("unigrams", true),
                new PipelineBuilder.Option("bigrams", true));
        return pb.build(options);
    }

    public static FeatureExtractionPipeline buildGenderPipeline(){
        PipelineBuilder pb = new PipelineBuilder();
        List<PipelineBuilder.Option> options = Lists.newArrayList(
                new PipelineBuilder.Option("tokeniser", ImmutableMap.of("type", "basic",
                                                                        "normalise_urls", "false",
                                                                        "lower_case", "true")),
                new PipelineBuilder.Option("first_name_gender_features", true));
        return pb.build(options);
    }

/*******************************
* Feature Marginals / SFE - common methods
*******************************/

    public static Int2DoubleOpenHashMap calculateWordProbabilities(Iterable<ProcessedInstance> data) {
        // Obtain word frequencies: F(w) -> Frequency of word w in the given data
        Int2DoubleOpenHashMap wordFreq = new Int2DoubleOpenHashMap();
        wordFreq.defaultReturnValue(0);

        int n = 0;

        // Count word Frequencies
        for (ProcessedInstance i : data) {
            for (int featIdx : i.features) {
                wordFreq.addTo(featIdx, 1.);
                n++;
            }
        }
        // P(w) -> Probability of word w in the given data
        Int2DoubleOpenHashMap wordProb = new Int2DoubleOpenHashMap();

        // Calculate Probabilities
        for (int k : wordFreq.keySet()) {
            wordProb.put(k, (wordFreq.get(k) / n));
        }

        return wordProb;
    }
}
