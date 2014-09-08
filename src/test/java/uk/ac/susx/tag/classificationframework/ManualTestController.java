package uk.ac.susx.tag.classificationframework;

/*
 * #%L
 * TestController.java - classificationframework - CASM Consulting - 2,013
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

import cmu.arktweetnlp.Tagger;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import uk.ac.susx.tag.classificationframework.classifiers.*;
import uk.ac.susx.tag.classificationframework.datastructures.Instance;
import uk.ac.susx.tag.classificationframework.datastructures.LogicalCollection;
import uk.ac.susx.tag.classificationframework.datastructures.ProcessedInstance;
import uk.ac.susx.tag.classificationframework.exceptions.FeatureExtractionException;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.FeatureExtractionPipeline;
import uk.ac.susx.tag.classificationframework.jsonhandling.JsonListStreamReader;
import uk.ac.susx.tag.classificationframework.classifiers.OVRClassifier;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.*;

/**
 * Created with IntelliJ IDEA.
 * User: Andrew D. Robertson
 * Date: 08/08/2013
 * Time: 10:18
 */
public class ManualTestController {


    public static void main(String[] args) throws FeatureExtractionException, IOException, ClassNotFoundException, ExecutionException, InterruptedException {

        String[] trainingArr = {
                //"/Users/thomas/DevSandbox/EpicDataShelf/tag-lab/europeanunion_data/labelled_training/demos-en-europeanunion-2-en-relevance1.model.converted",
                //"/Users/thomas/DevSandbox/EpicDataShelf/tag-lab/snowden_data/labelled_training/training.json",
                ////"/Users/thomas/DevSandbox/EpicDataShelf/tag-lab/bullying_data/training.json",
                ////"/Users/thomas/DevSandbox/EpicDataShelf/tag-lab/cleggproanti_data/training.json",
                ////"/Users/thomas/DevSandbox/EpicDataShelf/tag-lab/faggot_data/training.json",
                ////"/Users/thomas/DevSandbox/EpicDataShelf/tag-lab/insultcollection_data/training.json",
                "/Users/thomas/DevSandbox/EpicDataShelf/tag-lab/scotdecides_data/training.json",
                "/Users/thomas/DevSandbox/EpicDataShelf/tag-lab/scotdecides1_data/training.json",
                "/Users/thomas/DevSandbox/EpicDataShelf/tag-lab/seriousridiculous_data/training.json",
                ////"/Users/thomas/DevSandbox/EpicDataShelf/tag-lab/sluthoe_data/training.json",
                ////"/Users/thomas/DevSandbox/EpicDataShelf/tag-lab/traceymorgan_data/training.json"
        };
        String[] unlabelledArr = {
                //"/Users/thomas/DevSandbox/EpicDataShelf/tag-lab/europeanunion_data/unlabelled_training/tweets-en-europeanunion-2-en.converted",
                //"/Users/thomas/DevSandbox/EpicDataShelf/tag-lab/snowden_data/unlabelled_training/snowden-unlabelled.json",
                ////"/Users/thomas/DevSandbox/EpicDataShelf/tag-lab/bullying_data/insultscollectionbullyingnotbullying-unlabelled.json",
                ////"/Users/thomas/DevSandbox/EpicDataShelf/tag-lab/cleggproanti_data/lbc-debate-personality-unlabelled.json",
                ////"/Users/thomas/DevSandbox/EpicDataShelf/tag-lab/faggot_data/faggot-unlabelled.json",
                ////"/Users/thomas/DevSandbox/EpicDataShelf/tag-lab/insultcollection_data/insultcollection-unlabelled.json",
                "/Users/thomas/DevSandbox/EpicDataShelf/tag-lab/scotdecides_data/scotdecides-personality-politics-unlabelled.json",
                "/Users/thomas/DevSandbox/EpicDataShelf/tag-lab/scotdecides1_data/scotdecides-salmond-unlabelled.json",
                "/Users/thomas/DevSandbox/EpicDataShelf/tag-lab/seriousridiculous_data/serious-ridiculous-unlabelled.json",
                ////"/Users/thomas/DevSandbox/EpicDataShelf/tag-lab/sluthoe_data/sluthoe-unlabelled.json",
                ////"/Users/thomas/DevSandbox/EpicDataShelf/tag-lab/traceymorgan_data/traceymorgan-unlabelled.json"
        };
        String[] goldStandardArr = {
                //"/Users/thomas/DevSandbox/EpicDataShelf/tag-lab/europeanunion_data/gold_standard/tweets-en-europeanunion-2-en-gs.converted",
                //"/Users/thomas/DevSandbox/EpicDataShelf/tag-lab/snowden_data/gold_standard/snowden-gold-standard.json",
                ////"/Users/thomas/DevSandbox/EpicDataShelf/tag-lab/bullying_data/insultscollectionbullyingnotbullying-gold-standard.json",
                ////"/Users/thomas/DevSandbox/EpicDataShelf/tag-lab/cleggproanti_data/lbc-debate-personality-gold-standard.json",
                ////"/Users/thomas/DevSandbox/EpicDataShelf/tag-lab/faggot_data/faggot-gold-standard.json",
                ////"/Users/thomas/DevSandbox/EpicDataShelf/tag-lab/insultcollection_data/insultcollection-gold-standard.json",
                "/Users/thomas/DevSandbox/EpicDataShelf/tag-lab/scotdecides_data/scotdecides-personality-politics-gold-standard.json",
                "/Users/thomas/DevSandbox/EpicDataShelf/tag-lab/scotdecides1_data/scotdecides-salmond-gold-standard.json",
                "/Users/thomas/DevSandbox/EpicDataShelf/tag-lab/seriousridiculous_data/serious-ridiculous-gold-standard.json",
                ////"/Users/thomas/DevSandbox/EpicDataShelf/tag-lab/sluthoe_data/sluthoe-gold-standard.json",
                ////"/Users/thomas/DevSandbox/EpicDataShelf/tag-lab/traceymorgan_data/traceymorgan-gold-standard.json"
        };
        String[] names = {
                //"europeanunion",
                //"snowden",
                ////"bullying",
                ////"cleggproanti", // 3 labels
                ////"faggot",
                ////"insultcollection",
                "scotdecides", // 3 labels
                "scotdecides1", // 3 labels
                "seriousridiculous", // 3 labels
                ////"sluthoe",
                ////"traceymorgan" // 3labels
        };

        flamCheltuk(trainingArr, goldStandardArr, unlabelledArr, names);

// System.out.println("Max heapsize (MB): " + Runtime.getRuntime().maxMemory()/1024/1024);

//        fractionTest();
//        originalContextsTest();

//        taggerTest();

//        CacheManager cm = new CacheManager("localhost", 27017);
//        cm.deleteCacheManagerMetaData();
//        cm.deleteDatabase("test");

//        demonstration();
//        mainTest();
    }

