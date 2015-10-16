package uk.ac.susx.tag.classificationframework.clusters.clusteranalysis;

import com.google.common.collect.Ordering;
import org.apache.commons.math.stat.clustering.Cluster;
import uk.ac.susx.tag.classificationframework.clusters.ClusteredProcessedInstance;

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

    public TopDocuments(int clusterIndex, List<ClusteredProcessedInstance> documents) {
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
                topDocumentsPerCluster[i] = new TopDocuments(i, ordering.greatestOf(docs, K));
            }
            return topDocumentsPerCluster;
        }
        return null;
    }

    public static void main(String[] args) {

        // Have some collection of clustered documents
        List<ClusteredProcessedInstance> clusteredDocs = new ArrayList<>();

        // Get top K documents for each cluster where the cluster vectors represent probabilities of membership within each cluster
        TopDocuments[] topKDocumentsPerCluster = topKDocumentsPerCluster(clusteredDocs, 100, new OrderingOverMembershipProbabilities());
    }
}
