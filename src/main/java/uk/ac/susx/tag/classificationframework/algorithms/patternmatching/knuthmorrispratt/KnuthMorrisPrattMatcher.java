package uk.ac.susx.tag.classificationframework.algorithms.patternmatching.knuthmorrispratt;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Find all matches of multiple sequences within a larger sequence.
 *
 *   1. Patterns of the same type won't produce overlapping matches
 *   2. Patterns of a different type will produce overlapping matches.
 *   3. Patterns that are subpatterns of other patterns will produce overlapping matches with their superpatterns.
 *      So you could find separately the matches for ["brown", "dog"] and ["big", "brown", "dog"]
 *
 * Created by Andrew D. Robertson on 26/05/2016.
 */
public class KnuthMorrisPrattMatcher<E> {

    private SortedMap<PatternState<E>, PrefixFunction<E>> patternPrefixFunctions;

    public KnuthMorrisPrattMatcher() {
        patternPrefixFunctions = new TreeMap<>((o1, o2) -> Integer.compare(o2.size(), o1.size())); // Descending order so largest matches first
    }

    public List<Match<E>> getMatches(List<E> query) {
        List<Match<E>> matches = new ArrayList<>();


        for (int i = 0; i < query.size(); i++) {
            for (SortedMap.Entry<PatternState<E>, PrefixFunction<E>> entry : patternPrefixFunctions.entrySet()) {
                PatternState<E> p = entry.getKey();
                PrefixFunction<E> prefixFunc = entry.getValue();

                while(p.getState() > 0 && !p.nextPart().equals(query.get(i))) {
                    p.setState(prefixFunc.get(p.getState()));
                }
                if (p.nextPart().equals(query.get(i))){
                    p.incState();
                }
                if (p.isMatch()) {
                    matches.add(new Match<>(i - p.size() + 1, i, p.pattern));
                    p.resetState();
                }
            }
        }

        return matches;
    }

    public void addPattern(List<E> pattern){
        addPattern(ImmutableList.copyOf(pattern));
    }

    public void addPattern(ImmutableList<E> pattern){
        patternPrefixFunctions.put(new PatternState<>(pattern), new PrefixFunction<>(pattern));
    }

    public static class Match<E> {
        public int start;
        public int end;
        public ImmutableList<E> pattern;

        public Match(int start, int end, ImmutableList<E> pattern) {
            this.start = start;
            this.end = end;
            this.pattern = pattern;
        }

        @Override
        public String toString() {
            return "Match{" +
                    "start=" + start +
                    ", end=" + end +
                    ", pattern=" + pattern +
                    '}';
        }
    }

    private static class PatternState<E> {

        private ImmutableList<E> pattern;
        private int q;

        public PatternState(ImmutableList<E> pattern) {
            this.pattern = pattern;
            q = 0;
        }

        public int getState() {
            return  q;
        }

        public E nextPart() {
            return pattern.get(q);
        }

        public void setState(int q){
            this.q = q;
        }

        public void incState(){
            q++;
        }

        public void resetState() {
            q = 0;
        }

        public ImmutableList<E> getPattern(){
            return pattern;
        }

        public int size() {
            return pattern.size();
        }

        public boolean isMatch() {
            return q == pattern.size();
        }
    }

    private static class PrefixFunction<E> {

        private int[] prefixFunction;

        PrefixFunction(ImmutableList<E> pattern) {
            prefixFunction = new int[pattern.size()];

            int k = 0;
            for (int q = 1; q < pattern.size(); q++){
                while (k > 0 && !pattern.get(k).equals(pattern.get(q))) {
                    k = prefixFunction[k];
                }
                if (pattern.get(k).equals(pattern.get(q))) {
                    k++;
                }
                prefixFunction[q] = k;
            }
        }

        public int get(int q) {
            return prefixFunction[q];
        }

        public String toString() {
            return Arrays.toString(prefixFunction);
        }
    }

    public static void main(String[] args){


//        PrefixFunction<String> p = new PrefixFunction<>(ImmutableList.of("a", "b", "a", "b", "a", "b", "a", "b", "c", "a"));
//        System.out.println(p);

//        SortedMap<ImmutableList<String>, Integer> map = new TreeMap<>((o1, o2) -> Integer.compare(o2.size(), o1.size()));
//        map.put(ImmutableList.of("a", "b", "c"), 3);
//        map.put(ImmutableList.of("a", "b"), 2);
//        map.put(ImmutableList.of("a", "b", "c", "d"), 4);
//        map.put(ImmutableList.of("a", "b", "c", "d", "e"), 5);
//
//        for (SortedMap.Entry<ImmutableList<String>, Integer> entry : map.entrySet()) {
//            System.out.println(entry.getKey() + " : " + entry.getValue());
//        }
//
//        Iterator<SortedMap.Entry<ImmutableList<String>, Integer>> iterator = map.entrySet().iterator();
//        while (iterator.hasNext()){
//            SortedMap.Entry<ImmutableList<String>, Integer> entry = iterator.next();
//            System.out.println(entry.getKey() + " : " + entry.getValue());
//        }
//
        KnuthMorrisPrattMatcher<String> k = new KnuthMorrisPrattMatcher<>();
//        k.addPattern(ImmutableList.of("a", "b", "a", "b", "c", "a"));
        k.addPattern(ImmutableList.of("a", "b", "a"));
        k.addPattern(ImmutableList.of("a", "b"));

        for (Match m : k.getMatches(Lists.newArrayList("a", "b", "a", "b", "a"))){
            System.out.println(m);
        }
    }
}
