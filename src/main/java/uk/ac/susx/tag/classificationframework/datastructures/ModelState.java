package uk.ac.susx.tag.classificationframework.datastructures;

/*
 * #%L
 * ModelState.java - classificationframework - CASM Consulting - 2,013
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
import uk.ac.susx.tag.classificationframework.classifiers.NaiveBayesClassifier;
import uk.ac.susx.tag.classificationframework.classifiers.NaiveBayesClassifierFeatureMarginals;
import uk.ac.susx.tag.classificationframework.classifiers.NaiveBayesOVRClassifier;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.FeatureExtractionPipeline;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * A wrapper designed to be saved and loaded from disk.
 *
 * It wraps:
 *
 *  1. a NaiveBayesClassifier ,
 *  2. the labelled tweets that were trained on,
 *  3. the pipeline used to process that data.
 *  4. additional metadata
 *
 * The wrapped structures are saved to a specified directory as four files, respectively:
 *
 *  1. nbmodel.json
 *  2. training.json
 *  3. pipeline.ser
 *  4. metadata.json
 *
 *  ".ser" files are serialised using Java.
 *  ".json" files are serialised using Gson Json library.
 *
 *  IMPORTANT NOTES:
 *
 *  1. In another attempt to avoid "Alphabet Mismatch" type issues, although NaiveBayesClassifiers store all of their
 *     vocabulary as ints (indexed features), before the NaiveBayesClassifier is saved to disk all of the indexes are
 *     looked up and converted to their string representations.
 *
 *  2. If a pipeline isn't specified when the NaiveBayesClassifier is read from disk, then whatever pipeline it was saved
 *     with will be used to re-index the features. Otherwise a completely different pipeline can be used to re-index
 *     everything. The old pipeline will still be loaded into the ModelState.pipeline field, but this can be overwritten.
 *
 *  3. Often you're working with Instances that have already been processed into ProcessedInstances. These aren't
 *     serialised. There are convenience methods which extract the original Instance objects from those. See
 *     getSourceInstanceList() and setTrainingDocuments().
 *
 * User: Andrew D. Robertson
 * Date: 07/08/2013
 * Time: 14:41
 */
public class ModelState {

    private static final String METADATA_FILE = "metadata.json";
    private static final String MODEL_FILE = "nbmodel.json";
    private static final String PIPELINE_FILE = "pipeline.ser";
    private static final String TRAINING_FILE = "training.json";


    public NaiveBayesClassifier classifier = null;
    public List<Instance> trainingDocuments = null;
    public FeatureExtractionPipeline pipeline = null;
    public Map<String, Object> metadata = null;

	/**
	 * Enum to map from tokens to classifier class names
	 */
	public static enum ClassifierName {
		NB,
		NB_FM,
		NB_SFE,
		NB_OVR;
	}

    public ModelState() {}

    public ModelState(NaiveBayesClassifier classifier,
                      List<Instance> trainingDocuments,
                      FeatureExtractionPipeline pipeline){
        this.classifier = classifier;
        this.trainingDocuments = trainingDocuments;
        this.pipeline = pipeline;
    }

    /**
     * Convenience method. Often you will have already processed your Instance objects
     * into ProcessedInstance objects. These are not serialised, but they still
     * contain references to the original Instance objects.
     *
     * This function builds a list of references to those Instance objects, which you
     * could then assign to the trainingDocuments field, ready for serialisation.
     *
     * See also "setTrainingDocuments" for extra super convenience.
     */
    public static List<Instance> getSourceInstanceList(List<ProcessedInstance> documents){
        List<Instance> originalDocs = new LinkedList<>();
        for (ProcessedInstance document : documents) originalDocs.add(document.source);
        return originalDocs;
    }

