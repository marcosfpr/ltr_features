package ltr.collections.jrc.extractor;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.apache.lucene.index.IndexReader;

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

    private Retrieval<Feature> getRetrievalEngine(EuroVocAnalyzer analyzer) {
        if (this.retrievalEngine == null)
            this.retrievalEngine = new EuroVocRetrieval(analyzer.getAnalyzer("EN"));
        return retrievalEngine;
    }

}
