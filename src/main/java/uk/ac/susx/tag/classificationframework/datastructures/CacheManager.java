package uk.ac.susx.tag.classificationframework.datastructures;

/*
 * #%L
 * CacheManager.java - classificationframework - CASM Consulting - 2,013
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
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import org.apache.commons.math.fraction.BigFraction;
import uk.ac.susx.tag.classificationframework.exceptions.CachingException;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.FeatureExtractionPipeline;

import java.net.UnknownHostException;
import java.util.List;
import java.util.Set;

/**
 * Caching procedure:
 *
 *  CacheManager cm = new CacheManager(hostname, port);
 *
 *  FeatureExtractionPipeline pipeline = PipelineFactory.createCMUPipeline(true , true);
 *
 *  cm.assignCache(databaseName, collectionName, pipeline);
 *
 *  // Then later...
 *
 *  cm.close();
 *
 * Note the following:
 *
 *  - If you change the configuration of any of the pipeline's DocProcessors, or add or remove
 *    new ones, then you should call pipeline.updateCachingConfiguration().
 *
 *  - If you want the pipeline to use the cache but NOT update it, then call pipeline.setUpdateCache(false)
 *
 *  - You can use the cache manager with a try with resources
 *
 *  - There's a convenience static method for using a pipeline to cache a bunch of instances
 *
 * User: Andrew D. Robertson
 * Date: 21/01/2014
 * Time: 17:32
 */
public class CacheManager implements AutoCloseable {

    private MongoClient client;

    public CacheManager(MongoClient client) {
        this.client = client;
        ensureHashingIndexExists();
    }

    public CacheManager(String hostname, int port) throws UnknownHostException {
        this(new MongoClient(new ServerAddress(hostname, port)));
    }

    public void close() {
        client.close();
    }

    /**
     * Use this method to assign a cache to a pipeline. The benefit of this method is that it
     * creates a database called "cacheManagerMetadata" with a collection called "hashingIndex"
     * which tracks the mapping from pipeline configuration hash to its string version.
     *
     * @param databaseName Database you want to do caching in
     * @param collectionName Collection you want to do caching in
     * @param pipeline Pipeline to which you wish to assign a cache
     * @param allowUpdates Allow the pipeline to cache new instances if it has to perform new processing
     * @param overwriteCollidingHashes True if when manager discovers colliding hashes, you want it to overwrite the old string value
     */
    public void assignCache(String databaseName, String collectionName, FeatureExtractionPipeline pipeline, boolean allowUpdates, boolean overwriteCollidingHashes){
        DB db = client.getDB(databaseName);
        DBCollection cache = db.collectionExists(collectionName)?  db.getCollection(collectionName): setupCollection(db, collectionName);
        pipeline.setCache(cache, allowUpdates);
        addToHashingIndex(pipeline.getCacheConfiguration(), pipeline.getCacheConfigurationString(), overwriteCollidingHashes);
    }

    public void assignCache(String databaseName, String collectionName, FeatureExtractionPipeline pipeline){
        assignCache(databaseName, collectionName, pipeline, true, true);
    }

    /**
     * Cache the DocProcessing phase of a collection of Instances.
     * @param hostname Mongo host name
     * @param port Mongo port
     * @param databaseName Mongo database name
     * @param collectionName Mongo collection name
     * @param pipeline Pipeline used for processing the Instances
     * @param documents the Instances
     */
    public static void cache(String hostname,
                             int port,
                             String databaseName,
                             String collectionName,
                             FeatureExtractionPipeline pipeline,
                             Iterable<Instance> documents) throws UnknownHostException {

        try (CacheManager cm = new CacheManager(hostname, port)){
            cm.assignCache(databaseName,collectionName,pipeline);
            for (Instance document : documents) {
                pipeline.processDocument(document);
            }
        }
    }

    /**
     * Cache the doc processed version of Instance (like cache()), except here, replace the previously
     * cached versions.
     */
    public static void reCache(String hostname,
                               int port,
                               String databaseName,
                               String collectionName,
                               FeatureExtractionPipeline pipeline,
                               Iterable<Instance> documents) throws UnknownHostException {

        try (CacheManager cm = new CacheManager(hostname, port)){
            cm.assignCache(databaseName,collectionName,pipeline);
            for (Instance document : documents) {
                pipeline.reCache(document);
            }
        }
    }

    public void reCache(String databaseName,
                        String collectionName,
                        FeatureExtractionPipeline pipeline,
                        Iterable<Instance> documents){
        assignCache(databaseName, collectionName, pipeline);
        for (Instance document : documents) {
            pipeline.reCache(document);
        }
    }


    /*
     * Checking methods
     */

    /**
     * Determines how many documents are cached under a particular dataset with a particular pipeline config.
     */
    public long numberCached(String databaseName, String collectionName, FeatureExtractionPipeline pipeline){
        return client.getDB(databaseName).getCollection(collectionName).count(new BasicDBObject("pipelineConfig", pipeline.getCacheConfiguration()));
    }

