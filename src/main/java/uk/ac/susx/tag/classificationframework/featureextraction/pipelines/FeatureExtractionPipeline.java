package uk.ac.susx.tag.classificationframework.featureextraction.pipelines;

/*
 * #%L
 * FeatureExtractionPipeline.java - classificationframework - CASM Consulting - 2,013
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

import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import uk.ac.susx.tag.classificationframework.Util;
import uk.ac.susx.tag.classificationframework.datastructures.Document;
import uk.ac.susx.tag.classificationframework.datastructures.Instance;
import uk.ac.susx.tag.classificationframework.datastructures.ProcessedInstance;
import uk.ac.susx.tag.classificationframework.datastructures.StringIndexer;
import uk.ac.susx.tag.classificationframework.exceptions.CachingException;
import uk.ac.susx.tag.classificationframework.exceptions.FeatureExtractionException;
import uk.ac.susx.tag.classificationframework.featureextraction.documentprocessing.DocProcessor;
import uk.ac.susx.tag.classificationframework.featureextraction.documentprocessing.Service;
import uk.ac.susx.tag.classificationframework.featureextraction.filtering.TokenFilter;
import uk.ac.susx.tag.classificationframework.featureextraction.inference.FeatureInferrer;
import uk.ac.susx.tag.classificationframework.featureextraction.inference.FeatureInferrer.Feature;
import uk.ac.susx.tag.classificationframework.featureextraction.inference.featureselection.FeatureSelector;
import uk.ac.susx.tag.classificationframework.featureextraction.normalisation.TokenNormaliser;
import uk.ac.susx.tag.classificationframework.featureextraction.tokenisation.Tokeniser;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Class representing a pipeline which takes raw texts and produces a
 * collection of features ready to be trained on or classified, wrapped
 * in a ProcessedInstance instance.
 *
 * See "extractFeatures" method for details.
 *
 * A pipeline is created, then PipelineComponents can be added to it.
 * It is usually created with a tokeniser at minimum. The pipeline will
 * track an alphabet over the data, and index string features before
 * wrapping them in ProcessedInstances.
 *
 * Each component of the pipeline can be set on or offline using the
 * setOnline() method of the component. In order to modify components
 * after they've been added to the pipeline, either a reference
 * must be maintained externally, or when they are added to the
 * pipeline a String name should be provided, for later access. A
 * minimal example is shown below:
 *
 * FeatureExtractionPipeline pipeline = new FeatureExtractionPipeline(new TokeniserTwitterBasic(true));
 * pipeline.add(new TokenFilterRelevanceStopwords(), "stopwordRemover");
 * pipeline.add(new FeatureInferrerUnigrams());
 *
 * Notice that the stop-word removing component was added with the name "stopwordRemover". If it
 * was necessary to set the remover offline, it is possible to do so by the following:
 *
 * pipeline.getPipelineComponent("stopwordRemover").setOnline(false)
 *
 * Obviously, if any changes to the stopwordRemover were to be necessary which aren't
 * catered for in the "PipelineComponent" class, then you'd need to cast to TokenFilterRelevanceStopwords
 *
 * NOTE: By default, processDocument() and extractFeatures() will attempt to cache the Document instance in a
 *       MongoDB cache. The cache can be set up with the setCache() method. Caching can be avoided entirely using
 *       the processDocumentWithoutCache() and extractFeaturesWithoutCache() methods.
 *
 * User: Andrew D. Robertson
 * Date: 27/07/2013
 * Time: 12:30
 */
public class FeatureExtractionPipeline implements Serializable, AutoCloseable {

    private static final long serialVersionUID = 0L;

    private transient List<ProcessedInstance> handLabelledData    = new ArrayList<>();
    private transient List<ProcessedInstance> machineLabelledData = new ArrayList<>();

    // The following constitute the components of the pipeline
    private Tokeniser tokeniser = null;
    private List<DocProcessor>    docProcessors    = new ArrayList<>();
    private List<TokenFilter>     tokenFilters     = new ArrayList<>();
    private List<TokenNormaliser> tokenNormalisers = new ArrayList<>();
    private List<FeatureInferrer> featureInferrers = new ArrayList<>();

    // Map from component names to components, so that they can be accessed later
    private Map<String, PipelineComponent> componentMap = new HashMap<>();

    private transient DBCollection cache = null;       // Mongo DB collection for caching Document instances
    private transient boolean updateCache = true;      // True if pipeline can make additions to the cache
    private transient int configuration = 0;           // Hash of below.
    private transient String configurationString = ""; // Keep updated with updateCachingConfiguration(). Represents the configuration of the DocProcessors and Tokeniser, for caching purposes

    private transient ExecutorService threadPool = null;

    private final Pattern forNormalisingWhitespace = Pattern.compile("[\r\n\t]");
    private final Pattern forNormalisingZeroWidthCharacters = Pattern.compile("[\\ufeff\\u200b\\p{InVariation_Selectors}]");

    private StringIndexer labelIndexer = new StringIndexer();    // Indexes strings representing class labels
    private transient StringIndexer featureIndexer = new StringIndexer();  // Indexes strings representing features

    private boolean fixedVocabulary = false;

    /* Getters and Setters */
    public FeatureExtractionPipeline setTokeniser(Tokeniser tokeniser) { this.tokeniser = tokeniser; return this;}
    public Tokeniser getTokeniser() { return tokeniser; }

    public boolean getFixedVocabulary() {
        return fixedVocabulary;
    }
    public void setFixedVocabulary(boolean fixedVocabulary) {
        this.fixedVocabulary = fixedVocabulary;
    }

    /* Validation */
    public boolean tokeniserAssigned() { return tokeniser != null; }
    public boolean featureInferrersAssigned() { return featureInferrers.size() > 0; }

    // Use these to access the feature and label indexers
    public StringIndexer getLabelIndexer() { return labelIndexer; }
    public void setLabelIndexer(StringIndexer labelIndexer) { this.labelIndexer = labelIndexer;}
    public StringIndexer getFeatureIndexer() { return featureIndexer; }
    public void setFeatureIndexer(StringIndexer featureIndexer) { this.featureIndexer = featureIndexer;}

    // Get a reference to pipeline components which were named when added
    public PipelineComponent getPipelineComponent(String name) { return componentMap.get(name);}

    /**
     * For the hand labelled data, components will assume that the label on the Instance is correct,
     * for the machine labelled data, components will assume that the highest probability label on
     * the ProcessedInstance is correct.
     */
    public void setData(List<ProcessedInstance> handLabelledData, List<ProcessedInstance> machineLabelledData){
        this.handLabelledData = handLabelledData;
        this.machineLabelledData = machineLabelledData;
    }

