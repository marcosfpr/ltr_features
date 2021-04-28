package ltr.extraction;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.apache.lucene.search.similarities.TFIDFSimilarity;
import org.apache.lucene.util.BytesRef;

import ltr.features.Feature;
import ltr.features.QueryDocument;
import ltr.index.IndexInfo;
import ltr.retrieval.Retrieval;

/**
 * Definição de features genéricas para LTR
 */
public abstract class FeaturesDefinition {

    protected IndexReader documentReader;
    protected IndexReader conceptReader;
    protected Retrieval<Feature> retrievalEngine;

    protected Map<String, Feature> docNum;
    protected Map<String, Feature> docIdfs;
    protected Map<String, Feature> classFrequencies; // classID -> frequencia total no indice
    protected Map<String,Feature> parentsNum;
    protected Map<String,Feature> childrenNum;

    protected Map<Integer, String> indexToClass; // indice do lucene -> classID (id do genero)
    protected Map<String, String> mapClassToId; // class -> indice
    protected Map<String, Integer> docToIndex; // docID -> indice do lucene


    /**
     * Pesquisa o conteudo do documento (indice original) no corpo do conceito de
     * cada classe.
     * 
     * @param doc
     * @param queryField
     * @param docField
     * @param simFunc
     * @param params
     * @return
     * @throws ParseException
     */
    public Map<String, Feature> documentFeatureBased(QueryDocument doc, String queryField, String docField,
            String simFunc, List<Float> params) throws ParseException {

        Map<String, Feature> features = new HashMap<>();

        // configure engine
        retrievalEngine.setIndexReader(this.conceptReader);
        retrievalEngine.setField(docField);
        retrievalEngine.setSimName(simFunc);
        retrievalEngine.setParams(params);
        retrievalEngine.setIndexToClass(indexToClass);

        if (queryField.equalsIgnoreCase("TEXT")) {
            features = retrievalEngine.search(doc.getText(), doc.getId());
        } else if (queryField.equalsIgnoreCase("TITLE")) {
            features = retrievalEngine.search(doc.getTitle(), doc.getId());
        }

        return features;
    }

    /**
     * Para cada conceito de classe é gerada uma feature que retorna o número de
     * "documentos" relacionados a ele
     * 
     * @param doc
     * @return
     * @throws IOException
     */
    public Map<String, Feature> documentNumberFeatureBased() throws IOException {
        if(this.docNum == null){
            this.docNum = new HashMap<>();
            for (int i = 0; i < this.conceptReader.numDocs(); i++) {  
                String[] docs = conceptReader.document(i).get("DOCS").split("\\s+");
                Feature f = new Feature("docNum", (double)docs.length , this.indexToClass.get(i));
                docNum.put(this.indexToClass.get(i), f);
            }
        }
        return this.docNum;
    }

    /**
     * Feature a nível de classe que indica a frequencia total dela na coleção de
     * documentos
     * 
     * @return
     * @throws IOException
     */
    public Map<String, Feature> classTotalFrequency() throws IOException {
        if(this.classFrequencies == null){
            this.classFrequencies = new HashMap<>();
			for (Map.Entry<String, String> entry : this.mapClassToId.entrySet()){
                String[] keywords = entry.getKey().split("&");
                double value = 0;
                for(String keyword: keywords){
                    Term term = new Term("TEXT", keyword.trim()); 
                    value += this.documentReader.totalTermFreq(term);
                }
                this.classFrequencies.put(entry.getValue(), new Feature("classFreq", value, entry.getValue()));
            }
        }
        return this.classFrequencies;
    }


    /**
     * Para cada conceito de classe é gerada uma feature que retorna o IDF de cada conceito na coleção de documentos
     * @param doc
     * @return
     * @throws IOException 
     */
    public Map<String, Feature> idfFeature() throws IOException {
    	if(this.docIdfs == null) {
			this.docIdfs = new HashMap<String, Feature>();
	    	
            TFIDFSimilarity tfidfSIM = new DefaultSimilarity();
	     
			for (Map.Entry<String, String> entry : this.mapClassToId.entrySet())
			 {
                float idf = 0;
                String[] keywords = entry.getKey().split("&");
                for( String t1 : keywords){
                    Term term = new Term("TEXT", t1.trim()); 
                    idf += tfidfSIM.idf( this.documentReader.docFreq(term) ,this.documentReader.numDocs());
                }
                this.docIdfs.put(entry.getValue(), new Feature("idfClass",
                                 (double)idf/keywords.length, entry.getValue()));      
			 }
    	}
		
		return this.docIdfs;
    }
    

    /**
     * TF de cada classe em um documento
     * @return
     * @throws IOException 
     */
    public Map<String, Feature> tfFeature(QueryDocument doc) throws IOException {
		Map<String, Feature> docFrequencies = new HashMap<String, Feature>();
    	
		TFIDFSimilarity tfidfSIM = new DefaultSimilarity();
     
		 for (Map.Entry<String, String> entry : this.mapClassToId.entrySet())
		 {
			 String[] words = entry.getKey().split("&");

			 Terms termVector = this.documentReader.getTermVector(this.docToIndex.get(doc.getId()), "TEXT");
	         TermsEnum itr = termVector.iterator(null);
	         BytesRef text = null;
	         float freq = 0;

	         while((text = itr.next()) != null) {
	        	 String term = text.utf8ToString();
	        	 for(String word : words){
                     if(word.trim().toLowerCase().contains(term.trim().toLowerCase()))
	        		    freq++;
	        	 }
	         }
             float tf = tfidfSIM.tf( freq );
             docFrequencies.put(entry.getValue(), new Feature("tfClassDoc", (double)tf, entry.getValue()));     
		 }
    
		return docFrequencies;
    }
    
