package uk.ac.susx.tag.classificationframework.featureextraction.documentprocessing;

import com.google.common.base.CharMatcher;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import uk.ac.susx.tag.classificationframework.datastructures.AnnotatedToken;
import uk.ac.susx.tag.classificationframework.datastructures.Document;
import uk.ac.susx.tag.classificationframework.exceptions.FeatureExtractionException;
import uk.ac.susx.tag.dependencyparser.datastructures.Sentence;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * See DocProcessor class for the function of DocProcessors.
 *
 * This document processor converts CMU PoS tags and tokens to an intermediate
 * format more amenable to dependency parsing. However, the new tokens do not
 * replace the old, they are added as extra attributes to the document.
 *
 * This attribute is under the key "ExpandedTokens".
 *
 * This DocProcessor is a prerequisite of the TweetDependencyParser DocProcessor.
 *
 * TODO Ideas
 * - Maybe insert commas before "which" when it occurs non-initially.
 * - Deal with Twitter ellipsis syntax
 * - Support double and triple contractions
 * - Expand the pronoun list by tagging a tonne of tweets and extracting the words marked as pronouns
 * - "Imma/Ima fish" other "Im" related compounds
 * - Deal with y'all
 * - deal with "aint"
 * - negation without apostrophe
 *
 * User: Andrew D. Robertson
 * Date: 19/08/2013
 * Time: 13:47
 */
public class TweetTagConverter extends DocProcessor {

    private static final long serialVersionUID = 0L;

    private transient ConversionResource cr = new ConversionResource();
    private static final Pattern splitByApostrophe = Pattern.compile("'");

    private static final Set<String> cmuTagVocab = Sets.newHashSet(
            "O","V","D","A","N","P",",","^","L","~","@","U","$",
            "E","!","&","R","#","G","T","M","X","S","Z","Y");

    public Document process(Document document) {
        try { document.putAttribute("ExpandedTokens", stripNonSyntacticElementsAndConvert(document));}
        catch (FeatureExtractionException e){
            throw new FeatureExtractionException("FeatureExtractionException encountered: " + e.getMessage() +
                                                 "\nMost likely, you have not assigned (correct)PoS tags to your tokens.");
        }
        return document;
    }

    @Override
    public String configuration() { return ""; }

    /**
     * Given a Document instance whose tokens and PoS tags were produced with the CMU PoS tagger, remove any Twitter
     * syntax that isn't part of the grammatical syntax of the sentence. Then restrict any token and PoS tags to
     * ones which have direct equivalents in the Penn Style tokens/tags.
     *
     * Prepare tokens for parser.
     * Includes dirty hacks like adding a full stop and capitalising the first letter of the first word.
     */
    private List<Token> stripNonSyntacticElementsAndConvert(Document document){
        List<Token> newTokens = new ArrayList<>();
        boolean atRTStart = false;  // true if there's nothing or only punctuation between current token and last RT start.
        boolean discoveredContent = false; // true if we've seen a syntactic element
        for (int i = 0; i < document.size(); i++){
            AnnotatedToken token = document.get(i);
            String pos = token.get("pos"), form = token.get("form");
            if (pos.equals("~")) { // If we see a twitter-syntax tag
                if (form.toLowerCase().equals("rt")) {
                    if (i >= document.size()-1 || document.get(i+1).get("pos").equals("P") || document.get(i+1).get("pos").equals("D")){
                        newTokens.add(new Token(i+1, "RT", "V")); discoveredContent = true;
                    } else {
                        atRTStart = true; // It's the start of a true retweet
                        if (!newTokens.isEmpty() && discoveredContent)newTokens.add(new Token(i+1, ":", ","));
                    }
                } // TODO: could deal with ellipsis continuation syntax here...
            } else if (pos.equals("@")){ // If we see a @tag
                if (!atRTStart || i==0) newTokens.add(new Token(i+1, form, "^"));
            } else if (!cr.removableTags.contains(pos)){ // If we see any other non-removable tag
                if (!pos.equals(",")) {
                    atRTStart = false; discoveredContent = true;
                }
                if (!cmuTagVocab.contains(pos)) throw new FeatureExtractionException("PoS tags other than CMU are used.");
                singleTokenConversion(i, document, newTokens);
            }
        }
        if (!newTokens.isEmpty()) {
            // If no punctuation at the end of sentence, cheekily split the last token into 2 tokens: "token" and "."; Parser performs better with sentences ending in full stops...
            if (!newTokens.get(newTokens.size()-1).pos.equals(",")) newTokens.add(new Token(document.size(), ".", ","));

            // Cheekily capitalise the first letter of the first word (parser prefers this).
            Token firstToken = newTokens.get(0);
            firstToken.form = firstToken.form.substring(0, 1).toUpperCase() + firstToken.form.substring(1);
        }
        return newTokens;
    }

