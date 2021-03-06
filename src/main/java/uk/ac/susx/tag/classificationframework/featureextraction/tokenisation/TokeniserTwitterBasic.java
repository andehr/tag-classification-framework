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

import org.apache.commons.lang3.StringUtils;
import uk.ac.susx.tag.classificationframework.Util;
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
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

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

    // This was matching apostrophes at beginning and end of words
//    private static final String core = "(http://[\\\\.\\w\\-/]+)|([@#]?[\\p{L}\\p{Mn}\\d][\\p{L}\\p{Mn}'’‘`_\\d]*)|([_'’‘`]*[\\p{L}\\p{Mn}\\d][\\p{L}\\p{Mn}_'’‘`\\d]*)";
    // This only allows apostrophes inside words
    private static final String core = "(http://[\\\\.\\w\\-/]+)|([@#]?[\\p{L}\\p{Mn}\\d]([\\p{L}\\p{Mn}'’‘`_\\d]*[\\p{L}\\p{Mn}\\d]|[\\p{L}\\p{Mn}\\d]*))";
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
        setPunctuationPattern("[!?\"#$%&'()*+,-./:;<=>@\\[\\]^_`’‘{|}~]+");
    }

    public void setPunctuationFilteringOnline(){
        tokenPattern =  Pattern.compile(core+"|"+emoticon, Pattern.CASE_INSENSITIVE);
    }

    public void setNormaliseURLOnline(){
        normaliseURL = true;
    }

    public void setNormaliseURLOffline(){
        normaliseURL = false;
    }

    @Override
    public Document tokenise(Instance document) {
        Document tokenised = new Document(document);
        if (!Util.isNullOrEmptyText(document)) {
            String text = document.text;
            text = emoticonsToUpperCase(lowerCase ? text.toLowerCase() : text);
            Map<Integer, Integer> httpMatches = new HashMap<>();
            if (normaliseURL) {
                Matcher m = urlPattern.matcher(text);
                while(m.find()) {

                    int len = m.end() - m.start();
                    String replacement = StringUtils.repeat("L", len);
                    text = text.substring(0, m.start()) + replacement + text.substring(m.end());
                    httpMatches.put(m.start(), m.end());
                }
//                text = urlPattern.matcher(text).replaceAll("HTTPLINK");
            }
            Matcher m = tokenPattern.matcher(text);
            while (m.find()) {
                Integer start = m.start();
                Integer end = m.end();
                String token = text.substring(m.start(), m.end());
                if(httpMatches.containsKey(m.start())) {
                    token = "HTTPLINK";
                }
                AnnotatedToken annotatedToken = new AnnotatedToken(token);
                annotatedToken.start(start);
                annotatedToken.end(end);
                tokenised.add(annotatedToken);
            }
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

}
