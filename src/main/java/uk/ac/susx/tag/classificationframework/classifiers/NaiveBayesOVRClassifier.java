package uk.ac.susx.tag.classificationframework.classifiers;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import uk.ac.susx.tag.classificationframework.datastructures.ModelState.ClassifierName;
import uk.ac.susx.tag.classificationframework.datastructures.ProcessedInstance;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.FeatureExtractionPipeline;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Created by thk22 on 03/10/2014.
 *
 * NaiveBayes type specific OVR Classifier, inherits from NaiveBayesClassifier rather than OVRClassifier, because it
 *      a) fulfils the contract of a NaiveBayesClassifier and
 *      b) makes life downstream a lot easier
 *
 * Essentially it takes n NaiveBayesClassifiers and wraps them into a single new classifier, which is able to pretend to
 * be a NaiveBayesClassifier and handles all the differences internally.
 *
 * It is a schizophrenic creature meant to cause confusion and havoc!
 * (Thomas, 21.1.2015: It bloody does cause havoc, I didn't look at it for 2 months and it confuses the shit out of me...)
 *
 * On the downside of it inheriting from NaiveBayesClassifier rather than OVRClassifer is, it currently duplicates a fair bit of code already implemented in OVRClassifier which is identical to the stuff here.
 * Options for cleaning up that mess:
 *      a) Dress up in a black tie suit, grab a monocle, a pocket watch and a glass of Brandy, mix with some Enterprise Architect people and philosophise about Java Design Patterns and Best Practices and come up with a super-corporatistic-over-engineered solution.
 *      b) Delete OVRClassifier because its a prime example of pre-mature optimisation.
 *      c) Live with a badly written codebase which is a *dream* to maintain and where future developers (and myself) will curse me and my children and my childrens children, etc, for being an absolute useless and substandard code monkey.
 *      d) Something else.
 *
 * Note: Not all of the above statements are true anymore.
 *
 * Further Note: I went for option b), deleting the root of all evil, in the hopes of spreading some good karma across the whole project. (I will still keep the literary work above "as is", because its the only piece of documentation in this file)
 *
 */
public class NaiveBayesOVRClassifier<T extends NaiveBayesClassifier> extends NaiveBayesClassifier {
	public static final ClassifierName CLASSIFIER_NAME = ClassifierName.NB_OVR;

	private static final int OTHER_LABEL = Integer.MAX_VALUE;
	private static final String OTHER_LABEL_NAME = "__OVR_OTHER_LABEL__";
	private Map<String, Object> metadata = new HashMap<>();

    private Int2ObjectMap<T> ovrLearners;
    private Class<T> learnerClass;

    public NaiveBayesOVRClassifier(IntSet labels, Class<T> learnerClass) {
        super(labels);
        this.ovrLearners = new Int2ObjectOpenHashMap<>();
        this.learnerClass = learnerClass;
		this.metadata.put("classifier_class_name", CLASSIFIER_NAME);
		this.metadata.put("ovr_num_labels", labels.size());

		this.initOVRScheme();
    }

    public NaiveBayesOVRClassifier(IntSet labels, Class<T> learnerClass, Int2ObjectMap<T> ovrLearners) {
        super(labels);
        this.ovrLearners = ovrLearners;
        this.learnerClass = learnerClass;
		this.metadata.put("classifier_class_name", CLASSIFIER_NAME);
		this.metadata.put("ovr_num_labels", labels.size());
    }

	@Override
	public Map<String, Object> getMetadata() {
		ClassifierName ovrLearner = ClassifierName.NB;
		for (int k : this.ovrLearners.keySet()) {
			ovrLearner =  this.ovrLearners.get(k).getClassifierName();
			Map<String, Object> ovrMap = new HashMap<>();
			ovrMap.put("ovr_classifier_target_label", k);
			this.metadata.put("ovr_classifier_targets", ovrMap);
		}
		this.metadata.put("ovr_classifier_class_name", ovrLearner);
		return this.metadata;
	}

	@Override
	protected void writeJsonIntSet(JsonWriter writer, FeatureExtractionPipeline pipeline, IntSet set, boolean areFeatures) throws IOException {
		if (areFeatures) super.writeJsonIntSet(writer, pipeline, set, areFeatures);
		else {
			writer.beginArray();
			for (int i : set) {
				String labelString = (i == OTHER_LABEL) ? OTHER_LABEL_NAME : pipeline.labelString(i);
				writer.value(labelString);
			}
			writer.endArray();
		}
	}

