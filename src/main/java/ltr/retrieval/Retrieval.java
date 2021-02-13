package ltr.retrieval;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.util.Version;

import java.io.IOException;
import java.util.*;
import org.apache.log4j.Logger;

import static ltr.settings.Config.configFile;

/**
 * Responsável por fazer buscas e retornar scores dentro de uma coleção
 */
public abstract class Retrieval<T> {
    static final Logger logger = Logger.getLogger(Retrieval.class.getName());

    protected IndexReader indexReader;
    protected String simName;
    protected Similarity similarity;
    protected String field;
    protected Analyzer analyzer;
    protected List<String> commonWs;


    protected Map<Integer, String> indexToClass;
    protected List<Float> params;
    protected Similarity[] SIM_FUNCS;

    protected enum SimilarityFunction {
        LMD, 
        LMJ, 
        BM25, 
        VM 
    }

    protected abstract Map<String, T>  processHits(ScoreDoc[] hits, String queryID);

    public Retrieval(Analyzer analyzer){
        this.SIM_FUNCS=  new Similarity[]{
                new LMDirichletSimilarity(Float.parseFloat(configFile.getProperty("PARAMETERS_LM_DIRICHLET_MU"))),
                new LMJelinekMercerSimilarity(Float.parseFloat(configFile.getProperty("PARAMETERS_LM_JM_LAMBDA"))),
                new BM25Similarity(Float.parseFloat(configFile.getProperty("PARAMETERS_BM25_K1")),
                        Float.parseFloat(configFile.getProperty("PARAMETERS_BM25_b"))),
                new DefaultSimilarity() 
        };
        this.analyzer = analyzer;
    }

    public Map<String, T> search(String queryText, String queryID) throws ParseException {
        QueryParser queryParser = new QueryParser(Version.LUCENE_4_9, field, analyzer);

        queryText = QueryParser.escape(queryText.toLowerCase());

        int maxClause = 1024 * 1000 * queryText.split(" ").length;
        BooleanQuery.setMaxClauseCount(maxClause);

        Query query = queryParser.parse(queryText);

        this.similarity = this.SIM_FUNCS[SimilarityFunction.valueOf(simName).ordinal()];

        IndexSearcher searcher = new IndexSearcher(this.indexReader);
        searcher.setSimilarity(this.similarity);

        ScoreDoc[] hits = null;

        try {
            TopFieldCollector topFieldCollector = TopFieldCollector.create(Sort.RELEVANCE, this.indexReader.numDocs(),
                true, true, true, false);

            searcher.search(query, topFieldCollector);

            TopDocs response = topFieldCollector.topDocs();
            hits = response.scoreDocs;

            return processHits(hits, queryID);
        }
        catch (IOException e) {
            logger.error(e.getMessage());
        }
        return processHits(hits, queryID);
    }

    public void setIndexReader(IndexReader indexReader) {
        this.indexReader = indexReader;
    }

    public void setSimName(String simName) {
        this.simName = simName;
    }

    public void setSimilarity(Similarity similarity) {
        this.similarity = similarity;
    }

    public void setField(String field) {
        this.field = field;
    }

    public void setAnalyzer(Analyzer analyzer) {
        this.analyzer = analyzer;
    }

    public void setCommonWs(List<String> commonWs) {
        this.commonWs = commonWs;
    }

    public void setIndexToClass(Map<Integer, String> indexToClass) {
        this.indexToClass = indexToClass;
    }

    public void setParams(List<Float> params) {
        this.params = params;
    }
}