package uk.ac.susx.tag.classificationframework.featureextraction.pipelines;

import com.google.common.collect.Iterables;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import uk.ac.susx.tag.classificationframework.datastructures.Instance;
import uk.ac.susx.tag.classificationframework.datastructures.ProcessedInstance;
import uk.ac.susx.tag.classificationframework.featureextraction.inference.FeatureInferrer;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Created with IntelliJ IDEA.
 * User: Andrew D. Robertson
 * Date: 28/08/2015
 * Time: 15:39
 */
public interface DataDrivenComponent {

    public void update(FeatureExtractionPipeline.Data data);
}
