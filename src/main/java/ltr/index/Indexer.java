package ltr.index;

import org.apache.commons.io.FileUtils;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.Version;
import org.xml.sax.SAXException;

import ltr.parser.Parser;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;

@SuppressWarnings("deprecation")
/***
 * Classe responsável por, a partir dos objetos DOM, gerar um índice invertido em memória
 * e um índice conceitual. Recomendado para trabalhar com textos multirótulo.
 */
public abstract class Indexer<T> {

    static final Logger logger = Logger.getLogger(Indexer.class.getName());
    final protected static String tmpIndexSuffix = "_tmp";
    private String prefixFiles;
    private String extension;
    
    final protected String corpusPath;
    final protected String indexPath;
    final protected String conceptPath;

    final protected Boolean stopWs;
    final protected Boolean stemming;
    final protected Integer chunkSize;
    protected IndexWriter indexWriter;

    protected Parser<T> parser;



    /**
     * Gera um índice invertido
     * @param doms Doms objects
     * @param commonWs Com ou sem retorno das palavras mais comuns
     * @return As palavras mais comuns do indice
     */
    public abstract void generateIndex(String indexName, String indexPath, List<T> doms,
                                                    List<String> commonWs);
    /**
     * Gera um índice conceitual
     * @param conceptName
     * @param conceptPath
     * @param indexPath
     * @param commonWs
     */
    public abstract void generateConcept(String conceptName, String conceptPath, String indexPath,
                                         List<String> commonWs);


    /**
     *
     * @param corpusPath configFile.getProperty("CORPUS");
     * @param indexPath configFile.getProperty("INDEX");
     * @param conceptPath configFile.getProperty("CONCEPT");
     * @param stemming
     */
    public Indexer(String corpusPath, String indexPath, String conceptPath,
                   Parser<T> parser, Boolean stemming, Boolean stopWs, int chunkSize) {
        this.corpusPath = corpusPath;
        this.indexPath = indexPath;
        this.conceptPath = conceptPath;
        this.stemming = stemming;
        this.chunkSize = chunkSize;
        this.parser = parser;
        this.prefixFiles = "";
        this.stopWs = stopWs;
        this.extension = ".xml";

        this.resolvePaths();
    }

    public Indexer(String corpusPath, String indexPath, String conceptPath,
                   Parser<T> parser, Boolean stemming, Boolean stopWs, int chunkSize, String prefixFiles) {
        this(corpusPath, indexPath, conceptPath, parser, stemming, stopWs, chunkSize);
        this.prefixFiles = prefixFiles;
    }


    /**
     * Gera o indice invertido para a coleção.
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws XPathExpressionException
     */
    public void run() throws Exception {

        logger.info("Começando a indexação");


        Partitioner partitioner = new Partitioner(corpusPath, prefixFiles, this.chunkSize, this.extension);

        List<String> commonWs = temporaryIndex(partitioner);

        for(List<File> block : partitioner) {

            List<T> doms = parser.parse(block);

            generateIndex("OriginalIndex", indexPath, doms, commonWs);

        }

        generateConcept("ConceptIndex", conceptPath, indexPath, null);

    }

    protected List<String> temporaryIndex(Partitioner partitioner) throws Exception {
        if (this.stopWs) {
            for (List<File> block : partitioner) {

                List<T> doms = parser.parse(block);

                generateIndex("TemporaryIndex", indexPath, doms, null);

            }
            IndexReader tmp_ireader = IndexReader.open(new SimpleFSDirectory(new File(indexPath)));

            IndexInfo iInfo = new IndexInfo(tmp_ireader);
            return iInfo.getTopTermsTF("TEXT", 50);
        }
        return null;
    }


    /**
     * Cria um wrapper de configuração dos analisadores dos campos da coleção
     * @return
     */
    protected IndexWriterConfig setupIndex(Analyzer analyzer, Map<String, Analyzer> filters){
        PerFieldAnalyzerWrapper wrapper = new PerFieldAnalyzerWrapper(analyzer, filters);
        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_4_9, wrapper);
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        return config;
    }

    /**
     * Limpa os diretórios e prepara-os para indexação
     */
    private void resolvePaths() {
        try {
            File tmpIndex = new File(getTmpIndexPath());

            if (tmpIndex.exists()) {
                FileUtils.deleteDirectory(tmpIndex);
                logger.info("Deletando diretório temporário em: " + getTmpIndexPath());
                FileUtils.forceMkdir(new File(getTmpIndexPath()));
                logger.info("Criando diretório para ``tmp_index`` em: " + getTmpIndexPath());
            }

            File index = new File(indexPath);
            if (index.exists()) {
                FileUtils.deleteDirectory(index);
                logger.info("Deletando diretório do indice em: " + indexPath);
                FileUtils.forceMkdir(new File(indexPath));
                logger.info("Criando diretório para ``index`` em: " + indexPath);
            }

            File concept = new File(conceptPath);

            if (concept.exists()) {
                FileUtils.deleteDirectory(concept);
                logger.info("Deletando diretório do conceito em: " + conceptPath);
                FileUtils.forceMkdir(new File(conceptPath));
                logger.info("Criando diretório para ``concept_index`` em: " + conceptPath);
            }

        } catch (IOException ex) {
            logger.error(ex.getMessage());
        }
        
        
    }

    public String getExtension() {
		return extension;
	}
	public void setExtension(String extension) {
		this.extension = extension;
	}
	/**
     * Obtem o nome do indice temporario
     * @return
     */
    protected String getTmpIndexPath() {
        return this.indexPath +tmpIndexSuffix;
    }
}
