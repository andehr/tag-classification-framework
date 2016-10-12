package uk.ac.susx.tag.classificationframework.datastructures;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import uk.ac.susx.tag.classificationframework.featureextraction.filtering.TokenFilterRelevanceStopwords;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.FeatureExtractionPipeline;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Used for counting ngram occurrences centred on a word of interest.
 *
 * User: Andrew D. Robertson
 * Date: 10/11/2015
 * Time: 11:53
 */
public class RootedNgramCounter<N> {

    private Node root;

    private int minN;
    private int maxN;

    private double minLeafPruningThreshold;
    private int minimumNgramCount;
    private int level1NgramCount;
    private int level2NgramCount;
    private int level3NgramCount;

    private boolean pruned = false;

    private Set<N> stopwords;


    /**
     * Build a counter.
     *
     * Make successive calls to addContext() to count occurrences of phrases.
     *
     * Then finish with a call to topNGrams() to prune the tree of junk, and get the top ngrams. This
     * is not reversible, and one must recount to use different settings.
     *
     * @param root The item of interest, whose contexts we shall be counting.
     * @param minN The minimum size ngrams that we'll be interested in.
     * @param maxN The maximum size ngrams that we'll be interested in.
     * @param minLeafPruningThreshold Each n+1gram appeared a fraction of the time that its parent ngram occurred.
     *                                This is the minimum threshold on that fraction that the n+1gram must have
     *                                occurred to be considered a part of the phrase. The actual threshold could
     *                                be higher based on the other parameters.
     * @param minimumNgramCount       An ngram must have occurred at least this many times to be considered at all.
     * @param level1NgramCount        When the occurrences of an n+1gram's parent ngram is less than this threshold
     *                                the n+1gram must have occurred 100% of these times to be considered.
     * @param level2NgramCount        When the occurrences of an n+1gram's parent ngram is less than this threshold
     *                                the n+1gram must have occurred more than 75% of these times to be considered
     * @param level3NgramCount        When the occurrences of an n+1gram's parent ngram is less than this threshold
     *                                the n+1gram must have occurred more than 50% of these times to be considered
     * @param stopwords               A set of stopwords; when frequency does not differentiate between ngrams, the
     *                                number of stopwords an ngram contains, or whether or not the ngram ends with a
     *                                stopword can be used to pick more interesting ngrams.
     *
     * (The levels are evaluated in order 1-3, the first one that is true is applied).
     *
     * Otherwise, the threshold will be: max(1/choices, minLeafPruningThreshold), where "choices" is the number of
     * alternative n+1grams for this parent ngram.
     */
    public RootedNgramCounter(N root,
                              int minN,
                              int maxN,
                              double minLeafPruningThreshold,
                              int minimumNgramCount,
                              int level1NgramCount,
                              int level2NgramCount,
                              int level3NgramCount,
                              Set<N> stopwords) {

        this.root = new Node(null, newNullArc(root));
        this.minN = minN;
        this.maxN = maxN;

        this.minLeafPruningThreshold = minLeafPruningThreshold;
        this.minimumNgramCount = minimumNgramCount;

        this.level1NgramCount = level1NgramCount;
        this.level2NgramCount = level2NgramCount;
        this.level3NgramCount = level3NgramCount;

        this.stopwords = stopwords==null? new HashSet<>() : stopwords;
    }

    /**
     * Mostly sensible defaults, though be sure to use setStopwords() to assign stopwords to help its choice between
     * ngrams.
     */
    public RootedNgramCounter(N root){
        this(root,
             1, 6,  // min,max phrase length
             0.2,   // min pruning threshold
             4,     // min count for ngram
             5, 7, 15, // occurrence thresholds
             new HashSet<>());
    }

    public boolean isRootToken(N token){
        return token.equals(root.latestTokenForm());
    }

    public void setStopwords(Set<N> stopwords){
        this.stopwords = stopwords==null? new HashSet<>() : stopwords;
    }

    public N getRootToken() { return root.latestTokenForm(); }

    public void setMinN(int minN) {
        this.minN = minN;
    }

    public void setMaxN(int maxN) {
        this.maxN = maxN;
    }

    public void setMinLeafPruningThreshold(double minLeafPruningThreshold) {
        this.minLeafPruningThreshold = minLeafPruningThreshold;
    }

    public void setMinimumNgramCount(int minimumNgramCount) {
        this.minimumNgramCount = minimumNgramCount;
    }

