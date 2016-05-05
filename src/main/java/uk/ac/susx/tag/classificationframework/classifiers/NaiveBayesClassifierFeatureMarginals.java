package uk.ac.susx.tag.classificationframework.classifiers;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import org.apache.commons.math3.analysis.differentiation.UnivariateDifferentiableFunction;
import org.apache.commons.math3.analysis.solvers.NewtonRaphsonSolver;
import org.apache.commons.math3.exception.TooManyEvaluationsException;
import uk.ac.susx.tag.classificationframework.Util;
import uk.ac.susx.tag.classificationframework.datastructures.FeatureMarginalsConstraint;
import uk.ac.susx.tag.classificationframework.datastructures.ModelState.ClassifierName;
import uk.ac.susx.tag.classificationframework.datastructures.ProcessedInstance;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.FeatureExtractionPipeline;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by thomas on 2/22/14.
 *
 * Feature Marginals method based on Lucas, Downey (2013): http://aclweb.org/anthology/P/P13/P13-1034.pdf
 *
 * Variable names (more or less) correspond to names used in paper.
 */
public class NaiveBayesClassifierFeatureMarginals extends NaiveBayesClassifier implements NaiveBayesPrecomputable {

	// Turning the meta-knobs, max number of iterations
	public static final int DEFAULT_MAX_EVALUATIONS_NEWTON_RAPHSON = 1000000;
	public static final ClassifierName CLASSIFIER_NAME = ClassifierName.NB_FM;
	private static final int OTHER_LABEL = Integer.MAX_VALUE;
	private static final String OTHER_LABEL_NAME = "__OVR_OTHER_LABEL__";

	private int posLabel;
	private int otherLabel;

	// Maximum Number of iterations in the optimisation process
	private int maxEvaluationsNewtonRaphson;

	// Map for optimal class-conditional probabilities per label
	private Int2ObjectMap<Int2DoubleOpenHashMap> optClassCondFMProbs = new Int2ObjectOpenHashMap<>();

	private Map<String, Object> metadata = new HashMap<>();

	// TODO: Might it be a good idea to have NaiveBayesFMPreComputed?

	/**
	 * Create a source of NaiveBayesClassifierFeatureMarginals and specify the class labels.
	 * The current implementation of Feature Marginals is inherently binary, so there must not
	 * be more than 2 class labels. For multiclass problems the NaiveBayesClassifierFeatureMarginals
	 * needs to be wrapped in a NaiveBayesOVRClassifier.
	 * <p/>
	 * For efficiency reasons it needs to know about the labels in advance.
	 */
	public NaiveBayesClassifierFeatureMarginals(IntSet labels) {
		super(labels);
		this.maxEvaluationsNewtonRaphson = DEFAULT_MAX_EVALUATIONS_NEWTON_RAPHSON;
		this.metadata.put("classifier_class_name", CLASSIFIER_NAME);

		this.initLabels();
	}

	public NaiveBayesClassifierFeatureMarginals(IntSet labels, int maxEvaluationsNewtonRaphson) {
		super(labels);
		this.maxEvaluationsNewtonRaphson = maxEvaluationsNewtonRaphson;
		this.metadata.put("classifier_class_name", CLASSIFIER_NAME);

		this.initLabels();
	}

	private NaiveBayesClassifierFeatureMarginals()
	{
		super();
		this.maxEvaluationsNewtonRaphson = DEFAULT_MAX_EVALUATIONS_NEWTON_RAPHSON;
		this.metadata.put("classifier_class_name", CLASSIFIER_NAME);
	}

	@Override
	public ClassifierName getClassifierName() {
		return CLASSIFIER_NAME;
	}

	@Override
	public Map<String, Object> getMetadata() {
		return this.metadata;
	}

    public void setMaxEvaluationsNewtonRaphson(int maxEvaluationsNewtonRaphson)
    {
        this.maxEvaluationsNewtonRaphson = maxEvaluationsNewtonRaphson;
    }
    public int getMaxEvaluationsNewtonRaphson()
    {
        return this.maxEvaluationsNewtonRaphson;
    }

	public Int2ObjectMap<Int2DoubleOpenHashMap> getOptClassCondFMProbs () {
		return this.optClassCondFMProbs;
	}

