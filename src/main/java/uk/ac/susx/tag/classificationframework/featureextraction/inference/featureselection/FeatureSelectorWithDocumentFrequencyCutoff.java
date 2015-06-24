package uk.ac.susx.tag.classificationframework.featureextraction.inference.featureselection;

import java.util.HashSet;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: Andrew D. Robertson
 * Date: 24/06/2015
 * Time: 15:34
 */
public abstract class FeatureSelectorWithDocumentFrequencyCutoff extends FeatureSelector {

    private static final long serialVersionUID = 0L;

    public FeatureSelectorWithDocumentFrequencyCutoff() {
        super();
    }

    public FeatureSelectorWithDocumentFrequencyCutoff(Set<String> selectedFeatureTypes) {
        super(selectedFeatureTypes);
    }

    public abstract void setDocumentFrequencyCutoff(int cutoff);
}
