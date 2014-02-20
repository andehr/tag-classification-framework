package uk.ac.susx.tag.classificationframework.jsonhandling;

/*
 * #%L
 * JsonListStreamReader.java - classificationframework - CASM Consulting - 2,013
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

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import uk.ac.susx.tag.classificationframework.datastructures.Instance;
import uk.ac.susx.tag.classificationframework.datastructures.ProcessedInstance;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.FeatureExtractionPipeline;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;

/**
 * Class for reading a JSON array over Instance objects iteratively from a file.
 *
 * The Instances can be returned in 2 different ways, by calling one of 2 different methods.
 * Each method returns an Iterable:
 *
 *   iterableOverInstances() :
 *      Get an iterable over Instance objects.
 *
 *   iterableOverProcessedInstances()
 *      Pass the Instance's text field through the specified pipeline. Return a ProcessedInstance
 *      object, which contains the resulting features and the contents of the label field of the
 *      Instance object (indexed).
 *
 * Usage:
 *
 *  try(JsonListStreamReader sr = new JsonListStreamReader(new File("test.json"), new Gson())){
 *      for (Instance i : sr.iterableOverInstances()) System.out.println(i);
 *  }
 *
 * User: Andrew D. Robertson
 * Date: 13/08/2013
 * Time: 14:23
 */
public class JsonListStreamReader implements AutoCloseable {

    private final JsonReader jsonReader;
    private final Gson gson;

    public JsonListStreamReader(File jsonFile, Gson gson) throws IOException {
        jsonReader = new JsonReader(new InputStreamReader(new FileInputStream(jsonFile), "UTF8"));
        this.gson = gson;
    }

    public void close() throws IOException {
        jsonReader.close();
    }

    public Iterable<Instance> iterableOverInstances(){
        return new Iterable<Instance>() {
            @Override
            public Iterator<Instance> iterator() {
                try {
                    return new JsonIteratorInstance(jsonReader, gson);
                } catch (IOException e) {  e.printStackTrace(); return null; }
            }
        };
    }

    public Iterable<ProcessedInstance> iterableOverProcessedInstances(final FeatureExtractionPipeline pipeline){
        return new Iterable<ProcessedInstance>() {
            @Override
            public Iterator<ProcessedInstance> iterator() {
                try {
                    return new JsonIteratorProcessedInstance(jsonReader, pipeline, gson);
                } catch (IOException e) {  e.printStackTrace(); return null; }
            }
        };
    }
}