    /**
     * For the final version override the train method, first create NB/NB EM model as usual,
     * then calculate the Feature Marginals
     *
     * @param unlabelledData
     * @param labelledData
     */
    public void calculateFeatureMarginals(Iterable<ProcessedInstance> labelledData, Iterable<ProcessedInstance> unlabelledData) {
        // P(w) for all words
        Int2DoubleOpenHashMap wordProb = Util.calculateWordProbabilities(unlabelledData);

        // Calculate P(t|+) & P(t|-); the probabilities of a randomly drawn token from the labelled set being positive (ie. from a positively labelled instance) or negative
        // Calculate N(+), N(w|+), N(!w|+), N(-), N(w|-), N(!w|-)
        double posTokenProb = 0.;
        double negTokenProb = 0.;
        int tokenCount = 0;
        int posTokenCount = 0;
        int negTokenCount = 0;

        Int2IntOpenHashMap posWordMap = new Int2IntOpenHashMap();
        posWordMap.defaultReturnValue(0);

        Int2IntOpenHashMap negWordMap = new Int2IntOpenHashMap();
        negWordMap.defaultReturnValue(0);

        for (ProcessedInstance i : labelledData) {
            // collecting P(t|+), N(+)
            posTokenCount += (i.getLabel() == this.posLabel) ? i.features.length : 0;
            tokenCount += i.features.length;

            // N(w|+), N(w|-)
            if (i.getLabel() == this.posLabel) {
                for (int featIdx : i.features) {
                    posWordMap.addTo(featIdx, 1);
                }
            } else {
                for (int featIdx : i.features) {
                    negWordMap.addTo(featIdx, 1);
                }
            }
        }

        // N(-)
        negTokenCount = tokenCount - posTokenCount;

        // P(t|+), P(t|-)
        posTokenProb = ((double) posTokenCount) / tokenCount;
        negTokenProb = 1. - posTokenProb;

        //-- Shorthands K, l --//

        // l
        double l = posTokenProb / negTokenProb;

        // K
        Int2DoubleOpenHashMap kMap = new Int2DoubleOpenHashMap();
        kMap.defaultReturnValue(0);

        for (int key : wordProb.keySet()) {
            kMap.put(key, (wordProb.get(key) / negTokenProb));
        }

        // Target Interval [0, P(w) / P(t|+)]
        Int2DoubleOpenHashMap targetIntervalMax = new Int2DoubleOpenHashMap();
        for (int k : wordProb.keySet()) {
            targetIntervalMax.put(k, (wordProb.get(k) / posTokenProb));
        }

        // go for the real shit
        UnivariateDifferentiableFunction featureMarginals = null;
        NewtonRaphsonSolver solver = new NewtonRaphsonSolver();
        double result = -1.;

        Int2DoubleOpenHashMap pWPosFMOptimisedMap = new Int2DoubleOpenHashMap();
        pWPosFMOptimisedMap.defaultReturnValue(-1.);

        Int2DoubleOpenHashMap pWNegFMOptimisedMap = new Int2DoubleOpenHashMap();
        pWNegFMOptimisedMap.defaultReturnValue(-1.);

        for (int key : wordProb.keySet()) {
            // N(w|+), N(!w|+), N(w|-), N(!w|-), k
            int nWPos = posWordMap.get(key);
            int nNotWPos = posTokenCount - nWPos;
            int nWNeg = negWordMap.get(key);
            double k = kMap.get(key);
            int nNotWNeg = negTokenCount - nWNeg;

            // Check for N(!w|+) > 0 and N(w|-) > 0
            if (nNotWPos > 0 && nWNeg > 0) {

                featureMarginals = new FeatureMarginalsConstraint(nWPos, nNotWPos, nWNeg, nNotWNeg, k, l);

                try {
                    result = solver.solve(this.maxEvaluationsNewtonRaphson, featureMarginals, 0, targetIntervalMax.get(key));
                } catch (TooManyEvaluationsException ex) {
                    ex.printStackTrace();
                }
            }

            // Check result in target interval [0 P(w) / P(t|+)]
            if (result > 0. && result <= targetIntervalMax.get(key)) {
                pWPosFMOptimisedMap.put(key, result);

                // Solve for P(w|-)
                double pWNegOpt = (wordProb.get(key) - (result * posTokenProb)) / negTokenProb;
                pWNegFMOptimisedMap.put(key, pWNegOpt);
            }
            result = -1.;
        }

        // Normalise Probabilities
        pWPosFMOptimisedMap = this.normaliseProbabilities(pWPosFMOptimisedMap);
        pWNegFMOptimisedMap = this.normaliseProbabilities(pWNegFMOptimisedMap);

        this.optClassCondFMProbs.put(this.posLabel, pWPosFMOptimisedMap);
        this.optClassCondFMProbs.put(this.otherLabel, pWNegFMOptimisedMap);
    }