    public static void flamCheltuk(String[] trainingArr, String[] goldStandardArr, String[] unlabelledArr, String[] names) throws IOException
    {
        for (int i = 0; i < names.length; i++) {

            System.out.println("########## DATASET: " + names[i]);

            Gson gson = uk.ac.susx.tag.classificationframework.Util.getGson();

            FeatureExtractionPipeline pipeline = uk.ac.susx.tag.classificationframework.Util.buildBasicPipeline(true, false); // Exciting new pipeline builder

            JsonListStreamReader trainingStream = new JsonListStreamReader(new File(trainingArr[i]), gson);
            JsonListStreamReader unlabelledStream = new JsonListStreamReader(new File(unlabelledArr[i]), gson);
            JsonListStreamReader goldStandardStream = new JsonListStreamReader(new File(goldStandardArr[i]), gson);

            System.out.println("Loading training data...");
            List<ProcessedInstance> trainingData = Lists.newLinkedList(trainingStream.iterableOverProcessedInstances(pipeline));

            IntSet labels = new IntOpenHashSet();
            for (int l : pipeline.getLabelIndexer().getIndices()) {
                labels.add(l);
            }

            System.out.println("Doing bad things to the unlabelled data...");
            List<ProcessedInstance> unlabelledData = Lists.newLinkedList(unlabelledStream.iterableOverProcessedInstances(pipeline));

            /*
            System.out.print("=== NB OVR ===: ");
            OVRLearningScheme<NaiveBayesClassifier> ovrNB = new OVRLearningScheme<>(labels, NaiveBayesClassifier.class);
            ovrNB.train(trainingData);
            goldStandardStream = new JsonListStreamReader(new File(goldStandardArr[i]), gson);
            ovrNB.predict(goldStandardStream.iterableOverProcessedInstances(pipeline));

            System.out.print("=== NB FM OVR ===: ");
            OVRLearningScheme<NaiveBayesClassifierFeatureMarginals> ovrFM = new OVRLearningScheme<>(labels, NaiveBayesClassifierFeatureMarginals.class);
            ovrFM.train(trainingData, unlabelledData);
            goldStandardStream = new JsonListStreamReader(new File(goldStandardArr[i]), gson);
            ovrFM.predict(goldStandardStream.iterableOverProcessedInstances(pipeline));

            System.out.print("=== NB SFE OVR ===: ");
            OVRLearningScheme<NaiveBayesClassifierSFE> ovrSFE = new OVRLearningScheme<>(labels, NaiveBayesClassifierSFE.class);
            ovrSFE.train(trainingData, unlabelledData);
            goldStandardStream = new JsonListStreamReader(new File(goldStandardArr[i]), gson);
            ovrSFE.predict(goldStandardStream.iterableOverProcessedInstances(pipeline));
*/
            //System.out.println("Doing nasty things with the Gold Standard...");
            //List<ProcessedInstance> goldStandardData = Lists.newLinkedList(goldStandardStream.iterableOverProcessedInstances(pipeline));

            goldStandardStream = new JsonListStreamReader(new File(goldStandardArr[i]), gson);
            System.out.println("=== NB FM OVR ===");
            OVRClassifier<NaiveBayesClassifierFeatureMarginals> ovrFM = new OVRClassifier<>(labels, NaiveBayesClassifierFeatureMarginals.class);
            ovrFM.train(trainingData, unlabelledData);
            System.out.println(new Evaluation(ovrFM, pipeline, goldStandardStream.iterableOverProcessedInstances(pipeline)));
            System.out.println("====================");

            goldStandardStream = new JsonListStreamReader(new File(goldStandardArr[i]), gson);
            System.out.println("=== NB OVR ===");
            OVRClassifier<NaiveBayesClassifier> ovrNB = new OVRClassifier<>(labels, NaiveBayesClassifier.class);
            ovrNB.train(trainingData);
            System.out.println(new Evaluation(ovrNB, pipeline, goldStandardStream.iterableOverProcessedInstances(pipeline)));
            System.out.println("====================");

            /*
            goldStandardStream = new JsonListStreamReader(new File(goldStandardArr[i]), gson);
            System.out.println("=== NB SFE OVR ===");
            OVRLearningScheme<NaiveBayesClassifierSFE> ovrSFE = new OVRLearningScheme<>(labels, NaiveBayesClassifierSFE.class);
            ovrSFE.train(trainingData, unlabelledData);
            System.out.println(new Evaluation(ovrSFE, pipeline, goldStandardStream.iterableOverProcessedInstances(pipeline)));
            System.out.println("====================");
            */

            // NaiveBayesSFE
            NaiveBayesClassifierSFE nbSfe = new NaiveBayesClassifierSFE();
            // TODO: This is still buggy
            nbSfe.train(trainingData, unlabelledData);

            // Evaluate classifier with SFE method
            goldStandardStream = new JsonListStreamReader(new File(goldStandardArr[i]), gson);
            System.out.println("===== EVAL NB-SFE ==");
            System.out.println(new Evaluation(nbSfe, pipeline, goldStandardStream.iterableOverProcessedInstances(pipeline)));
            System.out.println("====================");

            // NaiveBayesFeatureMarginals
            NaiveBayesClassifierFeatureMarginals nbFm = new NaiveBayesClassifierFeatureMarginals();
            nbFm.train(trainingData, unlabelledData);
            //nbFm.train(trainingData);
            //nbFm.calculateFeatureMarginals(unlabelledData, trainingData);

            // Evaluate classifier with FM method
            goldStandardStream = new JsonListStreamReader(new File(goldStandardArr[i]), gson);
            System.out.println("==== EVAL NB-FM ====");
            System.out.println(new Evaluation(nbFm, pipeline, goldStandardStream.iterableOverProcessedInstances(pipeline)));
            System.out.println("====================");

            // Do some training & evaluating and see what happens
            NaiveBayesClassifier nb = new NaiveBayesClassifier();
            nb.train(trainingData);

            goldStandardStream = new JsonListStreamReader(new File(goldStandardArr[i]), gson);
            System.out.println("==== EVAL NB =======");
            System.out.println(new Evaluation(nb, pipeline, goldStandardStream.iterableOverProcessedInstances(pipeline)));
            System.out.println("====================");

            System.out.println("#########################################################");
        }
    }

