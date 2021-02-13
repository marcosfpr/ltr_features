package ltr.collections.rotten.indexer;

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

import ltr.collections.rotten.RottenConcept;
import ltr.collections.rotten.RottenDoc;
import ltr.index.Indexer;
import ltr.parser.Parser;

@SuppressWarnings("deprecation")

/**
 * Indexador da Rotten
 */
public class RottenIndexer extends Indexer<RottenDoc> {
    
    static final Logger logger = Logger.getLogger(RottenIndexer.class.getName());
    protected Analyzer analyzer;
    private Map<String, Analyzer> filterAnalyzers =  null; // filtrar campos que nao quero que sejam analisados profundamente
    private Map<String, String> mapClassToId;

    public RottenIndexer(String corpusPath, String indexPath, String conceptPath, String classesPath, Parser<RottenDoc> parser,
                        Boolean stemming, Boolean stopWs, int chunkSize) {
        super(corpusPath, indexPath, conceptPath, parser, stemming, stopWs, chunkSize);
        this.filterAnalyzers = new HashMap<>();
        this.configureFilters();
        this.loadClasses(classesPath);
    }

    @Override
    public void generateIndex(String indexName, String indexPath, List<RottenDoc> doms, List<String> commonWs) {
        logger.info("Criando indice " + indexName);

        this.analyzer = new RottenAnalyzer(stemming, commonWs).getAnalyzer("EN");

        try {
            IndexWriterConfig config = setupIndex(this.analyzer, this.filterAnalyzers);
            this.indexWriter = new IndexWriter(
                    new SimpleFSDirectory(new File(indexPath)),
                    config
            );

            for (RottenDoc dom : doms) {
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
                RottenConcept evc = extractConcept(clazz, docsEnum, originalReader);
                Document doc = toConcept(evc);
                this.indexWriter.addDocument(doc);
                logger.info("Documento " + evc + " foi indexado com sucesso.");
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
			    String[] pair = line.split("-");
			    this.mapClassToId.put(pair[0].trim().toLowerCase(), pair[1].trim());
			}
			br.close();
		} catch (FileNotFoundException e) {
			logger.error(e.getMessage());
		} catch (IOException e) {
			logger.error(e.getMessage());
		}
    	
    }

    public Document toDoc(RottenDoc dom) {
        Document doc = new Document();
        doc.add(new Field("ID", dom.getMovieId(), Field.Store.YES, Field.Index.NO));
        doc.add(new Field("TITLE", dom.getMovieTitle(), Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.WITH_POSITIONS));
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
    
    private RottenConcept extractConcept(BytesRef cID, DocsEnum docsEnum, IndexReader originalReader) {
    	RottenConcept evc = null;
        try {
            List<String> docs = new ArrayList<>();
            StringBuilder textSB = new StringBuilder();
            StringBuilder titleSB = new StringBuilder();
            int docIdEnum;

            while ((docIdEnum = docsEnum.nextDoc()) != DocIdSetIterator.NO_MORE_DOCS) {
                Document doc = originalReader.document(docIdEnum); // obtem o documento em memoria
                docs.add(doc.get("ID"));
                textSB.append(doc.get("TEXT")).append("\n");
                titleSB.append(doc.get("TITLE")).append("\n");
            }
            evc = new RottenConcept(cID.utf8ToString(), textSB.toString().trim(), titleSB.toString().trim(), docs);
        }
        catch (IOException e) {
            logger.error(e.getMessage());
        }
        return evc;
    }

    private Document toConcept(RottenConcept evc) {
        Document doc = new Document();

        doc.add(new Field("ID", evc.getId(), Field.Store.YES, Field.Index.NO));
        doc.add(new Field("TITLE", evc.getTitle(), Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.WITH_POSITIONS));
        doc.add(new Field("TEXT", evc.getText(), Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.WITH_POSITIONS));
        
        StringBuilder docs = new StringBuilder();
        for (String s : evc.getDocs()) {
            docs.append(s).append(" ");
        }
        doc.add(new Field("DOCS", docs.toString().trim(), Field.Store.YES, Field.Index.ANALYZED_NO_NORMS,Field.TermVector.YES));
       
        return doc;
    }
      
    
}