    /**
     * Given the index of a token which has a PoS indicating it is part of the syntax of the sentence,
     * add the appropriate token(s)
     */
    private void singleTokenConversion(int i, Document document, List<Token> tokensSoFar){
        int id = i+1;  // IDs are used to map expanded tokens to original ones. Original token IDs are assigned from 1.
        String form = document.get(i).get("form"), tag = document.get(i).get("pos");
        String formLowerCase = form.toLowerCase();

        if (formLowerCase.endsWith("n't") || formLowerCase.equals("cannot")) {
            tokensSoFar.add(new Token(id, form.substring(0,form.length()-3), "V"));
            tokensSoFar.add(new Token(id, form.substring(form.length()-3), "R"));
        } else if (cr.contractedNotWithoutApostrophe.contains(formLowerCase) && tag.equals("V")){
            tokensSoFar.add(new Token(id, form.substring(0,form.length()-2), "V"));
            tokensSoFar.add(new Token(id, "n't", "R"));
        } else if (tag.equals(",")) {
            if (form.startsWith("!")) tokensSoFar.add(new Token(id, "!", tag));
            else if (form.startsWith("?")) tokensSoFar.add(new Token(id, "?", tag));
            else tokensSoFar.add(new Token(id, form, tag));
        } else if (cr.imGonna.contains(formLowerCase)) {
            tokensSoFar.add(new Token(id, "I", "O"));
            tokensSoFar.add(new Token(id, "'m", "V"));
            if (i+1 >= document.size() || lookaheadForIfOnlyAdverbsTilNextVerb(i, document)) {
                tokensSoFar.add(new Token(id, "gonna", "V"));
            } else {
                tokensSoFar.add(new Token(id, "a", "D"));
            }
        } else {
            String[] items = splitByApostrophe.split(form); //TODO: Deal with double and triple contractions?
            if (items.length == 2 && !items[0].isEmpty() && !items[1].isEmpty()){
                if (items[0].equals("i")) items[0] = "I";
                if (tag.equals("L")){  // Probably nominal+verbal (sometimes L is used to tag some pretty tricky contracted phrases)
                    tokensSoFar.add(new Token(id, items[0], cr.pronouns.contains(items[0].toLowerCase())? "O" : "N")); //Check whether pronoun or noun
                    tokensSoFar.add(new Token(id, "'" + items[1], "V")); // Assume we have a nominal followed by a verb.
                } else if (cr.tagConverter.containsKey(tag)){ // If we recognise the tag to convert
                    tokensSoFar.add(new Token(id, items[0], cr.tagConverter.get(tag)));
                    tokensSoFar.add(new Token(id, "'"+items[1],(tag.equals("Z")||tag.equals("S"))?  "G" : "V" )); //Either possessive case or verb
                } else { // Make a guess on tag, based on whether a noun occurs after.
                    if (i>= document.size()-1 || (document.get(i+1).get("pos").equals("N") || document.get(i+1).get("pos").equals("^"))) { // If a noun is next, then maybe we have a posessive case token
                        tokensSoFar.add(new Token(id, items[0], tag));
                        tokensSoFar.add(new Token(id, "'"+items[1], "G"));
                    } else if (CharMatcher.JAVA_UPPER_CASE.matchesAllOf(items[0])){ // Some kinda abbreviation; leave as single token
                        tokensSoFar.add(new Token(id, form, tag));
                    } else { // If not an abbreviation or possessive case, then perhaps the contracted word is a verb
                        tokensSoFar.add(new Token(id, items[0], tag));
                        tokensSoFar.add(new Token(id, "'"+items[1],"V"));
                    }
                }
            } else if (items.length == 1 && cr.tagConverter.containsKey(tag)) {
                separateSuffixPosAided(id, form, tag, tokensSoFar); // No apostrophe, but tag suggests token needs expansion
            } else tokensSoFar.add(new Token(id, form, tag)); // Token needs no expansion (currently this happens with double and triple contractions too)
        }
    }