    /**
     * Valor booleano ponderado de cada classe em um documento
     * @return
     * @throws IOException 
     */
    public HashMap<String, Feature> countFeature(QueryDocument doc) throws IOException {
    	
		HashMap<String, Feature> docBoolean = new HashMap<String, Feature>();
    	     
		 for (Map.Entry<String, String> entry : this.mapClassToId.entrySet())
		 {
			 String[] words = entry.getKey().split("&");
			 
             int times = 0;

			 Terms termVector = this.documentReader.getTermVector(this.docToIndex.get(doc.getId()), "TEXT");
	         TermsEnum itr = termVector.iterator(null);
	         BytesRef text = null;
	         
             while((text = itr.next()) != null) {
                String term = text.utf8ToString();
                
                for(String word : words){
                    if(word.contains(term)){
                        times++;
                    }
                }
	        	 
	         }
	         
             docBoolean.put(entry.getValue(), new Feature("classInDoc", (double)times, entry.getValue()));     
		 }
	
		return docBoolean;
    }


        /**
     * Valor booleano ponderado de cada classe em um documento
     * @return
     * @throws IOException 
     */
    public HashMap<String, Feature> booleanFeature(QueryDocument doc) throws IOException {
    	
		HashMap<String, Feature> docBoolean = new HashMap<String, Feature>();
    	     
		 for (Map.Entry<String, String> entry : this.mapClassToId.entrySet())
		 {
			 String[] words = entry.getKey().split("&");
			 
             boolean or_clause = false;

			 Terms termVector = this.documentReader.getTermVector(this.docToIndex.get(doc.getId()), "TEXT");
	         TermsEnum itr = termVector.iterator(null);
	         BytesRef text = null;
	         
             while((text = itr.next()) != null && or_clause != true) {
                String term = text.utf8ToString();
                
                for(String word : words){
                    if(word.contains(term)){
                        or_clause = true;
                        break;
                    }
                }
	        	 
	         }

             double sim = or_clause ? 1.0 : 0.0;
	         
             docBoolean.put(entry.getValue(), new Feature("booleanDoc", (double)sim, entry.getValue()));     
		 }
	
		return docBoolean;
    }


        /**
     * Para cada conceito de classe é gerada uma feature que retorna o número de "pais" da classe
     * @param doc
     * @return
     */
    public Map<String, Feature> documentParentFeatureBased() {
        Map<String, Feature> features = new HashMap<>();

        if(this.parentsNum == null){
            parentsNum = new HashMap<>();
            IndexInfo indexInfo = new IndexInfo(this.conceptReader);

            for (int i = 0; i < this.conceptReader.numDocs(); i++) {
                Feature f = new Feature("pNum", indexInfo.getDocumentLength(i, "PARENTS").doubleValue(),
                        this.indexToClass.get(i));
                parentsNum.put(this.indexToClass.get(i), f);
            }

        }
        else{
            features = this.parentsNum;
        }

        return features;
    }

    /**
     * Para cada conceito de classe é gerada uma feature que retorna o número de "filhos" da classe
     * @param doc
     * @return
     */
    public Map<String, Feature> documentChildrenFeatureBased() {

        Map<String, Feature> features = new HashMap<>();

        if(childrenNum == null){
            childrenNum = new HashMap<>();
            IndexInfo indexInfo = new IndexInfo(this.conceptReader);

            for (int i = 0; i < this.conceptReader.numDocs(); i++) {
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

    public IndexReader getDocumentReader() {
        return documentReader;
    }

    public void setDocumentReader(IndexReader documentReader) {
        this.documentReader = documentReader;
    }

    public IndexReader getConceptReader() {
        return conceptReader;
    }

    public void setConceptReader(IndexReader conceptReader) {
        this.conceptReader = conceptReader;
    }

    public Retrieval<Feature> getRetrievalEngine() {
        return retrievalEngine;
    }

    public void setRetrievalEngine(Retrieval<Feature> retrievalEngine) {
        this.retrievalEngine = retrievalEngine;
    }

    public Map<String, Feature> getDocNum() {
        return docNum;
    }

    public void setDocNum(Map<String, Feature> docNum) {
        this.docNum = docNum;
    }

    public Map<String, Feature> getDocIdfs() {
        return docIdfs;
    }

    public void setDocIdfs(Map<String, Feature> docIdfs) {
        this.docIdfs = docIdfs;
    }

    public Map<Integer, String> getIndexToClass() {
        return indexToClass;
    }

    public void setIndexToClass(Map<Integer, String> indexToClass) {
        this.indexToClass = indexToClass;
    }

    public Map<String, String> getMapClassToId() {
        return mapClassToId;
    }

    public void setMapClassToId(Map<String, String> mapClassToId) {
        this.mapClassToId = mapClassToId;
    }

    public Map<String, Integer> getDocToIndex() {
        return docToIndex;
    }

    public void setDocToIndex(Map<String, Integer> docToIndex) {
        this.docToIndex = docToIndex;
    }
    
}

