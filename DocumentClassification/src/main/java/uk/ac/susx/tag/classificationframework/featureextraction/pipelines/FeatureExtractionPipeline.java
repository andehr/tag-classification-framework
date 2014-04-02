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

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import uk.ac.susx.tag.classificationframework.datastructures.Document;
import uk.ac.susx.tag.classificationframework.datastructures.Instance;
import uk.ac.susx.tag.classificationframework.datastructures.ProcessedInstance;
import uk.ac.susx.tag.classificationframework.datastructures.StringIndexer;
import uk.ac.susx.tag.classificationframework.exceptions.CachingException;
import uk.ac.susx.tag.classificationframework.exceptions.FeatureExtractionException;
import uk.ac.susx.tag.classificationframework.featureextraction.documentprocessing.DocProcessor;
import uk.ac.susx.tag.classificationframework.featureextraction.filtering.TokenFilter;
import uk.ac.susx.tag.classificationframework.featureextraction.inference.FeatureInferrer;
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.regex.Pattern;

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
public class FeatureExtractionPipeline implements Serializable {

    private static final long serialVersionUID = 0L;

    private Tokeniser tokeniser = null;
    private List<DocProcessor>    docProcessors    = new ArrayList<>();
    private List<TokenFilter>     tokenFilters     = new ArrayList<>();
    private List<TokenNormaliser> tokenNormalisers = new ArrayList<>();
    private List<FeatureInferrer> featureInferrers = new ArrayList<>();

    private Map<String, PipelineComponent> componentMap = new HashMap<>(); // Map from component names to components, so that they can be accessed later

    private transient DBCollection cache = null;       // Mongo DB collection for caching Document instances
    private transient boolean updateCache = true;      // True if pipeline can make additions to the cache
    private transient int configuration = 0;           // Hash of below.
    private transient String configurationString = ""; // Keep updated with updateCachingConfiguration(). Represents the configuration of the DocProcessors and Tokeniser, for caching purposes

    private final Pattern forNormalisingWhitespace = Pattern.compile("[\r\n\t]");
    private final Pattern forNormalisingZeroWidthWhitespace = Pattern.compile("[\\ufeff\\u200b]");

    private StringIndexer labelIndexer = new StringIndexer();    // Indexes strings representing class labels
    private StringIndexer featureIndexer = new StringIndexer();  // Indexes strings representing features

    /* Getters and Setters */
    public FeatureExtractionPipeline setTokeniser(Tokeniser tokeniser) { this.tokeniser = tokeniser; return this;}


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
    public int featureIndex(String featureString) { return featureIndexer.getIndex(featureString); }

    public String labelString(int labelIndex) { return labelIndexer.getValue(labelIndex); }
    public int labelIndex(String labelString) { return labelIndexer.getIndex(labelString); }
    /********************************************************/

    /**
     * This is the main important method of the class. Given a String representing a tweet:
     *
     *  1. Normalise whitespace (\n\t\r) to spaces.
     *  2. Tokenise the tweet.
     *  3. Run document processors.
     *  4. Apply filters.
     *  5. Apply normalisers.
     *  6. Use FeatureInferrers to extract features from the tokens.
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
     * NOTE: this is only public because it might be useful to know feature types,
     *       information which is lost after indexing in a ProcessedInstance.
     *       see Util.
     */
    public List<FeatureInferrer.Feature> extractUnindexedFeatures(Instance i){
        Document doc = processDocument(i);
        applyFilters(doc);
        applyNormalisers(doc);
        return extractInferredFeatures(doc);
    }

    private int[] indexFeatures(List<FeatureInferrer.Feature> features) {
        int[] indices = new int[features.size()];
        for (int i = 0; i < features.size(); i++) {
            indices[i] = featureIndexer.getIndex(features.get(i).value());
        }
        return indices;
    }

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
        document.text = forNormalisingZeroWidthWhitespace.matcher(document.text).replaceAll("");
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

    public FeatureExtractionPipeline add(FeatureSelector f, Iterable<Instance> documents){
        featureInferrers.add(setupFeatureSelector(f, documents, this));
        return this;
    }
    public FeatureExtractionPipeline add(FeatureSelector f, Iterable<Instance> documents, String name){
        componentMap.put(name, f);
        return add(f, documents);
    }

    public static FeatureSelector setupFeatureSelector(FeatureSelector f, Iterable<Instance> documents, FeatureExtractionPipeline pipeline){
        FeatureSelector.Evidence e = new FeatureSelector.Evidence();
        for (Instance document : documents)
            e.addEvidence(document.label, pipeline.extractUnindexedFeatures(document), f.getFeatureTypes());

        f.setTopFeatures(e);
        return f;
    }


    /**
     * Remove all feature inferrers. Handy for keeping all the processing, filtering, abd
     * normalisation the same (not re-loading any models), but changing the way you
     * do feature inference.
     */
    public void removeAllFeatureInferrers() {
        featureInferrers = new ArrayList<>();
    }

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
    private List<FeatureInferrer.Feature> extractInferredFeatures(Document document){
        List<FeatureInferrer.Feature> features = new ArrayList<>();
        for (FeatureInferrer featureInferrer : featureInferrers) {
            if(featureInferrer.isOnline()) features = featureInferrer.addInferredFeatures(document, features);
        }
        return features;
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
}