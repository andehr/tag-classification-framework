package uk.ac.susx.tag.classificationframework.datastructures;

/*
 * #%L
 * CompatibilityModelState.java - classificationframework - CASM Consulting - 2,013
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
import com.google.gson.reflect.TypeToken;
import uk.ac.susx.tag.classificationframework.Util;
import uk.ac.susx.tag.classificationframework.classifiers.NaiveBayesClassifierPreComputed;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.FeatureExtractionPipeline;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Provide functionality for loading up a converted model from the old framework.
 *
 * Obtained a converted old model, by using the convert function found in the old framework's Util class.
 *
 * User: Andrew D. Robertson
 * Date: 16/09/2013
 * Time: 10:56
 */
public class CompatibilityModelState {

    public NaiveBayesClassifierPreComputed classifier = null;
    public List<Instance> trainingDocuments = null;
    public FeatureExtractionPipeline pipeline = null;
    public Map<String, Object> metadata = null;

    public CompatibilityModelState() {}

    public static CompatibilityModelState load(File modelDirectory) throws IOException {
        return load(modelDirectory, null);
    }

    /**
     * Load a converted old model. If a pipeline is not specified, then the type of the old pipeline will be checked
     * (cmu or keywordremoving) and create a pipeline which replicates said pipeline.
     */
    public static CompatibilityModelState load(File modelDirectory, FeatureExtractionPipeline pipeline) throws IOException {
        // same as other, except the pipeline is the pipeline in the parameters
        CompatibilityModelState modelState = new CompatibilityModelState();
        Gson gson = new Gson();

        File metadataFile = new File(modelDirectory, "metadata.json");
        try (BufferedReader br = new BufferedReader(new FileReader(metadataFile))){
            modelState.metadata = gson.fromJson(br, new TypeToken<Map<String, Object>>(){}.getType());
        }

        File trainingData = new File(modelDirectory, "training.json");
        try (BufferedReader br = new BufferedReader(new FileReader(trainingData))){
            modelState.trainingDocuments = gson.fromJson(br, new TypeToken<List<Instance>>(){}.getType());
        }

        if (pipeline==null) { // If no pipeline is specified, try to use the equivalent to the type specified in metadata.json
            Boolean removeStopwords = (Boolean)modelState.metadata.get("conv:removeStopwords");
            if (removeStopwords==null) throw new NullPointerException("Metadata doesnt specify whether stopwords should be removed in new pipeline.");
            String pipeType = (String)modelState.metadata.get("conv:pipelineType");
            if (pipeType==null) throw new NullPointerException("No pipeline is specified for reading, not is one specified in state's metadata");
            else if (pipeType.equals("uk.ac.susx.tag.dualist.pipes.CMUPipe")){
                pipeline = Util.buildCMUPipeline(removeStopwords, false);
            } else if (pipeType.equals("uk.ac.susx.tag.dualist.pipes.KeywordRemovingTwitterPipe")){
                pipeline = Util.buildBasicPipeline(removeStopwords, false);
            }
        }

        File modelFile = new File(modelDirectory, "nbpmodel.json");
        modelState.classifier = NaiveBayesClassifierPreComputed.readJson(modelFile, pipeline);

        modelState.pipeline = pipeline;
        return modelState;
    }

}
