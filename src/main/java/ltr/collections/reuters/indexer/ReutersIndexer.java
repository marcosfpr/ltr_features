package ltr.collections.reuters.indexer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;

import ltr.collections.reuters.ReutersConcept;
import ltr.collections.reuters.parser.ReutersParser;
import ltr.features.QueryDocument;
import ltr.index.Indexer;
import ltr.parser.Parser;
import static ltr.settings.Config.configFile;

@SuppressWarnings("deprecation")

/**
 * Indexador da Rotten
 */
public class ReutersIndexer extends Indexer<QueryDocument> {

    public static void main(String[] args) throws Exception {
        Indexer<QueryDocument> indexer = new ReutersIndexer(
           configFile.getProperty("CORPUS_REUTERS"),
           configFile.getProperty("INDEX_REUTERS"),
           configFile.getProperty("CONCEPT_REUTERS"),
           configFile.getProperty("CLASSES_PATH_REUTERS"),
           new ReutersParser(),
           Boolean.parseBoolean(configFile.getProperty("STEMMING")),
           Boolean.parseBoolean(configFile.getProperty("STOP_WORDS")),
           Integer.parseInt(configFile.getProperty("CHUNK_SIZE"))
       );
       indexer.setExtension("");
       indexer.run();
    }
    
    static final Logger logger = Logger.getLogger(ReutersIndexer.class.getName());
    protected Analyzer analyzer;
    private Map<String, Analyzer> filterAnalyzers =  null; // filtrar campos que nao quero que sejam analisados profundamente
    private Map<String, String> mapClassToId;

    public ReutersIndexer(String corpusPath, String indexPath, String conceptPath, String classesPath, Parser<QueryDocument> parser,
                        Boolean stemming, Boolean stopWs, int chunkSize) {
        super(corpusPath, indexPath, conceptPath, parser, stemming, stopWs, chunkSize);
        this.filterAnalyzers = new HashMap<>();
        this.configureFilters();
        this.loadClasses(classesPath);
    }

    @Override
    public void generateIndex(String indexName, String indexPath, List<QueryDocument> doms, List<String> commonWs) {
        logger.info("Criando indice " + indexName);

        this.analyzer = new ReutersAnalyzer(stemming, commonWs).getAnalyzer("EN");

        try {
            IndexWriterConfig config = setupIndex(this.analyzer, this.filterAnalyzers);
            this.indexWriter = new IndexWriter(
                    new SimpleFSDirectory(new File(indexPath)),
                    config
            );

            for (QueryDocument dom : doms) {
                Document doc = toDoc(dom);
                this.indexWriter.addDocument(doc);
                logger.info("Documento " + dom + " foi indexado com sucesso.");
            }

            logger.info("Comitando alterações...");
            this.indexWriter.commit();
            this.indexWriter.close();


        } catch (IOException e) {
            logger.error(e.getMessage());
        }

    }

    @Override
    public void generateConcept(String conceptName, String conceptPath, String indexPath, List<String> commonWs) {
        try {
        	        	
            IndexReader originalReader = IndexReader.open(new SimpleFSDirectory(new File(indexPath)));

            IndexWriterConfig config = setupIndex(this.analyzer, this.filterAnalyzers);
            this.indexWriter = new IndexWriter(
                    new SimpleFSDirectory(new File(conceptPath)),
                    config
            );

            TermsEnum classesEnum = MultiFields.getTerms(originalReader, "CLASSES").iterator(null);
            BytesRef clazz;
            while ((clazz = classesEnum.next()) != null) {
                DocsEnum docsEnum = classesEnum.docs(null, null);
                ReutersConcept evc = extractConcept(clazz, docsEnum, originalReader);
                Document doc = toConcept(evc);
                this.indexWriter.addDocument(doc);
                logger.info("Conceito " + evc + " foi indexado com sucesso.");
            }


            logger.info("Comitando alterações...");
            this.indexWriter.commit();
            this.indexWriter.close();

        }
        catch (IOException e) {
            logger.error(e.getMessage());
        }
    }


    private void configureFilters() {
        filterAnalyzers.put("ID", new StandardAnalyzer(Version.LUCENE_4_9));
        filterAnalyzers.put("CLASSES", new StandardAnalyzer(Version.LUCENE_4_9));
        filterAnalyzers.put("DOCS", new StandardAnalyzer(Version.LUCENE_4_9));
    }
    
    private void loadClasses(String classesPath) {
    	this.mapClassToId = new HashMap<String, String>();
    	try {
			BufferedReader br = new BufferedReader(new FileReader(classesPath));
			String line;
			while ((line = br.readLine()) != null) {
			    String[] pair = line.split("=>");
			    this.mapClassToId.put(pair[0].trim().toLowerCase(), pair[1].trim());
			}
			br.close();
		} catch (FileNotFoundException e) {
			logger.error(e.getMessage());
		} catch (IOException e) {
			logger.error(e.getMessage());
		}
    	
    }

    public Document toDoc(QueryDocument dom) {
        Document doc = new Document();
        doc.add(new Field("ID", dom.getId(), Field.Store.YES, Field.Index.NO));
        doc.add(new Field("TEXT", dom.getText(), Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.WITH_POSITIONS));

        StringBuilder classes = new StringBuilder();

        for (String clazz : dom.getLabels()) {
        	
        	String cId = this.mapClassToId.get(clazz);
        	
        	if(clazz != null)
        		classes.append(cId).append(" ");
        }

        doc.add(new Field("CLASSES", classes.toString().trim(), Field.Store.YES,
                Field.Index.ANALYZED_NO_NORMS, Field.TermVector.YES));
        
        return doc;
    }
    
    private ReutersConcept extractConcept(BytesRef cID, DocsEnum docsEnum, IndexReader originalReader) {
    	ReutersConcept evc = null;
        try {
            List<String> docs = new ArrayList<>();
            StringBuilder textSB = new StringBuilder();
            int docIdEnum;

            while ((docIdEnum = docsEnum.nextDoc()) != DocIdSetIterator.NO_MORE_DOCS) {
                Document doc = originalReader.document(docIdEnum); // obtem o documento em memoria
                docs.add(doc.get("ID"));
                textSB.append(doc.get("TEXT")).append("\n");
            }

            evc = new ReutersConcept(cID.utf8ToString(), textSB.toString().trim(), docs);
        }
        catch (IOException e) {
            logger.error(e.getMessage());
        }
        return evc;
    }

    private Document toConcept(ReutersConcept evc) {
        Document doc = new Document();

        doc.add(new Field("ID", evc.getId(), Field.Store.YES, Field.Index.NO));
        doc.add(new Field("TEXT", evc.getText(), Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.WITH_POSITIONS));
        
        StringBuilder docs = new StringBuilder();
        for (String s : evc.getDocs()) {
            docs.append(s).append(" ");
        }
        doc.add(new Field("DOCS", docs.toString().trim(), Field.Store.YES, Field.Index.ANALYZED_NO_NORMS,Field.TermVector.YES));
       
        return doc;
    }
      
    
}
