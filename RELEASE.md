# Release Notes

## Version 1.6.0

* Added Evaluation class. Deprecates evaluation method in Util.
* Evaluation class includes full confusion matrix
* Added extractFeatures() method in pipeline, which takes an iterable, and outputs a lazy iterable over ProcessedInstances

## Version 1.5.0

Querying related:
* Added getAlphaValue() to Querying, calculates an automatically scaled alpha for a feature based on its frequency.
* Added another labelledFeatures2Strings() method to Querying.
* The threshold for correlation of feature queries is now an adjustable extra argument to queryFeatures.

Feature inference related:
* Added FeatureInferrerDependencyNGrams. Extracts syntactic bigrams and trigrams

ProcessedInstance related:
* Added hasFeatures() function; returns true when features array has length > 0
* Util function "inferVocabulary" which allows you to build the vocabulary of a collection of ProcessedInstances
* getLabelProbability() function added for convenience. Returns P(label|instance)
* ProcessInstance now selects best label randomly if the probabilities are uniform.
* processDocument() and extractFeatures() on the FeatureExtractionPipeline now check for a cache by default (but in the absence of a cache, default to re-processing the data without complaint)
* processDocumentWithoutCache() and extractFeaturesWithoutCache() methods now available on the FeatureExtractionPipeline, which avoid the caching check.
* JsonIteratorProcessedInstances now uses extractFeatureWithCache() on the pipeline (which defaults to old behaviour if no cache available).
* extractFeaturesCachedOnly() function added to FeatureExtractionPipeline. Will return only cached Documents. Allows use of pipeline with no tokeniser or document processors (good for debugging).

Parsing related:
* Moved DependencyTree into datastructures package, so that other classes and users can make use of it.
* Added pipeline factory function createCMUPipelineWithParserAlt() which uses the tokenisation and tagging of CMU separately, allowing the lowercasing of tweets which are likely to be headlines, before PoS tagging them (hopefully avoiding the 'tag everything as proper noun' problem). Remains to be seen if this is useful...
* In the convenience function createCMUPipelineWithParser() in the PipelineFactory, the parser is now a named PipelineComponent
* TweetTagConverter now splits "cannot" into "can" and "not", so that the parser can apply the "neg" relation.

Misc:
* TokenNormaliserByForm now has 3 factory methods, which can create normalisers which normalise intensifiers, negators and diminishers (see "Sentiment Analysis in Social Media Texts" by Balahur). PipelineConfiguation updated accordingly.
* Bug fixes. Nasty ones too. Parser stumbled over non-ascii characters... Token expander sometimes produced array out of bounds error...
* Added ability to construct exceptions from other exceptions.
* Added getAttribute() to Document. Convenience function for getting attributes.
* Added a gender classifier to the Util class, and the required pipeline classes to get it done.
* Pipeline configuration has more options.
* More pipeline components for filtering.
* Added a convenience factory method to TokenNormaliserByFormRegexRegexReplace which replaces repeated characters
* Updated PipelineConfiguration for more common options.
* Added method of feature selection (FeatureSelectors, subclass of FeatureInferrers). They have their own way of being added to a FeatureExtractionPipeline
* Bugfixes to gender pipeline.

---

## Version 1.4.0

* setCache() on FeatureExtractionPipeline now allows overriding of old collections.

---

## Version 1.3.0

* New convenience pipeline in PipelineFactory: createCMUPipelineWithParsing()
* Caching methods on FeatureExtractionPipeline, integration with MongoDB
* Use setCache() on FeatureExtractionPipeline to add a MongoDB collection for caching of Document instances.
* Added methods to interfaces of Tokenisers and DocProcessors, for obtaining a string representation of the configuration of the components.
* Commenting, refactoring for clarity.

---

## Version 1.2.0

* PipelineFactory and PipelineConfiguration now support dependency parsing, tag conversion and keywords set.
* Started new FeatureInferrer for using dependency relations and wildcarding.

---

## Version 1.1.0

* Added "remove" method to FeatureInferrerNegativeFeatures
* Added "binary" option to the FeatureInferrerNegativeFeatures, and corresponding setter and getter.
* Parser throws exception when converted tokens are not found when processing a document.
* Tested parser for empty documents, s'all good.
* Tag converter now throws an exception if CMU tags are not assigned to the document beforehand.
* Updated comments.
* Writer/Reader ensure utf-8
* Optimized imports

---

## Version 1.0.0

* DependencyParser now incorporated (including Jython code).
* CmuPipeline and BasicPipeline removed (changed CompatibilityModelState to reflect this)
* Updated comments.

---

## Version 0.8

* PipelineFactory and moved pipeline structures to pipelines package. Changed the way they are instantiated
* More dependency stuff, resource location.
* readMetadata() function on ModelState.

---

## Version 0.7.1

* Changed URL normalisation replacement to "HTTPLINK" from "HTTP-LINK" in cmupipeline.
* Updated comments in NaiveBayesClassifier

---

## Version 0.7

* Made FeatureExtractionPipeline abstract, must be subclassed to instantiate pipeline
* Added pipelines package in feature extraction with subclassed FeatureExtractionPipelines
* pipelines package replaces the Util methods which made pipelines.
* Added Jython parser interface
* the basic tokeniser now has optional URL normalisation
* Url normalisation is added as an option with the CMU Pipeline

---

## Version 0.6

* Begun DependencyParser.
* Jython is now a dependency.
* Loading of converted old models.
* Bugfixes in ModelState saving and loading for when fields are null.
* Changed serialisation of ModelState metadata to use JSON

---

## Version 0.5.2

* Bug fix in `unlabelFeature`

---

## Version 0.5.1

* `unlabelFeature` added to NaiveBayesClassifier.

---

## Version 0.5

* Release new `DocProcessor` the `TweetTagConverter`.

---

## Version 0.4

* `setFeatureAlphas()` added to `Util`. Convenience method which calls the existing `Util.setFeatureAlpha()` repeatedly for multiple features.
* `resetLabeling()` method on `ProcessedInstance` for setting its label to -1 and its labelProbabilities to an empty map.
* `setTrainingDocumentsWithNewLabels()` in ModelState, retrieves the source instances of a list of ProcessedInstance, overwriting the Instance labels with whatever the ProcessedInstance was labelled with.
* More progress on TweetTagConverter.

---

## Version 0.3.1

* `NaiveBayesClassifier.java` usage examples are now non-stupid
* `Querying.java` comments.

---

## Version 0.3

* `labelledFeatures2Strings()` in Util now returns a List of features ordered by information gain, rather than a set of high information gain features no longer ordered.
* Added `setTrainingDocuments()` to ModelState, which allows the assignment of a List of ProcessedInstances to a ModelState (it will automatically populate a list of the source Instances)
* `LogicalCollection` now maintains a `size` field, which it updates as collections are added to it, rather than calculating it on the fly for every call to `size()`
* Removing commented out code
* Adding comments (especially to Querying.java and ModelState.java)
* Bugfix somewhere that I forgot...
* `Instance` has `toString()` this may be in 0.2...
* More progress on `TweetTagConverter`

---
