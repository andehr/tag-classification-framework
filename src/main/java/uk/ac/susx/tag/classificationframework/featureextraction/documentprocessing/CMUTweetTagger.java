package uk.ac.susx.tag.classificationframework.featureextraction.documentprocessing;

import cmu.arktweetnlp.Tagger;
import cmu.arktweetnlp.impl.ModelSentence;
import cmu.arktweetnlp.impl.Sentence;
import uk.ac.susx.tag.classificationframework.datastructures.AnnotatedToken;
import uk.ac.susx.tag.classificationframework.datastructures.Document;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * See DocProcessor class for the function of DocProcessors.
 *
 * This DocProcessor applies the CMU PoS tagger (assuming that
 * the document has already been tokenised).
 *
 * Functionality is included for lowercasing all tokens if
 * the tweet is a news headline. A news headline in which
 * all tokens are capitalised can confuse the PoS tagger.
 *
 * Use this functionality by using a headlineThreshold
 * greater than 0. The threshold represents the fraction
 * of tokens in a tweet which need to be capitalised
 * before considering the tweet a headline.
 *
 * User: Andrew D. Robertson
 * Date: 05/10/2013
 * Time: 11:49
 */
public class CMUTweetTagger extends DocProcessor {

    private static final long serialVersionUID = 0L;

    private double headlineThreshold = 0;

    public transient Tagger tagger;

    public CMUTweetTagger() throws IOException {
        loadTagger();
    }

    public CMUTweetTagger(double headlineThreshold) throws IOException {
        this.headlineThreshold = headlineThreshold;
        loadTagger();
    }

    private void loadTagger() throws IOException {
        tagger = new Tagger();
        tagger.loadModel("/cmu/arktweetnlp/model.20120919");
    }

    @Override
    public Document process(Document document) {
        List<String> tokens = new ArrayList<>(document.size());
        for (AnnotatedToken token : document){
            tokens.add(token.get("form"));
        }
        if (headlineThreshold > 0) {
            if ((headlineThreshold==1 && isHeadline(tokens)) || isHeadline(tokens, headlineThreshold)) {
                normaliseHeadline(tokens);
            }
        }
        List<Tagger.TaggedToken> tagPreTokenised = tagPreTokenised(tokens);
        for (int i = 0; i < tagPreTokenised.size(); i++) {
            document.get(i).put("pos", tagPreTokenised.get(i).tag);
        }
        return document;
    }

    @Override
    public String configuration() {
        return "PARAM:headlineThreshold:"+headlineThreshold;
    }

    /**
     * I'm not sure about the level of naughtiness of this code... I copied
     * it from the tokenizeAndTag method of CMU's Tagger class, because they
     * don't provide easy access to a method that just tags a pre-tokenised
     * sentence.
     *
     * It seems to work fine...
     */
    private List<Tagger.TaggedToken> tagPreTokenised(List<String> tokens){
        Sentence sentence = new Sentence();
        sentence.tokens = tokens;
        ModelSentence ms = new ModelSentence(sentence.T());
        tagger.featureExtractor.computeFeatures(sentence, ms);
        tagger.model.greedyDecode(ms, false);

        ArrayList<Tagger.TaggedToken> taggedTokens = new ArrayList<>();

        for (int t=0; t < sentence.T(); t++) {
            Tagger.TaggedToken tt = new Tagger.TaggedToken();
            tt.token = tokens.get(t);
            tt.tag = tagger.model.labelVocab.name( ms.labels[t] );
            taggedTokens.add(tt);
        }
        return taggedTokens;
    }

    private boolean isHeadline(List<String> tokens){
        for (String token : tokens){
            if (!Character.isUpperCase(token.codePointAt(0))) return false;
        }
        return true;
    }

    private boolean isHeadline(List<String> tokens, double threshold){
        int count = 0;
        for (String token : tokens){
            if (Character.isUpperCase(token.codePointAt(0))) count++;
        }
        return ((double)count) / tokens.size() >= threshold;
    }

    private void normaliseHeadline(List<String> tokens){
        for (int i=0; i < tokens.size(); i++){
            tokens.set(i, tokens.get(i).toLowerCase());
        }
    }
}
