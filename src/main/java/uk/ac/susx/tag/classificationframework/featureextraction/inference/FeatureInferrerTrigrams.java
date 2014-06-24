package uk.ac.susx.tag.classificationframework.featureextraction.inference;

import uk.ac.susx.tag.classificationframework.Util;
import uk.ac.susx.tag.classificationframework.datastructures.AnnotatedToken;
import uk.ac.susx.tag.classificationframework.datastructures.Document;
import uk.ac.susx.tag.classificationframework.datastructures.Instance;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.FeatureExtractionPipeline;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Andrew D. Robertson
 * Date: 24/06/2014
 * Time: 11:23
 */
public class FeatureInferrerTrigrams extends FeatureInferrer{

    private static final long serialVersionUID = 0L;

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
        String tokenN1 = null;
        String tokenN2 = null;

        for (AnnotatedToken token : document){
            if (!token.isFiltered() || includeFilteredTokens){
                if(puncChecker == null || !puncChecker.isPunctuation(token)){
                    if ((tokenN1 != null && tokenN2 != null)){
                        featuresSoFar.add(new Feature(makeTrigram(tokenN2, tokenN1, token.get("form")), "trigram"));
                    }
                    tokenN2 = tokenN1;
                    tokenN1 = token.get("form");
                }
            }
        }
        return featuresSoFar;
    }

    private String makeTrigram(String token1, String token2, String token3){
        return token1 + "_" + token2 + "_" + token3;
    }
}
