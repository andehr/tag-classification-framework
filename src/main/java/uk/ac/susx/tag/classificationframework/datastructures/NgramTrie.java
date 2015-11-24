package uk.ac.susx.tag.classificationframework.datastructures;

import com.google.common.base.Joiner;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created with IntelliJ IDEA.
 * User: Andrew D. Robertson
 * Date: 10/11/2015
 * Time: 11:53
 */
public class NgramTrie {

    private Node root;

    public NgramTrie(String rootWord) {
        root = new Node(null, Arc.newNullArc(rootWord));
    }

    public boolean isRootToken(String token){
        return token.equals(root.latestTokenForm());
    }

    public void countNgram(List<String> ngram, int count){
        Node currentNode = root;
        boolean reverse = true;

        // TODO: this counts the reverse tokens in the wrong order
        for (String token : ngram) {
            if (isRootToken(token)) {
                reverse = false; continue;
            }
            if (reverse) {
                currentNode = currentNode.addReverseChild(token);
            } else {
                currentNode = currentNode.addForwardChild(token);
            }
        }

    }

    public static class Node {

        public Node parent;
        public Arc toParent;

        public Map<Arc, Node> children;

        public int count;
        public int terminatedCount;

        public Node(Node parent, Arc toParent) {
            this.parent = parent;
            this.toParent = toParent;
            children = new HashMap<>();
        }

        public boolean hasChildren() { return children.size() > 0; }

        public String latestTokenForm() {
            return toParent.form;
        }

        public Collection<Node> getChildrenWithinCountProportion(double proportion){
            if (count == 0)
                return children.values();
            else
                return children.entrySet().stream()
                        .filter(entry -> entry.getValue().count / count >= proportion)
                        .map(Map.Entry::getValue).collect(Collectors.toList());
        }

        public boolean hasChild(Arc child){
            return children.containsKey(child);
        }

        public Node getChild(Arc child) {
            return children.get(child);
        }

        public Node addForwardChild(String form){ return addChild(Arc.newForwardArc(form)); }
        public Node addReverseChild(String form){ return addChild(Arc.newReverseArc(form)); }
        public Node addChild(Arc a) {
            Node child = new Node(this, a);
            children.put(a, child);
            return child;
        }

        public void incCount(int inc) { count = Math.max(0, count+inc) ;}

        public List<String> getNgram(){
            List<String> reverseArcs = new ArrayList<>();
            List<String> forwardArcs = new ArrayList<>();


            Node currentNode = this;
            boolean forward = true;
            do {
                if (!forward || currentNode.toParent.isReverseArc()) {
                    forward = false;
                    reverseArcs.add(currentNode.latestTokenForm());
                } else {
                    forwardArcs.add(currentNode.latestTokenForm());
                }
            } while ((currentNode = currentNode.parent) != null);

            List<String> ngram = new ArrayList<>(reverseArcs.size() + forwardArcs.size() + 1);

            for (int i = reverseArcs.size()-1; i >= 0; i--){
                ngram.add(reverseArcs.get(i));
            }

            // Add the root form
            ngram.add(currentNode.toParent.form);

            for (String token : forwardArcs){
                ngram.add(token);
            }

            return ngram;
        }

        public String toString(){
            return Joiner.on("_").join(getNgram());
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
    }
}