    /**
     * Train on labelled documents.
     * See class documentation for training examples.
     * @param labelledDocs Iterable over TrainingInstances (which are (label, feature-list) pairs.
     * @param unlabelledDocs Iterable over unlabelled Instances, used for Feature Marginals
     * @param weight The weighting applied to the counts obtained.
     */
    public void train(Iterable<ProcessedInstance> labelledDocs, Iterable<ProcessedInstance>unlabelledDocs, double weight)
    {
        // Train on labelled data
        super.train(labelledDocs, weight);

        // Calculate Feature Marginals over unlabelled data
        this.calculateFeatureMarginals(labelledDocs, unlabelledDocs);
    }

    public void train(Iterable<ProcessedInstance> labelledDocs, Iterable<ProcessedInstance> unlabelledDocs)
    {
        this.train(labelledDocs, unlabelledDocs, 1.);
    }

    @Override
    public Int2DoubleOpenHashMap logpriorPlusLoglikelihood(int[] features)
    {
        Int2DoubleOpenHashMap labelScores = new Int2DoubleOpenHashMap();
        Int2DoubleMap labelPriors = labelPriors();
        Int2DoubleOpenHashMap fmMap = null;
        for (int label : this.labels) {
            double loglikelihood = 0.0;
            for (int feature : features) {
                if (this.vocab.contains(feature)){
                    if (super.getFromMap(label, this.optClassCondFMProbs).containsKey(feature)) {
                        loglikelihood += Math.log(super.getFromMap(label, this.optClassCondFMProbs).get(feature));
                    } else {
                        loglikelihood += Math.log(super.likelihood(feature, label));
                    }
                }
            }
            labelScores.put(label, Math.log(labelPriors.get(label)) + loglikelihood);
        }
        return labelScores;
    }

	/**
	 * Write classifier to file in JSON representation. Convert all features and labels to their string representation.
	 */
	@Override
	public void writeJson(JsonWriter writer, File out, FeatureExtractionPipeline pipeline) throws IOException {
		super.writeJson(writer, out, pipeline);
		writer.name("optClassCondFMProbs"); writeJsonInt2ObjectMap(writer, pipeline, optClassCondFMProbs);
	}

	/**
	 * Read classifier from file in JSON representation. Convert all features and labels from their string representation.
	 */
	public static NaiveBayesClassifierFeatureMarginals readJson(File in, FeatureExtractionPipeline pipeline) throws IOException {
		NaiveBayesClassifierFeatureMarginals nbFM = null;
		try (JsonReader reader = new JsonReader(new InputStreamReader(new FileInputStream(in), "UTF-8"))){
			reader.beginObject();
			nbFM = readJson(reader, pipeline);
			reader.endObject();
		}
		return nbFM;
	}