	@Override
	protected void writeJsonInt2DoubleMap(JsonWriter writer, FeatureExtractionPipeline pipeline, Int2DoubleOpenHashMap map, boolean areFeatures) throws IOException{
		if (areFeatures) super.writeJsonInt2DoubleMap(writer, pipeline, map, areFeatures);
		else {
			writer.beginObject();
			ObjectIterator<Int2DoubleMap.Entry> i = map.int2DoubleEntrySet().fastIterator();
			while (i.hasNext()) {
				Int2DoubleMap.Entry entry = i.next();
				String name = (entry.getIntKey() == OTHER_LABEL) ? OTHER_LABEL_NAME : pipeline.labelString(entry.getIntKey());
				writer.name(name);
				writer.value(entry.getDoubleValue());
			}
			writer.endObject();
		}
	}

	@Override
	protected void writeJsonInt2ObjectMap(JsonWriter writer, FeatureExtractionPipeline pipeline, Int2ObjectMap<Int2DoubleOpenHashMap> map) throws IOException{
		writer.beginObject();
		for(Int2ObjectMap.Entry<Int2DoubleOpenHashMap> entry : map.int2ObjectEntrySet()){
			String name = (entry.getIntKey() == OTHER_LABEL) ? OTHER_LABEL_NAME : pipeline.labelString(entry.getIntKey());
			writer.name(name);
			writeJsonInt2DoubleMap(writer, pipeline, entry.getValue(), true);
		}
		writer.endObject();
	}

	/*
		The values from all the indivdiual ovrLearners need to be propagated back to the NB Classifier that
		<this> object represents in order to make the serialisation work properly.

		Note that the NB Classifier that <this> object represents doesn't actually learn anything, its
		the ovrLearners it wraps that do the learning.

		Its a bit confusing to have the structure this way, but having the OVR Wrapper deriving the
		NB Classifier makes life downstream just so much easier.
	 */
	@Override
	public void writeJson(File out, FeatureExtractionPipeline pipeline) throws IOException
	{
		if (labels.size() > 2) {
			try (JsonWriter writer = new JsonWriter(new OutputStreamWriter(new FileOutputStream(out), "UTF-8"))) {
				writer.beginArray();
				for (Integer l : this.ovrLearners.keySet()) {
					T ovrLearner = this.ovrLearners.get(l);
					writer.beginObject();
					writer.name(l.toString());
						writer.beginObject();
						writer.name("labelSmoothing").value(ovrLearner.getLabelSmoothing());
						writer.name("featureSmoothing").value(ovrLearner.getFeatureSmoothing());
						writer.name("labelMultipliers"); writeJsonInt2DoubleMap(writer, pipeline, ovrLearner.labelMultipliers, false);
						writer.name("labels"); writeJsonIntSet(writer, pipeline, ovrLearner.labels, false);
						writer.name("vocab"); writeJsonIntSet(writer, pipeline, ovrLearner.vocab, true);
						writer.name("docCounts"); writeJsonInt2DoubleMap(writer, pipeline, ovrLearner.docCounts, false);
						writer.name("labelCounts"); writeJsonInt2DoubleMap(writer, pipeline, ovrLearner.labelCounts, false);
						writer.name("jointCounts"); writeJsonInt2ObjectMap(writer, pipeline, ovrLearner.jointCounts);
						writer.name("labelFeatureAlphas"); writeJsonInt2ObjectMap(writer, pipeline, ovrLearner.labelFeatureAlphas);
						writer.name("featureAlphaTotals"); writeJsonInt2DoubleMap(writer, pipeline, ovrLearner.featureAlphaTotals, false);
						writer.name("labelAlphas"); writeJsonInt2DoubleMap(writer, pipeline, ovrLearner.labelAlphas, false);
						writer.name("empiricalLabelPriors").value(ovrLearner.empiricalLabelPriors);
						writer.endObject();
					writer.endObject();
				}
				writer.endArray();
			}
		} else {
			this.ovrLearners.get(OTHER_LABEL).writeJson(out, pipeline);
		}
	}