    public void setLevel1NgramCount(int level1NgramCount) {
        this.level1NgramCount = level1NgramCount;
    }

    public void setLevel2NgramCount(int level2NgramCount) {
        this.level2NgramCount = level2NgramCount;
    }

    public void setLevel3NgramCount(int level3NgramCount) {
        this.level3NgramCount = level3NgramCount;
    }

    public int getIndexOfRootToken(List<N> context){
        return context.indexOf(root.latestTokenForm());
    }

    /**
     * Get an array of indices at which the root token occurs in a given context.
     */
    public int[] getIndicesOfRootTokenOccurrences(List<N> context){
        return IntStream.range(0, context.size())
                .filter(i -> context.get(i).equals(root.latestTokenForm()))
                .toArray();
    }

    public void print(FeatureExtractionPipeline pipeline ){
        root.print(pipeline);
    }

    public void print() {root.print(null);}

    public Node copyTrie(){
        return root.copy(null, root.toParent);
    }

    /**
     * Count up the contexts of the root token.
     *
     * @param context Context containing 0 or more instances of the root token
     * @param count the number of counts to assign for this instances (usually 1 unless you wanna upweight this example).
     */
    public void addContext(List<N> context, int count){
        int[] indicesOfRoot = getIndicesOfRootTokenOccurrences(context);

        if(indicesOfRoot.length == 0) return;

        for (int indexOfRoot : indicesOfRoot){

            root.incCount(count);

            Node currentNode = root;
            Node lastBeforeNode = root;

            List<N> beforeTokens = Lists.reverse(context.subList(Math.max(indexOfRoot-maxN+1, 0), indexOfRoot));
            List<N> afterTokens = indexOfRoot==context.size()-1? new ArrayList<>() : context.subList(indexOfRoot+1, Math.min(indexOfRoot+maxN-1, context.size()));

            // Make a phrase starting from root
            for (N tokenAfter : afterTokens){
                currentNode = currentNode.incForwardChild(tokenAfter, count);
            }
            // Make a phrase starting from 1...n before the root node
            for (int i = 0; i < beforeTokens.size(); i++) {
                N tokenBefore = beforeTokens.get(i);
                currentNode = lastBeforeNode.incReverseChild(tokenBefore, count);
                lastBeforeNode = currentNode;

                for (int j = 0; j < Math.min(afterTokens.size(), maxN - (i+2)); j++) {
                    N tokenAfter = afterTokens.get(j);
                    currentNode = currentNode.incForwardChild(tokenAfter, count);
                }
            }
        }
    }

    public void addContext(List<N> context){
        addContext(context, 1);
    }

    /**
     * The first time this method is called, the tree will be pruned according to the parameters given.
     * Currently this is irreversible. Then the top ngrams are found and returned.
     *
     * All future calls to this method for this counter will use the pruned tree..
     *
     * @param K The number of ngrams to attempt to find (maybe 0 if none match the criteria)
     * @return the ngrams found
     */
    public List<List<N>> topNgrams(int K) {
        if (!pruned) {
            root.recursivelyPruneChildren();
            pruned = true;
        }

        List<Node> topNodes = new LowestCommonAncestorDifferenceOrdering().greatestOf(root.getLeafNodes(), K);

        List<List<N>> topNgrams = topNodes.stream()
                .map(Node::getNgram)
                .filter(n -> n.size() >= minN)
                .collect(Collectors.toList());

        if (topNgrams.isEmpty() && minN <= 1){
            topNgrams = new ArrayList<>();
            topNgrams.add(Lists.newArrayList(root.latestTokenForm()));
        }

        return topNgrams;
    }

    /**
     * Does the same as topNgrams() except but by making a copy of the node structure
     * before pruning the tree, thus preserving the original structure. So this can
     * be repeatedly called, changing the parameters of the trimming in between.
     */
    public List<List<N>> topNgramsWithCopy(int K){
        Node newRoot = copyTrie();
        newRoot.recursivelyPruneChildren();
        List<Node> topNodes = new LowestCommonAncestorDifferenceOrdering().greatestOf(newRoot.getLeafNodes(), K);
        List<List<N>> topNgrams = topNodes.stream()
                .map(Node::getNgram)
                .filter(n -> n.size() >= minN)
                .collect(Collectors.toList());
        if (topNgrams.isEmpty() && minN <= 1){
            topNgrams = new ArrayList<>();
            topNgrams.add(Lists.newArrayList(newRoot.latestTokenForm()));
        }
        return topNgrams;
    }

