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

    public ClusterFeatureAnalysis(Collection<ClusteredProcessedInstance> documents, ClusterMembershipFunction f) {
        int numClusters = documents.iterator().next().getClusterVector().length;

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

    public interface ClusterMembershipFunction {
        boolean isDocumentInCluster(ClusteredProcessedInstance instance, int clusterIndex);
    }

}