    /**
     * Set all named components offline except *onlineComponentName*
     */
    public void setOnlyOnline(String onlineComponentName){
        for (Map.Entry<String, PipelineComponent> entry : componentMap.entrySet()){
            if (entry.getKey().equals(onlineComponentName))
                entry.getValue().setOnline();
            else entry.getValue().setOffline();
        }
    }

    public void setOnlyOnline(Set<String> onlineComponentNames){
        for (Map.Entry<String, PipelineComponent> entry : componentMap.entrySet()){
            if (onlineComponentNames.contains(entry.getKey()))
                entry.getValue().setOnline();
            else entry.getValue().setOffline();
        }
    }

    public void setOnlyPrecedingInferrersOnline(FeatureInferrer cutoff){
        boolean seenCutoff = true;
        for (FeatureInferrer inferrer : featureInferrers) {
            if (inferrer == cutoff)
                seenCutoff = false;
            inferrer.setOnline(seenCutoff);
        }
    }

    public void updateService(String oldUrl, String newUrl){
        for (DocProcessor d : docProcessors){
            if (d instanceof Service){
                Service s = (Service)d;
                if (s.getUrl().equals(oldUrl)){
                    s.setUrl(newUrl);
                }
            }
        }
    }

    public <C extends PipelineComponent> boolean hasComponent(Class<C> componentType, PipelineComponentFilter<C> componentFilter){
        return numComponents(componentType, componentFilter) > 0;
    }

    public <C extends PipelineComponent> int numComponents(Class<C> componentType, PipelineComponentFilter<C> componentFilter){
        if (DocProcessor.class.isAssignableFrom(componentType)){
            return (int)docProcessors.stream()
                    .filter(c -> componentType.isInstance(c))
                    .filter(c -> componentFilter.filter((C)c))
                    .count();
        } else if (TokenNormaliser.class.isAssignableFrom(componentType)){
            return (int)tokenNormalisers.stream()
                    .filter(c -> componentType.isInstance(c))
                    .filter(c -> componentFilter.filter((C)c))
                    .count();
        } else if (TokenFilter.class.isAssignableFrom(componentType)){
            return (int)tokenFilters.stream()
                    .filter(c -> componentType.isInstance(c))
                    .filter(c -> componentFilter.filter((C)c))
                    .count();
        } else if (FeatureInferrer.class.isAssignableFrom(componentType)){
            return (int)featureInferrers.stream()
                    .filter(c -> componentType.isInstance(c))
                    .filter(c -> componentFilter.filter((C)c))
                    .count();
        } return 0;
    }

    public void updateServices(PipelineComponentFilter<Service> serviceFilter, String newURL){
        docProcessors.stream()
                .filter(d -> d instanceof Service)
                .map(d -> (Service)d)
                .filter(serviceFilter::filter)
                .forEach(s -> s.setUrl(newURL));
    }

    public List<String> getServiceURLs(){
        return docProcessors.stream()
                .filter(d -> d instanceof Service)
                .map(d -> ((Service)d).getUrl())
                .collect(Collectors.toList());
    }

    public void setAllInferrersOnline(){
        featureInferrers.stream().forEach(PipelineComponent::setOnline);
    }

    /**
     * Some components' operation is dependent on learning their parameters from data. This usually means
     * that they must see data that has gone through the preceding pipeline components only. Often, this data
     * must also have been labelled. You can tell the pipeline about this data using the setData() method.
     *
     * This method will handle making sure only the preceding components are online before each data-driven
     * component gets it update. If you have a custom online/offline arrangement, this will be overridden
     * and won't be restored automatically to its original state. Sorry not sorry. //TODO
     *
     * Returns false if there was no data, or data-driven components to update with. True otherwise.
     */
    public boolean updateDataRequiringInferrers(int batchSize){
        boolean updated = false;
        // Only update if there is data
        if (!handLabelledData.isEmpty() || !machineLabelledData.isEmpty()) {
            for (FeatureInferrer i : featureInferrers) {
                // Only update if there are any data driven components.
                if (i instanceof DataDrivenComponent) {
                    updated = true;
                    setOnlyPrecedingInferrersOnline(i);
                    DataDrivenComponent c = (DataDrivenComponent) i;
                    c.update(batchSize > 1? getDataInBatches(batchSize) : getData());
                }
            }
            setAllInferrersOnline();
        }
        return updated;
    }

   /*
    * Constructors are protected.
    * This is to make clear that the proper method to create
    * a pipeline is to use the PipelineFactory. However, in a
    * pinch (usually in a agile experimental setup) you could
    * anonymously subclass this class and call the constructor
    * and add pipeline components.
    */
    protected FeatureExtractionPipeline() {

    }

    protected FeatureExtractionPipeline(Tokeniser tokeniser) {
        this.tokeniser = tokeniser;
    }
    /********************************************************/

    /* Conversions between feature/label indices and values */
    public String featureString(int featureIndex) { return featureIndexer.getValue(featureIndex); }
    public String featureString(int featureIndex, String indexNotPresentValue) { return featureIndexer.getValue(featureIndex, indexNotPresentValue); }
    public int featureIndex(String featureString) { return featureIndexer.getIndex(featureString); }

    public String labelString(int labelIndex) { return labelIndexer.getValue(labelIndex); }
    public int labelIndex(String labelString) { return labelIndexer.getIndex(labelString); }
    /********************************************************/

    public static interface PipelineChanges {
        void apply(FeatureExtractionPipeline pipeline);
        void undo(FeatureExtractionPipeline pipeline);
    }

    public static interface PipelineProcessing<R> {
        R apply(FeatureExtractionPipeline pipeline);
    }

    public void applyChanges(PipelineChanges changes){
        changes.apply(this);
    }

    public <R> R surroundProcessingWithChanges(PipelineChanges changes, PipelineProcessing<R> processing){
        changes.apply(this);
        R results = processing.apply(this);
        changes.undo(this);
        return results;
    }

/**********************************************************************************************************************
 * Full pipeline execution methods for batch extraction
 **********************************************************************************************************************/

    /**
     * Divide the data into batches.
     * Do each batch one at a time.
     * For each batch, do each stage of processing one at a time for all documents (e.g. all the tokenisation first)
     * But do the processing for each stage in parallel where the component allows.
     * Then collect together the results.
     */
    public List<ProcessedInstance> extractFeaturesInBatches(List<Instance> instances, int batchSize){
        return Lists.partition(instances, batchSize).stream()   // Divide instances into batches
                .map(batch -> extractFeaturesFromBatch(batch))  // Extract features concurrently where possible within each batch
                .flatMap(batch -> batch.stream())               // Flatten out each batch to be collected into single list
                .collect(Collectors.toList());
    }

