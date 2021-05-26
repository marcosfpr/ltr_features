package ltr.collections.jrc.extractor;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.apache.lucene.search.similarities.TFIDFSimilarity;
import org.apache.lucene.util.BytesRef;

import ltr.collections.jrc.indexer.EuroVocAnalyzer;
import ltr.collections.jrc.retrieval.EuroVocRetrieval;
import ltr.extraction.FeaturesDefinition;
import ltr.features.Feature;
import ltr.features.QueryDocument;
import ltr.index.IndexInfo;
import ltr.retrieval.Retrieval;

public class EuroVocDefinitions extends FeaturesDefinition{
	static final Logger logger = Logger.getLogger(FeaturesDefinition.class.getName());
    private HashMap<String,Feature> parentsNum;
    private HashMap<String,Feature> childrenNum;

    private HashMap<Integer,String> descriptions;

    public EuroVocDefinitions(IndexReader conceptReader, IndexReader docReader, EuroVocAnalyzer analyzer){
        this.conceptReader = conceptReader;
        this.documentReader = docReader;
        this.retrievalEngine = getRetrievalEngine(analyzer);
        loadIndexDocs();
    }

    public EuroVocDefinitions(IndexReader conceptReader, IndexReader docReader,
        EuroVocAnalyzer analyzer, Map<String, String> mapClassToId) {
        this.conceptReader = conceptReader;
        this.documentReader = docReader;
        this.retrievalEngine = getRetrievalEngine(analyzer);
        this.mapClassToId = mapClassToId;
        loadIndexDocs();
    }


    /**
     * Para cada conceito de classe é gerada uma feature que retorna o número de "pais" da classe
     * @param doc
     * @return
     */
    public HashMap<String, Feature> documentParentFeatureBased(QueryDocument doc) {
        HashMap<String, Feature> features = new HashMap<>();

        if(this.parentsNum == null){
            parentsNum = new HashMap<>();
            IndexInfo indexInfo = new IndexInfo(this.documentReader);

            for (int i = 0; i < this.documentReader.numDocs(); i++) {
                Feature f = new Feature("pNum", indexInfo.getDocumentLength(i, "PARENTS").doubleValue(),
                        this.indexToClass.get(i));
                parentsNum.put(this.indexToClass.get(i), f);
            }

        }
        else{
            features = parentsNum;
        }

        return features;
    }

    /**
     * Para cada conceito de classe é gerada uma feature que retorna o número de "filhos" da classe
     * @param doc
     * @return
     */
    public HashMap<String, Feature> documentChildrenFeatureBased(QueryDocument doc) {

        HashMap<String, Feature> features = new HashMap<>();

        if(childrenNum == null){
            childrenNum = new HashMap<>();
            IndexInfo indexInfo = new IndexInfo(this.documentReader);

            for (int i = 0; i < this.documentReader.numDocs(); i++) {
                Feature f = new Feature("cNum", indexInfo.getDocumentLength(i, "CHILDREN").doubleValue(),
                        this.indexToClass.get(i));
                childrenNum.put(this.indexToClass.get(i), f);
            }

        }
        else{
            features = childrenNum;
        }

        return features;
    }


    /**
     * Para cada conceito de classe é gerada uma feature que retorna o número de "documentos" relacionados à classe
     * @param doc
     * @return
     */
    public HashMap<String, Feature> documentNumberFeatureBased(QueryDocument doc) {
        if(this.docNum == null){
            docNum = new HashMap<>();
            for (int i = 0; i < this.documentReader.numDocs(); i++) {
                try {
                    String[] docs = documentReader.document(i).get("DOCS").split("\\s+");
                    Feature f = new Feature("docNum", (double)docs.length , this.indexToClass.get(i));
                    docNum.put(this.indexToClass.get(i), f);
                } catch (IOException ex) {
                    logger.error(ex.getMessage());
                }
            }
        }
        return new HashMap<>(docNum);

    }