    private class LowestCommonAncestorDifferenceOrdering extends Ordering<Node>{
        @Override
        public int compare(Node left, Node right) {
            return lowestCommonAncestorDifferenceExcludingSelf(left, right);
        }
    }

    /**
     * Lowest common ancestor excluding a and b (therefore makes the most sense using only leaf nodes).
     */
    private int lowestCommonAncestorDifferenceExcludingSelf(Node a, Node b) {
        Map<Node, Integer> ancestorsOfA = a.getAncestorsAsMap();

        for (AncestorNode ancestor : b.getAncestorsAsIterable()){
            if (ancestorsOfA.containsKey(ancestor.node)){
                int diff = ancestorsOfA.get(ancestor.node) - ancestor.childCount;
                if (diff == 0){
                    diff = b.getStopwordCount() - a.getStopwordCount(); // opposite way around since more stopwords means less favourable (unlike child count)
                }
                if (diff == 0){
                    if (a.endsWithStopword() && !b.endsWithStopword()){
                        diff = -1;
                    } else {
                        diff = !a.endsWithStopword() && b.endsWithStopword() ? 1 : 0;
                    }
                }
                return diff;
            }
        }

        throw new RuntimeException("This shouldn't be possible... The root node at least should always be a common ancestor, but none were found.");
    }

    /**
     * These are used when returning ancestors of a Node.
     * The count is the count of the immediate child from which we found this ancestor.
     * Hashing/Equals is based on the inner node, so can be compared as if it is a normal node.
     */
    public class AncestorNode {
        public Node node;
        public int childCount;

        public AncestorNode(Node node, int childCount) {
            this.node = node;
            this.childCount = childCount;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            AncestorNode that = (AncestorNode) o;

            if (node != null ? !node.equals(that.node) : that.node != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return node != null ? node.hashCode() : 0;
        }

        public String toString() {
            return "AncestorNode{" + "node=" + node + ", childCount=" + childCount + '}';
        }
    }


    public class Node {

        public Node parent;
        public Arc toParent;
        public int treeDepth;

        public Map<Arc, Node> children;

        public int count;

        public Node(Node parent, Arc toParent){  this(parent, toParent, 0); }
        public Node(Node parent, Arc toParent, int count) {
            this.parent = parent;
            this.toParent = toParent;
            children = new HashMap<>();
            this.count = count;
            treeDepth = parent == null? 0 : parent.treeDepth + 1;
        }

        public int getStopwordCount() {
            return (int) getNgram().stream()
               .filter(stopwords::contains)
               .count();
        }

        public Node copy(Node copyOfParent, Arc arcToParent){
            Node copy = new Node(copyOfParent, arcToParent);
            copy.treeDepth = treeDepth;
            copy.count = count;
            for (Map.Entry<Arc, Node> entry : children.entrySet()){
                Arc arc = entry.getKey();
                Node child = entry.getValue();
                copy.children.put(arc, child.copy(copy, arc));
            }
            return copy;
        }

        public boolean endsWithStopword(){
            return stopwords.contains(latestTokenForm());
        }

        public int getTreeDepth() { return treeDepth;  }
        public boolean isRoot() { return parent==null || toParent.isNullArc(); }
        public boolean hasParent() { return parent != null; }
        public boolean hasChildren() { return children.size() > 0; }
        public N latestTokenForm() {  return toParent.form; }
        public boolean hasChild(Arc child){ return children.containsKey(child); }
        public boolean hasForwardChild(N form) {
            return hasChild(newForwardArc(form));
        }
        public boolean hasReverseChild(N form){
            return hasChild(newReverseArc(form));
        }
        public Node getChild(Arc child) { return children.get(child); }

        public Node getAndAddForwardChildIfNotPresent(N form, int initialCount){
            return addIfNotPresent(newForwardArc(form), initialCount);
        }
        public Node getAndAddReverseChildIfNotPresent(N form, int initialCount){
            return addIfNotPresent(newReverseArc(form), initialCount);
        }
        public Node addIfNotPresent(Arc a, int initialCount) {
            if (hasChild(a)){
                return getChild(a);
            } else {
                return addChild(a, initialCount);
            }
        }

        public Collection<Node> getChildrenWithinCountProportion(double proportion){
            if (count == 0)
                return children.values();
            else
                return children.entrySet().stream()
                        .filter(entry -> entry.getValue().count / count >= proportion)
                        .map(Map.Entry::getValue).collect(Collectors.toList());
        }

