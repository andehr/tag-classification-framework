package uk.ac.susx.tag.classificationframework.featureextraction.inference;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import uk.ac.susx.tag.classificationframework.datastructures.AnnotatedToken;
import uk.ac.susx.tag.classificationframework.datastructures.Document;
import uk.ac.susx.tag.classificationframework.featureextraction.documentprocessing.PhraseMatcher;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by Andrew D. Robertson on 07/06/2016.
 */
public class FeatureInferrerPhraseNgrams extends FeatureInferrer{

    private static final long serialVersionUID = 0L;

    public static final String FEATURE_TYPE = "phraseNgram";

    private boolean ignoreFilteredTokens = true;

    public FeatureInferrerPhraseNgrams(boolean ignoreFilteredTokens){
        this.ignoreFilteredTokens = ignoreFilteredTokens;
    }

    @Override
    public List<Feature> addInferredFeatures(Document document, List<Feature> featuresSoFar) {
        String currentMatchId = null;
        ImmutableList.Builder<String> currentMatch = ImmutableList.builder();
        for (AnnotatedToken token : document) {
            if (!isFilteredForOtherReasons(token) || !ignoreFilteredTokens) {
                String matchId = token.getOrNull(PhraseMatcher.PHRASE_MATCH);
                if (matchId == null) { // current token not in match
                    if (currentMatchId != null) { // currently in a match
                        addFeature(currentMatch.build(), featuresSoFar);
                        currentMatchId = null;
                        currentMatch = ImmutableList.builder();
                    }
                } else { // current token in match
                    if (currentMatchId != null && !currentMatchId.equals(matchId)) {
                        addFeature(currentMatch.build(), featuresSoFar);
                        currentMatch = ImmutableList.builder();
                    }
                    currentMatchId = matchId;
                    currentMatch.add(token.get("form"));
                }
            }
        }
        if (currentMatchId != null){
            addFeature(currentMatch.build(), featuresSoFar);
        }
        return featuresSoFar;
    }

    private void addFeature(List<String> phrase, List<Feature> features){
        features.add(new Feature(phrase.stream().collect(Collectors.joining("_")), FEATURE_TYPE));
    }

    private boolean isFilteredForOtherReasons(AnnotatedToken t){
        return t.isFiltered() && t.getOrNull(PhraseMatcher.PHRASE_MATCH) == null;
    }

    @Override
    public Set<String> getFeatureTypes() {
        return Sets.newHashSet(FEATURE_TYPE);
    }
}
