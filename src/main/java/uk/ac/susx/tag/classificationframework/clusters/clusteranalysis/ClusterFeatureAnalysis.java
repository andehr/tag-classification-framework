package uk.ac.susx.tag.classificationframework.clusters.clusteranalysis;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import uk.ac.susx.tag.classificationframework.clusters.ClusteredProcessedInstance;

import java.util.Collection;

/**
 * Created with IntelliJ IDEA.
 * User: Andrew D. Robertson
 * Date: 16/10/2015
 * Time: 15:07
 */
public class ClusterFeatureAnalysis {

    private Int2IntOpenHashMap featureCounts;
    private Int2IntOpenHashMap[] jointCounts;

    private int[] totalFeatureCountPerCluster;

    private FeatureLabelJointCounter counts;

    public ClusterFeatureAnalysis(Collection<ClusteredProcessedInstance> documents){
        this(documents, FeatureLabelJointCounter.createDocumentBased(documents, new FeatureLabelJointCounter.HighestProbabilityOnly()));
    }

    public ClusterFeatureAnalysis(Collection<ClusteredProcessedInstance> documents, FeatureLabelJointCounter c) {
        counts = c;

        numClusters = documents.iterator().next().getClusterVector().length;
        totalFeatureCount = 0;
        totalFeatureCountPerCluster = new int[numClusters];

        // Initialise the counting data structures
        jointCounts = new Int2IntOpenHashMap[numClusters];
        for (int i = 0; i < jointCounts.length; i++)
            jointCounts[i] = new Int2IntOpenHashMap();

        featureCounts = new Int2IntOpenHashMap();

        // Obtain feature counts, and joint counts of features per cluster
        for (ClusteredProcessedInstance instance : documents) {
            for (int clusterIndex=0; clusterIndex < numClusters; clusterIndex++){
                for (int feature : instance.getDocument().features){

                }
            }
        }
    }

    public List<String> getTopFeatures(int clusterIndex, int K, FeatureExtractionPipeline pipeline){
        return getTopFeatures(clusterIndex, K).stream()
                .map(pipeline::labelString)
                .collect(Collectors.toList());
    }

    public List<Integer> getTopFeatures(int clusterIndex, int K) {
        return new LikelihoodPriorRatioOrdering(clusterIndex).greatestOf(jointCounts[clusterIndex].keySet(), K);
    }

//    private double featurePrior(int feature){
//        return featureCounts.get(feature) / totalFeatureCount;
//    }
//
//    private double likelihoodFeatureGivenCluster(int feature, int cluster){
//        return jointCounts[cluster].get(feature) / totalFeatureCountPerCluster[cluster];
//    }

    public class LikelihoodPriorRatioOrdering extends Ordering<Integer> {

        int clusterIndex = 0;

        public LikelihoodPriorRatioOrdering(int clusterIndex) {
            this.clusterIndex = clusterIndex;
        }

        public void setClusterIndex(int clusterIndex) {
            this.clusterIndex = clusterIndex;
        }

        @Override
        public int compare(Integer feature1, Integer feature2) {

            double leftRatio = Math.log(counts.featurePrior(feature1)) - Math.log(counts.likelihoodFeatureGivenLabel(feature1, clusterIndex));
            double rightRatio = Math.log(counts.featurePrior(feature2)) - Math.log(counts.likelihoodFeatureGivenLabel(feature2, clusterIndex));

            return Double.compare(leftRatio, rightRatio);
        }
    }

}
