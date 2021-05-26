package ltr.collections.rotten.extractor;

import org.apache.lucene.index.IndexReader;

import ltr.collections.rotten.indexer.RottenAnalyzer;
import ltr.collections.rotten.parser.RottenParser;
import ltr.extraction.FeaturesCalculator;
import ltr.extraction.FeaturesDefinition;
import ltr.features.QueryDocument;
import ltr.parser.Parser;

import static ltr.settings.Config.configFile;

public class RottenCalculator extends FeaturesCalculator {

    public static void main(String[] args) throws Exception {
        FeaturesCalculator fc = new RottenCalculator(
            configFile.getProperty("FEATURE_PATH"),
            configFile.getProperty("CORPUS"),
            configFile.getProperty("INDEX"),
            configFile.getProperty("CONCEPT"),
            configFile.getProperty("CLASSES_PATH")
        );
        fc.run();
    }

    public RottenCalculator(String featurePath, String corpusPath, String indexPath, String conceptIndexPath,
            String classesPath) {
        super(featurePath, corpusPath, indexPath, conceptIndexPath, classesPath, "", ".csv");
    }

    @Override
    public FeaturesDefinition getFeaturesDefinition(IndexReader conceptReader, IndexReader documentReader){
        return new RottenDefinitions(conceptReader, documentReader, new RottenAnalyzer(true));
    }

    @Override
    public Parser<QueryDocument> getParser() {
        return new RottenParser();
    }
    
}
