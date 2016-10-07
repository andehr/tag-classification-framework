package uk.ac.susx.tag.classificationframework.datastructures;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.FeatureExtractionPipeline;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

    public RootedNgramCounter(N rootWord, int minN, int maxN) {
        root = new Node(null, newNullArc(rootWord));
        this.minN = minN;
        this.maxN = maxN;
    }

    public boolean isRootToken(N token){
        return token.equals(root.latestTokenForm());
    }

    public N getRootToken() { return root.latestTokenForm(); }

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

    public void countNgramsInContext(List<N> context) { countNgramsInContext(context, 1, 2, 6); }
    public void oldCountNgramsInContext(List<N> context, int count, int minN, int maxN){

        // Find where the root token is in the ngram
        int indexOfRoot = getIndexOfRootToken(context);

        if (indexOfRoot == -1) return; //Ignore context because our rooted term of interest is not present

        for (CentredNgram c : centredNgrams(minN, maxN, context, indexOfRoot)) {

            // Track where we are in the graph
            Node currentNode = root;

            root.incCount(count);

            // Add reverse children (closest to root first), traversing the graph to the child
            for (N token : Lists.reverse(c.beforeTokens())) {
                currentNode = currentNode.incReverseChild(token, count);
            }

            // Add forward children (closest to root first), traversing the graph to the child
            for (N token : c.afterTokens()) {
                currentNode = currentNode.incForwardChild(token, count);
            }
        }
    }

    public void countNgramsInContext(List<N> context, int count, int minN, int maxN){
        // Find the locations where the root token appears in this context
        int[] indicesOfRoot = getIndicesOfRootTokenOccurrences(context);
        //Ignore context because our rooted term of interest is not present
        if (indicesOfRoot.length == 0) return;
        // For each of the appearances of the root token
        for (int indexOfRoot : indicesOfRoot) {
            // Track which nodes we've encountered
            Set<Node> seenNodes = new HashSet<>();
            // Since we've seen the root token again, we increment it once for this occurrence
            root.incCount(count);
            // We've now seen the root here, so might as well track it, though we won't be accidentally incrementing it anyway
            seenNodes.add(root);
            // We now find all the ngrams around this occurrence
            for (CentredNgram c : centredNgrams(minN, maxN, context, indexOfRoot)) {
                // Track where we are in the graph, start from the root token
                Node currentNode = root;
                // Add reverse children (closest to root first), traversing the graph to the child
                for (N token : Lists.reverse(c.beforeTokens())) {
                    // Add the child and traverse to it, start it with a zero count
                    currentNode = currentNode.getAndAddReverseChildIfNotPresent(token, 0);
                    // If we have not seen this ngram for this occurrence
                    if(!seenNodes.contains(currentNode)){
                        // Increment the count
                        currentNode.incCount(count);
                        // Track that we've now seen this ngram
                        seenNodes.add(currentNode);
                    }
                }

                // Add forward children (closest to root first), traversing the graph to the child
                for (N token : c.afterTokens()) {
                    currentNode = currentNode.getAndAddForwardChildIfNotPresent(token, 0);
                    if(!seenNodes.contains(currentNode)){
                        currentNode.incCount(count);
                        seenNodes.add(currentNode);
                    }
                }
            }
        }
    }

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
            for (N tokenBefore : beforeTokens){
                currentNode = lastBeforeNode.incReverseChild(tokenBefore, count);
                lastBeforeNode = currentNode;

                for (N tokenAfter : afterTokens) {
                    currentNode = currentNode.incForwardChild(tokenAfter, count);
                }
            }
        }
    }

    public List<List<N>> topNgrams(int K, double leafPruningThreshold) {
        root.recursivelyPruneChildren(leafPruningThreshold);

        List<Node> topNodes = new LowestCommonAncestorDifferenceOrdering().greatestOf(root.getLeafNodes(), K);

        return  topNodes.stream().map(Node::getNgram).collect(Collectors.toList());
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
                return ancestorsOfA.get(ancestor.node) - ancestor.childCount;
            }
        }

        throw new RuntimeException("This shouldn't be possible... The root node at least should always be a common ancestor, but none were found.");
    }


    private Iterable<CentredNgram> centredNgrams(int minN, int maxN, List<N> tokens, int indexOfCentreWord){
        if (minN < 2 || maxN < minN) throw new IllegalArgumentException("Requirements: minN > 1 & maxN < minN");

        return () -> new Iterator<CentredNgram>() {
            int currentN = minN;
            int currentToken = Math.max(indexOfCentreWord-currentN+1, 0);

            public boolean hasNext() {
                return currentToken <= indexOfCentreWord && currentToken < tokens.size() - currentN+1;
            }

            public CentredNgram next() {
                List<N> ngram = new ArrayList<>();
                int centre = 0;
                for (int i = currentToken; i < currentToken + currentN; i++) {
                    ngram.add((tokens.get(i)));
                    if (i == indexOfCentreWord)
                        centre = ngram.size() -1;
                }
                currentToken++;
                if ((currentToken > indexOfCentreWord || currentToken >= tokens.size() - currentN+1) && currentN < maxN){
                    currentN++;
                    currentToken = Math.max(indexOfCentreWord-currentN+1, 0);
                }
                return new CentredNgram(ngram, centre);
            }
        };
    }

    private class CentredNgram {
        List<N> ngram;
        int centre;

        public CentredNgram(List<N> ngram, int centre) {
            this.ngram = ngram;
            this.centre = centre;
        }

        public CentredNgram(List<N> context, int centre, int n){
            int start = centre - (n - 1);
            int end = centre + (n - 1);
            ngram = context.subList(start, end);
            this.centre = centre;
        }

        /**
         * @return All of the tokens in the ngram that occur before the centred token
         */
        public List<N> beforeTokens() {
            return ngram.subList(0, centre);
        }

        /**
         * @return All of the tokens in the ngram that occur after the centred token
         */
        public List<N> afterTokens() {
            return ngram.subList(centre + 1, ngram.size());
        }
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

        public void recursivelyPruneChildren(double countProportion){
            if (!children.isEmpty()) {
                for(Iterator<Map.Entry<Arc, Node>> it = children.entrySet().iterator(); it.hasNext(); ){
                    Map.Entry<Arc, Node> entry = it.next();
                    Node child = entry.getValue();
                    if (child.count / (double)count >= countProportion) {
                        child.recursivelyPruneChildren(countProportion);
                    } else {
                        it.remove();
                    }
                }
            }
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
                child.print(prefix + (isTail ? "    " : "|   "), false, pipeline);
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
