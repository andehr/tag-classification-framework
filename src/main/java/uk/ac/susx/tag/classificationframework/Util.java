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
import it.unimi.dsi.fastutil.ints.*;
import uk.ac.susx.tag.classificationframework.classifiers.Classifier;
import uk.ac.susx.tag.classificationframework.classifiers.NaiveBayesClassifier;
import uk.ac.susx.tag.classificationframework.clusters.ClusteredProcessedInstance;
import uk.ac.susx.tag.classificationframework.datastructures.Instance;
import uk.ac.susx.tag.classificationframework.datastructures.ProcessedInstance;
import uk.ac.susx.tag.classificationframework.datastructures.StringIndexer;
import uk.ac.susx.tag.classificationframework.exceptions.FeatureExtractionException;

import uk.ac.susx.tag.classificationframework.featureextraction.filtering.TokenFilterPunctuation;
import uk.ac.susx.tag.classificationframework.featureextraction.filtering.TokenFilterRelevanceStopwords;
import uk.ac.susx.tag.classificationframework.featureextraction.inference.FeatureInferrer;
import uk.ac.susx.tag.classificationframework.featureextraction.inference.FeatureInferrerUnigrams;

import uk.ac.susx.tag.classificationframework.featureextraction.normalisation.TokenNormaliserToLowercase;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.FeatureExtractionPipeline;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.PipelineBuilder;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.PipelineBuilder.OptionList;
import uk.ac.susx.tag.classificationframework.featureextraction.tokenisation.TokeniserCMUTokenOnly;
import uk.ac.susx.tag.classificationframework.featureextraction.tokenisation.TokeniserChineseStanford;
import uk.ac.susx.tag.classificationframework.jsonhandling.JsonInstanceListStreamWriter;
import uk.ac.susx.tag.classificationframework.jsonhandling.JsonListStreamReader;

import javax.ws.rs.HEAD;
import java.io.*;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.google.common.io.Files.copy;
import static java.nio.file.Files.move;

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
 * Common text patterns
 ********************************/

    public static final Pattern whitespacePattern = Pattern.compile("\\s+");

