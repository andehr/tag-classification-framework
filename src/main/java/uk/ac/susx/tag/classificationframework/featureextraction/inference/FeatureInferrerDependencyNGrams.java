package uk.ac.susx.tag.classificationframework.featureextraction.inference;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import uk.ac.susx.tag.classificationframework.datastructures.DependencyTree;
import uk.ac.susx.tag.classificationframework.datastructures.Document;
import uk.ac.susx.tag.classificationframework.datastructures.Instance;
import uk.ac.susx.tag.classificationframework.exceptions.FeatureExtractionException;
import uk.ac.susx.tag.classificationframework.featureextraction.documentprocessing.TweetTagConverter;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.FeatureExtractionPipeline;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static uk.ac.susx.tag.classificationframework.Util.buildParsingPipeline;

/**
 * See the FeatureInferrer class for the function of FeatureInferrers.
 *
 * This inferrer uses dependency relations to generate syntactic N-grams.
 * Syntactic N-grams are tuples of words which are adjacent in terms of
 * their paths in a syntactic tree, as opposed to lexical n-grams which
 * are tuples of words which are adjacent according to their order of
 * occurrence in an utterance.
 *
 * WARNING, Assumptions:
 *      1. CMU tagger and tokeniser was used
 *      2. TweetTagConverter for token expansion was used
 *      3. TweetDependencyParser was used with stanford dependencies
 *
 * User: Andrew D. Robertson
 * Date: 07/01/2014
 * Time: 11:33
 */
public class FeatureInferrerDependencyNGrams extends FeatureInferrer {

    private static final long serialVersionUID = 0L;

    private static final String FEATURE_TYPE_NGRAM = "dependencyNgram";

    private static final String prepositionPOS = "P";
    private static final String prepositionDeprel = "prep";
    private static final String prepositionComplementDeprel = "pcomp";
    private static final String prepositionObjectDeprel = "pobj";
    private static final String punctuationPOS = ",";

    private boolean ignorePunctuation = true;
    private boolean lowercase = true;
    private boolean useUnexpandedForm = true;

    private boolean collapsePrepositions = true;
    private boolean retainUncollapsedPrepositionalNgrams = true;

    private boolean includeBigrams = true;
    private boolean includeTrigrams = true;


    public FeatureInferrerDependencyNGrams(){ }


    public FeatureInferrerDependencyNGrams(boolean ignorePunctuation,
                                           boolean includeBigrams,
                                           boolean includeTrigrams,
                                           boolean collapsePrepositions,
                                           boolean retainUncollapsedPrepositionalNgrams,
                                           boolean useUnexpandedForm,
                                           boolean lowercase) {
        this.ignorePunctuation = ignorePunctuation;
        this.includeBigrams = includeBigrams;
        this.includeTrigrams = includeTrigrams;
        this.collapsePrepositions = collapsePrepositions;
        this.retainUncollapsedPrepositionalNgrams = retainUncollapsedPrepositionalNgrams;
        this.useUnexpandedForm = useUnexpandedForm;
        this.lowercase = lowercase;
    }

    @Override
    public List<Feature> addInferredFeatures(Document document, List<Feature> featuresSoFar) {
        List<TweetTagConverter.Token> tokens = (List<TweetTagConverter.Token>)document.getAttribute("ExpandedTokens");
        if (tokens == null) throw new FeatureExtractionException("Instances must have been dependency parsed before they can have DependencyNGrams extracted.");
        if (!(includeBigrams || includeTrigrams)) return featuresSoFar; // User asked for no ngrams, so do nothing


        if (useUnexpandedForm) {
            featuresSoFar.addAll(getUnexpandedNgrams(tokens, document));
        } else {
            //TODO: currently doesn't support collapsing of prepositions in this mode
            DependencyTree dt = new DependencyTree(tokens);
            featuresSoFar.addAll(getNGrams(dt, document));
        }
        return featuresSoFar;
    }

    @Override
    public Set<String> getFeatureTypes() {
        return Sets.newHashSet(FEATURE_TYPE_NGRAM);
    }


