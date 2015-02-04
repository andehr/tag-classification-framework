package uk.ac.susx.tag.classificationframework.featureextraction.inference;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import uk.ac.susx.tag.classificationframework.datastructures.AnnotatedToken;
import uk.ac.susx.tag.classificationframework.datastructures.Document;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This Inferrer extracts pre-defined Ngrams of any N.
 *
 * If you decide you want to extract the feature Ngrams "president_obama_says" and "itsy_bitsy_spider_sat_down_beside_her"
 * without having to include all 3-grams and 6-grams, this inferrer is for you.
 *
 * Simply supply a set of your custom Ngrams to the constructor, and they will be looked for in the documents passed
 * through this inferrer.
 *
 * NOTE: ensure that you separate terms with "_"
 *
 * User: Andrew D. Robertson
 * Date: 04/02/2015
 * Time: 13:57
 */
public class FeatureInferrerCustomNgrams extends FeatureInferrer {

    private static final long serialVersionUID = 0L;

    private static final String FEATURE_TYPE_CUSTOM_NGRAM = "customNgram";

    private boolean includeFilteredTokens;
    private Trie ngramTrie;

    public FeatureInferrerCustomNgrams(Collection<String> customNgrams, boolean includeFilteredTokens){
        this.includeFilteredTokens = includeFilteredTokens;
        ngramTrie = new Trie(customNgrams);
    }

    public FeatureInferrerCustomNgrams(Set<String> customNgrams){
        this(customNgrams, false);
    }

    @Override
    public List<Feature> addInferredFeatures(Document document, List<Feature> featuresSoFar) {
        List<String> tokens = new ArrayList<>();
        for (AnnotatedToken token : document) {
            if (!token.isFiltered() || includeFilteredTokens) {
                tokens.add(token.get("form"));
            }
        }
        for (String ngram : ngramTrie.longestNgramsPresent(tokens)){
            featuresSoFar.add(new Feature(ngram, FEATURE_TYPE_CUSTOM_NGRAM));
        }
        return featuresSoFar;
    }

    @Override
    public Set<String> getFeatureTypes() {
        return Sets.newHashSet(FEATURE_TYPE_CUSTOM_NGRAM);
    }

    public static class Trie {

        private static final long serialVersionUID = 0L;

        public TrieNode root;

        public Trie(Collection<String> ngrams){
            root = new TrieNode("", null);
            for (String ngram : ngrams)
                addNgram(ngram);
        }

        public void addNgram(String ngram){
            String[] data = ngram.split("_");
            TrieNode currentNode = root;
            for (String datum : data){
                currentNode = currentNode.addAndGetChild(datum);
            }
        }

        public Set<String> longestNgramsPresent(List<String> tokens){
            Set<String> ngrams = new HashSet<>();
            TrieNode current = root;

            int i = 0;
            int start = 0; // Allows us to keep track of where we started the currently matching Ngram, allowing us to go back to 1 word after the start and check for a different match

            while (i < tokens.size()){
                String token = tokens.get(i);
                if (current.hasChild(token)){
                    if (current.isRoot())
                        start = i;
                    current = current.getChild(token);
                } else if (!current.isRoot()) {
                    i = start;
                    current = root;
                } else {
                    start = i;
                }
                if (!current.hasChildren() && !current.isRoot()){
                    ngrams.add(current.toString());
                    current = root;
                    i = start;
                }
                i++;
            }
            return ngrams;
        }

    }

    public static class TrieNode {

        private static final long serialVersionUID = 0L;

        public Map<String, TrieNode> children;
        public TrieNode parent;
        public String data;

        public TrieNode(String data, TrieNode parent){
            this.data = data;
            this.parent = parent;
            children = new HashMap<>();
        }

        public Collection<TrieNode> getChildren(){
            return children.values();
        }

        public boolean isRoot(){
            return !hasParent();
        }

        public boolean hasParent(){
            return parent != null;
        }

        public boolean hasChildren(){
            return children.size() > 0;
        }

        public boolean hasChild(String data){
            return children.containsKey(data);
        }

        public TrieNode getChild(String data){
            return children.get(data);
        }

        public void addChild(String data){
            if (!children.containsKey(data))
                children.put(data, new TrieNode(data, this));
        }

        public TrieNode addAndGetChild(String data){
            if (!children.containsKey(data))
                children.put(data, new TrieNode(data, this));
            return children.get(data);
        }

        public String toString() {
            List<String> allData = new ArrayList<>();
            TrieNode current = this;
            while (current.hasParent()){
                allData.add(current.data);
                current = current.parent;
            }
            return Joiner.on("_").join(Lists.reverse(allData));
        }
    }
}