        public void recursivelyPruneChildren(){

            // Remove all children with zero count
            for (Iterator<Map.Entry<Arc, Node>> it = children.entrySet().iterator(); it.hasNext();){
                if(it.next().getValue().count == 0){
                    it.remove();
                }
            }
            // If children remain
            if (!children.isEmpty()) {
                // Process forward and reverse children separately so their occurrence totals make sense
                Map<Arc, Node> forwardChildren = children.entrySet().stream().filter(e -> e.getKey().isForwardArc()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                Map<Arc, Node> reverseChildren = children.entrySet().stream().filter(e -> e.getKey().isReverseArc()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

                for (Node child : filterChildrenByCountProportion(forwardChildren).values()){
                    child.recursivelyPruneChildren();
                }
                for (Node child : filterChildrenByCountProportion(reverseChildren).values()){
                    child.recursivelyPruneChildren();
                }

                // Set children of node to new filtered children
                children = Stream.of(forwardChildren, reverseChildren)
                            .map(Map::entrySet)
                            .flatMap(Collection::stream)
                            .collect(Collectors.toMap(
                                    Map.Entry::getKey,
                                    Map.Entry::getValue
                            ));
            }
        }

        private Map<Arc, Node> filterChildrenByCountProportion(Map<Arc, Node> children){
            int choices = children.size();
            int childOccurrenceTotal = children.entrySet().stream().mapToInt(e -> e.getValue().count).sum();

            if(childOccurrenceTotal == choices){
                children.clear();
            } else {
                double dynamicThreshold = calcDynamicThreshold(choices, childOccurrenceTotal);
                for (Iterator<Map.Entry<Arc, Node>> it = children.entrySet().iterator(); it.hasNext();){
                    Map.Entry<Arc, Node> entry = it.next();
                    Node child = entry.getValue();
                    // If the child's occurrences is less than the minimum required, or count proportion of the current node is less than dynamic threshold, then prune
                    if(child.count < minimumNgramCount){
                        it.remove();
                    } else {
                        double proportion = child.count / (double)count;
                        if (proportion < 1 && proportion <= dynamicThreshold){
                            it.remove();
                        }
                    }
                }
            }
            return children;
        }

        private double calcDynamicThreshold(int numChoices, int totalOccurrences){
            if (totalOccurrences < level1NgramCount){
                return 1.0;
            } else if (totalOccurrences < level2NgramCount){
                return 0.75;
            } else if (totalOccurrences < level3NgramCount){
                return 0.5;
            }
            return Math.max(1.0/numChoices, minLeafPruningThreshold);
        }

        public Node addForwardChild(N form, int count){ return addChild(newForwardArc(form), count); }
        public Node addReverseChild(N form, int count){ return addChild(newReverseArc(form), count); }
        public Node addChild(Arc a, int count) {
            Node child = new Node(this, a, count);
            children.put(a, child);
            return child;
        }

        public Node incForwardChild(N form, int count) { return incChild(newForwardArc(form), count); }
        public Node incReverseChild(N form, int count) { return incChild(newReverseArc(form), count); }
        public Node incChild(Arc a, int count){
            Node child;
            if (hasChild(a)){
                child = getChild(a);
                child.incCount(count);
            } else {
                child = addChild(a, count);
            }
            return child;
        }

        public void incCount(int inc) { count = Math.max(0, count+inc); }

        public List<Node> getLeafNodes() {
            List<Node> toBeExplored = Lists.newArrayList(this);
            List<Node> leafNodes = new ArrayList<>();
            while (!toBeExplored.isEmpty()) {
                Node currentNode = toBeExplored.remove(toBeExplored.size()-1);
                Collection<Node> childNodes = currentNode.children.values();

                for (Node child : childNodes) {
                    if (child.hasChildren()) {
                        toBeExplored.add(child);
                    } else {
                        leafNodes.add(child);
                    }
                }
            }
            return leafNodes;
        }

        public Map<Node, Integer> getAncestorsAsMap(){
            if (this.isRoot()) return new HashMap<>();

            Map<Node, Integer> ancestors = new HashMap<>();
            AncestorNode currentNode = new AncestorNode(this.parent, this.count);
            while (!currentNode.node.isRoot()){
                ancestors.put(currentNode.node, currentNode.childCount);
                currentNode = new AncestorNode(currentNode.node.parent, currentNode.node.count);
            }
            ancestors.put(currentNode.node, currentNode.childCount); //add root node
            return ancestors;
        }

        public Set<AncestorNode> getAncestorsAsSet(){
            if (this.isRoot()) return new HashSet<>();

            Set<AncestorNode> ancestors = Sets.newHashSet();
            AncestorNode currentNode = new AncestorNode(this.parent, this.count);
            while (!currentNode.node.isRoot()){
                ancestors.add(currentNode);
                currentNode = new AncestorNode(currentNode.node.parent, currentNode.node.count);
            }
            ancestors.add(currentNode); //add root node
            return ancestors;
        }

        public Iterable<AncestorNode> getAncestorsAsIterable(){
            if (this.isRoot()) return new ArrayList<>();

            AncestorNode parent = new AncestorNode(this.parent, this.count);
            return () -> new Iterator<AncestorNode>() {
                boolean returnedRoot = false;
                AncestorNode currentNode = parent;

                public boolean hasNext() {
                    return !returnedRoot;
                }

                public AncestorNode next() {
                    AncestorNode toBeReturned = currentNode;
                    if(currentNode.node.hasParent())
                        currentNode = new AncestorNode(currentNode.node.parent, currentNode.node.count);
                    else returnedRoot = true;
                    return toBeReturned;
                }
            };
        }

        public List<N> getNgram() {

            if (isRoot()) {
                List<N> out = new ArrayList<>();
                out.add(this.latestTokenForm());
                return out;
            }

            List<N> ancestors = new ArrayList<>();
            int reverseStart = 0;

            Node currentNode = this;
            while (!currentNode.isRoot()) {
                if(currentNode.toParent.isForwardArc()){
                    reverseStart++;
                }
                ancestors.add(currentNode.latestTokenForm());
                currentNode = currentNode.parent;
            }

            Iterable<N> tokens = Iterables.concat(
                    ancestors.subList(reverseStart, ancestors.size()),
                    Lists.newArrayList(currentNode.latestTokenForm()),
                    Lists.reverse(ancestors.subList(0,reverseStart)));

            return Lists.newArrayList(tokens);
        }

        public String toString(){
            return Joiner.on(" ").join(getNgram());
        }

        public void print(FeatureExtractionPipeline pipeline) { print("", true, pipeline); }
        private void print(String prefix, boolean isTail, FeatureExtractionPipeline pipeline) {
            String form = (pipeline == null)? latestTokenForm().toString() : pipeline.featureString(Integer.parseInt(latestTokenForm().toString()));

            System.out.println(prefix + (connectionForPrint(isTail, toParent.type)) + form +"("+count+")");

            Collection<Node> childNodes = children.values();
            int i = 0;
            Iterator<Node> iterator = childNodes.iterator();
            while (i < children.size()-1 && iterator.hasNext()) {
                Node child = iterator.next();
                child.print(prefix + (isTail ? "    " : "│   "), false, pipeline);
                i++;
            }

            if (children.size() > 0) {
                iterator.next().print(prefix + (isTail ?"    " : "│   "), true, pipeline);
            }
        }

        private String connectionForPrint(boolean isTail, ARC_TYPE t){
            switch (t) {
                case FORWARD:
                    return isTail? "└>─ " : "├>─ ";
                case REVERSE:
                    return isTail? "└<─ " : "├<─ ";
                default:
                    return isTail? "└── " : "├── ";
            }
        }
    }

    public static enum ARC_TYPE {
        FORWARD, REVERSE, NULL
    }

    public Arc newReverseArc(N form){
        return new Arc(form, ARC_TYPE.REVERSE);
    }

    public Arc newForwardArc(N form){
        return new Arc(form, ARC_TYPE.FORWARD);
    }

    public Arc newNullArc(N form){
        return new Arc(form, ARC_TYPE.NULL);
    }

    public class Arc {


        public ARC_TYPE type;
        public N form;

        private Arc(N form, ARC_TYPE type){
            this.form = form;
            this.type = type;
        }

        public boolean isForwardArc() { return type == ARC_TYPE.FORWARD; }
        public boolean isReverseArc() { return type == ARC_TYPE.REVERSE; }
        public boolean isNullArc()    { return type == ARC_TYPE.NULL;    }


        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Arc arc = (Arc) o;

            if (form != null ? !form.equals(arc.form) : arc.form != null) return false;
            if (type != arc.type) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = type != null ? type.hashCode() : 0;
            result = 31 * result + (form != null ? form.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "Arc{" +
                    "type=" + type +
                    ", form='" + form + '\'' +
                    '}';
        }
    }
}