    /**
     * Produce a mapping from token ID to the token's headIDs. In the parse of a sentence, tokens only have a single
     * head, but this parse occurs on expanded tokens. If we de-expand those tokens, it is possible that a
     * de-expanded token (which inherits the head of the expanded tokens) may inherit more than one head. So a set
     * of head IDs is produced.
     */
    private Map<Integer, Set<Integer>> resolveHeads(List<TweetTagConverter.Token> tokens, Document document){
        Map<Integer, Set<Integer>> tokenHeads = new HashMap<>();
        // Initialise sets for each Token ID (IDs are indexed from 1)
        for (int i = 1; i <= document.size(); i++)
            tokenHeads.put(i, new HashSet<Integer>());
        for (TweetTagConverter.Token token : tokens){
            // The "oldID" of an expanded token refers to the ID of the token in *document* from which this token was expanded
            // The "head" of an expanded token refers to the ID of the head of the token from the expanded tokens list (i.e. different ID space)
            // So we must get the head ID, find the expanded token that this refers to, then get the oldID of that token, to get the unexpanded head ID
            int resolvedHeadID = token.head == 0? 0 : tokens.get(token.head-1).oldID;

            // The oldID and resolvedHeadID will be the same if an expanded tokens lists a second expanded token as its head, but both tokens resolve to the same unexpanded token. We ignore these relations
            // The resolvedHeadID is 0 when the head is the root. We also ignore these relations
            if (token.oldID != resolvedHeadID && resolvedHeadID != 0) {
                // We may ignore punctuation
                if(!ignorePunctuation || !token.pos.equals(punctuationPOS)) {
                    tokenHeads.get(token.oldID).add(resolvedHeadID);
                }
            }
        }
        return tokenHeads;
    }

    /**
     * Get a set of the IDs of the tokens that are actually prepositions that can be collapsed.
     */
    private Set<Integer> getCollapsablePrepositions(List<TweetTagConverter.Token> tokens){
        Set<Integer> collapsablePrepositionIDs = new HashSet<>();

        // First, all tokens marked as prepositions (by their PoS) and that have the dependency relation indicating they are initiating as a prepositional phrase are considered candidates for being collapsable
        Set<Integer> candidates = new HashSet<>();
        for (TweetTagConverter.Token token : tokens){
            if (token.pos.equals(prepositionPOS) && (token.deprel.equals(prepositionDeprel) || token.deprel.equals(prepositionComplementDeprel))){
                candidates.add(token.oldID);
            }
        }
        // Then only those candidates which have a prepositional object or prepositional complement as a child token are considered collapsable
        for (TweetTagConverter.Token token : tokens) {
            if (token.deprel.equals(prepositionObjectDeprel) || token.deprel.equals(prepositionComplementDeprel)){
                int resolvedHeadID = token.head == 0? 0 : tokens.get(token.head-1).oldID;
                if (candidates.contains(resolvedHeadID)){
                    collapsablePrepositionIDs.add(resolvedHeadID);
                }
            }
        } return collapsablePrepositionIDs;
    }


    private List<Feature> getUnexpandedNgrams(List<TweetTagConverter.Token> tokens, Document document) {
        // Get a mapping between tokens and their heads
        Map<Integer, Set<Integer>> tokenHeads = resolveHeads(tokens, document);

        // Get a set of the ids of tokens that are collapsable prepositions
        Set<Integer> collapsablePrepositionIDs = getCollapsablePrepositions(tokens);

        // Keep track of all the unique ngrams we might want to extract (uniqueness determined by token ID not token form)
        Set<Ngram> allNgrams = new HashSet<>();

        for (int i = 1; i <= document.size(); i++) {

            // If this is a token that we shouldn't be ignoring
            if (retainUncollapsedPrepositionalNgrams || !collapsablePrepositionIDs.contains(i)
                    && (!ignorePunctuation || !document.get(i-1).get("pos").equals(punctuationPOS))){

                // A bigram is two tokens linked by a single relation (though certain relations can be collapsed)
                Set<Ngram> bigrams = new HashSet<>();

                // See "resolveHeads()" for why we may have multiple heads
                addNgrams(i, bigrams, document, tokenHeads, collapsablePrepositionIDs);

                if (includeBigrams)
                    allNgrams.addAll(bigrams);

                // Trigrams extend bigrams by finding and attaching all the bigrams from the end point of the original bigrams
                if (includeTrigrams) {
                    for (Ngram ngram : bigrams) {
                        addNgrams(ngram.getHeadID(), ngram, allNgrams, document, tokenHeads, collapsablePrepositionIDs);
                    }
                }
            }
        }
        return makeFeatures(allNgrams, document);
    }


    private void addNgrams(int tokenID, Set<Ngram> ngramsSoFar, Document document, Map<Integer, Set<Integer>> tokenHeads, Set<Integer> collapsablePrepositionIDs){
        addNgrams(tokenID, new Ngram(tokenID), ngramsSoFar, document, tokenHeads, collapsablePrepositionIDs);
    }