    public static boolean checkEqual(List<Tagger.TaggedToken> sentence1, List<Tagger.TaggedToken> sentence2){
        if (sentence1.size() != sentence2.size())
            return false;
        else {
            for (int i = 0; i < sentence1.size(); i++){
                Tagger.TaggedToken token1 = sentence1.get(i);
                Tagger.TaggedToken token2 = sentence2.get(i);
                if (!token1.tag.equals(token2.tag))
                    return false;
                if (!token1.token.equals(token2.token))
                    return false;
            }
            return true;
        }
    }

    public static class Result implements Comparable<Result>{

        public int id;
        public List<Tagger.TaggedToken> sentence;
        public List<Tagger.TaggedToken> serialSentence;
        public String text;

        public Result(int id, String text){
            this.id = id;
            this.text = text;
            sentence = null;
            serialSentence = null;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Result result = (Result) o;

            if (id != result.id) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return id;
        }

        @Override
        public int compareTo(Result o) {
            if (o==null) throw new NullPointerException();
            return id - o.id;
        }
    }

    public static void taggerTest() throws IOException, ExecutionException, InterruptedException {
        File unlabelledTraining = new File("/Volumes/LocalDataHD/data/sentiment_analysis/unlabelled/tweets-en-europeanunion-2-en.converted");
        JsonListStreamReader unlabelledStream = new JsonListStreamReader(unlabelledTraining, new Gson());
        int id = 0;
        List<Result> sentences = new ArrayList<>();

        final Tagger tagger = new Tagger();
        tagger.loadModel("/cmu/arktweetnlp/model.20120919");

        for (Instance i : unlabelledStream.iterableOverInstances()){
            sentences.add(new Result(id, i.text));
            id++;
        }
        for (Result r : sentences){
            r.serialSentence = tagger.tokenizeAndTag(r.text);
        }
        taggerParallelTest(sentences, tagger);

        for(Result r: sentences){
            if (!checkEqual(r.sentence, r.serialSentence)){
                System.out.println("Not equal!");
            }
        }


        System.out.println("Done.");
    }

