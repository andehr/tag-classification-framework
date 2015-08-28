package uk.ac.susx.tag.classificationframework.featureextraction.pipelines;

import uk.ac.susx.tag.classificationframework.datastructures.Instance;
import uk.ac.susx.tag.classificationframework.datastructures.ProcessedInstance;
import uk.ac.susx.tag.classificationframework.featureextraction.inference.FeatureInferrer;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Andrew D. Robertson
 * Date: 28/08/2015
 * Time: 15:39
 */
public abstract class DataBackedComponent {

    public abstract void update(Iterable<Datum> data, FeatureExtractionPipeline pipeline);


    public static class Datum {

        public boolean handLabelled;
        public List<FeatureInferrer.Feature> features;
        public Instance instance;
        public ProcessedInstance processedInstance;

        private Datum(boolean handLabelled){
            this.handLabelled = handLabelled;
        }

        public boolean isHandLabelled(){   return handLabelled;   }


        public Datum createHandLabelled(ProcessedInstance oldProcessedInstance, FeatureExtractionPipeline pipeline){
            return new Datum(true);

        }

        public Datum createMachineLabelled(ProcessedInstance oldProcessedInstance, FeatureExtractionPipeline pipeline){
            return new Datum(false);
            // Update the label of the source instance with machine label
            // Run through pipeline, get new features
            // Get the new processed instance and keep track of it. Pass the old labelling over.
        }
    }

    public Iterable<Datum> iterableOverUpdatedData(Iterable<ProcessedInstance> handLabelledData, Iterable<ProcessedInstance> machineLabelledData) {

    }
}