    private void loadIndexDocs() {
        this.indexToClass = new TreeMap<>();
        this.docToIndex = new TreeMap<>();
        this.descriptions = new HashMap<>();

        for(int i = 0; i < this.conceptReader.numDocs(); i++) {
            try{
                String cId = this.conceptReader.document(i).get("ID");
                String desc = this.conceptReader.document(i).get("DESC");
                this.descriptions.put(i, desc);
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

    private Retrieval<Feature> getRetrievalEngine(EuroVocAnalyzer analyzer) {
        if (this.retrievalEngine == null)
            this.retrievalEngine = new EuroVocRetrieval(analyzer.getAnalyzer("EN"));
        return retrievalEngine;
    }


    /**
     * Para cada conceito de classe é gerada uma feature que retorna o IDF de cada conceito na coleção de documentos
     * @param doc
     * @return
     * @throws IOException 
     */
    @Override
    public Map<String, Feature> idfFeature() throws IOException {
    	if(this.idf == null) {
			this.idf = new HashMap<String, Feature>();
	    	
            TFIDFSimilarity tfidfSIM = new DefaultSimilarity();

             for (int i = 0; i < this.conceptReader.numDocs(); i++) {  
                 String desc  = this.descriptions.getOrDefault(i, "");
                 Term term = new Term("TEXT", desc.trim());
                 float idf = tfidfSIM.idf( this.documentReader.docFreq(term) ,this.documentReader.numDocs());
                 this.idf.put(this.indexToClass.get(i), new Feature("idfClass",
                                  (double)idf, this.indexToClass.get(i)));
             }
    	}
		
		return this.idf;
    }
    

    /**
     * TF de cada classe em um documento
     * @return
     * @throws IOException 
     */
    @Override
    public Map<String, Feature> tfFeature(QueryDocument doc) throws IOException {
		Map<String, Feature> docFrequencies = new HashMap<String, Feature>();
    	
		TFIDFSimilarity tfidfSIM = new DefaultSimilarity();

        for (int i = 0; i < this.conceptReader.numDocs(); i++) {  
            String desc  = this.descriptions.getOrDefault(i, "");

            float freq = 0;
            for (String term : doc.getTitle().split(" ")) {
                term = term.trim();
                if (desc.contains(term)) freq++;
            }
            
            float tf = tfidfSIM.tf( freq );

            docFrequencies.put(this.indexToClass.get(i), new Feature("tfClassDoc", (double)tf, this.indexToClass.get(i)));
        }
    
		return docFrequencies;
    }
    
    /**
     * Valor booleano ponderado de cada classe em um documento
     * @return
     * @throws IOException 
     */
    @Override
    public HashMap<String, Feature> countFeature(QueryDocument doc) throws IOException {
    	
		HashMap<String, Feature> docBoolean = new HashMap<String, Feature>();

        for (int i = 0; i < this.conceptReader.numDocs(); i++) {  
            String desc  = this.descriptions.getOrDefault(i, "");
            
            int times = 0;

            String[] terms = doc.getTitle().split(" ");
            for (String term : terms) {
                term = term.trim();
                if(desc.contains(term)){
                    times++;
                }  	 
	         }

            docBoolean.put(this.indexToClass.get(i), new Feature("classInDoc", (double)times, this.indexToClass.get(i)));     
        }
	
		return docBoolean;
    }


        /**
     * Valor booleano ponderado de cada classe em um documento
     * @return
     * @throws IOException 
     */
    @Override
    public HashMap<String, Feature> booleanFeature(QueryDocument doc) throws IOException {
    	
		HashMap<String, Feature> docBoolean = new HashMap<String, Feature>();

        for (int i = 0; i < this.conceptReader.numDocs(); i++) {  
            String desc  = this.descriptions.getOrDefault(i, "");
            
            boolean or_clause = false;

            String[] terms = doc.getText().split(" ");
            for(String term : terms) {
                term = term.trim();
                if(desc.contains(term)){
                    or_clause = true;
                    break;
                }  	 
            }
            double sim = or_clause ? 1.0 : 0.0;
	         
            docBoolean.put(this.indexToClass.get(i), new Feature("booleanDoc", (double)sim, this.indexToClass.get(i)));  
        }
	
		return docBoolean;
    }


}
