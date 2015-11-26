package uk.ac.susx.tag.classificationframework.clusters.clusteranalysis;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import uk.ac.susx.tag.classificationframework.Util;
import uk.ac.susx.tag.classificationframework.clusters.ClusteredProcessedInstance;
import uk.ac.susx.tag.classificationframework.datastructures.Instance;
import uk.ac.susx.tag.classificationframework.datastructures.ProcessedInstance;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.FeatureExtractionPipeline;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.PipelineBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Represents the top documents for a given cluster. Static method present for looking through
 * collection of ClusteredProcessedInstances and making an array of TopDocuments for each cluster.
 *
 * TopDocuments are found by using a definition of Ordering of documents within a cluster.
 *
 * A custom ordering can be used, but there are two defaults.
 *
 * 1. The cluster vector is a vector of probabilities of membership within the clusters (OrderingOverMembershipProbabilities)
 * 2. The cluster vector is a vector of distances from the centroids of the clusters (OrderingOverDistances)
 *
 * See topKDocumentsPerCluster() and the main method for usage.
 *
 * User: Andrew D. Robertson
 * Date: 14/10/2015
 * Time: 14:37
 */
public class TopDocuments {

    public int clusterIndex;
    public List<ClusteredProcessedInstance> documents;

    private TopDocuments(int clusterIndex, List<ClusteredProcessedInstance> documents) {
        this.clusterIndex = clusterIndex;
        this.documents = documents;
    }

    public abstract static class DocumentOrderingPerCluster extends Ordering<ClusteredProcessedInstance> {

        // This is the index of the cluster that the documents are currently being ordered with respect to.
        protected int clusterIndex;
        public DocumentOrderingPerCluster() {
            this.clusterIndex = 0;
        }
        public void setClusterIndex(int index){ clusterIndex = index; }
    }

    public static class OrderingOverMembershipProbabilities extends DocumentOrderingPerCluster {

        /**
         * Sorts such that those documents come first whose probability of cluster membership is greatest.
         */
        @Override
        public int compare(ClusteredProcessedInstance left, ClusteredProcessedInstance right) {
            return Double.compare(right.getClusterVector()[clusterIndex], left.getClusterVector()[clusterIndex]);
        }
    }

    public static class OrderingOverDistances extends DocumentOrderingPerCluster {

        /**
         * Sorts such that those documents come first whose distance from the cluster centroid is least.
         */
        @Override
        public int compare(ClusteredProcessedInstance left, ClusteredProcessedInstance right) {
            return Double.compare(left.getClusterVector()[clusterIndex], right.getClusterVector()[clusterIndex]);
        }
    }

    public static TopDocuments[] topKDocumentsPerCluster(Collection<ClusteredProcessedInstance> docs, int K){
        return topKDocumentsPerCluster(docs, K, new OrderingOverMembershipProbabilities());
    }

    public static TopDocuments[] topKDocumentsPerCluster(Collection<ClusteredProcessedInstance> docs , int K, DocumentOrderingPerCluster ordering){
        if (!docs.isEmpty()) {
            int n = docs.iterator().next().getClusterVector().length;
            TopDocuments[] topDocumentsPerCluster = new TopDocuments[n];
            for (int i = 0; i < n; i++){
                ordering.setClusterIndex(i);
                topDocumentsPerCluster[i] = new TopDocuments(i, ordering.leastOf(docs, K));
            }
            return topDocumentsPerCluster;
        }
        return null;
    }

    public static void main(String[] args) {

        FeatureExtractionPipeline pipeline = new PipelineBuilder().build(new PipelineBuilder.OptionList()
                        .add("tokeniser", ImmutableMap.of(
                                        "type", "cmuTokeniseOnly",
                                        "filter_punctuation", true,
                                        "normalise_urls", true,
                                        "lower_case", true
                                )
                        )
                        .add("unigrams", true)
        );

        System.out.println(pipeline.featureIndex("#crap"));
        System.out.println(pipeline.featureIndex("#good"));
        System.out.println(pipeline.featureIndex("#bad"));
//        System.out.println(pipeline.featureIndex("#turd"));

        // Have some collection of clustered documents
        List<ClusteredProcessedInstance> clusteredDocs = new ArrayList<>();

        clusteredDocs.add(new ClusteredProcessedInstance(new ProcessedInstance(0, new int[]{0, 1, 1, 2}, new Instance("", "test2", "")), new double[]{0.8, 0.2}));
        clusteredDocs.add(new ClusteredProcessedInstance(new ProcessedInstance(0, new int[]{0, 1},    new Instance("", "test1", "")), new double[]{0.2, 0.8}));
//        clusteredDocs.add(new ClusteredProcessedInstance(new ProcessedInstance(0, new int[]{1, 0, 0}, new Instance("", "test3", "")), new double[]{0.7, 0.3}));

        List<Instance> backgroundDocs = new ArrayList<>();

//        backgroundDocs.add(new ClusteredProcessedInstance(new ProcessedInstance(0, new int[]{0, 1, 2, 2, 2, 3}, new Instance("", "test2", "")), new double[]{0.8, 0.2}));
//        backgroundDocs.add(new ClusteredProcessedInstance(new ProcessedInstance(0, new int[]{3, 4, 5, 6, 6},    new Instance("", "test1", "")), new double[]{0.2, 0.8}));
//        backgroundDocs.add(new ClusteredProcessedInstance(new ProcessedInstance(0, new int[]{3, 4, 2, 1, 6, 0}, new Instance("", "test3", "")), new double[]{0.5, 0.6}));


        backgroundDocs.add(new Instance("", "good bad excellent", ""));

        ClusterFeatureAnalysis a = new ClusterFeatureAnalysis(clusteredDocs, backgroundDocs, pipeline, new FeatureClusterJointCounter.FeatureBasedCounts(), new FeatureClusterJointCounter.HighestProbabilityOnly(), 0, 1);
//        a.getCounts().pruneFeaturesWithCountLessThan(3);


        // Get top K documents for each cluster where the cluster vectors represent probabilities of membership within each cluster
        TopDocuments[] topKDocumentsPerCluster = topKDocumentsPerCluster(clusteredDocs, 2, new OrderingOverMembershipProbabilities());

        for (ClusteredProcessedInstance c : topKDocumentsPerCluster[0].documents){
            System.out.println(c.getDocument().source.text);
        }
    }
}
