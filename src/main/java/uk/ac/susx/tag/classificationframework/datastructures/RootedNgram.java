package uk.ac.susx.tag.classificationframework.datastructures;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created with IntelliJ IDEA.
 * User: Andrew D. Robertson
 * Date: 10/11/2015
 * Time: 11:53
 */
public class RootedNgram {

    public static void main(String[] args) {
        RootedNgram n = new RootedNgram("dog");

        List<String> examples = Lists.newArrayList(
                "the brown dog ran",
                "the brown dog ran home",
                "cat knows the brown dog ran home"
        );

        for (String example : examples){
            n.countNgramsInContext(Lists.newArrayList(example.split(" ")));
        }

        n.print();

        for (Node leaf : n.root.getLeafNodes()){
            System.out.println(leaf);
        }

        n.root.recursivelyPruneChildren(0.5);

        n.print();

        for (Node leaf : n.root.getLeafNodes()){
            System.out.println(leaf);
        }
    }

    private Node root;

    public RootedNgram(String rootWord) {
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

    public static Iterable<CentredNgram> centredNgrams(int minN, int maxN, List<String> tokens, int indexOfCentreWord){
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

    public static class CentredNgram {

        List<String> ngram;
        int centre;

        public CentredNgram(List<String> ngram, int centre) {
            this.ngram = ngram;
            this.centre = centre;
        }
    }


    public static class Node {

        public Node parent;
        public Arc toParent;

        public Map<Arc, Node> children;

        public int count;

        public Node(Node parent, Arc toParent){  this(parent, toParent, 0); }
        public Node(Node parent, Arc toParent, int count) {
            this.parent = parent;
            this.toParent = toParent;
            children = new HashMap<>();
            this.count = count;
        }


        public boolean isRoot() { return parent==null || toParent.isNullArc(); }
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
            return Joiner.on("_").join(getNgram());
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
