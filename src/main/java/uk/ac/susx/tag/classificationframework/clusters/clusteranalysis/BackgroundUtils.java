package uk.ac.susx.tag.classificationframework.clusters.clusteranalysis;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import uk.ac.susx.tag.classificationframework.datastructures.Instance;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.FeatureExtractionPipeline;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.PipelineBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Iterator;
import java.util.Map;

public class BackgroundUtils {




    public static Iterator<Instance> wikiIterator(File file) throws IOException {
        Gson gson = new Gson();

        LineIterator itr = FileUtils.lineIterator(file, "UTF-8");

        return new Iterator<Instance>() {

            int i = 0;

            private Instance next = null;

            @Override
            public boolean hasNext() {
                String index = null ;
                String data = null ;
                if(itr.hasNext()) {
                    index = itr.nextLine();
                }
                if(itr.hasNext()) {
                    data = itr.nextLine();
                }

                if(index == null || data == null) {
                    return false;
                } else {
                    String text = (String) gson.fromJson(data, Map.class).get("text");
                    String id = (String) ( (Map)  gson.fromJson(index, Map.class).get("index")).get("_id");
                    System.out.println(text);
                    System.out.println(id);
                    next = new Instance(null, text, id);
                    return true;
                }
            }

            @Override
            public Instance next() {
                if(++i % 1000 == 0) {
                    System.out.println(i);
                }
                return next;
            }
        };
    }


    /**
     * Wiki data obtained from the 'text' attribute of Cirrus dumps (available from cirrussearch).
     * See https://pypi.org/project/wikiextractor/ (extractor was not used).
     */
    public static void german() throws IOException {

        String input = "/home/sw206/Downloads/dewiki-20210823-cirrussearch-content.json";
        String pipelineOut = "/home/sw206/de-pipeline.ser";
        String countsOut = "/home/sw206/de-wiki-20210823-cirrussearch-content-feat-counts.ser";

        FeatureExtractionPipeline pipeline = new PipelineBuilder().build(new PipelineBuilder.OptionList() // Instantiate the pipeline.
                .add("tokeniser", ImmutableMap.of(
                                "type", "germanstanford",
                                "filter_punctuation", true,
                                "normalise_urls", true
                        )
                )
                .add("remove_stopwords", ImmutableMap.of(
                        "use", "true",
                        "lang", "de"))
//                .add("filter_regex", "[\\-（()）【\\[\\]】]")
                .add("unigrams", true)
        );

        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(new File(pipelineOut)))){
            out.writeObject(pipeline);
        }

        File file = new File(input);

        Iterator<Instance> itr = wikiIterator(file);

        IncrementalFeatureCounter cNew = new IncrementalFeatureCounter(0.1);
        cNew.incrementCounts(itr, pipeline, 500);
        cNew.pruneFeaturesWithCountLessThanN(3);
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(countsOut))){
            out.writeObject(cNew);
        }
    }

    public static void main(String[] args) throws IOException {
        german();
    }
}
