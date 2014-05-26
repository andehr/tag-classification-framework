package uk.ac.susx.tag.classificationframework.datastructures;

import uk.ac.susx.tag.classificationframework.featureextraction.documentprocessing.TweetTagConverter;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Class representing a navigable tree of dependencies. The tokens can be accessed via index (from 0) or ID (from 1)
 * or they can be accessed using parent, sibling, child relations.
 *
 * At the bottom of this class are a number of static utility functions for inference over dependency trees.
 *
 * The dependency tree nodes themselves provide the functionality to get the parent, children, and siblings of nodes.
 *
 * WARNING: The data field of the root node will be NULL. To be safe, you can use the isRoot() method of a Node instance.
 */
public class DependencyTree {

    private List<Node> nodes; // List of tokens wrapped in Node instances.
    private Node root;        // Root of the sentence (the artificial root node)

    /**
     * If you have a Document object "doc" then typically, you'd create a DependencyTree instance like this:
     *
     * DependencyTree dt = new DependencyTree((List<TweetTagConverter.Token>)document.getAttribute("ExpandedTokens"));
     */
    public DependencyTree(List<TweetTagConverter.Token> tokens){
        this.nodes = new ArrayList<>(tokens.size()); // Create list with same capacity as *tokens*

        root = new Node(0, null);  // Create artificial root node

        // Wrap tokens in Node instances, and store them in list
        for (TweetTagConverter.Token token : tokens){
            // The ID should be the size of node array + 1 at each iteration.
            // e.g. when there are no items in the array, the ID of the next node would be 1.
            nodes.add(new Node(nodes.size()+1, token));
        }

        // Assign pointers to parent and children nodes
        for (Node node : nodes){
            Node parent = getNodeById(node.data.head);
            parent.children.add(node);
            node.parent = parent;
        }
    }

    /**
     * Retrieve a node by its sentence ID.
     * When a sentence is parsed, each token receives an ID starting from 1.
     * The ID of the root node, is 0.
     */
    public Node getNodeById(int id){
        try {
            return nodes.get(id-1);
        } catch (IndexOutOfBoundsException e) { // id == 0 (root), or invalid
            return root;
        }
    }

    /**
     * This method returns a node based on its actual index in the list of nodes.
     * You probably want "getNodeByID".
     */
    public Node getNode(int index){ return nodes.get(index); }

    /**
     * @return The length of the longest DIRECTED path in this tree.
     */
    public int longestDirectedPath(){ return root.longestDirectedPath(); }

    /** get all nodes (not including artificial root) **/
    public List<Node> getNodes(){ return nodes; }

    /**
     * get artificial root node. It is then possible to use the Node methods to
     * walk through the tree.
     */
    public Node getRoot(){ return root; }

    public String toString(){ return root.toString(); }


    /**
     * Class representing a word in a dependency tree.
     *
     * The natural ordering of Nodes is their ordering in the utterance.
     */
    public static class Node implements Comparable<Node>{

        private TweetTagConverter.Token data;
        private int id;
        private Node parent;
        private List<Node> children;

        private Node(int id, TweetTagConverter.Token data) {
            this.data = data;
            this.parent = null;
            this.children = new ArrayList<>();
            this.id = id;
        }

        // will return 0 when called on the root node (since that's the ID of root)
        public int getID() { return id; }

        // 3 accessors below will return null when performed on the root node
        public String getForm() { return data == null? null : data.form; }
        public String getPos()  { return data == null? null : data.pos; }
        public String getDeprel() { return data == null? null : data.deprel; }

        // will return -1 when called on root node
        public int getHead() { return data == null? -1 : data.head; }

        /**
         * @return true if this is the artificial root node.
         */
        public boolean isRoot(){ return parent == null;}

        /**
         * @return the original token that this Node object is wrapping.
         */
        public TweetTagConverter.Token getData() { return data; }

        // null if called on root
        public Node getParent() { return parent; }

