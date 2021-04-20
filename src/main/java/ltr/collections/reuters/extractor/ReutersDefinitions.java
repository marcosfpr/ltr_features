package ltr.collections.reuters.extractor;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.apache.lucene.index.IndexReader;

import ltr.collections.reuters.indexer.ReutersAnalyzer;
import ltr.collections.reuters.retrieval.ReutersRetrieval;
import ltr.extraction.FeaturesDefinition;
import ltr.features.Feature;
import ltr.retrieval.Retrieval;

public class ReutersDefinitions extends FeaturesDefinition {
	static final Logger logger = Logger.getLogger(FeaturesDefinition.class.getName());

    public ReutersDefinitions(IndexReader conceptReader, IndexReader docReader, ReutersAnalyzer analyzer){
        this.conceptReader = conceptReader;
        this.documentReader = docReader;
        this.retrievalEngine = getRetrievalEngine(analyzer);
        loadIndexDocs();
    }

    public ReutersDefinitions(IndexReader conceptReader, IndexReader docReader,
    		ReutersAnalyzer analyzer, Map<String, String> mapClassToId) {
        this.conceptReader = conceptReader;
        this.documentReader = docReader;
        this.retrievalEngine = getRetrievalEngine(analyzer);
        this.mapClassToId = mapClassToId;
    	loadIndexDocs();
    }

    private void loadIndexDocs() {
        this.indexToClass = new TreeMap<>();
        this.docToIndex = new TreeMap<>();
        
        for(int i = 0; i < this.conceptReader.numDocs(); i++) {
            try{
                String cId = this.conceptReader.document(i).get("ID");
                this.indexToClass.put(i, cId);
            }
            catch (IOException e){
                logger.error(e.getMessage());
            }
        }
        
        for(int i = 0; i < this.documentReader.numDocs(); i++) {
        	try {
        		String did = this.documentReader.document(i).get("ID");
        		this.docToIndex.put(did, i);
        	}
        	catch (IOException e){
                logger.error(e.getMessage());
            }
        }
    }

    private Retrieval<Feature> getRetrievalEngine(ReutersAnalyzer analyzer) {
        if (this.retrievalEngine == null)
            this.retrievalEngine = new ReutersRetrieval(analyzer.getAnalyzer("EN"));
        return retrievalEngine;
    }
        

}