    /**
     * Given some dataset of instances and a pipeline to process them, calculate the fraction of those
     * instances which are already in the specified cache.
     */
    public double fractionCachedOfDataset(String databaseName, String collectionName, FeatureExtractionPipeline pipeline, Iterable<Instance> documents){
        long total = 0;
        long cached = 0;
        DB database = client.getDB(databaseName);
        if (database.collectionExists(collectionName)) {
            DBCollection dataset = database.getCollection(collectionName);
            if (dataset.findOne() == null) return 0.0; // No items in collection
            int config = pipeline.getCacheConfiguration();

            for (Instance document : documents) {
                total++;
                if (null != dataset.findOne(new BasicDBObject("pipelineConfig", config).append("instanceID", document.id))) {
                    cached++;
                }
            }
            return new BigFraction(cached, total).doubleValue();
        } else {
            return 0.0;
        }
    }

    /*
     * Listing methods
     */

    public List<String> getDatabaseNames() {
        return client.getDatabaseNames();
    }

    public Set<String> getCollectionNames(String databaseName) {
        return client.getDB(databaseName).getCollectionNames();
    }

    public List<Integer> getCacheConfigs(String databaseName, String collectionName) {
        DBCollection collection = client.getDB(databaseName).getCollection(collectionName);
        return collection.distinct("pipelineConfig"); // Shh... It'll all be okay... We only ever add ints to this field
    }

    /*
     * Delete methods
     */

    public void deleteHashingIndex() {
        client.getDB("cacheManagerMetadata").getCollection("hashingIndex").drop();
    }

    public void deleteCacheManagerMetaData() {
        client.getDB("cacheManagerMetadata").dropDatabase();
    }

    public void deleteDatabase(String databaseName){
        client.getDB(databaseName).dropDatabase();
    }

    public void deleteDataset(String databaseName, String collectionName) {
        client.getDB(databaseName).getCollection(collectionName).drop();
    }

    public void deleteCache(String databaseName, String collectionName, FeatureExtractionPipeline pipeline){
        deleteCache(databaseName, collectionName, pipeline.getCacheConfiguration());
    }

    public void deleteCache(String databaseName, String collectionName, int configuration) {
        DB db = client.getDB(databaseName);
        if (db.collectionExists(collectionName)) {
            DBCollection collection = db.getCollection(collectionName);
            collection.remove(new BasicDBObject("pipelineConfig", configuration));
        }
    }

    private DBCollection setupCollection(DB database, String collectionName) {
        DBCollection collection = database.getCollection(collectionName);

        // Setup for compound index
        BasicDBObject obj = new BasicDBObject();
        obj.put("pipelineConfig", 1);
        obj.put("instanceID", 1);

        // Create options
        BasicDBObject opts = new BasicDBObject();
        opts.put("unique", true);

        collection.ensureIndex(obj, opts);

//        collection.createIndex(new BasicDBObject("instanceID", 1));
//        collection.createIndex(new BasicDBObject("pipelineConfig", 1));

        return collection;
    }

    /**
     * Ensure that the hashing index has been created.
     */
    private void ensureHashingIndexExists() {
        DB metadataDB = client.getDB("cacheManagerMetadata");
        if (!metadataDB.collectionExists("hashingIndex")) {
            DBCollection hashingIndex = metadataDB.getCollection("hashingIndex");
            hashingIndex.ensureIndex(new BasicDBObject("hashValue", 1),
                                     new BasicDBObject("unique", 1));
        }
    }

    /**
     * Add a (hash, stringvalue) pair to the hashing index.
     * @param hashValue The hash of a pipelines tokeniser and docprocessor config.
     * @param stringValue The string value of the config
     * @param overwrite If true, and the hash collides with another hash (i.e. the string value differs), then overwrite the old
     */
    private void addToHashingIndex(int hashValue, String stringValue, boolean overwrite){
        DBCollection hashingIndex = client.getDB("cacheManagerMetadata").getCollection("hashingIndex");

        DBObject currentIndex = hashingIndex.findOne(new BasicDBObject("hashValue", hashValue));

        BasicDBObject hash = new BasicDBObject();
        hash.put("hashValue", hashValue);
        hash.put("stringValue", stringValue);

        if (currentIndex == null) {  // No current index for this hash, so just add this one
            hashingIndex.insert(hash);
        } else if (!((String)currentIndex.get("stringValue")).equals(stringValue)) { // Problem! The hashes match, but the strings representations are different!
            if (overwrite) { // Overwrite the old
                hashingIndex.update(currentIndex, hash);
            } else {
                throw new CachingException("The hash configuration of this pipeline has been seen before, but their string representations are different.");
            }
        }
    }
}
