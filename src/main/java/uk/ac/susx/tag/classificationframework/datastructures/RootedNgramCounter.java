package uk.ac.susx.tag.classificationframework.datastructures;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created with IntelliJ IDEA.
 * User: Andrew D. Robertson
 * Date: 10/11/2015
 * Time: 11:53
 */
public class RootedNgramCounter {

    private Node root;

    public RootedNgramCounter(String rootWord) {
        root = new Node(null, Arc.newNullArc(rootWord));
    }

    public boolean isRootToken(String token){
        return token.equals(root.latestTokenForm());
    }

    public int getIndexOfRootToken(List<String> context){
        return context.indexOf(root.latestTokenForm());
    }

    public void print(){
        root.print();
    }

    public void countNgramsInContext(List<String> context) { countNgramsInContext(context, 1, 2, 6); }
    public void countNgramsInContext(List<String> context, int count, int minN, int maxN){

        // Find where the root token is in the ngram
        int indexOfRoot = getIndexOfRootToken(context);

        if (indexOfRoot == -1) throw new IllegalArgumentException("The context must contain the root word.");

        for (CentredNgram c : centredNgrams(minN, maxN, context, indexOfRoot)) {

            // All tokens before it will be added as reverse children
            List<String> beforeRootTokens = c.ngram.subList(0, c.centre);

            // All after will be added as forward children
            List<String> afterRootTokens = c.ngram.subList(c.centre + 1, c.ngram.size());

            // Track where we are in the graph
            Node currentNode = root;

            root.incCount(count);

            // Add reverse children (closest to root first), traversing the graph to the child
            for (String token : Lists.reverse(beforeRootTokens)) {
                currentNode = currentNode.incReverseChild(token, count);
            }

            // Add forward children (closest to root first), traversing the graph to the child
            for (String token : afterRootTokens) {
                currentNode = currentNode.incForwardChild(token, count);
            }
        }
    }

    public List<String> topNgrams(int K, double leafPruningThreshold) {
        root.recursivelyPruneChildren(leafPruningThreshold);

        List<Node> topNodes = new LowestCommonAncestorDifferenceOrdering().greatestOf(root.getLeafNodes(), K);

        return  topNodes.stream().map(Node::toString).collect(Collectors.toList());
    }

    private static class LowestCommonAncestorDifferenceOrdering extends Ordering<Node>{
        @Override
        public int compare(Node left, Node right) {
            return lowestCommonAncestorDifferenceExcludingSelf(left, right);
        }
    }

    /**
     * Lowest common ancestor excluding a and b (therefore makes the most sense using only leaf nodes).
     */
    private static int lowestCommonAncestorDifferenceExcludingSelf(Node a, Node b) {
        Map<Node, Integer> ancestorsOfA = a.getAncestorsAsMap();

        for (AncestorNode ancestor : b.getAncestorsAsIterable()){
            if (ancestorsOfA.containsKey(ancestor.node)){
                return ancestorsOfA.get(ancestor.node) - ancestor.childCount;
            }
        }

        throw new RuntimeException("This shouldn't be possible... The root node at least should always be a common ancestor, but none were found.");
    }


    private static Iterable<CentredNgram> centredNgrams(int minN, int maxN, List<String> tokens, int indexOfCentreWord){
        if (minN < 2 || maxN < minN) throw new IllegalArgumentException("Requirements: minN > 1 & maxN < minN");

        return () -> new Iterator<CentredNgram>() {
            int currentN = minN;
            int currentToken = Math.max(indexOfCentreWord-currentN+1, 0);

            public boolean hasNext() {
                return currentToken <= indexOfCentreWord && currentToken < tokens.size() - currentN+1;
            }

            public CentredNgram next() {
                List<String> ngram = new ArrayList<>();
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

    private static class CentredNgram {
        List<String> ngram;
        int centre;

        public CentredNgram(List<String> ngram, int centre) {
            this.ngram = ngram;
            this.centre = centre;
        }
    }

    /**
     * These are used when returning ancestors of a Node.
     * The count is the count of the immediate child from which we found this ancestor.
     * Hashing/Equals is based on the inner node, so can be compared as if it is a normal node.
     */
    public static class AncestorNode {
        public Node node;
        public int childCount;

        public AncestorNode(Node node, int childCount) {
            this.node = node;
            this.childCount = childCount;
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            AncestorNode that = (AncestorNode) o;

            if (node != null ? !node.equals(that.node) : that.node != null) return false;

            return true;
        }

        public int hashCode() {
            return node != null ? node.hashCode() : 0;
        }

        public String toString() {
            return "AncestorNode{" + "node=" + node + ", childCount=" + childCount + '}';
        }
    }


    public static class Node {

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
        public String latestTokenForm() {  return toParent.form; }
        public boolean hasChild(Arc child){ return children.containsKey(child); }
        public Node getChild(Arc child) { return children.get(child); }

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

        public Node addForwardChild(String form, int count){ return addChild(Arc.newForwardArc(form), count); }
        public Node addReverseChild(String form, int count){ return addChild(Arc.newReverseArc(form), count); }
        public Node addChild(Arc a, int count) {
            Node child = new Node(this, a, count);
            children.put(a, child);
            return child;
        }

        public Node incForwardChild(String form, int count) { return incChild(Arc.newForwardArc(form), count); }
        public Node incReverseChild(String form, int count) { return incChild(Arc.newReverseArc(form), count); }
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

        public List<String> getNgram() {

            if (isRoot()) return Lists.newArrayList(latestTokenForm());

            List<String> ancestors = new ArrayList<>();
            int reverseStart = 0;

            Node currentNode = this;
            while (!currentNode.isRoot()) {
                if(currentNode.toParent.isForwardArc()){
                    reverseStart++;
                }
                ancestors.add(currentNode.latestTokenForm());
                currentNode = currentNode.parent;
            }

            Iterable<String> tokens = Iterables.concat(
                    ancestors.subList(reverseStart, ancestors.size()),
                    Lists.newArrayList(currentNode.latestTokenForm()),
                    Lists.reverse(ancestors.subList(0,reverseStart)));

            return Lists.newArrayList(tokens);
        }

        public String toString(){
            return Joiner.on(" ").join(getNgram());
        }

        public void print() { print("", true); }
        private void print(String prefix, boolean isTail) {
            System.out.println(prefix + (connectionForPrint(isTail, toParent.type)) + latestTokenForm()+"("+count+")");

            Collection<Node> childNodes = children.values();
            int i = 0;
            Iterator<Node> iterator = childNodes.iterator();
            while (i < children.size()-1 && iterator.hasNext()) {
                Node child = iterator.next();
                child.print(prefix + (isTail ? "    " : "|   "), false);
                i++;
            }

            if (children.size() > 0) {
                iterator.next().print(prefix + (isTail ?"    " : "│   "), true);
            }
        }

        private String connectionForPrint(boolean isTail, Arc.TYPE t){
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

    public static class Arc {
        public static enum TYPE {
            FORWARD, REVERSE, NULL
        }

        public TYPE type;
        public String form;

        private Arc(String form, TYPE type){
            this.form = form;
            this.type = type;
        }

        public boolean isForwardArc() { return type == TYPE.FORWARD; }
        public boolean isReverseArc() { return type == TYPE.REVERSE; }
        public boolean isNullArc()    { return type == TYPE.NULL;    }

        public static Arc newReverseArc(String form){
            return new Arc(form, TYPE.REVERSE);
        }

        public static Arc newForwardArc(String form){
            return new Arc(form, TYPE.FORWARD);
        }

        public static Arc newNullArc(String form){
            return new Arc(form, TYPE.NULL);
        }

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