    /**
     * Look starting from token i+1, if you see a verb either immediately or only after having
     * seen only adverbs, then return true. Otherwise false.
     */
    private boolean lookaheadForIfOnlyAdverbsTilNextVerb(int i, Document d){
        i++;
        while (i < d.size()) {
            String pos = d.get(i).get("pos");
            if (pos.equals("V")) return true;
            else if (!pos.equals("R")) break;
            i++;
        } return false;
    }

    /**
     * If *tag* is one of {S, Z, Y, L, M}, then it needs expanding. This method is necessary for
     * a particular word which has been assigned one of these labels, but there is no apostrophe
     * in the word, so it cannot easily be split.
     *
     * Attempt to find a way to split the word sensibly, to use more traditional PoS tags.
     */
    private void separateSuffixPosAided(int id, String word, String tag, List<Token> tokensSoFar){
        if (tag.equals("S") || tag.equals("Z")){ // If tagged as a nominal in possessive case.
            if (word.endsWith("s")){
                String upToS = word.substring(0, word.length()-1);
                tokensSoFar.add(new Token(id, upToS.equals("i")? "I" : upToS, cr.tagConverter.get(tag)));
                tokensSoFar.add(new Token(id, "'s", "G"));
            } else {
                tokensSoFar.add(new Token(id, word.equals("i")? "I" : word, cr.tagConverter.get(tag)));
                tokensSoFar.add(new Token(id, "'s", "G"));
            }
        } else if (tag.equals("L") && cr.lTagExpansion.containsKey(word.toLowerCase())) {
            getLTagExpansionWithID(id, word, tokensSoFar); // If tagged as nominal+verbal, and we recognise word: expand.
        } else { // Otherwise check for a contracted verb ending
            String[] splitToken = splitByVerbEnding(word);
            if (splitToken == null) { // Worst case. No idea what to do, just duplicate word and use more traditional tags.
                tokensSoFar.add(new Token(id, word, cr.tagConverter.get(tag)));
                tokensSoFar.add(new Token(id, word, "V"));
            } else {
                if (splitToken[0].equals("i")) splitToken[0] = "I";
                tokensSoFar.add(new Token(id, splitToken[0], cr.tagConverter.get(tag)));
                tokensSoFar.add(new Token(id, "'"+splitToken[1], "V"));
            }
        }
    }

    private void getLTagExpansionWithID(int id, String token, List<Token> tokensSoFar){
        Split splitDef = cr.lTagExpansion.get(token.toLowerCase());
        String pronoun = token.substring(0, splitDef.split);
        if (pronoun.equals("i")) pronoun = "I";
        tokensSoFar.add(new Token(id, pronoun, "O"));
        tokensSoFar.add(new Token(id, splitDef.contractedForm, "V"));
    }

    private String[] splitByVerbEnding(String token) {
        String tokenLower = token.toLowerCase();
        for (String ending : cr.verbEndings) {
            if (tokenLower.endsWith(ending)) { // If we recognise a verbal ending, split by it.
                int split = token.length()-ending.length();
                return new String[] {token.substring(0, split), token.substring(split)};
            }
        } return null; // Verb ending not found.
    }

    @Override
    public boolean isThreadSafe() {
        return true;
    }

    private static class ConversionResource {

        public Set<String> pronouns;  // Strings considered to be pronouns
        public Map<String, String> tagConverter; // A mapping from compound tags to their expanded second element tag
        public List<String> verbEndings;   // A list of suffixes indicating contracted verbs
        public Set<String> removableTags;  // Tags indicating tokens which are not part of the syntax of the sentence
        public Map<String, Split> lTagExpansion; //Mapping from L tag type tokens, to their expanded versions
        public Set<String> imGonna;
        public Set<String> contractedNotWithoutApostrophe;

