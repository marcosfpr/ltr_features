package ltr.collections.reuters.retrieval;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.ScoreDoc;

import ltr.features.Feature;
import ltr.retrieval.Retrieval;

/**
 * Define o processamento dos hits de cada consulta rotten
 */
public class ReutersRetrieval extends Retrieval<Feature>{
    static final Logger logger = Logger.getLogger(ReutersRetrieval.class.getName());
    
	public ReutersRetrieval(Analyzer analyzer) {
        super(analyzer);
    }

    @Override
    protected Map<String, Feature> processHits(ScoreDoc[] hits, String queryID) {
        Map<String,Feature>  results = new HashMap<> ();
        StringBuilder ranking = new StringBuilder();
        for (int i = 0; i < hits.length; i++) {
            Double Score = (double) hits[i].score;
            String classID = this.indexToClass.get(hits[i].doc);
            Feature f = new Feature(similarity.toString(),Score, queryID, classID, i+1);
            results.put(classID, f);
            
            ranking.append("[" + classID).append(":" + Score + "],");
        }
        return results;
    }
}