/********************************
 * Feature extraction convenience methods
 *******************************/

    public static boolean isNullOrEmptyText(Instance i){
        return i.text == null || i.text.trim().equals("");
    }

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

    /**
     * Return the fraction of documents which are labelled with *label*
     */
    public static double getIndicativeness(int label, List<ProcessedInstance> documents){
        return documents.stream()
                        .filter(document -> document.getLabel() == label)
                        .count()
               /(double)documents.size();
    }

    /**
     * Return the fraction of documents which are labelled with *label*
     */
    public static double getIndicativeness(String label, List<Instance> documents){
        return documents.stream()
                        .filter(document -> document.label.equals(label))
                        .count()
               /(double)documents.size();
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
        return () -> new Iterator<ProcessedInstance>() {
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

    /**
     * Given an iterable over ProcessedInstances, return an iterable over COPIES of the original Instances that the
     * ProcessedInstances came from. Except that whatever the label on the original Instance was before processing, it is
     * REPLACED ON THE COPY with the de-indexed label found on the ProcessedInstance.
     *
     * TIP: Use this with Guava's Iterables.concat() if you want to concatenate this with an iterable over human-annotated
     *      Instances.
     *
     * So, if I have an Instance *i* with label "positive", which I process through a pipeline to get ProcessedInstance *pi*,
     * then assign label probabilities to it using a classifier, and the most probable label is now "negative", this
     * function will ensure that while the original text and id of the Instance is returned, the label field of the copy
     * will be "negative" instead of "positive".
     *
     * Feature selection use case:
     *
     *   - You have decided to do feature selection on documents that you automatically classified.
     *   - Notice that to setup a feature selector, you must pass it an iterable of *Instances*, so that they can be
     *     processed through a pipeline so that feature statistics can be calculated. These instances must have labels
     *     assigned.
     *   - Usually your instance only has a label if it's been labelled by a human. Since if it was labelled automatically,
     *     it would first have become a ProcessedInstance.
     *   - To achieve an iterable of Instances, with the Instances updated with automatic labels after becoming a
     *     ProcessInstance, use this method.
     */
    public static Iterable<Instance> getInstancesWithUpdatedLabels(final Iterable<ProcessedInstance> instances, final FeatureExtractionPipeline pipeline){
        final Iterator<ProcessedInstance> instanceIterator = instances.iterator();
        return () -> new Iterator<Instance>() {
            @Override
            public boolean hasNext() {  return instanceIterator.hasNext(); }

            @Override
            public Instance next() {
                if (instanceIterator.hasNext()){
                    ProcessedInstance i = instanceIterator.next();
                    return new Instance(pipeline.labelString(i.getLabel()), i.source.text, i.source.id);
                } else {
                    throw new NoSuchElementException();
                }
            }

            @Override
            public void remove() { throw new UnsupportedOperationException(); }
        };
    }

    public static List<ProcessedInstance> extractOriginalProcessedInstances(Collection<ClusteredProcessedInstance> instances){
        return instances.stream()
                   .map(ClusteredProcessedInstance::getDocument)
                   .collect(Collectors.toList());
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
        data.put("M", new HashMap<>());
        data.put("F", new HashMap<>());

        for (File input_file : inputDir.listFiles((dir, name) -> name.startsWith("yob"))) {

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

    public static FeatureExtractionPipeline buildServicePipeline(String url){
        PipelineBuilder pb = new PipelineBuilder();
        OptionList options = new OptionList()
                // Document processing pipeline
                .add("tokeniser", ImmutableMap.of("type", "cmu",
                                                "filter_punctuation", "true",
                                                "normalise_urls", "true",
                                                "lower_case", "false"))
                // The tag converter and the CMU tagger is necessary for the dependency parser service
                .add("tag_converter", "true")
                .add("http_service", ImmutableMap.of("url", url))
                // Features to be extracted
                .add("unigrams", "true")
                .add("dependency_ngrams", ImmutableMap.of("include_bigrams", "true",
                                                          "include_trigrams", "true"));
        return pb.build(options);
    }

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
//                new PipelineBuilder.Option("bigrams", true),
                new PipelineBuilder.Option("dependency_parser", true),
                new PipelineBuilder.Option("dependency_ngrams", ImmutableMap.of("include_bigrams", "true",
                        "include_trigrams", "true")));
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

/***********************************************
 * String tools
 ***********************************************/

    /**
     * Iterable over all ngrams from minN to maxN.
     */
    public static Iterable<List<String>> ngrams(int minN, int maxN, List<String> tokens){
        return new Iterable<List<String>>(){
            int currentToken = 0;
            int currentN = minN;

            @Override
            public Iterator<List<String>> iterator() {
                return new Iterator<List<String>>() {
                    public boolean hasNext() {
                        return currentToken < tokens.size() - currentN + 1;
                    }

                    public List<String> next() {
                        // Create the current ngram
                        List<String> ngram = new ArrayList<>();
                        for (int i = currentToken; i < currentToken + currentN; i++)
                            ngram.add((tokens.get(i)));
                        currentToken++;

                        // Possibly update currentN
                        if (currentToken >= tokens.size() - currentN + 1 && currentN < maxN){
                            currentN++;
                            currentToken = 0;
                        }
                        return ngram;
                    }
                };
            }
        };
    }

/***************************************************************************************
 * General Utilities
 ***************************************************************************************/
    /**
     * Get batches over a collection, where only one batch is built using List.subList as and when the "next()" method
     * is called.
     *
     * This algorithm is tolerant of:
     *  - Batch sizes greater than the number of elements
     *  - Batch sizes that don't evenly fit into the total number of elements (last batch may be smaller)
     *  - Empty lists (returns empty iterator; hasNext() will only ever return false)
     */
    public static <E> Iterator<List<E>> iteratorOverBatches(List<E> elements, int batchSize) {
        return new Iterator<List<E>>() {
            int batchIndex = 0;

            @Override
            public boolean hasNext() {
                return batchIndex < elements.size() / (double)batchSize;
            }

            @Override
            public List<E> next() {
                if (hasNext()) {
                    int start = batchIndex * batchSize;
                    int end = Math.min(start + batchSize, elements.size());
                    batchIndex++;
                    return elements.subList(start, end);
                } else throw new NoSuchElementException();
            }
        };
    }

    public enum type {
        EXAMPLE, EXAMPLE2
    }


    @FunctionalInterface
    public interface SaveFunction {
        void save(File toBeSaved) throws IOException; // Throw exception if save failed.
    }

    /**
     * Queue up a series of files and associated save functions that need to be performed. This class takes care of
     * creating backups, saving to a separate backup location, then overwriting the save file safely. If a problem
     * occurs, then all files are reverted that were overwritten.
     *
     * Upon calling the save() function, backups of all files will be taken. Then the save functions will be executed
     * in order. If an exception is encountered, all files modified so far will be reverted to their backups and execution
     * will halt with an IOException. If a backup couldn't be used for some reason, this will be included in the IOException message,
     * and the backups will be left on disk to be resolved by a dev.
     *
     * E.g:
     *
     *  new SafeSave()
     *      .add(file1, (f) -> saveFunc1)
     *      .add(file2, (f) -> saveFunc2)
     *      .add(file3, (f) -> saveFunc3)
     *    .save()
     *
     * Alternatively, to save a single file, you can use static one-file save method:
     *
     *  SafeSave.save(file, (f) -> saveFunc)
     */
    public static class SafeSave {

        static final String BACKUP_SUFFIX = ".safe-save-backup";
        static final String TEMP_SAVE_SUFFIX = ".safe-save-temp-save";

        List<File> files;
        List<SaveFunction> saveFunctions;

        public SafeSave() {
            this.files = new ArrayList<>();
            this.saveFunctions = new ArrayList<>();
        }

        /**
         * Shortcut convenience function for saving a single file.
         */
        public static void save(File file, SaveFunction save) throws IOException {
            new SafeSave()
                    .add(file, save)
                    .save();
        }

        /**
         * Queue up a save function to be executed later with the save() method.
         */
        public SafeSave add(File file, SaveFunction save){
            files.add(file);
            saveFunctions.add(save);
            return this;
        }

        /**
         * Begin executions of the save functions after taking backups. Use the backups if exception is encountered.
         * @throws IOException Thrown if executed had to be halted and backups used.
         */
        public void save() throws IOException {
            List<File> backedup = new ArrayList<>();
            Set<File> backupErrors = new HashSet<>();
            int idx = 0;
            try {
                // Step through save functions
                for (idx = 0; idx < files.size(); idx++) {
                    File toBeSaved = files.get(idx);

                    // Create backup of file - abort and revert everything if this fails
                    backedup.add(createBackup(toBeSaved));

                    // Perform save function to separate backup location - abort and revert everything if this fails
                    saveFunctions.get(idx).save(getTempSave(toBeSaved));

                    // Move temp save to original location in atomic operation
                    overwriteOriginalWithTempSave(toBeSaved);
                }
            } catch (Throwable throwable) {
                // Revert all files that have been overwritten so far
                backupErrors = revert(backedup);
                // Throw exception explaining what's gone down
                throwIOException(files.get(idx), backupErrors, throwable);
            } finally {
                // Delete any lingering backups or temp files (except those for which reverting failed)
                deleteBackupsAndTempSaves(backupErrors);
            }
        }

        private void throwIOException(File exceptionFile, Set<File> backupErrors, Throwable e) throws IOException{
            // Details on which was the problem file
            String info = "\nAn error occurred during backing-up/saving of file: " + exceptionFile.getAbsolutePath();

            // Details on reverted files
            info +=  "\nReverting the following files to backups (or deleting if previously didn't exist): " + files.stream().map(File::getAbsolutePath).collect(Collectors.toList());

            // Any backup errors
            if (!backupErrors.isEmpty()) {
                info += "\nACTION REQUIRED: Failed to copy or rename backups for the following files, so they are potentially corrupted, and their backups remain on disk: " + backupErrors.stream().map(File::getAbsolutePath).collect(Collectors.toList());
            }

            throw new IOException(info, e);
        }

        /**
         * Revert a list of files to their initial backup, recording a set of files for which the reverting failed.
         */
        private Set<File> revert(List<File> backedup){
            Set<File> errors = new HashSet<>();
            for (File file : backedup){
                if (!restoreBackup(file)){
                    errors.add(file);
                }
            }
            return errors;
        }

        /**
         * Restore backup of file. Return true if successful.
         */
        private boolean restoreBackup(File file){
            File backup = getBackup(file);
            try {
                if (backup.exists()){
                    copy(backup, file);
                } else {
                    // If the backup doesn't exist, then the savefile didn't exist previously. So delete current save attempt
                    // in order to revert to non-existence.
                    // If a problem with backing up had occurred, then we would have not listed it as having been backed up to be reverted.
                    file.delete();
                }
            } catch (IOException e){
                // Failed to copy backup back. Try just renaming
                if (file.exists()) file.delete();
                // Return true if succeed
                return backup.renameTo(file);
            }
            return true;
        }

        /**
         * Perform atomic move of a new save to the original save location (overwriting existing).
         */
        private void overwriteOriginalWithTempSave(File original) throws IOException {
            move(getTempSave(original).toPath(), original.toPath(), StandardCopyOption.ATOMIC_MOVE);
        }

        /**
         * Get pointer to file used for initial backup of a file.
         */
        private File getBackup(File file){
            return new File(file.getAbsolutePath() + BACKUP_SUFFIX);
        }

        /**
         * Get pointer to a file used for temporary save of another file.
         */
        private File getTempSave(File file) {
            return new File(file.getAbsolutePath() + TEMP_SAVE_SUFFIX);
        }

        /**
         * Create initial backup of a file.
         */
        private File createBackup(File file) throws IOException {
            if (file.exists()) {
                File backup = getBackup(file);
                copy(file, backup);
            }
            return file;
        }

        /**
         * Delete initial backup of a file
         */
        private void deleteBackup(File file){
            File backup = getBackup(file);
            if (backup.exists()){
                backup.delete();
            }
        }

        /**
         * Delete temporal save of a file
         */
        private void deleteTempSave(File file){
            File temp = getTempSave(file);
            if (temp.exists()){
                temp.delete();
            }
        }

        /**
         * Delete all backups and temp saves except those for which there was a problem reverting to backup.
         */
        private void deleteBackupsAndTempSaves(Set<File> exceptions) {
            for (File file : files){
                if (!exceptions.contains(file)) {
                    deleteBackup(file);
                    deleteTempSave(file);
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
//        ConfigHandlerPhraseNgrams c = new ConfigHandlerPhraseNgrams();

//        FeatureExtractionPipeline pipeline = new FeatureExtractionPipeline(){};
//        pipeline.setTokeniser(new TokeniserChineseStanford());
//        pipeline.add(new TokenNormaliserToLowercase());
//        pipeline.add(new TokenFilterRelevanceStopwords("zh"));
//        pipeline.add(new FeatureInferrerUnigrams());
//        pipeline.add(new TokenFilterPunctuation(true));
//        List<FeatureInferrer.Feature> result = pipeline.extractUnindexedFeatures(new Instance("", "知识就是力量。 Knowledge is power.", ""));
//        for (FeatureInferrer.Feature feature : result) {
//            String value = feature.value();
//            System.out.println(feature.value());
//        }

//        git rev-parse --abbrev-ref HEAD


        PipelineBuilder pb = new PipelineBuilder();
        OptionList l = new OptionList()

                .add("tokeniser", ImmutableMap.of(
                        "type", "arabicstanford",
                        "filter_punctuation", true,
                        "normalise_urls", true,
                        "lower_case", true))
                .add("remove_stopwords", ImmutableMap.of(
                        "use", "true",
                        "lang", "ar"))
                .add("unigrams", true);

        FeatureExtractionPipeline p = pb.build(l);
        p.extractUnindexedFeatures(new Instance("", "   جامعة الدول العربية أنتِ هي منظمة اتضم؛؛ أجمع   دولا، في وإذ هيهات  مايزال حيث «« الأوسط»» لستما", "")).forEach(System.out::println);
//                p.extractUnindexedFeatures(new Instance("", "مافتئ ماي مئتان أربعة أربعمائة أربعمئة وكان", "")).forEach(System.out::println);
//        p.extractUnindexedFeatures(new Instance("", "ياتي ذلك ثم ثم ثم  ؛ سعيدة وُشْكَانَ ذال ، مستقر\"", "")).forEach(System.out::println);


//        Instance doc = new Instance("test", "知识就是力量。","");
//        Instance doc1 = new Instance("","尼采曾经说过，这句话被当作金科玉律。","");
//        Instance doc2 = new Instance("","卡塔尔的总统叶绿素答应了特朗普的全部条件","");
//        Instance doc3 = new Instance("","天之道，损有余而补不足。","");
//
//        List<ProcessedInstance> docs = p.extractFeaturesInBatches(Lists.newArrayList(doc, doc1, doc2, doc3), 2);

//        for (ProcessedInstance doc_f: docs){
//            System.out.println(doc_f.source);
//        }
//        p.close();

//        Gson gson = new Gson();
//        FeatureExtractionPipeline pipeline = buildBasicPipeline(false, true);
//        Instance doc = new Instance("", "test @andehr", "");
//        ProcessedInstance pDoc = pipeline.extractFeatures(doc);
//        List<ProcessedInstance> o = Util.getOriginalContextDocuments("@andehr", Lists.newArrayList(pDoc), pipeline);
//        o.forEach(System.out::println);
//
//        FeatureExtractionPipeline pipeline2 = buildParsingPipeline(false, false);
//        Instance doc = new Instance("test", "I am famous", "");
//        Instance doc1 = new Instance("", "I am red", "");
//        Instance doc2 = new Instance("", "I am hungry", "");
//        Instance doc3 = new Instance("", "I am angry", "");
////
//        List<ProcessedInstance> docs = pipeline.extractFeaturesInBatches(Lists.newArrayList(doc, doc1, doc2, doc3), 2);
//
//        docs.get(0).setLabeling(ImmutableMap.of(1, 0.2, 2, 0.8));
//
//        List<ProcessedInstance> redocs = pipeline2.reprocessBatchWithSourceLabels(docs);
//
//        List<ProcessedInstance> redocs2 = pipeline2.reprocessBatchWithProcessedLabels(docs);
//
//        pipeline.close();

//        FeatureExtractionPipeline pipeline = new PipelineBuilder().build(new PipelineBuilder.OptionList() // Instantiate the pipeline.
//                        .add("tokeniser", ImmutableMap.of(
//                                        "type", "cmuTokeniseOnly",
//                                        "filter_punctuation", true,
//                                        "normalise_urls", true,
//                                        "lower_case", true
//                                )
//                        )
////                        .add("http_service", ImmutableMap.of("url", "http://test.co.uk"))
//                        .add("unigrams", true)
//                        .add("normalise_leading_trailing_punctuation", ImmutableMap.of("exclude_twitter_tags", false))
//        );
//
//        System.out.println(  );
//
//        List<FeatureInferrer.Feature> result = pipeline.extractUnindexedFeatures(new Instance("", "This is a \uFE50\uFE00@test", ""));
//
//
//
//        for (FeatureInferrer.Feature feature : result) {
//            String value = feature.value();
//            System.out.println(feature.value());
//        }

//        String s = result.stream().map(f -> f.value()).collect(Collectors.joining(" "));

//        System.out.println();
//        pipeline.updateService("http://test.co.uk", "http://newtest.co.uk");

//        System.out.println(pipeline.extractUnindexedFeatures(new Instance("", "this is a test @brexit'", "")));

//        Instance doc = new Instance("", "This. is. a. test.", "");
//
//        List<String> features1 = pipeline.extractUnindexedFeatures(doc).stream().map(FeatureInferrer.Feature::value).collect(Collectors.toList());
//
//        ((TokeniserTwitterBasic)pipeline.getTokeniser()).setPunctuationFilteringOffline();
//        pipeline.getPipelineComponent("remove_stopwords").setOffline();
//
//        List<String> features2 = pipeline.extractUnindexedFeatures(doc).stream().map(FeatureInferrer.Feature::value).collect(Collectors.toList());
//
//        System.out.println();

    }

}