        public ConversionResource(){
            pronouns = Sets.newHashSet("i","you","he","she","it","one","they","we","what","that","which","who","wat","wot"); // TODO: tag a craptonne of tweets and extract all the strings which are marked as pronouns, to expand this list.
            tagConverter = getTagConverter();
            verbEndings = Lists.newArrayList("s","ll","d","re","ve");
            removableTags = Sets.newHashSet("U","E","#");
            lTagExpansion = getLTagExpansionMap();
            imGonna = Sets.newHashSet("ima", "i'ma", "imma", "i'mma");
            contractedNotWithoutApostrophe = Sets.newHashSet("aint", "arent", "cant", "couldnt", "doesnt", "dont", "hasnt", "havent", "isnt", "wont", "wouldnt");
        }

        private Map<String, String> getTagConverter() {
            Map<String, String> converter = new HashMap<>();
            converter.put("S", "N");
            converter.put("Z", "^");
            converter.put("Y", "X");
            converter.put("L", "N");
            converter.put("M", "^");
            return converter;
        }

        public Map<String, Split> getLTagExpansionMap(){
            Map<String, Split> map = new HashMap<>();

            map.put("im", new Split(1, "am"));

            Set<String> suffixVE = Sets.newHashSet("i","you","they","we","who","wat","wot","what","that","which");
            Set<String> suffixS = Sets.newHashSet("he", "she", "it", "one","who","wat","wot","what","that","which");
            Set<String> suffixRE = Sets.newHashSet("you","we","they","who","wat","wot","what","that","which");

            for (String pronoun : pronouns) {
                map.put(pronoun+"d", new Split(pronoun.length(), "'d"));
                map.put(pronoun+"ll", new Split(pronoun.length(), "'ll"));
                if (suffixVE.contains(pronoun)){
                    map.put(pronoun+"v", new Split(pronoun.length(), "'ve"));
                    map.put(pronoun+"ve", new Split(pronoun.length(), "'ve"));
                }
                if (suffixS.contains(pronoun)){
                    map.put(pronoun+"s", new Split(pronoun.length(), "'s"));
                }
                if (suffixRE.contains(pronoun)) {
                    map.put(pronoun+"re", new Split(pronoun.length(), "'re"));
                    map.put(pronoun+"r", new Split(pronoun.length(), "'re"));
                }
            }
            return map;
        }
    }

    /**
     * Represents an expanded token. It has a particular form
     * and a particular PoS. Then ID refers to the token from
     * which it was expanded. If the original sentences has
     * tokens: 1, 2, 3.
     *
     * And token 2 was expanded into two tokens, then the
     * resulting list of expanded tokens will have IDs: 1, 2, 2, 3
     */
    public static class Token implements Serializable, Sentence.PoSandFormBearing {
        private static final long serialVersionUID = 0L;

        public String form;
        public String pos;
        public int oldID;
        public String deprel = null; // The point of this conversion is to make the tokens amenable to a dependency parser
        public int head = -1;   // So these fields are there so that the parser can just fill them

        public Token(int id, String form, String pos) {
            this.oldID = id;
            this.form = form;
            this.pos = pos;
        }

        public Token(int id, String form, String pos, int head, String deprel){
            oldID = id;
            this.form = form;
            this.pos = pos;
            this.head = head;
            this.deprel = deprel;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(form);
            sb.append("(");
            sb.append(oldID); sb.append(",");
            sb.append(pos); sb.append(",");
            sb.append(head==-1? "_" : Integer.toString(head)); sb.append(",");
            sb.append(deprel==null? "_" : deprel);
            sb.append(")");
            return sb.toString();
        }

        @Override
        public String getForm() {
            return form;
        }

        @Override
        public String getPos() {
            return pos;
        }
    }

    /**
     * Represents a splitting of a token, where the left-hand side
     * of the split should remain the same, but the right-hand side
     * is replaced by the contents of "contractedForm".
     */
    private static class Split {

        public int split;
        public String contractedForm;

        private Split(int split, String contractedForm) {
            this.split = split;
            this.contractedForm = contractedForm;
        }
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        cr = new ConversionResource();
    }


}


