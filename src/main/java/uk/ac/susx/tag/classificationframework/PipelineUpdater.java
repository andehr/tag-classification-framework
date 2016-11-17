package uk.ac.susx.tag.classificationframework;

import uk.ac.susx.tag.classificationframework.datastructures.ModelState;

import java.io.File;
import java.io.IOException;

/**
 * Class for doing maintenance on old FeatureExtractionPipelines
 *
 * Created by Andrew D. Robertson on 16/11/2016.
 */
public class PipelineUpdater {

    public static void updateServicesInPipeline(File modelDir, String oldUrl, String newUrl) throws IOException, ClassNotFoundException {
        ModelState state = ModelState.load(modelDir);
        state.pipeline.updateService(oldUrl, newUrl);
        state.save(modelDir);
    }

    public static void updateServiceInAllPipelines(File directoryContainingModels, String oldUrl, String newUrl) throws IOException, ClassNotFoundException {
        for (File modelDir : directoryContainingModels.listFiles(File::isDirectory)) {
            System.out.println("Updating "+modelDir.getName());
            updateServicesInPipeline(modelDir, oldUrl, newUrl);
        }
    }

    public static void main(String[] args) throws Exception {
//        FeatureExtractionPipeline pipeline = new PipelineBuilder().build(new PipelineBuilder.OptionList() // Instantiate the pipeline.
//                .add("tokeniser", ImmutableMap.of(
//                        "type", "basic",
//                        "filter_punctuation", true,
//                        "normalise_urls", true,
//                        "lower_case", true
//                        )
//                )
//                .add("http_service", ImmutableMap.of("url", "http://test.co.uk"))
//                .add("unigrams", true)
//        );
//
//        ModelState test = new ModelState(new NaiveBayesClassifier(new IntOpenHashSet()), new ArrayList<>(), pipeline);
//        test.save(new File("C:\\Users\\Andy\\Desktop\\test"));
//
//        updateServiceInAllPipelines(new File("C:\\Users\\Andy\\Desktop\\models"), "http://test.co.uk", "http://newtest.co.uk");
//
        ModelState test = ModelState.load(new File("/Volumes/LocalDataHD/git/method52/data/root/dev/nb-models/js-hmic-relevance-2"));
//
        System.out.println();

//        switch(args[0]){
//            case "updateService":
//                File dirContainingModels = new File(args[1]);
//                String oldUrl = args[2];
//                String newUrl = args[3];
//                updateServiceInAllPipelines(dirContainingModels, oldUrl, newUrl);
//            default:
//                System.out.println("Unrecognised maintenance type.");
//        }
    }
}