    /**
     * Reprocess ProcessedInstances in batches, each reprocessed ProcessedInstance will inherit the probabilistic labels
     * found on the original PROCESSED INSTANCE.
     */
    public List<ProcessedInstance> reprocessInBatchesWithProcessedLabels(List<ProcessedInstance> instances, int batchSize){
        return Lists.partition(instances, batchSize).stream()
                .map(batch -> reprocessBatchWithProcessedLabels(batch))
                .flatMap(batch -> batch.stream())
                .collect(Collectors.toList());
    }

    /**
     * Reprocess ProcessedInstances in batches, each reprocessed ProcessedInstance will inherit the hand labelled
     * non-probabilistic labels from the original SOURCE INSTANCE.
     */
    public List<ProcessedInstance> reprocessInBatchesWithSourceLabels(List<ProcessedInstance> instances, int batchSize){
        return Lists.partition(instances, batchSize).stream()
                .map(batch -> reprocessBatchWithSourceLabels(batch))
                .flatMap(batch -> batch.stream())
                .collect(Collectors.toList());
    }

    /**
     * Per-stage concurrent processing for a single batch of instances.
     */
    public List<ProcessedInstance> extractFeaturesFromBatch(List<Instance> instances) {
        ExecutorService pool = getThreadPool();

        // Tokenise batch concurrently
        List<Document> documents = tokeniseDocumentBatch(instances, pool);

        // Perform document processing concurrently where possible
        documents = processDocumentBatch(documents, pool);

        // Apply filters concurrently where possible
        applyFiltersToBatch(documents, pool);

        // Apply normalisers concurrently where possible
        applyNormalisersToBatch(documents, pool);

        // Extract features concurrently where possible
        List<List<Feature>> featuresPerDocument = extractInferredFeaturesFromBatch(documents, pool);

        // Build ProcessedDocuments by indexing features and labels
        List<ProcessedInstance> out = new ArrayList<>();
        for (int i = 0; i < featuresPerDocument.size(); i++){
            Document doc = documents.get(i);
            int label = doc.source.label.trim().isEmpty()? -1 : labelIndexer.getIndex(doc.source.label);
            out.add(new ProcessedInstance(label, indexFeatures(featuresPerDocument.get(i)), doc.source));
        }
        return out;
    }

    /**
     * Reprocess a batch of ProcessedInstance, keeping the label probabilities assigned to the original ProcessedInstances.
     */
    public List<ProcessedInstance> reprocessBatchWithProcessedLabels(List<ProcessedInstance> instances){
        List<Instance> sourceInstances = instances.stream().map(i -> i.source).collect(Collectors.toList());

        List<ProcessedInstance> reprocessedInstances = extractFeaturesFromBatch(sourceInstances);

        for (int i = 0; i < instances.size(); i++){
            ProcessedInstance newOne = reprocessedInstances.get(i);
            ProcessedInstance oldOne = instances.get(i);
            newOne.setLabeling(oldOne.getLabelProbabilities());
        }

        return reprocessedInstances;
    }

    /**
     * Reprocess a batch of ProcessedInstance, keeping the non-probabilistic labels assigned to the original source Instance.
     */
    public List<ProcessedInstance> reprocessBatchWithSourceLabels(List<ProcessedInstance> instances){
        return extractFeaturesFromBatch(instances.stream().map(i -> i.source).collect(Collectors.toList()));
    }

    public Stream<List<Feature>> extractUnindexedFeaturesInBatchesToStream(List<Instance> instances, int batchSize){
        return Lists.partition(instances, batchSize).stream()
                .map(batch -> extractUnindexedFeaturesFromBatch(batch))
                .flatMap(batch -> batch.stream());
    }

    public Iterator<List<Feature>> extractUnindexedFeaturesInBatchesToIterator(Iterator<Instance> instances, int batchSize){
        final Iterator<List<Instance>> batches = Iterators.partition(instances, batchSize);

        return new Iterator<List<Feature>>() {
            Iterator<List<Feature>> currentBatch = extractUnindexedFeaturesFromBatch(batches.next()).iterator();

            @Override
            public boolean hasNext() {
                if (currentBatch.hasNext()){
                    return true;
                } else {
                    if (batches.hasNext()){
                        currentBatch = extractUnindexedFeaturesFromBatch(batches.next()).iterator();
                        return currentBatch.hasNext();
                    } else {
                        return false;
                    }
                }
            }

            @Override
            public List<Feature> next() {
                if (hasNext()){
                    return currentBatch.next();
                } else throw new NoSuchElementException();
            }
        };
    }

    public List<List<Feature>> extractUnindexedFeaturesFromBatch(List<Instance> instances){
        ExecutorService pool = getThreadPool();

        // Tokenise batch concurrently
        List<Document> documents = tokeniseDocumentBatch(instances, pool);

        // Perform document processing concurrently where possible
        documents = processDocumentBatch(documents, pool);

        // Apply filters concurrently where possible
        applyFiltersToBatch(documents, pool);

        // Apply normalisers concurrently where possible
        applyNormalisersToBatch(documents, pool);

        // Extract features concurrently where possible
        return extractInferredFeaturesFromBatch(documents, pool);
    }


    private List<Document> tokeniseDocumentBatch(List<Instance> instances, ExecutorService threadPool) {
        List<Future<Document>> futures = new ArrayList<>();
        // Submit tokenisation tasks
        for (Instance i : instances) {
            futures.add(threadPool.submit(() -> {
                i.text = forNormalisingWhitespace.matcher(i.text).replaceAll(" ");
                i.text = forNormalisingZeroWidthCharacters.matcher(i.text).replaceAll("");
                return tokeniser.tokenise(i);
            }));
        }
        // Iterate through futures, blocking until each is done, producing a list of tokenised documents in the original order
        return futures.stream().map(f -> {
            try {
                return f.get();
            } catch (InterruptedException | ExecutionException e) { throw new FeatureExtractionException(e);}
        }).collect(Collectors.toList());
    }

