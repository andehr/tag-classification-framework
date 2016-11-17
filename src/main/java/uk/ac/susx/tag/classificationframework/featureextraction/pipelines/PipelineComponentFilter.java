package uk.ac.susx.tag.classificationframework.featureextraction.pipelines;

/**
 * Created by Andrew D. Robertson on 17/11/16.
 */
public interface PipelineComponentFilter<C extends PipelineComponent> {
    /**
     * Return true if component should be allowed through the filter.
     */
    boolean filter(C component);
}
