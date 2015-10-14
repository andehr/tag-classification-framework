package uk.ac.susx.tag.classificationframework.clusters;

import uk.ac.susx.tag.classificationframework.datastructures.ProcessedInstance;

/**
 * Represents a ProcessedInstance that has been clustered in some fashion.
 * User: Andrew D. Robertson
 * Date: 14/10/2015
 * Time: 12:59
 */
public class ClusteredProcessedInstance {

    private ProcessedInstance document;
    private double[] clusterVector;

    public ClusteredProcessedInstance(ProcessedInstance document, double[] clusterVector) {
        this.document = document;
        this.clusterVector = clusterVector;
    }

    public ProcessedInstance getDocument() {  return document; }
    public void setDocument(ProcessedInstance document) { this.document = document;}
    public double[] getClusterVector() { return clusterVector; }
    public void setClusterVector(double[] clusterVector) { this.clusterVector = clusterVector; }
}