    private List<Document> processDocumentBatch(List<Document> documents, ExecutorService threadPool) {
        List<Future<Document>> futures;
        for (DocProcessor dp : docProcessors){
            if (dp.isOnline()) {
                try { // If component wants to do its own batch processing, let it
                    documents = dp.processBatch(documents);
                } catch (UnsupportedOperationException e) { // Otherwise handle it here
                    // If safe, do processing concurrently
                    if (dp.isThreadSafe()) {
                        futures = new ArrayList<>();
                        // Submit a process task for each document
                        for (Document d : documents) {
                            futures.add(threadPool.submit(() -> {
                                return dp.process(d);
                            }));
                        }
                        // Wait for each task in original order
                        for (int i = 0; i < futures.size(); i++) {
                            try {
                                documents.set(i, futures.get(i).get());
                            } catch (InterruptedException | ExecutionException taskEx) { throw new FeatureExtractionException(taskEx); }
                        }
                    } else { // Otherwise if unsafe just process serially
                        for (int i = 0; i < documents.size(); i++) {
                            documents.set(i , dp.process(documents.get(i)));
                        }
                    }
                }
            }
        }
        return documents;
    }

    private void applyFiltersToBatch(List<Document> documents, ExecutorService threadPool) {
        List<Future> futures;
        for (TokenFilter f : tokenFilters) {
            if (f.isOnline()) {
                try { // If component wants to do its own batch processing, let it
                    f.filterBatch(documents);
                } catch (UnsupportedOperationException e) { // Otherwise handle it here
                    // If safe, do processing concurrently
                    if (f.isThreadSafe()) {
                        futures = new ArrayList<>();
                        // Submit a filter task for each document
                        for (Document d : documents) {
                            futures.add(threadPool.submit((Runnable) () -> {
                                for (int i=0; i<d.size(); i++) {
                                    if (f.filter(i, d)) {
                                        d.get(i).setFiltered(true);
                                    }
                                }
                            }));
                        }
                        // Wait for each task in original order
                        futures.forEach(future -> {
                            try {
                                future.get();
                            } catch (InterruptedException | ExecutionException taskEx) { throw new FeatureExtractionException(taskEx); }
                        });
                    } else { // Otherwise if unsafe just process serially
                        for (Document d : documents){
                            for (int i=0; i<d.size(); i++) {
                                if (f.filter(i, d)) {
                                    d.get(i).setFiltered(true);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void applyNormalisersToBatch(List<Document> documents, ExecutorService threadPool){
        List<Future> futures;
        for (TokenNormaliser n : tokenNormalisers) {
            if (n.isOnline()) {
                try { // If component wants to do its own batch processing, let it
                    n.normaliseBatch(documents);
                } catch (UnsupportedOperationException e) {
                    if (n.isThreadSafe()) {
                        futures = new ArrayList<>();
                        for (Document d : documents) {
                            futures.add(threadPool.submit((Runnable) ()-> {
                                for (int i = 0; i < d.size(); i++) {
                                    n.normalise(i, d);
                                }
                            }));
                        }
                        // Wait for each task in original order
                        futures.forEach(future -> {
                            try {
                                future.get();
                            } catch (InterruptedException | ExecutionException taskEx) { throw new FeatureExtractionException(taskEx); }
                        });
                    } else { // Otherwise if unsafe just process serially
                        for (Document d : documents){
                            for (int i = 0; i < d.size(); i++) {
                                n.normalise(i, d);
                            }
                        }
                    }
                }
            }
        }
    }

    private List<List<Feature>> extractInferredFeaturesFromBatch(List<Document> documents, ExecutorService threadPool){
        List<List<Feature>> featuresPerDocument = new ArrayList<>();
        for (int i = 0; i < documents.size(); i++)
            featuresPerDocument.add(new ArrayList<>());
        List<Future<List<Feature>>> futures;
        for (FeatureInferrer fi : featureInferrers) {
            if (fi.isOnline()) {
                try {
                    featuresPerDocument = fi.addInferredFeaturesFromBatch(documents, featuresPerDocument);
                } catch (UnsupportedOperationException e) {
                    if (fi.isThreadSafe()){
                        futures = new ArrayList<>();
                        for (int i = 0; i < documents.size(); i++) {
                            final int finalI = i;
                            final List<Feature> features = featuresPerDocument.get(i);
                            futures.add(threadPool.submit(() ->
                                    fi.addInferredFeatures( documents.get(finalI), features)));
                        }
                        // Wait for each task in original order
                        for (int i = 0; i < futures.size(); i++) {
                            try {
                                featuresPerDocument.set(i, futures.get(i).get());
                            } catch (InterruptedException | ExecutionException taskEx) { throw new FeatureExtractionException(taskEx); }
                        }
                    } else { // Otherwise if unsafe just process serially
                        for (int i = 0; i < documents.size(); i++) {
                            featuresPerDocument.set(i, fi.addInferredFeatures(documents.get(i), featuresPerDocument.get(i)));
                        }
                    }
                }
            }
        }
        return featuresPerDocument;
    }


/**********************************************************************************************************************
 * Full pipeline execution methods for non-batch extraction
 **********************************************************************************************************************/

    /**
     * This is the main important method of the class. Given a String representing a tweet:
     *
     *  1. Normalise whitespace (\n\t\r) to spaces.
     *  2. Tokenise the tweet.
     *  3. Run document processors.
     *  4. Apply filters.
     *  5. Apply normalisers.
     *  6. Use FeatureInferrers to extract features from the tokens. (May include feature selection)
     *  7. Return all extracted features as part of a ProcessedInstance object.
     *
     *  Within each category of feature extraction or document processing, the processors are applied in the order
     *  in which they were added to the pipeline, i.e. each FeatureInferrer is applied in the order they were added,
     *  but even if a TokenFilter was added afterward, it would still be applied before all FeatureInferrers.
     *  FeatureInferrers have access to all the features that have been inferred by previous inferrers in the pipeline.
     *
     *  Only those processors, extractors, filters, etc. whose "online" property is set to "true" will be used. This
     *  allows for modification of a pipeline without having to re-construct one.
     *
     *  The intermediate Document instance (the annotated tokens before features are extracted) is cached in a
     *  MongoDB collection. See processDocument(). Use setCache() to assign a cache. If no cache is
     *  assigned, then functionality is identical to "extractFeaturesWithoutCache()".
     */
    public ProcessedInstance extractFeatures(Instance i) {
        return extractFeatures(processDocument(i));
    }

    /**
     * You could use this if you want the functionality of extractFeatures(), but also want to keep track of the
     * mapping from each feature (including type info) to the set of documents in which it was found. Could be expensive...
     * Might be best just find such sets as and when you need them using Util.getOriginalContextDocuments(), though
     * that's only possible if you don't mind only working with the feature string (no type info).
     */
    public ProcessedInstance extractFeatures(Instance i, Map<Feature, Set<ProcessedInstance>> feature2DocumentIndex){
        Document doc = processDocument(i);
        applyFilters(doc);
        applyNormalisers(doc);
        List<Feature> features = extractInferredFeatures(doc);

        int label = doc.source.label.trim().isEmpty()? -1 : labelIndexer.getIndex(doc.source.label);
        ProcessedInstance processed = new ProcessedInstance(label, indexFeatures(features), doc.source);

        for (Feature feature : features) {
            if (!feature2DocumentIndex.containsKey(feature)) {
                feature2DocumentIndex.put(feature, new HashSet<ProcessedInstance>());
            }
            feature2DocumentIndex.get(feature).add(processed);
        }
        return processed;
    }

    /**
     * Given an iterable over instances, return an iterable over the feature-extracted resulting ProcessedInstances
     * (lazily evaluated).
     */
    public Iterable<ProcessedInstance> extractedFeatures(Iterable<Instance> docs){
        final Iterator<Instance> instanceIterator = docs.iterator();
        return new Iterable<ProcessedInstance>() {
            public Iterator<ProcessedInstance> iterator() {
                return new Iterator <ProcessedInstance>() {
                    public boolean hasNext() { return instanceIterator.hasNext(); }

                    public ProcessedInstance next() {
                        if (instanceIterator.hasNext()){
                            return extractFeatures(instanceIterator.next());
                        } else throw new NoSuchElementException();
                    }
                    public void remove() { throw new UnsupportedOperationException(); }
                };
            }
        };
    }

    /**
     * Same functionality as "extractFeatures" except that the intermediate Document instance is not cached.
     */
    public ProcessedInstance extractFeaturesWithoutCache(Instance i) {
        return extractFeatures(processDocumentWithoutCache(i));
    }

    /**
     * Extract features from a document, assuming tokenisation and document processing has already be done.
     */
    public ProcessedInstance extractFeatures(Document doc) {
        applyFilters(doc);
        applyNormalisers(doc);
        int label = doc.source.label.trim().isEmpty()? -1 : labelIndexer.getIndex(doc.source.label);
        return new ProcessedInstance(label, indexFeatures(extractInferredFeatures(doc)), doc.source);
    }

    /**
     * Extract features, without indexing them into a ProcessedInstance.
     * NOTE: this is only public because it might be useful to know feature types, information which is lost after
     *       indexing in a ProcessedInstance. See Util.
     */
    public List<Feature> extractUnindexedFeatures(Instance i){
        Document doc = processDocument(i);
        applyFilters(doc);
        applyNormalisers(doc);
        return extractInferredFeatures(doc);
    }

    /**
     * Given features produced by extractUnindexedFeatures, index them into an int array appropriate for a
     * ProcessedInstance.
     */
    public int[] indexFeatures(List<Feature> features) {
        int[] indices = new int[features.size()];
        for (int i = 0; i < features.size(); i++) {
            indices[i] = featureIndexer.getIndex(features.get(i).value(), !fixedVocabulary);
        }
        return indices;
    }


    /**
     * Every feature selector may have a set of features that it ALWAYS lets through. This is useful
     * for ensuring that your feature selector will always let through any features that you have
     * given pseudo-counts to.
     *
     * The issue is, that a pipeline may have several such selectors, and the set of features may change
     * often, ie. more often that you would like to re-build pipelines.
     *
     * These methods allow you to quickly manipulate these feature sets for all feature selectors in this
     * pipeline.
     *
     * If you just wish to add new features, use the add method. You can also remove particular features, or
     * replace the set entirely (even with an empty set).
     */
    public void setAdditionalFeaturesForFeatureSelection(Set<String> additionalFeatures){
        featureInferrers.stream()
                .filter(featureInferrer -> featureInferrer instanceof FeatureSelector)
                .forEach(featureInferrer -> ((FeatureSelector) featureInferrer).setAdditionalFeatures(additionalFeatures));
    }
    public void removeAdditionalFeaturesForFeatureSelection(Set<String> featuresToBeRemoved){
        featureInferrers.stream()
                .filter(featureInferrer -> featureInferrer instanceof FeatureSelector)
                .forEach(featureInferrer -> ((FeatureSelector) featureInferrer).removeAdditionalFeatures(featuresToBeRemoved));
    }
    public void addAdditionalFeaturesForFeatureSelection(Set<String> supplementaryFeatures){
        featureInferrers.stream()
                .filter(featureInferrer -> featureInferrer instanceof FeatureSelector)
                .forEach(featureInferrer -> ((FeatureSelector) featureInferrer).addAdditionalFeatures(supplementaryFeatures));
    }

    public void clearAdditionalFeaturesForFeatureSelection(){
        featureInferrers.stream()
                .filter(featureInferrer -> featureInferrer instanceof FeatureSelector)
                .forEach(featureInferrer -> ((FeatureSelector) featureInferrer).clearAdditionalFeatures());
    }

/**********************************************************************************************************************
 * Document processing: the stage before feature extraction and filtering: tokenisation and annotation
 **********************************************************************************************************************/

    /**
     * Only process a document, do not extract features. Document instance is cached in
     * a MongoDB collection. If no cache has been set, functionality is identical to
     * processDocumentWithoutCache(). Use setCache() to assign a cache. If you want to
     * use a cache, but not update it, then pass false to setUpdateCache()
     *
     * A Document is some text that has been tokenised and fully annotated with any
     * NLP tools, ready for features to be extracted from it.
     */
    public Document processDocument(Instance i) {
        DBObject cached;

        if (cache != null) {
            cached = cache.findOne(new BasicDBObject("pipelineConfig", configuration).append("instanceID", i.id));
        } else {
            return processDocumentWithoutCache(i);
        }

        try {
            if (cached!=null) {
                return byteArray2Document((byte[]) cached.get("cached"));
            } else {
                Document processed = processDocumentWithoutCache(i);
                if (updateCache) {
                    BasicDBObject newCached = new BasicDBObject();
                    newCached.put("pipelineConfig", configuration);
                    newCached.put("instanceID", i.id);
                    newCached.put("cached", document2ByteArray(processed));
                    cache.insert(newCached);
                }
                return processed;
            }
        } catch (ClassNotFoundException | IOException e) { throw new FeatureExtractionException(e); }
    }

    /**
     * Same functionality as processDocument(), except that the Document instance is not cached.
     */
    public Document processDocumentWithoutCache(Instance document) {
        document.text = forNormalisingWhitespace.matcher(document.text).replaceAll(" ");
        document.text = forNormalisingZeroWidthCharacters.matcher(document.text).replaceAll("");
        Document processedDoc = tokeniser.tokenise(document);
        for (DocProcessor docProcessor : docProcessors){
            if (docProcessor.isOnline()) processedDoc = docProcessor.process(processedDoc);
        }
        return processedDoc;
    }

    public Document processDocumentCachedOnly(Instance i){
        try {
            DBObject cached = cache.findOne(new BasicDBObject("_id", i.id));
            return byteArray2Document((byte[]) cached.get("doc"));
        } catch (IOException | ClassNotFoundException | NullPointerException e) { throw new FeatureExtractionException(e); }
    }

/**********************************************************************************************************************
 * Add/remove components to/from the pipeline
 **********************************************************************************************************************/

    // Use these methods for adding the various types of token processing and feature extraction to the pipeline.
    // If a name is specified, then the particular component can be accessed using "getPipelineComponent" (see class documentation above)
    public FeatureExtractionPipeline add(DocProcessor d) { docProcessors.add(d); return this;}
    public FeatureExtractionPipeline add(DocProcessor d, String name){
        componentMap.put(name, d);
        return add(d);
    }

    public FeatureExtractionPipeline add(TokenFilter f)  { tokenFilters.add(f); return this;}
    public FeatureExtractionPipeline add(TokenFilter f, String name) {
        componentMap.put(name, f);
        return add(f);
    }

    public FeatureExtractionPipeline add(TokenNormaliser n){ tokenNormalisers.add(n); return this;}
    public FeatureExtractionPipeline add(TokenNormaliser n, String name){
        componentMap.put(name, n);
        return add(n);
    }

    public FeatureExtractionPipeline add(FeatureInferrer c){ featureInferrers.add(c); return this;}
    public FeatureExtractionPipeline add(FeatureInferrer c, String name){
        componentMap.put(name, c);
        return add(c);
    }

    public boolean removeComponent(String name){
        if (componentMap.containsKey(name)){
            PipelineComponent p = componentMap.get(name);
            componentMap.remove(name);
            if (p instanceof DocProcessor){
                return docProcessors.removeIf(d -> d == p);
            } else if (p instanceof TokenNormaliser){
                return tokenNormalisers.removeIf(n -> n == p);
            } else if (p instanceof TokenFilter){
                return tokenFilters.removeIf(f -> f == p);
            } else if (p instanceof FeatureInferrer){
                return featureInferrers.removeIf(i -> i == p);
            } else {
                throw new FeatureExtractionException("Pipeline had a component in the component map but didn't match any existing components");
            }
        } else {
            return false;
        }
    }

    public <C extends PipelineComponent> boolean removeComponents(Class<C> componentType){
        return removeComponents(componentType, component -> true, false); // Deletes all components with matching class
    }

    public <C extends PipelineComponent> boolean removeComponentsDuplicatesOnly(Class<C> componentType,PipelineComponentFilter<C> componentFilter ){
        return removeComponents(componentType, componentFilter, true);
    }

    public <C extends PipelineComponent> boolean removeComponents(Class<C> componentType, PipelineComponentFilter<C> componentFilter, boolean duplicatesOnly){
        Set<PipelineComponent> toBeUnMapped = new HashSet<>();
        boolean seen = false;
        if (DocProcessor.class.isAssignableFrom(componentType)){
            for (Iterator<DocProcessor> iterator = docProcessors.iterator(); iterator.hasNext(); ) {
                DocProcessor c = iterator.next();
                if (componentType.isInstance(c) && componentFilter.filter((C)c)){
                    if (seen || !duplicatesOnly) {
                        toBeUnMapped.add(c);
                        iterator.remove();
                    }
                    seen = true;
                }
            }
        } else if (TokenNormaliser.class.isAssignableFrom(componentType)){
            for (Iterator<TokenNormaliser> iterator = tokenNormalisers.iterator(); iterator.hasNext(); ) {
                TokenNormaliser c = iterator.next();
                if (componentType.isInstance(c) && componentFilter.filter((C)c)){
                    if (seen || !duplicatesOnly) {
                        toBeUnMapped.add(c);
                        iterator.remove();
                    }
                    seen = true;
                }
            }
        } else if (TokenFilter.class.isAssignableFrom(componentType)){
            for (Iterator<TokenFilter> iterator = tokenFilters.iterator(); iterator.hasNext(); ) {
                TokenFilter c = iterator.next();
                if (componentType.isInstance(c) && componentFilter.filter((C)c)){
                    if (seen || !duplicatesOnly) {
                        toBeUnMapped.add(c);
                        iterator.remove();
                    }
                    seen = true;
                }
            }
        } else if (FeatureInferrer.class.isAssignableFrom(componentType)){
            for (Iterator<FeatureInferrer> iterator = featureInferrers.iterator(); iterator.hasNext(); ) {
                FeatureInferrer c = iterator.next();
                if (componentType.isInstance(c) && componentFilter.filter((C)c)){
                    if (seen || !duplicatesOnly) {
                        toBeUnMapped.add(c);
                        iterator.remove();
                    }
                    seen = true;
                }
            }
        }
        removeComponentsFromNameMapping(toBeUnMapped);
        return seen;
    }

    private void removeComponentsFromNameMapping(Set<PipelineComponent> toBeUnMapped){
        for (Iterator<Map.Entry<String, PipelineComponent>> iterator = componentMap.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<String, PipelineComponent> entry = iterator.next();
            if (toBeUnMapped.contains(entry.getValue())){
                iterator.remove();
            }
        }
    }


//    public FeatureExtractionPipeline add(FeatureSelector f, Iterable<Instance> documents){
//        featureInferrers.add(setupFeatureSelector(f, documents, this));
//        return this;
//    }
//    public FeatureExtractionPipeline add(FeatureSelector f, Iterable<Instance> documents, String name){
//        componentMap.put(name, f);
//        return add(f, documents);
//    }
//
//    public static FeatureSelector setupFeatureSelector(FeatureSelector f, Iterable<Instance> documents, FeatureExtractionPipeline pipeline){
//        f.update(documents, pipeline);
//        return f;
//    }

    /**
     * Remove all feature inferrers. Handy for keeping all the processing, filtering, abd
     * normalisation the same (not re-loading any models), but changing the way you
     * do feature inference.
     */
    public void removeAllFeatureInferrers() {
        featureInferrers = new ArrayList<>();
    }

    @Override
    public void close() throws Exception {
        shutdownThreadPool();
        docProcessors.forEach(PipelineComponent::close);
        featureInferrers.forEach(PipelineComponent::close);
        tokenNormalisers.forEach(PipelineComponent::close);
        tokenFilters.forEach(PipelineComponent::close);
    }

    private ExecutorService getThreadPool() {
        if (threadPool == null) {
            threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() - 1);
        }
        return threadPool;
    }

    private void shutdownThreadPool() {
        if (threadPool != null && !threadPool.isShutdown()) {
            threadPool.shutdown();
        }
    }

/**********************************************************************************************************************
 * Data backed component functionality
 **********************************************************************************************************************/

    public static class Data {

        public Iterable<Datum> handLabelled;
        public Iterable<Datum> machineLabelled;

        public Data(Iterable<Datum> handLabelled, Iterable<Datum> machineLabelled) {
            this.handLabelled = handLabelled;
            this.machineLabelled = machineLabelled;
        }

        public Iterable<Datum> allData(){ return Iterables.concat(handLabelled, machineLabelled); }
    }

    public static class Datum {

        public boolean handLabelled;
        public List<Feature> features;
        public String label;
        public HashMap<String, Double> labelProbabilities;

        private Datum(boolean handLabelled){
            this.handLabelled = handLabelled;
        }
        public boolean isHandLabelled(){ return handLabelled; }

        public static Datum createHandLabelled(ProcessedInstance oldProcessedInstance, FeatureExtractionPipeline pipeline){
            Datum d =  new Datum(true);

            d.features = pipeline.extractUnindexedFeatures(oldProcessedInstance.source);

            d.label = oldProcessedInstance.source.label;

            d.labelProbabilities = new HashMap<>();
            d.labelProbabilities.put(d.label, 1.0);

            return d;
        }

        public static Datum createMachineLabelled(ProcessedInstance oldProcessedInstance, FeatureExtractionPipeline pipeline){
            Datum d = new Datum(false);

            d.features = pipeline.extractUnindexedFeatures(oldProcessedInstance.source);

            d.label = pipeline.labelString(oldProcessedInstance.getLabel());

            d.labelProbabilities = new HashMap<>();
            for (Int2DoubleMap.Entry entry : oldProcessedInstance.getLabelProbabilities().int2DoubleEntrySet()){
                d.labelProbabilities.put(pipeline.labelString(entry.getIntKey()), entry.getDoubleValue());
            }

            return d;
        }
    }

    public Data getData(){
        return getData(handLabelledData, machineLabelledData, this);
    }

    public static Data getData(Collection<ProcessedInstance> handLabelledData, Collection<ProcessedInstance> machineLabelledData, FeatureExtractionPipeline pipeline) {
        Iterable<Datum> handLabelledIterable = () -> new Iterator<Datum>() {
            Iterator<ProcessedInstance> data = handLabelledData.iterator();
            public boolean hasNext() {
                return data.hasNext();
            }

            public Datum next() {
                if (hasNext()) {
                    ProcessedInstance original = data.next();
                    return Datum.createHandLabelled(original, pipeline);
                } throw new NoSuchElementException();
            }
        };
        Iterable<Datum> machineLabelledIterable = () -> new Iterator<Datum>(){
            Iterator<ProcessedInstance> data = machineLabelledData.iterator();
            public boolean hasNext() {
                return data.hasNext();
            }

            public Datum next() {
                if (hasNext()){
                    ProcessedInstance original = data.next();
                    return Datum.createMachineLabelled(original, pipeline);
                } throw new NoSuchElementException();
            }
        };
        return new Data(handLabelledIterable, machineLabelledIterable);
    }

    public Data getDataInBatches(int batchSize){
        return getDataInBatches(handLabelledData, machineLabelledData, this, batchSize);
    }

    public static Data getDataInBatches(List<ProcessedInstance> handLabelledData, List<ProcessedInstance> machineLabelledData, FeatureExtractionPipeline pipeline, int batchSize){
        return new Data(handLabelledInBatches(handLabelledData, pipeline, batchSize),
                        machineLabelledInBatches(machineLabelledData, pipeline, batchSize));
    }

    public static Iterable<Datum> handLabelledInBatches(List<ProcessedInstance> handLabelledData, FeatureExtractionPipeline pipeline, int batchSize){
        if (handLabelledData.isEmpty())
            return Lists.newArrayList();
        return () -> new Iterator<Datum>() {
            Iterator<List<ProcessedInstance>> batches = Util.iteratorOverBatches(handLabelledData, batchSize);
            Iterator<Datum> currentData = new ArrayList<Datum>().iterator();

            @Override
            public boolean hasNext() { return currentData.hasNext() || batches.hasNext(); }

            @Override
            public Datum next() {
                if (hasNext()) {
                    if (!currentData.hasNext()){
                        List<Datum> newData = new ArrayList<>();
                        List<ProcessedInstance> batch = batches.next();
                        List<List<Feature>> featuresPerDocument = pipeline.extractUnindexedFeaturesFromBatch(
                                batch.stream().map(p -> p.source).collect(Collectors.toList())
                        );

                        for (int i = 0; i < batch.size(); i++) {
                            Datum d = new Datum(true);
                            d.label = batch.get(i).source.label;
                            d.labelProbabilities = new HashMap<>();
                            d.labelProbabilities.put(d.label, 1.0);
                            d.features = featuresPerDocument.get(i);
                            newData.add(d);
                        }
                        currentData = newData.iterator();
                    }
                    return currentData.next();
                } else throw new NoSuchElementException();
            }
        };
    }

    public static Iterable<Datum> machineLabelledInBatches(List<ProcessedInstance> machineLabelledData, FeatureExtractionPipeline pipeline, int batchSize){
        if (machineLabelledData.isEmpty())
            return Lists.newArrayList();
        return () -> new Iterator<Datum>() {
            Iterator<List<ProcessedInstance>> batches = Util.iteratorOverBatches(machineLabelledData, batchSize);
            Iterator<Datum> currentData = new ArrayList<Datum>().iterator();

            @Override
            public boolean hasNext() { return currentData.hasNext() || batches.hasNext(); }

            @Override
            public Datum next() {
                if (hasNext()) {
                    if (!currentData.hasNext()){
                        List<Datum> newData = new ArrayList<>();
                        List<ProcessedInstance> batch = batches.next();
                        List<List<Feature>> featuresPerDocument = pipeline.extractUnindexedFeaturesFromBatch(
                                batch.stream().map(p -> p.source).collect(Collectors.toList()));

                        for (int i = 0; i < batch.size(); i++) {
                            Datum d = new Datum(false);
                            d.label = pipeline.labelString(batch.get(i).getLabel());;
                            d.labelProbabilities = new HashMap<>();
                            for (Int2DoubleMap.Entry entry : batch.get(i).getLabelProbabilities().int2DoubleEntrySet()){
                                d.labelProbabilities.put(pipeline.labelString(entry.getIntKey()), entry.getDoubleValue());
                            }
                            d.features = featuresPerDocument.get(i);
                            newData.add(d);
                        }
                        currentData = newData.iterator();
                    }
                    return currentData.next();
                } else throw new NoSuchElementException();
            }
        };
    }

/**********************************************************************************************************************
 * Caching functionality
 **********************************************************************************************************************/

    /**
     * Assign a MongoDB collection to this pipeline to be used as a cache for storing
     * Document instances. This stops tweets from having to be processed more
     * than once.
     *
     * WARNING: see updateCachingConfiguration()
     *
     * @param allowUpdates if false, then the pipeline will not add any more to the cache
     */
    public void setCache(DBCollection collection, boolean allowUpdates) {
        configurationString = docProcessingConfiguration();
        configuration = configurationString.hashCode();
        updateCache = allowUpdates;
        cache = collection;
    }

    public void setCache(DBCollection collection){
        setCache(collection, true);
    }

    /**
     * WARNING:
     * If you add a cache to this pipeline, and subsequently change any of its DocProcessors, or add/remove
     * any DocProcessors, then you must all this update method, otherwise caching will not be performed correctly,
     * and you'll get in a big mess later.
     */
    public void updateCachingConfiguration() {
        configurationString = docProcessingConfiguration();
        configuration = configurationString.hashCode();
    }

    public int getCacheConfiguration() {
        return configuration;
    }

    public String getCacheConfigurationString() {
        return configurationString;
    }

    /**
     * If a MongoDB is being used as a cache, then setting this to false will stop the cache being updated.
     */
    public void setUpdateCache(boolean updateCache) {
        this.updateCache = updateCache;
    }

    /**
     * It may be the case that you have 1 or more Instances that have been processed
     * and their Document instance cached, but for some reason, probably a fault, you
     * wish to forget about the cached version, and overwrite it with a new cached
     * version. Use this method for that.
     */
    public void reCache(Instance i) {
        if (cache == null) throw new CachingException("No cache set.");

        BasicDBObject newCached = new BasicDBObject();
        newCached.put("pipelineConfig", configuration);
        newCached.put("instanceID", i.id);
        try {
            newCached.put("cached", document2ByteArray(processDocumentWithoutCache(i)));
        } catch (IOException e) { throw new FeatureExtractionException(e); }

        cache.update(new BasicDBObject("pipelineConfig", configuration).append("instanceID", i.id),
                newCached, true , false); // Upsert is true, so insert is done if it didn't already exist
    }

    /**
     * Produce an int, representing the configuration of
     * the tokeniser and DocProcessors. These account for
     * any processing an instance goes under before features
     * are extracted. In this way, two pipelines can be compared
     * in order to see if they perform the same processing of
     * documents (used for validating pipeline configurations when
     * using setCache()).
     */
    private String docProcessingConfiguration(){
        StringBuilder sb = new StringBuilder();
        sb.append("Tokeniser:");
        sb.append(tokeniser.getClass().getName());
        sb.append(":");
        sb.append(tokeniser.configuration());
        for (DocProcessor d : docProcessors){
            sb.append(":DocProcessor:");
            sb.append(d.getClass().getName());
            sb.append(":");
            sb.append(d.configuration());
        }
        return sb.toString();
    }


/********************************************************************************************************************
 * Run pipeline components in serial
 ********************************************************************************************************************/

    /**
     * Apply filters to tokens. Tokens which are filtered do not appear in the list of unigram features.
     * This is accomplished by setting the "filtered" property of the AnnotatedTokens. In this way, subsequent
     * feature extraction objects can choose to ignore or respect this property using the "isFiltered()" function
     * on the AnnotatedToken.
     */
    private void applyFilters(Document document) {
        for (int i=0; i<document.size(); i++) {
            for (TokenFilter tokenFilter : tokenFilters){
                if (tokenFilter.isOnline()) {
                    if (tokenFilter.filter(i, document)) {
                        document.get(i).setFiltered(true);
                        break;
                    }
                }
            }
        }
    }

    /**
     * Apply normalisations to tokens. Well-behaved normalisers should only modify the token of which they are
     * given the index. However, a reference to the whole document is provided to them so that context-dependent
     * normalisation can occur. Normalisations generally involve modifying the "form" feature of an AnnotatedToken
     * (e.g. token.put("form", "URL")). These normalisations will then be present in the unigram features.
     */
    private void applyNormalisers(Document document) {
        for (int i = 0; i < document.size(); i++) {
            for (TokenNormaliser tokenNormaliser : tokenNormalisers) {
                if (tokenNormaliser.isOnline())
                    if (!tokenNormaliser.normalise(i, document)) break;
            }
        }
    }

    /**
     * Attain features by running each feature inferrer. FeatureInferrers directly produce features from Documents.
     * They have a choice whether or not to ignore the "filtered" property of an AnnotatedToken.
     */
    private List<Feature> extractInferredFeatures(Document document){
        List<Feature> features = new ArrayList<>();
        for (FeatureInferrer featureInferrer : featureInferrers) {
            if(featureInferrer.isOnline()) features = featureInferrer.addInferredFeatures(document, features);
        }
        return features;
    }


/**********************************************************************************************************************
 * Serialisation helpers
 **********************************************************************************************************************/

    /**
     * Convert a Document instance into a byte array (used for storing
     * documents in a Mongo database).
     */
    public static byte[] document2ByteArray(Document d) throws IOException {
        try(ByteArrayOutputStream b = new ByteArrayOutputStream();
            ObjectOutputStream o = new ObjectOutputStream(b)) {
            o.writeObject(d);
            return b.toByteArray();
        }
    }

    /**
     * Convert a byte array into a Document instance (used for retrieving
     * documents from a Mongo database).
     */
    public static Document byteArray2Document(byte[] bytes) throws IOException, ClassNotFoundException {
        try (ObjectInputStream o = new ObjectInputStream(new ByteArrayInputStream(bytes))){
            return (Document)o.readObject();
        }
    }

/**********************************************************************************************************************
 * Pipeline maintenance
 **********************************************************************************************************************/


    private void writeObject(ObjectOutputStream out) throws IOException, ClassNotFoundException {
        out.defaultWriteObject();

        if(fixedVocabulary) {
            out.writeObject(featureIndexer);
        }
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();

//        labelIndexer = new StringIndexer();
        if(fixedVocabulary) {
            featureIndexer = (StringIndexer)in.readObject();
        } else {
            featureIndexer = new StringIndexer();
        }

        handLabelledData = new ArrayList<>();
        machineLabelledData = new ArrayList<>();
    }
}