	public static NaiveBayesOVRClassifier<? extends NaiveBayesClassifier> readJson(File in, FeatureExtractionPipeline pipeline, Class<? extends NaiveBayesClassifier> learnerClass, Map<String, Object> ovrMetadata) throws IOException {
		NaiveBayesOVRClassifier<? extends NaiveBayesClassifier> nbOVR = null;

		if (ovrMetadata.containsKey("ovr_num_labels") && ((Double)ovrMetadata.get("ovr_num_labels")).intValue() > 2) {
			try (JsonReader reader = new JsonReader(new InputStreamReader(new FileInputStream(in), "UTF-8"))) {
				IntSet labels = new IntOpenHashSet();

				reader.beginArray();
				while (labels.size() < ((Double)ovrMetadata.get("ovr_num_labels")).intValue()) {
					reader.beginObject();

					//if (currLabel == null) {
						String label = reader.nextName();
						labels.add(Integer.parseInt(label));
					//}
					System.out.println("NAME: " + label);
					System.out.println("GET INT:" +  Integer.parseInt(label));
					//reader.beginObject();
					reader.beginObject();
					while (reader.hasNext()) {
						String name = reader.nextName();
						System.out.println("NEXT NAME: " + name);
						 // TODO: the actual read needs to be done from an instance of the learnerClass
						switch (name) { // Don't worry; this is okay in Java 7 onwards
							case "labelSmoothing":   System.out.println(reader.nextDouble()); break;
							case "featureSmoothing": System.out.println(reader.nextDouble()); break;
							case "labelMultipliers": System.out.println(readJsonInt2DoubleMap(reader, pipeline, false)); break;
							case "labels": System.out.println(readJsonIntSet(reader, pipeline, false)); break;
							case "vocab": System.out.println(readJsonIntSet(reader, pipeline, true));  break;
							case "docCounts": System.out.println(readJsonInt2DoubleMap(reader, pipeline, false)); break;
							case "labelCounts": System.out.println(readJsonInt2DoubleMap(reader, pipeline, false)); break;
							case "jointCounts": System.out.println(readJsonInt2ObjectMap(reader, pipeline)); break;
							case "labelFeatureAlphas": System.out.println(readJsonInt2ObjectMap(reader, pipeline)); break;
							case "featureAlphaTotals": System.out.println(readJsonInt2DoubleMap(reader, pipeline, false)); break;
							case "labelAlphas": System.out.println(readJsonInt2DoubleMap(reader, pipeline, false)); break;
							case "empiricalLabelPriors": System.out.println(reader.nextBoolean()); break;
						}
					}
					reader.endObject();
				}
				reader.endObject();
				reader.endArray();
			}
		} else {
			try {
				Method m = learnerClass.getMethod("readJson", File.class, FeatureExtractionPipeline.class);
				NaiveBayesClassifier nb = learnerClass.cast(m.invoke(learnerClass, in, pipeline)); // static type NB is fine, dynamic type is handled by the learnerClass

				Int2ObjectMap ovrMap = new Int2ObjectOpenHashMap<>();
				ovrMap.put(OTHER_LABEL, nb);

				nbOVR = new NaiveBayesOVRClassifier(nb.getLabels(), learnerClass, ovrMap);

			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}

		return nbOVR;
	}


    @Override
    public void setLabelSmoothing(double smoothingValue)
    {
		for (int l : this.ovrLearners.keySet()) {
			this.ovrLearners.get(l).setLabelSmoothing(smoothingValue);
		}
    }

	@Override
	public void setFeatureSmoothing(double smoothingValue)
	{
		for (int l : this.ovrLearners.keySet()) {
			this.ovrLearners.get(l).setFeatureSmoothing(smoothingValue);
		}
	}

	@Override
	public void setLabelAlpha(int label, double alpha)
	{
		if (this.ovrLearners.keySet().size() > 1) {
			this.ovrLearners.get(label).setLabelAlpha(label, alpha);
		} else {
			this.ovrLearners.get(OTHER_LABEL).setLabelAlpha(label, alpha);
		}
	}

	@Override
	public Int2DoubleOpenHashMap getLabelAlphas()
	{
		Int2DoubleOpenHashMap labelAlphas = null;

		if (this.ovrLearners.keySet().size() > 1) {
			labelAlphas = new Int2DoubleOpenHashMap();
			for (int l : this.labels) {
				T ovrLearner = ovrLearners.get(l);
				labelAlphas.put(l, ovrLearner.labelAlphas.get(l));
			}
		} else {
			labelAlphas = this.ovrLearners.get(OTHER_LABEL).getLabelAlphas();
		}

		return labelAlphas;
	}

	@Override
	public void setFeatureAlpha(int feature, int label, double alpha)
	{
		if (this.ovrLearners.keySet().size() > 1) {
			for (int l : this.labels) {
				int targetLabel = (l == label) ? label : OTHER_LABEL;
				this.ovrLearners.get(l).setFeatureAlpha(feature, targetLabel, alpha);
			}
		} else {
			this.ovrLearners.get(OTHER_LABEL).setFeatureAlpha(feature, label, alpha);
		}
	}

	@Override
	public Int2DoubleOpenHashMap getLabelMultipliers()
	{
		if (this.ovrLearners.keySet().size() > 1) {
			Int2DoubleOpenHashMap labelMultipliers = new Int2DoubleOpenHashMap();
			for (int l : this.labels) {
				labelMultipliers.putAll(this.ovrLearners.get(l).getLabelMultipliers());
			}
			// Remove OTHER_LABEL
			labelMultipliers.remove(OTHER_LABEL);

			return labelMultipliers;
		} else {
			return this.ovrLearners.get(OTHER_LABEL).getLabelMultipliers();
		}
	}

	@Override
	public Int2ObjectMap<Int2DoubleOpenHashMap> getLabelledFeatures()
	{
		// TODO: Check if the statement below is true
		// As all classifiers have the same labelledFeatures (might be with different labels, but the actual features are the same)
		// we can just return the labelled features from *any* of the classifiers

		// TODO: This looks weird and ugly, can we do it differently?
		Iterator<T> iter = this.ovrLearners.values().iterator();
		return iter.next().getLabelledFeatures();
	}

	@Override
	public void trainOnInstance(int label, int[] features, double labelProbability, double weight)
	{
		if (this.ovrLearners.keySet().size() > 1) {
			/*
			 * The specialist classifier for the given label needs to be positively trained on that instance
			 *
			 * The other classifier need to be negatively trained on that instance (that is with label=OTHER_LABEL)
			 */
			for (int l : this.labels) {
				int targetLabel = (l == label) ? label : OTHER_LABEL;
				this.ovrLearners.get(l).trainOnInstance(targetLabel, features, labelProbability, weight);
			}
		} else {
			this.ovrLearners.get(OTHER_LABEL).trainOnInstance(label, features, labelProbability, weight);
		}
	}

	@Override
	public void unlabelFeature(int feature, int label)
	{
		if (this.ovrLearners.keySet().size() > 1) {
			for (int l : this.labels) {
				int targetLabel = (l == label) ? label : OTHER_LABEL;
				this.ovrLearners.get(l).unlabelFeature(feature, targetLabel);
			}
		} else {
			this.ovrLearners.get(OTHER_LABEL).unlabelFeature(feature, label);
		}
	}

	@Override
	public void setLabelMultiplier(int label, double multiplier)
	{
		int targetLabel = (this.ovrLearners.keySet().size() > 1) ? label : OTHER_LABEL;
		this.ovrLearners.get(targetLabel).setLabelMultiplier(label, multiplier);
	}

	@Override
	public double likelihood(int feature, int label)
	{
		return (this.ovrLearners.keySet().size() > 1) ? this.ovrLearners.get(label).likelihood(feature, label) : this.ovrLearners.get(OTHER_LABEL).likelihood(feature, label);
	}

    @Override
    public void train(Iterable<ProcessedInstance> labelledDocuments, Iterable<ProcessedInstance> unlabelledDocuments)
    {
        if (this.labels.size() > 2) {
            this.trainOVRSemiSupervised(labelledDocuments, unlabelledDocuments);
        } else {
            this.trainBinarySemiSupervised(labelledDocuments, unlabelledDocuments);
        }
    }

    @Override
    public void train(Iterable<ProcessedInstance> labelledDocuments)
    {
            if (this.labels.size() > 2) {
                this.trainOVRSupervised(labelledDocuments);
            } else {
                this.trainBinarySupervised(labelledDocuments);
            }
    }

    @Override
    public IntSet getLabels() {
        return this.labels;
    }

    @Override
    public IntSet getVocab() {
        // All learners have the same vocab, so we just return the vocab of the first one
        return (this.ovrLearners.size() > 0 ? this.ovrLearners.get(0).getVocab() : null);
    }

    public Int2ObjectMap<T> getOvrLearners()
    {
        return this.ovrLearners;
    }

	@Override
	public Int2DoubleOpenHashMap logpriorPlusLoglikelihood(int[] features)
	{
		Int2DoubleOpenHashMap jll = new Int2DoubleOpenHashMap();
		for (T learner : this.ovrLearners.values()) {
			jll.putAll(learner.logpriorPlusLoglikelihood(features));
		}

		// Remove other label again
		jll.remove(OTHER_LABEL);

		return jll;
	}

	@Override
	public Int2DoubleMap labelPriors()
	{
		Int2DoubleMap priors = new Int2DoubleOpenHashMap();

		for (T learner : this.ovrLearners.values()) {
			priors.putAll(learner.labelPriors());
		}

		// Remove OTHER_LABEL
		priors.remove(OTHER_LABEL);

		return priors;
	}

    public Int2DoubleOpenHashMap predict(int[] features)
    {
        Int2DoubleOpenHashMap prediction = new Int2DoubleOpenHashMap();
        for (T learner : this.ovrLearners.values()) {
            prediction.putAll(learner.predict(features));
        }

        // Remove other label, so all that remains are the predictions for the existing labels
        prediction.remove(OTHER_LABEL);

        return prediction;
    }

    public int bestLabel(int[] features)
    {
        Int2DoubleMap prediction = this.predict(features);

        double maxPrediction = Double.MIN_VALUE;
        int bestLabel = -1;

        for (int key : prediction.keySet()) {
            if (prediction.get(key) > maxPrediction) {
                maxPrediction = prediction.get(key);
                bestLabel = key;
            }
        }

        return bestLabel;
    }

    private void trainBinarySupervised(Iterable<ProcessedInstance> labelledDocs)
    {
        this.ovrLearners.get(OTHER_LABEL).train(labelledDocs);
    }

    private void trainBinarySemiSupervised(Iterable<ProcessedInstance> labelledDocs, Iterable<ProcessedInstance> unlabelledDocuments)
    {
        this.ovrLearners.get(OTHER_LABEL).train(labelledDocs, unlabelledDocuments);
    }

    private void trainOVRSupervised(Iterable<ProcessedInstance> labelledDocs)
    {
        for (int l : this.labels) {
            T currLearner = this.ovrLearners.get(l);
            currLearner.train(this.binariseLabelledDocuments(labelledDocs, l));
        }
    }

    private void trainOVRSemiSupervised(Iterable<ProcessedInstance> labelledDocs, Iterable<ProcessedInstance> unlabelledDocs)
    {
        for (int l : this.labels) {
            T currLearner = this.ovrLearners.get(l);
            currLearner.train(this.binariseLabelledDocuments(labelledDocs, l), unlabelledDocs);
        }
    }

    private Iterable<ProcessedInstance> binariseLabelledDocuments(Iterable<ProcessedInstance> labelledDocs, int currLabel)
    {
        List<ProcessedInstance> binarisedDocs = new ArrayList<>();

        for (ProcessedInstance p : labelledDocs) {
            binarisedDocs.add(new ProcessedInstance((p.getLabel() == currLabel ? p.getLabel() : OTHER_LABEL), p.features, p.source));
        }

        return binarisedDocs;
    }

    private void initOVRScheme() {
        try {
			Class[] typeArgs = {IntSet.class};
			Constructor<T> constructor = this.learnerClass.getConstructor(typeArgs);

            if (this.labels.size() > 2) {
                for (int l : this.labels) {
					int[] binarisedLabels = {l, OTHER_LABEL};
					Object[] args = {new IntOpenHashSet(binarisedLabels)};

                    this.ovrLearners.put(l, constructor.newInstance(args));
                }
            } else {
				Object[] args = {this.labels};
                this.ovrLearners.put(OTHER_LABEL, constructor.newInstance(args));
            }
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
        } catch (NoSuchMethodException e) {
			e.printStackTrace();
		}
    }
}
