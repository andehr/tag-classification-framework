package uk.ac.susx.tag.classificationframework.datastructures;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: Andrew D. Robertson
 * Date: 10/11/2015
 * Time: 11:53
 */
public class NgramTrie {

    private Node root;

    public NgramTrie() {
        root = new Node(null);
    }

    public void pruneByCountProportion(double proportion){

    }

    public void incCount(List<String> ngram, int inc) {
        Node currentNode = root;
        for (String token : ngram){
            if (currentNode.hasChild(token)){
                currentNode = currentNode.getChild(token);
            } else {
                currentNode.addChild(token);
                currentNode = currentNode.getChild(token);
            }
        }
        currentNode.incCount(inc); // Allows negative increment; never goes below zero.
    }

    public int getCount(List<String> ngram){
        Node currentNode = root;
        int count = 0;
        for (String token : ngram) {
            if (!currentNode.hasChild(token)) {
                return 0;
            } else {
                currentNode = currentNode.getChild(token);
                count = currentNode.count;
            }
        }
        return count;
    }

    public static class Node {

        public Node parent;
        public int count;
        public Map<String, Node> children;

        public Node(Node parent) {
            this.parent = parent;
            count = 0;
            children = new HashMap<>();
        }

        public boolean hasChildren() { return children.size() > 0; }

        public boolean hasChild(String child){
            return children.containsKey(child);
        }

        public Node getChild(String child) {
            return children.get(child);
        }

        public void addChild(String text) {
            children.put(text, new Node(this));
        }

        public void incCount(int inc) { count = Math.max(0, count+inc) ;}

    }
}