    private void addNgrams(int tokenID, Ngram ngram, Set<Ngram> ngramsSoFar, Document document, Map<Integer, Set<Integer>> tokenHeads, Set<Integer> collapsablePrepositionIDs) {
        for (int headID : tokenHeads.get(tokenID)) {
            if (!ignorePunctuation || !document.get(headID-1).get("pos").equals(punctuationPOS)) {
                if (!ngram.isAlreadyVisited(headID)){
                    boolean canCollapse = collapsePrepositions && collapsablePrepositionIDs.contains(headID) && !onlyHeadIsRoot(headID, tokenHeads);
                    if (!canCollapse || retainUncollapsedPrepositionalNgrams) {
                        Ngram finalNgram = new Ngram(ngram);
                        finalNgram.setDestinationID(headID);
                        ngramsSoFar.add(finalNgram);
                    }
                    if(canCollapse) {
                        Ngram grownNgram = new Ngram(ngram);
                        grownNgram.addIntermediateID(headID);
                        addNgrams(headID, grownNgram, ngramsSoFar, document, tokenHeads, collapsablePrepositionIDs);
                    }
                }
            }
        }
    }

    private boolean onlyHeadIsRoot(int tokenID, Map<Integer, Set<Integer>> tokenHeads) {
        return tokenHeads.get(tokenID).size()==0;
    }

    private List<Feature> makeFeatures(Set<Ngram> ngrams, Document document){
        List<Feature> features = new ArrayList<>(ngrams.size());
        for (Ngram ngram : ngrams) {
            List<String> forms = new ArrayList<>(ngrams.size());
            for (int i : ngram.asImmutableSortedList()) {
                forms.add(getTokenForm(i, document));
            }
            features.add(makeFeature(FEATURE_TYPE_NGRAM, forms));
        }
        return features;
    }

