package ltr.collections.jrc.retrieval;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.ScoreDoc;

import ltr.features.Feature;
import ltr.retrieval.Retrieval;

import java.util.HashMap;

public class EuroVocRetrieval extends Retrieval<Feature> {

    public EuroVocRetrieval(Analyzer analyzer) {
        super(analyzer);
    }

    @Override
    protected HashMap<String, Feature> processHits(ScoreDoc[] hits, String queryID) {
        HashMap<String,Feature>  results = new HashMap<> ();
        for (int i = 0; i < hits.length; i++) {
            Double Score = (double) hits[i].score;
            String classID = this.indexToClass.get(hits[i].doc);
            Feature f = new Feature(similarity.toString(),Score, queryID, classID, i+1);
            results.put(classID, f);
        }
        return results;
    }
}