        /**
         * With degree <= 1, this method is equal to getParent().
         *
         * Otherwise this method recursively acquires the parent
         * of a node to a specified degree. E.g. degree = 2 implies
         * getting the parent of this node's parent.
         *
         * If the root node is encountered when attempting this, then
         * the root node is returned (regardless of whether the root
         * node is the specified degrees of ancestorship away).
         */
        public Node getAncestor(int degree) {
            if (parent == null) return this;
            else if (degree <= 1) return parent;
            else return parent.getAncestor(degree - 1);
        }

        public List<Node> getChildren() { return children; }

        public List<Node> getSiblings() {
            List<Node> siblings = new ArrayList<>();
            for (Node node : parent.getChildren()){
                if (node != this) siblings.add(node);
            } return siblings;
        }

        public int getOutDegree(){
            return children.size();
        }

        /**
         * Find the longest DIRECTED path from this node. Return its length.
         */
        public int longestDirectedPath(){
            int longestPath = 0;
            for (Node child : children) {
                int path = 1 + child.longestDirectedPath();
                if (path > longestPath) longestPath = path;
            }
            return longestPath;
        }

        /**
         * Output the form of the token, and recursively the forms of its children.
         */
        public String toString() {
            return (isRoot()? "ROOT" : data.form) + (children.isEmpty()? "" :children.toString());
        }

        @Override
        public int compareTo(Node o) {
            if (o==null) throw new NullPointerException();
            return id - o.id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Node node = (Node) o;

            return id == node.id;
        }

        @Override
        public int hashCode() {
            return id;
        }
    }


/***********************************************
 * Inference tools, over dependency trees / nodes.
 ***********************************************/

    /*
     * Determine properties of a node
     */

    /**
     * return true if token is negated (has a dependant with the "neg" relation)
     * Note that there are some cases I can think of where negation happens through the DET relation
     * plus negation words like "no".
     */
    public static boolean isNegated(DependencyTree.Node node){
        for (DependencyTree.Node dependant : node.getChildren()){
            if (dependant.getData().deprel.equals("neg")) return true;
        } return false;
    }

    /**
     * Return true if token has a "conj" relation.
     *
     * So in the phrase "The car was red, blue and yellow." The tokens "blue" and "yellow" would return true.
     * But not "red". Read is the head of the conjunction. See "isConjunctIncHead()" below.
     *
     */
    public static boolean isConjunct(DependencyTree.Node node) {
        return !node.isRoot() && node.getData().deprel.equals("conj");
    }

    /**
     * Return true if token is in conjunction with another. This includes the following two situations:
     *
     * 1. The token has a "conj" relation.
     * 2. Some other token has a "conj" relation with this token.
     */
    public static boolean isConjunctIncHead(DependencyTree.Node node) {
        return !node.isRoot() &&
                // This node either has a "conj" relation, or some node has a "conj" relation with this node.
                ( node.getData().deprel.equals("conj") || !getConjuncts(node).isEmpty() );
    }

    /*
     * Get related nodes
     */

    /**
     * Get the conjuncts of a node.
     */
    public static List<Node> getConjuncts(DependencyTree.Node node){
        return extractNodesWithRelation(node.getChildren(), "conj");
    }



    /*
     * Extract nodes by the relations of tokens.
     */

    public static List<Node> filterDependants(DependencyTree.Node node, String relation){
        return extractNodesWithRelation(node.getChildren(), relation);
    }

    public static List<Node> extractNodesWithRelation(List<Node> nodes, Set<String> relations){
        List<Node> filtered = new ArrayList<>();
        for (DependencyTree.Node node : nodes) {
            if (!node.isRoot() && relations.contains(node.getData().deprel)) filtered.add(node);
        } return filtered;
    }
    public static List<Node> extractNodesWithRelation(List<Node> nodes, String relation){
        List<Node> filtered = new ArrayList<>();
        for (DependencyTree.Node node : nodes) {
            if (!node.isRoot() && relation.equals(node.getData().deprel)) filtered.add(node);
        } return filtered;
    }
    public static List<Node> extractNodesWithoutRelation(List<Node> nodes, String relation){
        List<Node> filtered = new ArrayList<>();
        for (DependencyTree.Node node : nodes) {
            if (!node.isRoot() && !relation.equals(node.getData().deprel)) filtered.add(node);
        } return filtered;
    }


