package uk.ac.susx.tag.classificationframework;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import uk.ac.susx.tag.classificationframework.datastructures.ModelState;
import uk.ac.susx.tag.classificationframework.featureextraction.documentprocessing.ArcEagerDependencyParser;
import uk.ac.susx.tag.classificationframework.featureextraction.documentprocessing.Service;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.FeatureExtractionPipeline;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.PipelineComponentFilter;

import java.io.File;
import java.io.IOException;
import java.util.Map;

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
        ModelState test = ModelState.load(new File("/Volumes/LocalDataHD/modeldirs/casm-backup/spuyten-blame"));

//
        System.out.println();

        test.pipeline.add(new Service("http://somethingsomething/dependency-parse"));

        Map<String, Object> metadata = test.metadata;
        FeatureExtractionPipeline pipeline = test.pipeline;

//        int present = test.pipeline.numComponents(Service.class, component -> component.getUrl().endsWith("dependency-parse"));

//        boolean deleted = test.pipeline.removeComponents(ArcEagerDependencyParser.class);
//        test.pipeline.removeComponentsDuplicatesOnly(Service.class, component -> component.getUrl().endsWith("dependency-parse"));

        String parserServiceURL = "http://127.0.0.1:"+ "rootserverport" + "/supergui/document-processing/dependency-parse";

        if(metadata.containsKey("dependency_parser") && metadata.get("dependency_parser").equals("true")) {
            // Remove from config
            metadata.remove("dependency_parser");
            // Remove archaic uses of non-service style parser from pipeline too
            pipeline.removeComponents(ArcEagerDependencyParser.class);

            // Add/Update config with latest http parser service URL
            metadata.put("http_service",  new Gson().toJson(ImmutableMap.of("url", parserServiceURL)));

            // Delete duplicate parser services (should only be one, but bug might have led to more, should only happen to pipelines that had "dependency_parse" in the config)
            boolean foundAny = pipeline.removeComponentsDuplicatesOnly(Service.class, component -> component.getUrl().endsWith("dependency-parse"));

            // If no parser services, then this pipeline hasn't been upgraded to use the service yet, so add one
            if (!foundAny){
                pipeline.add(new Service(parserServiceURL), "service:"+parserServiceURL);
            } else {
                pipeline.updateServices(component -> component.getUrl().endsWith("dependency-parse"), parserServiceURL);
            }

        } else if (metadata.containsKey("http_service")) {
            // Otherwise just make sure parser service URL is up to date in pipeline and config
            pipeline.updateServices(component -> component.getUrl().endsWith("dependency-parse"), parserServiceURL);
            metadata.put("http_service", new Gson().toJson(ImmutableMap.of("url", parserServiceURL)));
        }



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