    public static void taggerParallelTest(Iterable<Result> sentences, final Tagger tagger) throws IOException, InterruptedException, ExecutionException {
//        ExecutorService pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        ExecutorService pool = Executors.newFixedThreadPool(20);

        System.out.println("Submitting tasks...");
        for (final Result r : sentences){
            pool.execute(new Runnable() {
                @Override
                public void run() {
                    r.sentence = tagger.tokenizeAndTag(r.text);
                }
            });
        }
        pool.shutdown();
        try {
            pool.awaitTermination(20, TimeUnit.DAYS);
        } catch (InterruptedException e) { throw new RuntimeException(e);}
    }

    public static void fractionTest() throws IOException {
        File data = new File("/Volumes/LocalDataHD/data/sentiment_analysis/unlabelled/tweets-en-europeanunion-2-en.converted");
        JsonListStreamReader dataReader = new JsonListStreamReader(data, new Gson());

        System.out.println("Loading pipeline...");
        FeatureExtractionPipeline pipeline = Util.buildCMUPipeline(false, false);

        System.out.println("Processing...");
        List<ProcessedInstance> trainingData = Lists.newLinkedList(dataReader.iterableOverProcessedInstances(pipeline));

        System.out.println("Processed...");

        System.out.println(trainingData.size());

        Set<String> query;
        boolean quit = false;
        Scanner scanIn = new Scanner(System.in);

        while(!quit) {
            System.out.print("Enter query here : ");

            query = Sets.newHashSet(scanIn.nextLine().split(","));

            if (query.size()==1 && query.contains("q"))
                quit=true;
            else {
                for (Map.Entry<String, Double> fraction : Util.documentOccurrenceFractions(query, trainingData, pipeline).entrySet()) {
                    System.out.println(fraction.getKey() + ": " + fraction.getValue());
                }
            }
        }
        scanIn.close();
    }

