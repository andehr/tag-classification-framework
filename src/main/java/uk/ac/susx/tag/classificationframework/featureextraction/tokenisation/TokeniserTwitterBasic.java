package uk.ac.susx.tag.classificationframework.featureextraction.tokenisation;

/*
 * #%L
 * TokeniserTwitterBasic.java - classificationframework - CASM Consulting - 2,013
 * %%
 * Copyright (C) 2013 - 2014 CASM Consulting
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import uk.ac.susx.tag.classificationframework.datastructures.AnnotatedToken;
import uk.ac.susx.tag.classificationframework.datastructures.Document;
import uk.ac.susx.tag.classificationframework.datastructures.Instance;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * See Tokeniser class for the function of Tokenisers.
 *
 * Regex tokeniser. Attempts to be Twitter-aware.
 *
 * Incorporates the stripping of punctuation, which is configurable, by providing a
 * regex pattern for the tokenisation which should be allowed.
 *
 * Emoticons are also upper-cased.
 *
 * It's also possible to lowercase the text.
 *
 * User: Andrew D. Robertson
 * Date: 19/08/2013
 * Time: 10:03
 */
public class TokeniserTwitterBasic implements Tokeniser {

    private static final long serialVersionUID = 0L;

//    private String core = "(http://[\\\\.\\w\\-/]+)|([\\@\\#]?[\\p{L}\\p{Mn}][\\p{L}\\p{Mn}'_]+)"; // This seems to not allow tokens of a single character
//    private String core = "(http://[\\\\.\\w\\-/]+)|([@#]?[\\p{L}\\p{Mn}][\\p{L}\\p{Mn}'_]*)|([_']*[\\p{L}\\p{Mn}][\\p{L}\\p{Mn}_']*)";
    private static final String core = "(http://[\\\\.\\w\\-/]+)|([@#]?[\\p{L}\\p{Mn}\\d][\\p{L}\\p{Mn}'_\\d]*)|([_']*[\\p{L}\\p{Mn}\\d][\\p{L}\\p{Mn}_'\\d]*)";
    private static final String emoticon = "([:;=][-o^]?[)(/\\\\p])|([/\\\\)(d][-o^]?[:;=x])";

    private Pattern tokenPattern;
    private Pattern emoticonPattern;
    private Pattern urlPattern = Pattern.compile("(?:https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*");

    private boolean lowerCase;
    private boolean normaliseURL;


    public TokeniserTwitterBasic(){
        this("[?!]+", false, true);
    }

    public TokeniserTwitterBasic(boolean lowerCase){
        this("[?!]+", lowerCase, true);
    }

    public TokeniserTwitterBasic(boolean lowerCase, boolean normaliseURL){
        this("[?!]+", lowerCase, normaliseURL);
    }

    /**
     * If punctuationPattern is empty string, or null, then all punctuation is stripped.
     * Otherwise provide a pattern for the punctuation which should NOT be stripped, e.g. [?!]+
     *
     * If lowercase is true, then everything apart from emoticons will be lowercased.
     * @param punctuationPattern
     * @param lowerCase
     */
    public TokeniserTwitterBasic(String punctuationPattern, boolean lowerCase, boolean normaliseURL) {
        this.normaliseURL = normaliseURL;
        this.lowerCase = lowerCase;
        tokenPattern = punctuationPattern==null || punctuationPattern.trim().isEmpty()?
                         Pattern.compile(core+"|"+emoticon, Pattern.CASE_INSENSITIVE):
                         Pattern.compile(core+"|("+punctuationPattern+")|"+emoticon, Pattern.CASE_INSENSITIVE);

        emoticonPattern = Pattern.compile(emoticon, Pattern.CASE_INSENSITIVE);
    }


    /**
     * Set the punctuation patter. Caution: involves re-compiling of regex.
     * @param punctuationPattern
     */
    public void setPunctuationPattern(String punctuationPattern) {
        tokenPattern = Pattern.compile(core+"|("+punctuationPattern+")|"+emoticon, Pattern.CASE_INSENSITIVE);
    }

    public void setPunctuationFilteringOffline(){
        setPunctuationPattern("[!?\"#$%&'()*+,-./:;<=>@\\[\\]^_`{|}~]+");
    }

    public void setPunctuationFilteringOnline(){
        tokenPattern =  Pattern.compile(core+"|"+emoticon, Pattern.CASE_INSENSITIVE):
    }

    @Override
    public Document tokenise(Instance document) {
        Document tokenised = new Document(document);
        String text = document.text;
        text = emoticonsToUpperCase(lowerCase? text.toLowerCase() : text);
        if (normaliseURL) text = urlPattern.matcher(text).replaceAll("HTTPLINK");
        Matcher m = tokenPattern.matcher(text);
        while (m.find()) {
            tokenised.add(new AnnotatedToken(text.substring(m.start(), m.end())));
        }
        return tokenised;
    }

    @Override
    public String configuration() {
        return "PARAM:normaliseURL:"+String.valueOf(normaliseURL)+
              ":PARAM:lowerCase:" + lowerCase +
              ":PARAM:tokenPattern:"+tokenPattern.toString();
    }


    private String emoticonsToUpperCase(String document) {
        Matcher m = emoticonPattern.matcher(document);
        StringBuilder sb = new StringBuilder();
        int lastIndex = 0;
        while (m.find()) {
            sb.append(document.substring(lastIndex, m.start()));
            sb.append(m.group(0).toUpperCase());
            lastIndex = m.end();
        }
        sb.append(document.substring(lastIndex));
        return sb.toString();
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException {
//        TokeniserTwitterBasic t = new TokeniserTwitterBasic("[!?\"#$%&'()*+,-./:;<=>@\\[\\]^_`{|}~]+", true ,true);

        File test = new File("testokeniser.ser");
//        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(test))){
//            out.writeObject(t);
//        }

        TokeniserTwitterBasic t2;
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(test))){
            t2 = (TokeniserTwitterBasic) in.readObject();
        }

        System.out.println();
    }

}