    /*
     * Filter nodes by the form of the tokens.
     */

    public static List<Node> filterNodesByForm(DependencyTree dt, String form){
        return filterNodesByForm(dt.getNodes(), form);
    }
    public static List<Node> filterNodesByForm(List<Node> nodes, String form){
        List<Node> filtered = new ArrayList<>();
        for (DependencyTree.Node node : nodes){
            if (!node.isRoot() && node.getData().form.equals(form)) filtered.add(node);
        } return filtered;
    }
    public static List<Node> filterNodesByForm(DependencyTree dt, Set<String> forms){
        return filterNodesByForm(dt.getNodes(), forms);
    }
    public static List<Node> filterNodesByForm(List<Node> nodes, Set<String> forms){
        List<Node> filtered = new ArrayList<>();
        for (DependencyTree.Node node : nodes){
            if (!node.isRoot() && forms.contains(node.getData().form)) filtered.add(node);
        } return filtered;
    }

/**********************************
 * CoNLL format reading
 **********************************/

    /**
     * Reader for iteratively reading in CoNLL (2009 I think?) format dependency file.
     */
    public static class CoNLLSentenceReader implements Iterator<List<TweetTagConverter.Token>>, AutoCloseable{

        private BufferedReader reader;
        private List<TweetTagConverter.Token> nextSentence;

        public CoNLLSentenceReader(File conllFile) throws IOException {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(conllFile), "UTF8"));
            readNextSentence();
        }

        private void readNextSentence() throws IOException {
            boolean inSentence = false;
            List<TweetTagConverter.Token> sentence = new ArrayList<>();
            String line = reader.readLine();

            while (line != null) {
                if (line.trim().length() > 0){
                    String[] items = line.trim().split("\t");
                    sentence.add(new TweetTagConverter.Token(Integer.parseInt(items[0]), items[1], items[4], Integer.parseInt(items[8]), items[10]));
                    inSentence = true;
                } else if (inSentence) break;
                line = reader.readLine();
            }
            nextSentence = sentence;
            if (line == null) reader.close();
        }

        @Override
        public boolean hasNext() { return nextSentence.size() > 0; }

        @Override
        public List<TweetTagConverter.Token> next() {
            if (hasNext()){
                List<TweetTagConverter.Token> toReturn = nextSentence;
                try {
                    readNextSentence();
                } catch (IOException e) { e.printStackTrace();}

                return toReturn;

            } else throw new NoSuchElementException();
        }

        public void remove() { throw new UnsupportedOperationException(); }

        @Override
        public void close() throws IOException { reader.close(); }

    }


    public static class CoNLLSentenceWriter implements AutoCloseable {

        private final BufferedWriter writer;

        public CoNLLSentenceWriter(File output) throws IOException {
            if (! output.exists())
                output.createNewFile();
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(output), "UTF8"));
        }

        public void writeSentence(List<TweetTagConverter.Token> sentence) throws IOException {
            for (int i = 0; i < sentence.size(); i++) {
                writer.write(formatToken(i+1, sentence.get(i)));
            } writer.write("\n");
        }

        private String formatToken(int id, TweetTagConverter.Token token) {
            StringBuilder sb = new StringBuilder();
            sb.append(id); sb.append("\t");
            sb.append(token.form); sb.append("\t_\t_\t");
            sb.append(token.pos); sb.append("\t_\t_\t_\t");
            sb.append(token.head); sb.append("\t_\t");
            sb.append(token.deprel); sb.append("\t_\t_\t_\n");
            return sb.toString();
        }

        @Override
        public void close() throws IOException {
            writer.close();
        }
    }
}