    public static void originalContextsTest() throws IOException {
        File data = new File("/Volumes/LocalDataHD/data/sentiment_analysis/unlabelled/tweets-en-europeanunion-2-en.converted");
        JsonListStreamReader dataReader = new JsonListStreamReader(data, new Gson());

        FeatureExtractionPipeline pipeline = Util.buildCMUPipeline(false, false);

        List<ProcessedInstance> trainingData = Lists.newLinkedList(dataReader.iterableOverProcessedInstances(pipeline));

        System.out.println(trainingData.size());

        String feature;
        boolean quit = false;
        Scanner scanIn = new Scanner(System.in);

        System.out.print("Enter feature here : ");
        while (!quit){
            feature = scanIn.nextLine();
            if (feature.equals("q")) quit=true;
            else{
                for (String originalContext : Util.getOriginalContextStrings(feature, trainingData, pipeline)){
                    System.out.println(" " + originalContext);
                }
                System.out.println(Util.occurrenceFraction(feature, trainingData, pipeline));
            }

        }
        scanIn.close();

    }


    /**
     * Example training and evaluation cycle.
     *
     * By default uses a CMU oriented pipeline. This may be fairly slow on large data.
     * Could use a PipelineFactory.createBasicPipeline() instead. It's less sophisticated though.
     *
     * You could also use the new caching functionality if it's too slow. So that you only have
     * to process it once. Take a look at the CacheManager class in datastructures. You'll need
     * to have access to a MongoDB instance. The CacheManager is used to assign a cache to a
     * pipeline, and will manage the MongoDB side for you.
     *
     * The EM is done DUALIST style (as opposed to our bootstrapping style, which hasn't been
     * severely evaluated yet, and bugs have been reported when calling it from Method51, but
     * I BET that's not MY fault :P )
     */
    public static void demonstration() throws IOException {

        // Data locations
        File labelledTraining = new File("/Volumes/LocalDataHD/data/sentiment_analysis/training/demos-en-europeanunion-2-en-relevance1.model.converted");
        File unlabelledTraining = new File("/Volumes/LocalDataHD/data/sentiment_analysis/unlabelled/tweets-en-europeanunion-2-en.converted");
        File goldStandard = new File("/Volumes/LocalDataHD/data/sentiment_analysis/gold_standard/tweets-en-europeanunion-2-en-gs.converted");

        // Alt. data locations
//        File labelledTraining = Util.convertJson(new File("/Volumes/LocalDataHD/data/sentiment_analysis/training/demos-en-europeanunion-2-en-sentiment1.model"));
//        File unlabelledTraining = Util.convertJson(new File("/Volumes/LocalDataHD/data/sentiment_analysis/unlabelled/tweets-en-europeanunion-2-en-relevance1-attitude1"));
//        File goldStandard = Util.convertJson(new File("/Volumes/LocalDataHD/data/sentiment_analysis/gold_standard/tweets-en-europeanunion-2-en-relevance1-attitude1-gs"));

        Gson gson = Util.getGson();

        // Json streams
        JsonListStreamReader trainingStream = new JsonListStreamReader(labelledTraining, gson);
        JsonListStreamReader unlabelledStream = new JsonListStreamReader(unlabelledTraining, gson);
        JsonListStreamReader goldStandardStream = new JsonListStreamReader(goldStandard, gson);

        // Create a suitable pipeline
//        FeatureExtractionPipeline pipeline = PipelineFactory.createCMUPipeline(true, false); // Example pipeline
//        FeatureExtractionPipeline pipeline = PipelineFactory.createBasicPipeline(true, false);
        FeatureExtractionPipeline pipeline = Util.buildBasicPipeline(true, false); // Exciting new pipeline builder
//        FeatureExtractionPipeline pipeline = Util.buildCMUPipeline(true, false);

        // Set up optional cache
//        CacheManager cm = new CacheManager("localhost", 27017);
//        cm.assignCache("test", "test", pipeline);

        // Read in training data, processing each document through the pipeline
        List<ProcessedInstance> trainingData = Lists.newLinkedList(trainingStream.iterableOverProcessedInstances(pipeline));

        // Train on labelled data
        NaiveBayesClassifier nb = new NaiveBayesClassifier();
        nb.train(trainingData);

        // Read in unlabelled data, processing each document through the pipeline
        List<ProcessedInstance> unlabelledData = Lists.newLinkedList(unlabelledStream.iterableOverProcessedInstances(pipeline));

        // Do EM on unlabelled data, in the DUALIST style
        NaiveBayesClassifier emptyNB = Util.initNBWithLabels(pipeline, "Relevant", "Irrelevant");
//        NaiveBayesClassifier emptyNB = Util.initNBWithLabels(pipeline, "Positive", "Negative", "Neutral");

        // - Making a precomputed NBC will allow faster labelling of a large amount of unlabelled data
        NaiveBayesClassifierPreComputed emptyNBP = new NaiveBayesClassifierPreComputed(emptyNB);

        // - 1 step of EM
        nb.emTrain(unlabelledData, emptyNBP);

        // Evaluate classifier with new method
        System.out.println(new Evaluation(nb, pipeline, goldStandardStream.iterableOverProcessedInstances(pipeline)));

        trainingStream.close();
        unlabelledStream.close();
        goldStandardStream.close();
    }














