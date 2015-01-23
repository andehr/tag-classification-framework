package uk.ac.susx.tag.classificationframework.classifiers;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import uk.ac.susx.tag.classificationframework.datastructures.ProcessedInstance;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.FeatureExtractionPipeline;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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

    private static final int OTHER_LABEL = Integer.MAX_VALUE;
	private static final String OTHER_LABEL_NAME = "__OVR_OTHER_LABEL__";

    private Int2ObjectMap<T> ovrLearners;
    private Class<T> learnerClass;

    public NaiveBayesOVRClassifier(IntSet labels, Class<T> learnerClass) {
        super(labels);
        this.ovrLearners = new Int2ObjectOpenHashMap<>();
        this.learnerClass = learnerClass;

        this.initOVRScheme();
    }

    public NaiveBayesOVRClassifier(IntSet labels, Class<T> learnerClass, Int2ObjectMap<T> ovrLearners) {
        super(labels);
        this.ovrLearners = ovrLearners;
        this.learnerClass = learnerClass;
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
					writer.name("labelMultipliers");
					writeJsonInt2DoubleMap(writer, pipeline, ovrLearner.labelMultipliers, false);
					writer.name("labels");
					writeJsonIntSet(writer, pipeline, ovrLearner.labels, false);
					writer.name("vocab");
					writeJsonIntSet(writer, pipeline, ovrLearner.vocab, true);
					writer.name("docCounts");
					writeJsonInt2DoubleMap(writer, pipeline, ovrLearner.docCounts, false);
					writer.name("labelCounts");
					writeJsonInt2DoubleMap(writer, pipeline, ovrLearner.labelCounts, false);
					writer.name("jointCounts");
					writeJsonInt2ObjectMap(writer, pipeline, ovrLearner.jointCounts);
					writer.name("labelFeatureAlphas");
					writeJsonInt2ObjectMap(writer, pipeline, ovrLearner.labelFeatureAlphas);
					writer.name("featureAlphaTotals");
					writeJsonInt2DoubleMap(writer, pipeline, ovrLearner.featureAlphaTotals, false);
					writer.name("labelAlphas");
					writeJsonInt2DoubleMap(writer, pipeline, ovrLearner.labelAlphas, false);
					writer.name("empiricalLabelPriors").value(ovrLearner.empiricalLabelPriors);
					writer.endObject();
					writer.endObject();
				}
				writer.endArray();
			}
		} else {
			this.ovrLearners.get(OTHER_LABEL).writeJson(out, pipeline);
		}

		/* This works, v1
		if (this.labels.size() > 2) {
			for (int l : this.ovrLearners.keySet()) { // Note, the shared variables (labelSmoothing, featureSmoothing, ...) are overridden with each iteration, this is a known uglyness and will be properly resolved once creativity (and motivation) return into town.
				T ovrLearner = this.ovrLearners.get(l);
				
				super.setLabelSmoothing(ovrLearner.getLabelSmoothing());
				super.setFeatureSmoothing(ovrLearner.getFeatureSmoothing());
				super.vocab = ovrLearner.vocab;
				super.empiricalLabelPriors = ovrLearner.empiricalLabelPriors;

				// Leave empty if empty, otherwise modify
				if (ovrLearner.labelMultipliers.containsKey(l)) {
					super.labelMultipliers.put(l, ovrLearner.labelMultipliers.get(l));
				}
				if (ovrLearner.docCounts.containsKey(l)) {
					super.docCounts.put(l, ovrLearner.docCounts.get(l));
				}
				if (ovrLearner.labelCounts.containsKey(l)) {
					super.labelCounts.put(l, ovrLearner.labelCounts.get(l));
				}
				if (ovrLearner.jointCounts.containsKey(l)) {
					super.jointCounts.put(l, ovrLearner.jointCounts.get(l));
				}
				if (ovrLearner.labelFeatureAlphas.containsKey(l)) {
					super.labelFeatureAlphas.put(l, ovrLearner.labelFeatureAlphas.get(l));
				}
				if (ovrLearner.featureAlphaTotals.containsKey(l)) {
					super.featureAlphaTotals.put(l, ovrLearner.featureAlphaTotals.get(l));
				}
				if (ovrLearner.labelAlphas.containsKey(l)) {
					super.labelAlphas.put(l, ovrLearner.labelAlphas.get(l));
				}
			}
		} else {
			super.setLabelSmoothing(this.ovrLearners.get(OTHER_LABEL).getLabelSmoothing());
			super.setFeatureSmoothing(this.ovrLearners.get(OTHER_LABEL).getFeatureSmoothing());
			super.labelMultipliers = this.ovrLearners.get(OTHER_LABEL).labelMultipliers;
			super.labels = this.ovrLearners.get(OTHER_LABEL).labels;
			super.vocab = this.ovrLearners.get(OTHER_LABEL).vocab;
			super.docCounts = this.ovrLearners.get(OTHER_LABEL).docCounts;
			super.labelCounts = this.ovrLearners.get(OTHER_LABEL).labelCounts;
			super.jointCounts = this.ovrLearners.get(OTHER_LABEL).jointCounts;
			super.labelFeatureAlphas = this.ovrLearners.get(OTHER_LABEL).labelFeatureAlphas;
			super.featureAlphaTotals = this.ovrLearners.get(OTHER_LABEL).featureAlphaTotals;
			super.labelAlphas = this.ovrLearners.get(OTHER_LABEL).labelAlphas;
			super.empiricalLabelPriors = this.ovrLearners.get(OTHER_LABEL).empiricalLabelPriors;
		}

		super.writeJson(out, pipeline);
		*/
	}

	public static NaiveBayesOVRClassifier readJson(File in, FeatureExtractionPipeline pipeline, Class<? extends NaiveBayesClassifier> learnerClass) throws IOException {
		IntSet labels = null;

		// Re-create the OVR business by first reading in the labels and then constructing the OVR Setup from there
		try (JsonReader reader = new JsonReader(new InputStreamReader(new FileInputStream(in), "UTF-8"))) {
			reader.beginObject();
			while (reader.hasNext()) {
				String name = reader.nextName();
				switch (name) { // Don't worry; this is okay in Java 7 onwards
					case "labels":
						labels = NaiveBayesClassifier.readJsonIntSet(reader, pipeline, false);
						break;
				}
			}
			reader.endObject();
		}

		NaiveBayesOVRClassifier<? extends NaiveBayesClassifier> nbOVR = new NaiveBayesOVRClassifier(labels, learnerClass);

		if (labels.size() > 2) {

		} else {
			// This NB Classifier acts as a proxy
			NaiveBayesClassifier nb = NaiveBayesClassifier.readJson(in, pipeline);

		}


		// TODO: Need to maintain a set of "otherCounts" for every attribute in the JSON to
		// properly initialise all of the OVR NB models
		Int2ObjectMap<Int2DoubleOpenHashMap> otherLabelMultipliers = new Int2ObjectOpenHashMap<>();
		Int2ObjectMap<Int2DoubleOpenHashMap> otherDocCounts = new Int2ObjectOpenHashMap<>();
		Int2ObjectMap<Int2DoubleOpenHashMap> otherLabelCounts = new Int2ObjectOpenHashMap<>();
		Int2ObjectMap<Int2ObjectMap<Int2DoubleOpenHashMap>> otherJointCounts = new Int2ObjectOpenHashMap<>();
		Int2ObjectMap<Int2ObjectMap<Int2DoubleOpenHashMap>> otherLabelFeatureAlphas = new Int2ObjectOpenHashMap<>();
		Int2ObjectMap<Int2DoubleOpenHashMap> otherFeatureAlphaTotals = new Int2ObjectOpenHashMap<>();
		Int2ObjectMap<Int2DoubleOpenHashMap> otherLabelAlphas = new Int2ObjectOpenHashMap<>();

		for (int l : labels) {
			try (JsonReader reader = new JsonReader(new InputStreamReader(new FileInputStream(in), "UTF-8"))) {
				reader.beginObject();
				while (reader.hasNext()) {
					String name = reader.nextName();
					switch (name) { // Don't worry; this is okay in Java 7 onwards
						case "labelSmoothing":
							nbOVR.ovrLearners.get(l).setLabelSmoothing(reader.nextDouble());
							break;
						case "featureSmoothing":
							nbOVR.ovrLearners.get(l).setFeatureSmoothing(reader.nextDouble());
							break;
						case "labelMultipliers": {
							// TODO: Collect counts for other lables
							Int2DoubleOpenHashMap temp = readJsonInt2DoubleMap(reader, pipeline, false);
							for (int ll : labels) {
								if (ll == l) continue;
								otherLabelMultipliers.get(l).addTo(OTHER_LABEL, temp.get(ll));
							}
							nbOVR.ovrLearners.get(l).labelMultipliers.put(l, temp.get(l));
							nbOVR.ovrLearners.get(l).labelMultipliers.put(OTHER_LABEL, otherLabelMultipliers.get(l).get(OTHER_LABEL));
							break;
						}
						case "labels":
							nbOVR.ovrLearners.get(l).labels = readJsonIntSet(reader, pipeline, false);
							break;

						case "vocab":
							nbOVR.ovrLearners.get(l).vocab = readJsonIntSet(reader, pipeline, false);
							break;
						case "docCounts": {
							Int2DoubleOpenHashMap temp = readJsonInt2DoubleMap(reader, pipeline, false);
							for (int ll : labels) {
								if (ll == l) continue;
								otherDocCounts.get(l).addTo(OTHER_LABEL, temp.get(ll));
							}
							nbOVR.ovrLearners.get(l).docCounts.put(l, temp.get(l));
							nbOVR.ovrLearners.get(l).docCounts.put(OTHER_LABEL, otherDocCounts.get(l).get(OTHER_LABEL));
							break;
						}
						case "labelCounts": {
							Int2DoubleOpenHashMap temp = readJsonInt2DoubleMap(reader, pipeline, false);
							for (int ll : labels) {
								if (ll == l) continue;
								otherLabelCounts.get(l).addTo(OTHER_LABEL, temp.get(ll));
							}
							nbOVR.ovrLearners.get(l).labelCounts.put(l, temp.get(l));
							nbOVR.ovrLearners.get(l).labelCounts.put(OTHER_LABEL, otherLabelCounts.get(l).get(OTHER_LABEL));
							break;
						}
						case "jointCounts": {
							/*
							Int2DoubleOpenHashMap temp = readJsonInt2DoubleMap(reader, pipeline, false);
							for (int ll : labels) {
								if (ll == l) continue;
								otherJointCounts.get(l).addTo(OTHER_LABEL, temp.get(ll));
							}
							nbOVR.ovrLearners.get(l).jointCounts.put(l, temp.get(l));
							nbOVR.ovrLearners.get(l).jointCounts.put(OTHER_LABEL, otherLabelCounts.get(l).get(OTHER_LABEL));
							*/
							break;
						}
						case "labelFeatureAlphas":
							//nb.labelFeatureAlphas = readJsonInt2ObjectMap(reader, pipeline);
							break;
						case "featureAlphaTotals":{
							Int2DoubleOpenHashMap temp = readJsonInt2DoubleMap(reader, pipeline, false);
							for (int ll : labels) {
								if (ll == l) continue;
								otherFeatureAlphaTotals.get(l).addTo(OTHER_LABEL, temp.get(ll));
							}
							nbOVR.ovrLearners.get(l).featureAlphaTotals.put(l, temp.get(l));
							nbOVR.ovrLearners.get(l).featureAlphaTotals.put(OTHER_LABEL, otherFeatureAlphaTotals.get(l).get(OTHER_LABEL));
							break;
						}
						case "labelAlphas": {
							Int2DoubleOpenHashMap temp = readJsonInt2DoubleMap(reader, pipeline, false);
							for (int ll : labels) {
								if (ll == l) continue;
								otherLabelAlphas.get(l).addTo(OTHER_LABEL, temp.get(ll));
							}
							nbOVR.ovrLearners.get(l).labelAlphas.put(l, temp.get(l));
							nbOVR.ovrLearners.get(l).labelAlphas.put(OTHER_LABEL, otherLabelAlphas.get(l).get(OTHER_LABEL));
							break;
						}
						case "empiricalLabelPriors":
							nbOVR.ovrLearners.get(l).empiricalLabelPriors = reader.nextBoolean();
							break;
					}
				}
				reader.endObject();
			}
		}

		return null;
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