	public static NaiveBayesClassifierFeatureMarginals readJson(JsonReader reader, FeatureExtractionPipeline pipeline) throws IOException {
		NaiveBayesClassifierFeatureMarginals nbFM = new NaiveBayesClassifierFeatureMarginals();
		while (reader.hasNext()){
			String name = reader.nextName();
			switch (name) { // Don't worry; this is okay in Java 7 onwards
				case "labelSmoothing":   nbFM.setLabelSmoothing(reader.nextDouble()); break;
				case "featureSmoothing": nbFM.setFeatureSmoothing(reader.nextDouble()); break;
				case "labelMultipliers": nbFM.labelMultipliers = readJsonInt2DoubleMap(reader, pipeline, false); break;
				case "labels": nbFM.labels = readJsonIntSet(reader, pipeline, false); nbFM.initLabels(); break;
				case "vocab": nbFM.vocab = readJsonIntSet(reader, pipeline, true);  break;
				case "docCounts": nbFM.docCounts = readJsonInt2DoubleMap(reader, pipeline, false); break;
				case "labelCounts": nbFM.labelCounts = readJsonInt2DoubleMap(reader, pipeline, false); break;
				case "jointCounts": nbFM.jointCounts = readJsonInt2ObjectMap(reader, pipeline); break;
				case "labelFeatureAlphas": nbFM.labelFeatureAlphas = readJsonInt2ObjectMap(reader, pipeline); break;
				case "featureAlphaTotals": nbFM.featureAlphaTotals = readJsonInt2DoubleMap(reader, pipeline, false); break;
				case "labelAlphas": nbFM.labelAlphas = readJsonInt2DoubleMap(reader, pipeline, false); break;
				case "empiricalLabelPriors": nbFM.empiricalLabelPriors = reader.nextBoolean(); break;
				case "optClassCondFMProbs": nbFM.optClassCondFMProbs = readJsonInt2ObjectMap(reader, pipeline); break;
			}
		}
		return nbFM;
	}

	protected static IntSet readJsonIntSet(JsonReader reader, FeatureExtractionPipeline pipeline, boolean areFeatures) throws IOException {
		IntSet set = new IntOpenHashSet();
		reader.beginArray();
		while (reader.hasNext()){
			if (areFeatures) {
				set.add(pipeline.featureIndex(reader.nextString()));
			} else {
				String labelName = reader.nextString();
				set.add(labelName.equals(OTHER_LABEL_NAME) ? OTHER_LABEL : pipeline.labelIndex(labelName));
			}
		}
		reader.endArray();
		return set;
	}
	protected static Int2DoubleOpenHashMap readJsonInt2DoubleMap(JsonReader reader, FeatureExtractionPipeline pipeline, boolean areFeatures) throws IOException {
		Int2DoubleOpenHashMap map = new Int2DoubleOpenHashMap();
		reader.beginObject();
		while (reader.hasNext()){
			if (areFeatures) {
				map.put(pipeline.featureIndex(reader.nextName()), reader.nextDouble());
			} else {
				String labelName = reader.nextName();
				map.put(labelName.equals(OTHER_LABEL_NAME) ? OTHER_LABEL : pipeline.labelIndex(labelName), reader.nextDouble());
			}
		}
		reader.endObject();
		return map;
	}
	protected static Int2ObjectMap<Int2DoubleOpenHashMap> readJsonInt2ObjectMap(JsonReader reader, FeatureExtractionPipeline pipeline) throws IOException {
		Int2ObjectMap<Int2DoubleOpenHashMap> map = new Int2ObjectOpenHashMap<>();
		reader.beginObject();
		while (reader.hasNext()){
			String labelName = reader.nextName();
			int i = labelName.equals(OTHER_LABEL_NAME) ? OTHER_LABEL : pipeline.labelIndex(labelName);
			map.put(i, readJsonInt2DoubleMap(reader,pipeline, true));
		}
		reader.endObject();
		return map;
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

    private Int2DoubleOpenHashMap normaliseProbabilities(Int2DoubleOpenHashMap map) {
        double sum = 0.;

        // Collect sum
        for (double d : map.values()) {
            sum += d;
        }

        // Divide by sum
        for (int k : map.keySet()) {
            map.put(k, map.get(k) / sum);
        }

        return map;
    }

	private void initLabels()
	{
		if (this.labels.size() > 2) {
			System.err.println("*** [WARNING]: NaiveBayesClassifierFeatureMarginals called with more than 2 target labels! ***");
		} else if (this.labels.size() < 2) {
			System.err.println("*** [WARNING]: NaiveBayesClassifierFeatureMarginals called with less than 2 target labels! ***");
		}

		int[] l = this.labels.toIntArray();
		this.posLabel = l[0];
		this.otherLabel = l[1];
	}

	public AbstractNaiveBayesClassifier getPrecomputedClassifier() {
		return new NaiveBayesClassifierFMPreComputed(this);
	}
}