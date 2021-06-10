package ltr.collections.jrc.extractor;

import org.apache.lucene.index.IndexReader;

import ltr.collections.jrc.indexer.EuroVocAnalyzer;
import ltr.collections.jrc.parser.EuroVocParser;
import ltr.extraction.FeaturesCalculator;
import ltr.extraction.FeaturesDefinition;
import ltr.features.QueryDocument;
import ltr.parser.Parser;


import static ltr.settings.Config.configFile;


/**
 * Calcula as features para a coleção JRC-Acquis
 */
public class EuroVocCalculator extends FeaturesCalculator{


    public static void main(String[] args) throws Exception {
        FeaturesCalculator fc = new EuroVocCalculator(
            configFile.getProperty("FEATURE_PATH_JRC"),
            configFile.getProperty("TEST_JRC"),
            configFile.getProperty("INDEX_JRC"),
            configFile.getProperty("CONCEPT_JRC"),
            configFile.getProperty("CLASSES_PATH_JRC")
        );
        fc.run();
    }

    
    public EuroVocCalculator(String featurePath, String corpusPath, String indexPath, String conceptIndexPath,
            String classesPath) {
        super(featurePath, corpusPath, indexPath, conceptIndexPath, classesPath, "jrc", ".xml");
    }

    @Override
    public FeaturesDefinition getFeaturesDefinition(IndexReader conceptReader, IndexReader documentReader) {
        return new EuroVocDefinitions(conceptReader, documentReader, new EuroVocAnalyzer(true));
    }

    @Override
    public Parser<QueryDocument> getParser() {
        return new EuroVocParser();
    }

}