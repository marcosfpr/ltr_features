package ltr.extraction;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
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
    protected Map<Integer, String> indexToClass; // indice do lucene -> classID (id do genero)
    protected Map<String, String> mapClassToId;
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
                Term term = new Term("TEXT", entry.getKey()); 
                float idf = tfidfSIM.idf( this.documentReader.docFreq(term) ,this.documentReader.numDocs() );
                this.docIdfs.put(entry.getValue(), new Feature("idfClass", (double)idf, entry.getValue()));      
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
	        	 if(ArrayUtils.contains(words, term)) {
	        		 freq++;
	        	 }
	         }
             float tf = tfidfSIM.tf( freq );
             docFrequencies.put(entry.getValue(), new Feature("tfClassDoc", (double)tf, entry.getValue()));     
		 }
    
		return docFrequencies;
    }
    
        /**
     * TF de cada classe em um documento
     * @return
     * @throws IOException 
     */
    public HashMap<String, Feature> booleanFeature(QueryDocument doc) throws IOException {
    	
		HashMap<String, Feature> docBoolean = new HashMap<String, Feature>();
    	     
		 for (Map.Entry<String, String> entry : this.mapClassToId.entrySet())
		 {
			 List<String> words = Arrays.asList(entry.getKey().split("&"));
			 
			 Terms termVector = this.documentReader.getTermVector(this.docToIndex.get(doc.getId()), "TEXT");
	         TermsEnum itr = termVector.iterator(null);
	         BytesRef text = null;
	         while((text = itr.next()) != null) {
	        	 String term = text.utf8ToString();
	        	 if(words.contains(term)) {
	        		 for(int i=0; i < words.size(); i++) {
	        			 if(words.get(i) == term) words.remove(i);
	        		 }
	        	 }
	         }
	         
             int sim = words.isEmpty() ? 1 : 0;
             docBoolean.put(entry.getValue(), new Feature("classInDoc", (double)sim, entry.getValue()));     
		 }
	
		return docBoolean;
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