    /**
     * Convenience method. Acquire only the metadata from a model file.
     */
    public static Map<String, Object> readMetadata(File modelDirectory) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(new File(modelDirectory, METADATA_FILE)))){
            return new Gson().fromJson(br, new TypeToken<Map<String, Object>>(){}.getType());
        }
    }

    /**
     * Convenience method. Often you will have already processed your Instance objects
     * into ProcessedInstance objects. These are not serialised, but they still
     * contain references to the original Instance objects.
     *
     * This function builds a list of references to those Instance objects, assigning them
     * straight to the trainingDocuments field, ready for serialisation. See
     * getSourceInstanceList if this behaviour isn't desired.
     */
    public void setTrainingDocuments(List<ProcessedInstance> documents){
        trainingDocuments = new LinkedList<>();
        for (ProcessedInstance document : documents) trainingDocuments.add(document.source);
    }

    /**
     * Same as "setTrainingDocuments" except that the label of the ProcesssedInstance
     * is used to overwrite the label of the source Instance before storing it.
     */
    public void setTrainingDocumentsWithNewLabels(List<ProcessedInstance> documents, FeatureExtractionPipeline pipeline){
        trainingDocuments = new LinkedList<>();
        for (ProcessedInstance document : documents){
            document.source.label = pipeline.labelString(document.getLabel());
            trainingDocuments.add(document.source);
        }
    }

    /**
     * Default save. All indexed features and labels in the classifier
     * are restored to their String values using the pipeline in the pipeline field.
     */
    public void save(File modelDirectory) throws IOException {
        save(modelDirectory, pipeline);
    }

    /**
     * By specifying a separate pipelineForWriting, the indexers of this pipeline will
     * be used to restore all indexed features and labels to their String value. It's
     * unclear whether is as useful as the load method equivalent, but it's provided
     * for completeness.
     *
     * The pipeline in the field of the ModelState is still the one that is saved to file.
     * (if you didn't want this to be the case, then you should just set the ModelState
     * field to the pipelineForWriting).
     */
    public void save(File modelDirectory, FeatureExtractionPipeline pipelineForWriting) throws IOException {
        if (modelDirectory.exists()) {
            if (!modelDirectory.isDirectory()) throw new IOException("Must specify a valid directory.");
        } else if (!modelDirectory.mkdirs()) throw new IOException("Cannot create one or more directories.");

        Gson gson = new Gson();

        File modelFile = new File(modelDirectory, MODEL_FILE);
        if (classifier!=null) classifier.writeJson(modelFile, pipelineForWriting);

		File trainingDataFile = new File(modelDirectory, TRAINING_FILE);
        if (trainingDocuments!=null) {
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(trainingDataFile))){
                gson.toJson(trainingDocuments, new TypeToken<List<Instance>>(){}.getType(), bw);
            }
        }

        File pipelineFile = new File(modelDirectory, PIPELINE_FILE);
        if (pipeline!= null){
            try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(pipelineFile))){
                out.writeObject(pipeline);
            }
        }

        File metadataFile = new File(modelDirectory, METADATA_FILE);


		if (metadata == null) {
            metadata = new HashMap<>();
        }

        metadata.putAll(classifier.getMetadata());

		try (BufferedWriter bw = new BufferedWriter(new FileWriter(metadataFile))){
			gson.toJson(metadata, Map.class, bw);
		}
    }


    /**
     * Default load. The pipeline found in the "pipeline.ser" file will be used to index the Strings
     * found in the NaiveBayesClassifier JSON serialisation.
     */
    public static ModelState load(File modelDirectory) throws IOException, ClassNotFoundException {
        if (!modelDirectory.isDirectory()) throw new IOException("Must specify a valid directory.");

        ModelState modelState = new ModelState();

        File pipelineFile = new File(modelDirectory, PIPELINE_FILE);
        if (!pipelineFile.exists()) throw new NullPointerException("Neither a pipeline was specified, nor one was found in pipeline.ser. It is necessary for reading in a model.");
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(pipelineFile))){
            modelState.pipeline = (FeatureExtractionPipeline)in.readObject();
        }

        loadTheRest(modelState, modelDirectory);
		loadTheClassifier(modelState, modelDirectory, modelState.pipeline);
        return modelState;
    }

    /**
     * By specifying a pipelineForReading, the features and labels of the NaiveBayesClassifier will be
     * re-indexed upon de-serialised by the pipelineForReading, rather than using whatever pipeline was
     * serialised with the classifier. The old pipeline is still de-serialised and assigned to the pipeline
     * field.
     */
    public static ModelState load(File modelDirectory, FeatureExtractionPipeline pipelineForReading) throws IOException, ClassNotFoundException {
        if (!modelDirectory.isDirectory()) throw new IOException("Must specify a valid directory.");
        if (pipelineForReading==null) throw new NullPointerException("Neither a pipeline was specified, nor one was found in pipeline.ser. It is necessary for reading in a model.");

        ModelState modelState = new ModelState();

        File pipelineFile = new File(modelDirectory, PIPELINE_FILE);
        if (pipelineFile.exists()){
            try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(pipelineFile))){
                modelState.pipeline = (FeatureExtractionPipeline)in.readObject();
            }
        }
        loadTheRest(modelState, modelDirectory);
		loadTheClassifier(modelState, modelDirectory, pipelineForReading);
        return modelState;
    }

    /**
     * Perform the remaining de-serialisation tasks. Separate method to avoid code duplication.
     */
    private static void loadTheRest(ModelState modelState, File modelDirectory) throws IOException, ClassNotFoundException {
        Gson gson = new Gson();

		File metadataFile = new File(modelDirectory, METADATA_FILE);
		if (metadataFile.exists()){
			try (BufferedReader br = new BufferedReader(new FileReader(metadataFile))){
				modelState.metadata = gson.fromJson(br, new TypeToken<Map<String, Object>>(){}.getType());
			}
		}

		File trainingData = new File(modelDirectory, TRAINING_FILE);
        if (trainingData.exists()){
            try (BufferedReader br = new BufferedReader(new FileReader(trainingData))){
                modelState.trainingDocuments = gson.fromJson(br, new TypeToken<List<Instance>>(){}.getType());
            }
        }
    }

	private static void loadTheClassifier(ModelState modelState, File modelDirectory, FeatureExtractionPipeline pipelineForReading) {
		ClassifierName clfName = modelState.metadata.containsKey("classifier_class_name") ? ClassifierName.valueOf((String)modelState.metadata.get("classifier_class_name")) : ClassifierName.NB;
		Class<? extends NaiveBayesClassifier> khlav = getClassifierClassForName(clfName);

		try {
			Method m;
			File modelFile = new File(modelDirectory, MODEL_FILE);

			if (clfName.equals(ClassifierName.NB_OVR)) {
				Class<? extends NaiveBayesClassifier> ovrKhlav = getClassifierClassForName(ClassifierName.valueOf((String)modelState.metadata.get("ovr_classifier_class_name")));
				m = khlav.getMethod("readJson", File.class, FeatureExtractionPipeline.class, Class.class, Map.class);
				if (modelFile.exists()) modelState.classifier = khlav.cast(m.invoke(khlav, modelFile, pipelineForReading, ovrKhlav, modelState.metadata));
			} else {
				m = khlav.getMethod("readJson", File.class, FeatureExtractionPipeline.class);
				if (modelFile.exists()) modelState.classifier = khlav.cast(m.invoke(khlav, modelFile, pipelineForReading));
			}
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	public static Class<? extends NaiveBayesClassifier> getClassifierClassForName(ClassifierName clfName) {
		Class<? extends NaiveBayesClassifier> khlavKalash; // <== classifierClass -> clfCls -> khlavKalash :D
		switch (clfName) {
			case NB_OVR:
				khlavKalash = NaiveBayesOVRClassifier.class;
				break;
			case NB_FM:
				khlavKalash = NaiveBayesClassifierFeatureMarginals.class;
				break;
			default:
				khlavKalash = NaiveBayesClassifier.class;
		}

		return khlavKalash;
	}

	/**
     * check whether this looks like a model path
     */
    public static boolean isValidModelPath(File path) {

        if(!path.isDirectory()) {
            return false;
        }

        Set<String> p = new HashSet<>(Arrays.asList(path.list()));

        if(!p.contains(METADATA_FILE) || !p.contains(MODEL_FILE) || !p.contains(PIPELINE_FILE) || !p.contains(TRAINING_FILE)) {
            return false;
        } else {
            return true;
        }
    }
}
