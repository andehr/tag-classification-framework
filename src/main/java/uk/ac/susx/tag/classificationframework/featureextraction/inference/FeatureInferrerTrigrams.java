package uk.ac.susx.tag.classificationframework.featureextraction.inference;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import uk.ac.susx.tag.classificationframework.datastructures.AnnotatedToken;
import uk.ac.susx.tag.classificationframework.datastructures.Document;

import java.util.List;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: Andrew D. Robertson
 * Date: 24/06/2014
 * Time: 11:23
 */
public class FeatureInferrerTrigrams extends FeatureInferrer{

    private static final long serialVersionUID = 0L;

    private static final String FEATURE_TYPE_TRIGRAM = "trigram";

    private boolean includeFilteredTokens;
    private FeatureInferrerBigrams.PunctuationChecker puncChecker;

    public FeatureInferrerTrigrams(boolean includeFilteredTokens,
                                   FeatureInferrerBigrams.PunctuationChecker p){

        this.includeFilteredTokens = includeFilteredTokens;
        this.puncChecker = p;
    }

    public FeatureInferrerTrigrams() {
        this(false, new FeatureInferrerBigrams.PunctuationCheckerByRegex("[!?\"#$%&'()*+,-./:;<=>@\\[\\]^_`{|}~]+"));
    }


    @Override
    public List<Feature> addInferredFeatures(Document document, List<Feature> featuresSoFar) {
        AnnotatedToken tokenN1 = null;
        AnnotatedToken tokenN2 = null;

        for (AnnotatedToken token : document){
            if (!token.isFiltered() || includeFilteredTokens){
                if(puncChecker == null || !puncChecker.isPunctuation(token)){
                    if ((tokenN1 != null && tokenN2 != null)){
                        Feature feature = new Feature(makeTrigram(tokenN2.get("form"), tokenN1.get("form"), token.get("form")), FEATURE_TYPE_TRIGRAM);
                        feature.attributes = ImmutableMap.of("start", tokenN2, "end", token.end());
                        featuresSoFar.add(feature);
                    }
                    tokenN2 = tokenN1;
                    tokenN1 = token;
                }
            }
        }
        return featuresSoFar;
    }

    @Override
    public Set<String> getFeatureTypes() {
        return Sets.newHashSet(FEATURE_TYPE_TRIGRAM);
    }

    private String makeTrigram(String token1, String token2, String token3){
        return token1 + "_" + token2 + "_" + token3;
    }

    @Override
    public boolean isThreadSafe() {
        return true;
    }
}
