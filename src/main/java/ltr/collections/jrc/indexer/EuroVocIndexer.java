package ltr.collections.jrc.indexer;


import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.*;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;

import ltr.collections.jrc.EuroVocConcept;
import ltr.collections.jrc.EuroVocConfig;
import ltr.collections.jrc.parser.EuroVocParser;
import ltr.features.QueryDocument;
import ltr.index.Indexer;
import ltr.parser.Parser;

import static ltr.settings.Config.configFile;


import org.apache.log4j.Logger;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("deprecation")

/**
 * Implementação básica de um Indexador relacionado à coleção jrc-acquis
 */
public class EuroVocIndexer extends Indexer<QueryDocument> {

    public static final Logger logger = Logger.getLogger(EuroVocIndexer.class.getName());

    public static void main(String[] args) throws Exception {
        Indexer<QueryDocument> indexer = new EuroVocIndexer(
           configFile.getProperty("CORPUS_JRC"),
           configFile.getProperty("INDEX_JRC"),
           configFile.getProperty("CONCEPT_JRC"),
           new EuroVocParser(),
           Boolean.parseBoolean(configFile.getProperty("STEMMING")),
           Boolean.parseBoolean(configFile.getProperty("STOP_WORDS")),
           Integer.parseInt(configFile.getProperty("CHUNK_SIZE"))
       );
       indexer.setExtension(".xml");
       indexer.run();
    }

    protected EuroVocConfig euroVocConfig;
    protected Analyzer analyzer;
    private Map<String, Analyzer> filterAnalyzers =  null; // filtrar campos que nao quero que sejam analisados profundamente

    final private String descPath;
    final private  String unDescPath;
    final private String hierarchyPath;


    public EuroVocIndexer(String corpusPath, String indexPath, String conceptPath,
                          Parser<QueryDocument> parser, Boolean stemming, Boolean stopWs, int chunkSize) {
        super(corpusPath, indexPath, conceptPath, parser, stemming, stopWs, chunkSize, "jrc");

        this.descPath = configFile.getProperty("CONCEPTS_DESC_FILE_PATH");
        this.unDescPath = configFile.getProperty("CONCEPTS_UNDESC_FILE_PATH");;
        this.hierarchyPath = configFile.getProperty("CONCEPTS_HIERARCHY_FILE_PATH");;
        this.filterAnalyzers = new HashMap<>();
        this.configureFilters();
    }

    public EuroVocIndexer(String corpusPath, String indexPath, String conceptPath,
                          String descPath, String unDescPath, String hierarchyPath,
                          Parser<QueryDocument> parser, Boolean stemming, Boolean stopWs, int chunkSize) {
        super(corpusPath, indexPath, conceptPath, parser, stemming, stopWs, chunkSize, "jrc");

        this.descPath = descPath;
        this.unDescPath = unDescPath;
        this.hierarchyPath = hierarchyPath;

        this.filterAnalyzers = new HashMap<>();
        this.configureFilters();
    }


    /**
     * Gera um documento do Lucene a partir de um DOM extraído.
     * @param dom
     * @return
     */
    public Document toDoc(QueryDocument dom) {
        Document doc = new Document();
        doc.add(new Field("ID", dom.getId(), Field.Store.YES, Field.Index.NO)); 
        doc.add(new Field("TITLE", dom.getTitle(), Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.WITH_POSITIONS));
        doc.add(new Field("TEXT", dom.getText(), Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.WITH_POSITIONS));

        StringBuilder Classes = new StringBuilder();
        for (String s : dom.getLabels()) {
            Classes.append(s).append(" ");
        }

        doc.add(new Field("CLASSES", Classes.toString().trim(), Field.Store.YES,
                Field.Index.ANALYZED_NO_NORMS, Field.TermVector.YES));
        return doc;
    }