    private String getTokenForm(int tokenID, Document document) {
        try {
            return document.get(tokenID-1).get("form");
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Given a node in a dependency tree, recursively find all syntactic bigrams and trigrams of
     * this node and all others below this node in the syntactic tree.
     *
     * WARNING: don't call this with the root node.
     */
    private List<Feature> getNGrams(DependencyTree.Node node, Document document){

        List<Feature> ngrams = new ArrayList<>();

        if (ignorePunctuation && node.getData().deprel.equals("punct")) return ngrams;

        List<DependencyTree.Node> children = node.getChildren();

        ngrams.addAll(makeBigramsAndTrigrams(node));


        // Recurse on children
        for (DependencyTree.Node child : children) {
            ngrams.addAll(getNGrams(child, document));
        }
        return ngrams;
    }


    /**
     * Given a dependency tree, find all syntactic bigrams and trigrams of all tokens.
     */
    private List<Feature> getNGrams(DependencyTree dt, Document document) {
        List<Feature> ngrams = new ArrayList<>();

        for (DependencyTree.Node child : dt.getRoot().getChildren()) {
            ngrams.addAll(getNGrams(child, document));
        }

        return ngrams;
    }

    /**
     * Given a node in a dependency tree, make bigrams with it's children, and
     * incorporate grandchildren for trigrams. The order of the elements in the
     * ngrams are consistent with the order in which they occur in the utterance.
     */
    private List<Feature> makeBigramsAndTrigrams(DependencyTree.Node head) {

        List<Feature> ngrams = new ArrayList<>();

        for (DependencyTree.Node child : ignorePunctuation? DependencyTree.extractNodesWithoutRelation(head.getChildren(), "punct"): head.getChildren()) {

            if (includeBigrams) {
                // Order the words in the bigrams by their position in the sentence
                if (child.compareTo(head) < 0)
                    ngrams.add(makeFeature(FEATURE_TYPE_NGRAM, child.getData().form, head.getData().form));
                else
                    ngrams.add(makeFeature(FEATURE_TYPE_NGRAM, head.getData().form, child.getData().form));
            }

            if (includeTrigrams) {
                for (DependencyTree.Node grandChild : ignorePunctuation? DependencyTree.extractNodesWithoutRelation(child.getChildren(), "punct"): child.getChildren()){

                    ArrayList<DependencyTree.Node> nodes = Lists.newArrayList(grandChild, child, head);

                    // Order the nodes by their position in the sentence
                    Collections.sort(nodes);

                    ngrams.add(makeFeature(FEATURE_TYPE_NGRAM,
                            nodes.get(0).getData().form,
                            nodes.get(1).getData().form,
                            nodes.get(2).getData().form));
                }
            }
        }
        return ngrams;
    }

    private Feature makeFeature(String name, String... forms){
        StringBuilder sb = new StringBuilder();
        for (int i=0; i < forms.length-1; i++) {
            sb.append(lowercase? forms[i].toLowerCase() : forms[i]);
            sb.append("-");
        }
        sb.append(lowercase? forms[forms.length-1].toLowerCase() : forms[forms.length-1]);
        return new Feature(sb.toString(), name);
    }

    private Feature makeFeature(String name, List<String> forms){
        StringBuilder sb = new StringBuilder();
        for (int i=0; i < forms.size()-1; i++) {
            sb.append(lowercase ? forms.get(i).toLowerCase() : forms.get(i));
            sb.append("-");
        }
        sb.append(lowercase ? forms.get(forms.size() - 1).toLowerCase() : forms.get(forms.size() - 1));
        return new Feature(sb.toString(), name);
    }

    private static class Ngram {

        private int startingID;
        private Set<Integer> pathIDs;
        private int destinationID;

        public Ngram(int startingID) {
            this(startingID, new HashSet<Integer>(), 0);
        }

        public Ngram(int startingID, Set<Integer> pathIDs) {
            this(startingID, pathIDs, 0);
        }

        public Ngram(int startingID, Set<Integer> pathIDs, int destinationID) {
            this.startingID = startingID;
            this.pathIDs = new HashSet<>(pathIDs);
            this.destinationID = destinationID;
        }

        public Ngram(int startingID, int destinationID){
            this(startingID, new HashSet<Integer>(), destinationID);
        }

        public Ngram(Ngram ngram) {
            this(ngram.startingID);
            for (int i : ngram.pathIDs) {
                this.pathIDs.add(i);
            }
            if (ngram.destinationID != 0)
                this.pathIDs.add(ngram.destinationID);
        }

        public Ngram(int... ids) {
            if (ids.length < 2) throw new RuntimeException("No starting point AND/OR no destination point");

            if (ids.length == 2) {
                startingID = ids[0];
                pathIDs = new HashSet<>();
                destinationID = ids[1];
            } else {
                startingID = ids[0];
                destinationID = ids[ids.length-1];
                pathIDs = new HashSet<>();
                for (int i = 1; i < ids.length-1; i++)
                    pathIDs.add(ids[i]);
            }
        }

        public boolean isAlreadyVisited(int id){
            return pathIDs.contains(id) || id == startingID || id == destinationID;
        }


        public int getHeadID(){
            return destinationID;
        }

        public int getStartingID(){
            return startingID;
        }

        public void addIntermediateID(int id) {
            if (isAlreadyVisited(id)) throw new RuntimeException("Attempting to represent a cycle");
            pathIDs.add(id);
        }

        public void setDestinationID(int id) {
            if (isAlreadyVisited(id)) throw new RuntimeException("Attempting to represent a cycle");
            destinationID = id;
        }

        public ImmutableList<Integer> asImmutableSortedList() {
            if (destinationID == 0) throw new RuntimeException("Destination is root / unassigned");

            List<Integer> items = new ArrayList<>(pathIDs.size()+1);
            items.add(startingID);
            items.addAll(pathIDs);
            items.add(destinationID);

            Collections.sort(items);

            return ImmutableList.<Integer>builder().addAll(items).build();
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (o== null || getClass() != o.getClass()) return false;

            Ngram ngram = (Ngram)o;

            return asImmutableSortedList().equals(ngram.asImmutableSortedList());
        }

        public int hashCode(){
            return asImmutableSortedList().hashCode();
        }

        public String toString() {
            return asImmutableSortedList().toString();
        }
    }


    // TESTING
    public static void main(String[] args) throws IOException {

        FeatureExtractionPipeline pipeline = buildParsingPipeline(true, true);
//        String exampleSentence = "Economic news have little effect on financial markets.";
//        String exampleSentence = "I'm gonna go!";
        String exampleSentence = "The badger'll never kill him before me";
//        String exampleSentence = "He came out from under the bed.";
        Document document = pipeline.processDocumentWithoutCache(new Instance("", exampleSentence, ""));

        FeatureInferrerDependencyNGrams d = new FeatureInferrerDependencyNGrams();
//        DependencyTree dt = new DependencyTree((List<TweetTagConverter.Token>)document.getAttribute("ExpandedTokens"));


        List<Feature> ngrams = d.getUnexpandedNgrams((List<TweetTagConverter.Token>) document.getAttribute("ExpandedTokens"),
                document);
//        d.resolveHeads((List<TweetTagConverter.Token>)document.getAttribute("ExpandedTokens"), document);
//        for (Feature s : d.getNGrams(dt, document)) {
//            System.out.println(s);
//        }


        for(Feature f : ngrams){
            System.out.println(f.value());
        }


        System.out.println("---- Done.");
    }

    @Override
    public boolean isThreadSafe() {
        return true;
    }

}
