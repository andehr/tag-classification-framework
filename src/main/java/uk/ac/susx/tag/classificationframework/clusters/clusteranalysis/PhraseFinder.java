package uk.ac.susx.tag.classificationframework.clusters.clusteranalysis;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import uk.ac.susx.tag.classificationframework.clusters.ClusteredProcessedInstance;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.FeatureExtractionPipeline;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: Andrew D. Robertson
 * Date: 06/11/2015
 * Time: 13:58
 */
public class PhraseFinder {

    private IntSet[] featuresOfInterest;
    private ClusterFeatureAnalysis analysis;
    private FeatureExtractionPipeline pipeline;
    private int numOfFeatures;
    private int numOfPhrases;

    public PhraseFinder(ClusterFeatureAnalysis analysis, FeatureExtractionPipeline pipeline, int numOfFeatures, int numOfPhrases) {
        this.featuresOfInterest = new IntSet[analysis.getNumOfClusters()];
        this.analysis = analysis;
        this.pipeline = pipeline;
        this.numOfFeatures = numOfFeatures;
        this.numOfPhrases = numOfPhrases;
    }

    public static Map<String, List<String>> find(ClusterFeatureAnalysis analysis,
                                                 Collection<ClusteredProcessedInstance> documents,
                                                 FeatureExtractionPipeline pipeline,
                                                 int numOfFeatures, int numOfPhrases,
                                                 int minPhraseSize, int maxPhraseSize){

        IntSet[] featuresOfInterest = new IntSet[analysis.getNumOfClusters()];

        for (int c = 0; c < featuresOfInterest.length; c++){
            featuresOfInterest[c] = new IntOpenHashSet(analysis.getTopFeatures(c, numOfFeatures));
        }



        return null;
    }

    public static List<String> makePhrases(List<String> tokens, Set<String> tokensOfInterest, int minPhraseSize, int maxPhraseSize){
        return null;
    }

}