    /**
     * Gera um índice invertido
     * @param doms Doms objects
     * @param commonWs Com ou sem retorno das palavras mais comuns
     * @param commonWs Numero de palavras retornadas
     * @return As palavras mais comuns do indice
     */
    @Override
    public void generateIndex(String indexName, String indexPath,  List<QueryDocument> doms,
                                List<String> commonWs) {
        logger.info("Criando indice " + indexName);

        this.analyzer = new EuroVocAnalyzer(stemming, commonWs).getAnalyzer("EN");

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
        loadConceptConfiguration();
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
                EuroVocConcept evc = extractConcept(clazz, docsEnum, originalReader);
                Document doc = toConcept(evc);
                this.indexWriter.addDocument(doc);
                EuroVocIndexer.logger.info("Documento " + evc + " foi indexado com sucesso.");
            }


            logger.info("Comitando alterações...");
            this.indexWriter.commit();
            this.indexWriter.close();

        }
        catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    private void loadConceptConfiguration() {
        this.euroVocConfig = new EuroVocConfig(descPath, unDescPath, hierarchyPath);
    }

    private void configureFilters() {
        filterAnalyzers.put("ID", new StandardAnalyzer(Version.LUCENE_4_9));
        filterAnalyzers.put("CLASSES", new StandardAnalyzer(Version.LUCENE_4_9));
        filterAnalyzers.put("DOCS", new StandardAnalyzer(Version.LUCENE_4_9));
    }

    private EuroVocConcept extractConcept(BytesRef cID, DocsEnum docsEnum, IndexReader originalReader) {
        EuroVocConcept evc = null;
        try {
            ArrayList<String> docs = new ArrayList<>();
            StringBuilder textSB = new StringBuilder();
            StringBuilder titleSB = new StringBuilder();
            int docIdEnum;

            while ((docIdEnum = docsEnum.nextDoc()) != DocIdSetIterator.NO_MORE_DOCS) {
                Document doc = originalReader.document(docIdEnum); // obtem o documento em memoria
                docs.add(doc.get("ID"));
                textSB.append(doc.get("TEXT")).append("\n");
                titleSB.append(doc.get("TITLE")).append("\n");
            }
            evc = new EuroVocConcept(cID.utf8ToString(), textSB.toString().trim(), titleSB.toString().trim(), docs);
        }
        catch (IOException e) {
            EuroVocIndexer.logger.error(e.getMessage());
        }
        return evc;
    }

    private Document toConcept(EuroVocConcept evc) {
        Document doc = new Document();

        doc.add(new Field("ID", evc.getId(), Field.Store.YES, Field.Index.NO));
        doc.add(new Field("TITLE", evc.getTitle(), Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.WITH_POSITIONS));
        doc.add(new Field("TEXT", evc.getText(), Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.WITH_POSITIONS));
        StringBuilder docs = new StringBuilder();
        for (String s : evc.getDocs()) {
            docs.append(s).append(" ");
        }
        doc.add(new Field("DOCS", docs.toString().trim(), Field.Store.YES, Field.Index.ANALYZED_NO_NORMS,Field.TermVector.YES));

        ArrayList<String> fields = this.euroVocConfig.conceptInfoExtractor(evc.getId());

        doc.add(new Field("PARENTS", fields.get(0), Field.Store.YES, Field.Index.ANALYZED_NO_NORMS, Field.TermVector.YES));
        doc.add(new Field("CHILDREN", fields.get(1), Field.Store.YES, Field.Index.ANALYZED_NO_NORMS,  Field.TermVector.YES));

        doc.add(new Field("DESC", fields.get(2), Field.Store.YES, Field.Index.ANALYZED,Field.TermVector.YES));
        doc.add(new Field("UNDESC", fields.get(3), Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.YES));

        doc.add(new Field("CUMDESC", fields.get(4), Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.YES));
        doc.add(new Field("CUMUNDESC", fields.get(5), Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.YES));

        return doc;
    }
}
