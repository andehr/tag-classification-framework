package uk.ac.susx.tag.classificationframework.featureextraction.documentprocessing;

import com.google.common.io.Resources;
import uk.ac.susx.tag.classificationframework.datastructures.Document;
import uk.ac.susx.tag.classificationframework.exceptions.FeatureExtractionException;
import uk.ac.susx.tag.dependencyparser.Parser;
import uk.ac.susx.tag.dependencyparser.datastructures.Sentence;
import uk.ac.susx.tag.dependencyparser.datastructures.Token;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.List;

/**
 * Arc eager parsing. This is a wrapper for the dependency parser project, which allows its use in the
 * classification framework.
 *
 * See the maven dependency "uk.ac.susx.tag:dependencyparser"
 *
 * User: Andrew D. Robertson
 * Date: 25/04/2014
 * Time: 13:57
 */
public class ArcEagerDependencyParser extends DocProcessor {

    private static final long serialVersionUID = 0L;
    private static final String temporaryModel = "*TEMPORARY-TESTING-MODEL*";
    private static final String defaultModel = "*PARSER-DEFAULT*";


    private transient Parser parser;
    private String modelName;

    /**
     * Create parser with default model.
     * The default model is based on all of the WSJ with CMU PoS tags and Stanford dependency relations
     */
    public ArcEagerDependencyParser() throws IOException {
        modelName = defaultModel;
        parser = new Parser();
    }

    /**
     * Create a parser with a model name that can be found in the jar resources.
     * If you want to test a new model without having to put things in resources (involves re-compiling),
     * then see the testing 2-arg constructor.
     */
    public ArcEagerDependencyParser(String modelName) throws IOException {
        this.modelName = modelName;
        loadParserFromResources(modelName);
    }

    @Override
    public Document process(Document document) {
        List<TweetTagConverter.Token> tokens = (List<TweetTagConverter.Token>)document.getAttribute("ExpandedTokens");

        // Tokens would only be null if the "TweetTagConverter" isn't used beforehand (user created pipeline incorrectly)
        if (tokens==null)
            throw new FeatureExtractionException("TweetTagConverter must be applied to document before DependencyParser.");

        // Tokens would be empty if the sentence was the empty string
        if (tokens.isEmpty())
            return document; // If there are no tokens

        // Otherwise try to parse
        try {
            // Notice that we use the "Sentence" factory method (from the dependency parser project) to build from the expanded tokens a Sentence in the form that the parser expects.
            extractDependencies(parser.parseSentence(Sentence.createFromPoSandFormBearingTokens(tokens)), tokens);

        } catch (NullPointerException e) { // This would probably arise if the parser was constructed incorrectly (no model)
            e.printStackTrace();
            throw new FeatureExtractionException("Null pointer. Probably because you created a parser with a temporary model, then serialised and deserialised, without re-loading that temporary model. Less likely is that there was a null pointer at some point during the parsing. Check stacktrace above.");
        }
        return document;
    }

    /**
     * Given the output from the parser, annotate the expanded token list with said output.
     */
    public List<TweetTagConverter.Token> extractDependencies(List<Token> sentence, List<TweetTagConverter.Token> tokens) {
        for (int i = 0; i < sentence.size(); i++){
            Token parsedToken = sentence.get(i);
            TweetTagConverter.Token pipelineToken = tokens.get(i);
            pipelineToken.head = parsedToken.getHead().getID();
            pipelineToken.deprel = parsedToken.getDeprel();
        } return tokens;
    }

    /**
     * Outside of the pipeline framework, if you want to parse a list of these token types then use this.
     */
    public List<TweetTagConverter.Token> standaloneParse(List<TweetTagConverter.Token> tokens) {
        return extractDependencies(parser.parseSentence(Sentence.createFromPoSandFormBearingTokens(tokens)), tokens);
    }


    /**
     * Outside of the pipeline framework, if you want to parse the token types that the parser knows about, use this.
     */
    public List<Token> standaloneParse(Sentence tokens) {
        return parser.parseSentence(tokens);
    }

    @Override
    public String configuration() {
        return "modelName:"+modelName;
    }

    /**
     * TESTING PURPOSES ONLY. Will not de-serialise properly.
     *
     * If you simply must use this for testing, then upon deserialisation of the pipeline containing this parser,
     * call "loadTemporaryModel()" with the index and model files in order to complete the deserialisation of the parser.
     *
     * For this, you'll need a reference to the parser. So when adding this parser to the pipeline, be sure to use the
     * 2 argument "add" method, which names a PipelineComponent (i.e. this parser), so later you can use that name to
     * get a reference to that parser.
     */
    public ArcEagerDependencyParser(File index, File model) throws IOException {
        loadTemporaryModel(index, model);
    }

    /**
     * TESTING PURPOSES ONLY. See 2-argument constructor.
     */
    public void loadTemporaryModel(File index, File model) throws IOException {
        modelName = temporaryModel;
        parser = new Parser(index, model);
    }



    private void loadParserFromResources(String parserName) throws IOException {
        File model = File.createTempFile("model", null);
        model.deleteOnExit();

        try (BufferedOutputStream modelStream = new BufferedOutputStream(new FileOutputStream(model)) ){
            Resources.copy(Resources.getResource(parserName+"-model"), modelStream);
        }
        parser = new Parser(Resources.getResource(parserName+"-index").openStream(), model);

        if (!model.delete()) System.err.print("WARNING: model temp file was not deleted: "+ model.getAbsolutePath());
    }

    /**
     * The serialisation of the parser model is not the duty of this class. So this ensures that the model
     * is re-loaded by the relevant party upon deserialisation.
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        if (!modelName.equals(temporaryModel)){
            if (modelName.equals(defaultModel)) parser = new Parser();
            else loadParserFromResources(modelName);
        }
    }
}
