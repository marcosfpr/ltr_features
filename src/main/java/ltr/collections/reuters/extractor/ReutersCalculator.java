package ltr.collections.reuters.extractor;

import org.apache.lucene.index.IndexReader;

import ltr.collections.reuters.indexer.ReutersAnalyzer;
import ltr.collections.reuters.parser.ReutersParser;
import ltr.extraction.FeaturesCalculator;
import ltr.extraction.FeaturesDefinition;
import ltr.features.QueryDocument;
import ltr.parser.Parser;

import static ltr.settings.Config.configFile;

public class ReutersCalculator extends FeaturesCalculator {

    public static void main(String[] args) throws Exception {
        FeaturesCalculator fc = new ReutersCalculator(
            configFile.getProperty("FEATURE_PATH_REUTERS"),
            configFile.getProperty("CORPUS_REUTERS"),
            configFile.getProperty("INDEX_REUTERS"),
            configFile.getProperty("CONCEPT_REUTERS"),
            configFile.getProperty("CLASSES_PATH_REUTERS")
        );
        fc.run();
    }

    public ReutersCalculator(String featurePath, String corpusPath, String indexPath, String conceptIndexPath,
            String classesPath) {
        super(featurePath, corpusPath, indexPath, conceptIndexPath, classesPath, "", "");
    }

    @Override
    public FeaturesDefinition getFeaturesDefinition(IndexReader conceptReader, IndexReader documentReader){
        return new ReutersDefinitions(conceptReader, documentReader, new ReutersAnalyzer(true));
    }

    @Override
    public Parser<QueryDocument> getParser() {
        return new ReutersParser();
    }
    
}