    public static void mainTest() throws IOException, ClassNotFoundException {

        System.out.println("Initialising pipeline...");

        // Create a feature extraction pipeline
        FeatureExtractionPipeline pipeline = Util.buildCMUPipeline(true, false);
//        FeatureExtractionPipeline pipeline = PipelineFactory.createCMUPipelineWithParsing(true, true);
//        FeatureExtractionPipeline pipeline = Util.getBasicPipeline(true);
//        MongoClient mongoDB = getMongoDBTest();
//        pipeline.setCache(mongoDB.getDB("documentCaching"), "withParsing", true);


        // Data setup
        System.out.println("Converting improper JSON...");
        File training = Util.convertJson(new File("/Volumes/LocalDataHD/data/sentiment_analysis/training/demos-en-europeanunion-2-en-relevance1.model"));
        File unlabelled = Util.convertJson(new File("/Volumes/LocalDataHD/data/sentiment_analysis/unlabelled/tweets-en-europeanunion-2-en"));
        File goldStandard = Util.convertJson(new File("/Volumes/LocalDataHD/data/sentiment_analysis/gold_standard/tweets-en-europeanunion-2-en-gs"));

        Gson gson = Util.getGson();

        JsonListStreamReader trainingStream = new JsonListStreamReader(training, gson);
        JsonListStreamReader unlabelledStream = new JsonListStreamReader(unlabelled, gson);
        JsonListStreamReader goldStandardStream = new JsonListStreamReader(goldStandard, gson);


        System.out.println("Loading training data...");
        List<ProcessedInstance> trainingData = Lists.newLinkedList(trainingStream.iterableOverProcessedInstances(pipeline));

        // Train classifier
        System.out.println("-- Training on labelled data...");
        NaiveBayesClassifier nb = new NaiveBayesClassifier();
        nb.train(trainingData);


        System.out.println("Loading unlabelled data...");
        long startTime = System.currentTimeMillis();

        List<ProcessedInstance> unlabelledData = Lists.newLinkedList(unlabelledStream.iterableOverProcessedInstances(pipeline));

        long endTime = System.currentTimeMillis();

//        System.out.println("- That took " + TimeUnit.MILLISECONDS.toMinutes(endTime - startTime) + " minutes.");
//        System.out.println("- That took " + TimeUnit.MILLISECONDS.toSeconds(endTime - startTime) + " seconds.");

        System.out.println("-- Training on unlabelled data...");
        NaiveBayesClassifier emptyNB = Util.initNBWithLabels(pipeline, "Relevant", "Irrelevant");
        NaiveBayesClassifierPreComputed emptyNBP = new NaiveBayesClassifierPreComputed(emptyNB);
        nb.emTrain(unlabelledData, emptyNBP);
        NaiveBayesClassifierPreComputed preCompNB = new NaiveBayesClassifierPreComputed(nb);

        System.out.println("Querying features...");
        System.out.println("-- Classifying unlabelled...");
        Util.classifyInstances(unlabelledData, preCompNB);
        LogicalCollection<ProcessedInstance> allData = new LogicalCollection<>(trainingData).add(unlabelledData);
        System.out.println("-- Sorting and correlating features...");
        Int2ObjectOpenHashMap<Int2DoubleOpenHashMap> labelledFeatures = new Int2ObjectOpenHashMap<>();
//        labelledFeatures.put(0,new Int2DoubleOpenHashMap());
//        labelledFeatures.get(0).put(0, 50);
        Map<String, List<String>> correlatedFeatures = Querying.labelledFeatures2Strings(Querying.queryFeatures(allData, labelledFeatures, 100, 0.75, null, null), pipeline);
//        Map<String, Set<String>> correlatedFeatures = Querying.labelledFeatures2Strings(Querying.commonFeatures(allData, null, 100), pipeline);




        System.out.println(correlatedFeatures.get("Relevant"));
        System.out.println(correlatedFeatures.get("Irrelevant"));

//        System.out.println("Querying Instances...");
//        Querying.queryInstances(unlabelledData);

//        for (int i=0; i<25;i++){
//            System.out.println(unlabelledData.get(i).source.text);
//        }

//        System.out.println("Saving model...");
//        nb.setLabelMultiplier(0, 0.7);
//        ModelState m = new ModelState(nb, ModelState.getSourceInstanceList(trainingData), pipeline);
//        m.save(new File("savetest"));
////
//        System.out.println("Loading model...");
//        ModelState n = ModelState.load(new File("savetest"));
////
//        nb = n.classifier;
//        pipeline = n.pipeline;

        // Test classifier
        System.out.println("Evaluating on gold standard...");
//        System.out.println(Util.evaluate(nb, pipeline, goldStandardStream.iterableOverInstances()));

        trainingStream.close();
        unlabelledStream.close();
        goldStandardStream.close();
//        mongoDB.close();


        System.out.println("Done.");
    }
}