package uk.ac.susx.tag.classificationframework.featureextraction.pipelines;

/**
 * Created with IntelliJ IDEA.
 * User: Andrew D. Robertson
 * Date: 28/08/2015
 * Time: 15:39
 */
public interface DataDrivenComponent {

    public void update(FeatureExtractionPipeline.Data data);
}
