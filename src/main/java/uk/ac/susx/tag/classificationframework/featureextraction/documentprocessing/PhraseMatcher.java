package uk.ac.susx.tag.classificationframework.featureextraction.documentprocessing;

import com.google.common.collect.ImmutableList;
import uk.ac.susx.tag.classificationframework.algorithms.patternmatching.ahocorasick.AhoCorasickMatcher;
import uk.ac.susx.tag.classificationframework.algorithms.patternmatching.ahocorasick.Emit;
import uk.ac.susx.tag.classificationframework.datastructures.AnnotatedToken;
import uk.ac.susx.tag.classificationframework.datastructures.Document;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Andrew D. Robertson on 07/06/2016.
 */
public class PhraseMatcher extends DocProcessor {

    private static final long serialVersionUID = 0L;

    public static final String PHRASE_MATCH = "phraseMatch";

    private Collection<ImmutableList<String>> patterns;
    private boolean lowerCase;
    private boolean allowOverlaps;
    private boolean filterMatches;
    private transient AhoCorasickMatcher<String> matcher;

    public PhraseMatcher(Collection<ImmutableList<String>> patterns,
                         boolean lowerCase,
                         boolean allowOverlaps,
                         boolean filterMatches) {

        this.patterns = patterns;
        this.lowerCase = lowerCase;
        this.allowOverlaps = allowOverlaps;
        this.filterMatches = filterMatches;

        matcher = setupMatcher(patterns, lowerCase, allowOverlaps);
    }

    public static AhoCorasickMatcher<String> setupMatcher(Collection<ImmutableList<String>> patterns, boolean lowerCase, boolean allowOverlaps){
        AhoCorasickMatcher.Builder<String> builder = AhoCorasickMatcher.builder(lowerCase? String::toLowerCase : null);
        if (allowOverlaps)
            builder.allowOverlaps();
        patterns.forEach(builder::addPattern);
        return builder.build();
    }

    @Override
    public Document process(Document document) {
        List<String> tokens = document.stream().map(t -> t.get("form")).collect(Collectors.toList());

        List<Emit<String>> parse = matcher.parse(tokens);
        for (int j = 0; j < parse.size(); j++) {
            Emit<String> match = parse.get(j);
            for (int i = match.getStart(); i <= match.getEnd(); i++) {
                AnnotatedToken t = document.get(i);
                if (filterMatches)
                    t.setFiltered(true);
                t.put(PHRASE_MATCH, Integer.toString(j));
            }
        }
        return document;
    }

    @Override
    public String configuration() {
        return "PARAM:lowerCase" + lowerCase
            + ":PARAM:allowOverlaps" + allowOverlaps;
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        matcher = setupMatcher(patterns, lowerCase, allowOverlaps);
    }
}
