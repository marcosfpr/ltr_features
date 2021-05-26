package ltr.extraction;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.store.SimpleFSDirectory;

import ltr.features.Feature;
import ltr.features.FeatureNormalizer;
import ltr.features.QueryDocument;
import ltr.fileutils.FileExtraction;
import ltr.index.Partitioner;
import ltr.parser.Parser;

/**
 * Calcula features
 */
@SuppressWarnings("deprecation")
public abstract class FeaturesCalculator {

    public static final Logger logger = Logger.getLogger(FeaturesCalculator.class.getName());
    private final String corpusPath;
    private final String conceptIndexPath;
    private final String indexPath;
    private final String extension;
    private final String suffix;
    private Map<String, String> mapClassToId;

    public static int queryID = 1;
    public static String featuresPath;
    public static ReentrantLock accessFile = new ReentrantLock();

    public static final FeaturesType[] usedFeatures = new FeaturesType[] { FeaturesType.POPULARITY,
            FeaturesType.POPULARITY, // FeaturesType.BOOLEAN, FeaturesType.TF, FeaturesType.IDF, FeaturesType.CLASS_FREQUENCY,
            FeaturesType.SIM_TXT_TXT_LMD, FeaturesType.SIM_TXT_TXT_LMJ, FeaturesType.SIM_TXT_TXT_BM25,// FeaturesType.SIM_TXT_TXT_VM,
            FeaturesType.SIM_TXT_TIT_LMD, FeaturesType.SIM_TXT_TIT_LMJ, FeaturesType.SIM_TXT_TIT_BM25, //FeaturesType.SIM_TXT_TIT_VM,
            FeaturesType.SIM_TIT_TXT_LMD, FeaturesType.SIM_TIT_TXT_LMJ, FeaturesType.SIM_TIT_TXT_BM25, //FeaturesType.SIM_TIT_TXT_VM,
            FeaturesType.SIM_TIT_TIT_LMD, FeaturesType.SIM_TIT_TIT_LMJ, FeaturesType.SIM_TIT_TIT_BM25, //FeaturesType.SIM_TIT_TIT_VM,
            /*
             * FeaturesType.SIM_TXT_DESC_LMD, FeaturesType.SIM_TXT_DESC_LMJ,
             * FeaturesType.SIM_TXT_DESC_BM25, FeaturesType.SIM_TXT_UNDESC_LMD,
             * FeaturesType.SIM_TXT_UNDESC_LMJ, FeaturesType.SIM_TXT_UNDESC_BM25,
             * FeaturesType.SIM_TIT_DESC_LMD, FeaturesType.SIM_TIT_DESC_LMJ,
             * FeaturesType.SIM_TIT_DESC_BM25, FeaturesType.SIM_TIT_UNDESC_LMD,
             * FeaturesType.SIM_TIT_UNDESC_LMJ, FeaturesType.SIM_TIT_UNDESC_BM25,
             * FeaturesType.PARENTS, FeaturesType.CHILDREN
             */
    };

    /**
     * @param featurePath      configFile.getProperty("FEATURE_PATH")
     * @param corpusPath       configFile.getProperty("CORPUS")
     * @param conceptIndexPath configFile.getProperty("CONCEPT")
     */
    public FeaturesCalculator(String featurePath, String corpusPath, String indexPath, String conceptIndexPath,
            String classesPath, String suffix, String extension) {
        this.resolvePaths(featurePath);
        loadClasses(classesPath);
        this.corpusPath = corpusPath;
        this.conceptIndexPath = conceptIndexPath;
        this.indexPath = indexPath;
        this.suffix = suffix;
        this.extension = extension;
    }

    public abstract FeaturesDefinition getFeaturesDefinition(IndexReader conceptReader, IndexReader documentReader);

    public abstract Parser<QueryDocument> getParser();

    public void run() throws Exception {

        try {
            IndexReader conceptReader = IndexReader.open(new SimpleFSDirectory(new File(this.conceptIndexPath)));

            IndexReader docReader = IndexReader.open(new SimpleFSDirectory(new File(this.indexPath)));

            FeaturesDefinition featuresDefinition = getFeaturesDefinition(conceptReader, docReader);
            featuresDefinition.setMapClassToId(this.mapClassToId);

            Parser<QueryDocument> parser = getParser();

            Partitioner partitioner = new Partitioner(corpusPath, suffix, 2000);
            partitioner.setExtension(extension);

            // processei até 4000 docs...

            // for (List<File> block : partitioner) {
            List<File> block = partitioner.getAllFiles();
            ExecutorService executor = Executors.newFixedThreadPool(6);

            List<QueryDocument> docs = parser.parse(block);

            int count = 0;
            for (QueryDocument docAsQuery : docs) {
                if (CollectionUtils.containsAny(docAsQuery.getLabels(), this.mapClassToId.keySet())) {
                    FeaturesCalculator.logger.info(
                            "[" + count++ + "] Começando a extrair features do documento : " + docAsQuery.getTitle());
                    executor.submit(new FeatureCalculatorCallable(docAsQuery, featuresDefinition, mapClassToId));
                }
            }

            executor.shutdown();

            try {
                if (!executor.awaitTermination(48, TimeUnit.HOURS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
            }
            // }

            logger.info("Fim do processamento.");

        } catch (IOException | InterruptedException e) {
            logger.error(e.getMessage());
        }
    }

    private void resolvePaths(String featurePath) {
        try {
            File foldDir = new File(featurePath);
            if (foldDir.exists()) {
                FileUtils.deleteDirectory(foldDir);
                logger.info("Deletando diretório dos folds em " + featurePath);
            }
            foldDir.mkdirs();
            File file = new File(featurePath + "/all_folds.txt");
            file.createNewFile();
            FeaturesCalculator.featuresPath = featurePath + "/all_folds.txt";
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
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
}

class FeatureCalculatorCallable implements Runnable {

    private final FeaturesType[] features = FeaturesCalculator.usedFeatures;
    private final QueryDocument docAsQuery;
    private final FeaturesDefinition featuresDefinition;
    private Map<String, String> mapClassToId;

    public FeatureCalculatorCallable(QueryDocument docAsQuery, FeaturesDefinition featuresDefinition,
            Map<String, String> mapClassToId) {

        this.docAsQuery = docAsQuery;
        this.featuresDefinition = featuresDefinition;
        this.mapClassToId = mapClassToId;
    }

    @Override
    public void run() {

        Map<Integer, Map<String, Feature>> allFeaturesOneDoc = new TreeMap<>();
        for (int i = 0; i < features.length; i++) {
            int fnum = i + 1;
            FeaturesType ftype = features[i];
            try {
                allFeaturesOneDoc.put(fnum, calculateFeatures(docAsQuery, ftype, featuresDefinition));
            } catch (ParseException | IOException e) {
                FeaturesCalculator.logger.error("Erro na extração da Feature " + fnum + ": " + e.getMessage());
            }
        }

        FeaturesCalculator.accessFile.lock();
        int qid = FeaturesCalculator.queryID++;
        // FeaturesCalculator.logger.info("Todas features da consulta " + qid + " foram calculadas");
        writeOnDisk(docAsQuery, allFeaturesOneDoc, qid);
        FeaturesCalculator.accessFile.unlock();

    }

    public Map<String, Feature> calculateFeatures(QueryDocument docAsQuery, FeaturesType ftype, FeaturesDefinition fdef) throws ParseException,
            IOException {
        Map<String, Feature> allFeatures = new HashMap<>();

        Map<String, Feature> f;
        List<Float> params;

        long start = System.currentTimeMillis();
        switch (ftype) {
            case POPULARITY: // popularidade da classe
            
            f = fdef.documentNumberFeatureBased();
            allFeatures.putAll(f);
            break;
            case PARENTS: // numero de pais da classe
            f = fdef.documentParentFeatureBased();
            allFeatures.putAll(f);
            break;
            case CHILDREN: // numero de filhos da classe
            f = fdef.documentChildrenFeatureBased();
            allFeatures.putAll(f);
            break;
            case CLASS_FREQUENCY: // frequencia total da classe na colecao
            f = fdef.countFeature(docAsQuery);
            allFeatures.putAll(f);
            break;
            case IDF: // idf da classe
            f = fdef.idfFeature();
            allFeatures.putAll(f);
            break;
            case TF: // tf da classe no documento
            f = fdef.tfFeature(docAsQuery);
            allFeatures.putAll(f);
            break;
            case BOOLEAN: // consula booleana com AND
            f = fdef.booleanFeature(docAsQuery);
            allFeatures.putAll(f);
            break;
                default:
                String[] args = ftype.getParams();
                params = Collections.singletonList(Float.valueOf(args[0]));
                String queryField = args[1];
                String docField = args[2];
                String simFunc = args[3];
                f = fdef.documentFeatureBased(docAsQuery, queryField, docField, simFunc, params);
                allFeatures.putAll(f);
                break;
        }

        long end = System.currentTimeMillis();    
        // FeaturesCalculator.logger.warn("Tempo para calcular a feature: " + ftype +  ": " + (end - start));
        return allFeatures;
    }

    private void writeOnDisk(QueryDocument docAsQuery, Map<Integer, Map<String, Feature>> allFeatures, int queryID) {

        HashMap<String, TreeMap<Integer, Feature>> docs = new HashMap<>();

        Map<Integer, Map<String, Feature>> allFeaturesNormalized = FeatureNormalizer.oneQueryNormalizer(allFeatures);

        for (Map.Entry<Integer, Map<String, Feature>> feature : allFeaturesNormalized.entrySet()) {
            Integer fnum = feature.getKey();
            for (Map.Entry<String, Feature> row : feature.getValue().entrySet()) {
                TreeMap<Integer, Feature> fs = docs.get(row.getKey());
                if (fs == null)
                    fs = new TreeMap<>();
                fs.put(fnum, row.getValue());
                docs.put(row.getKey(), fs);
            }
        }

        try {
            PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(FeaturesCalculator.featuresPath, true)));

            StringBuilder lines = new StringBuilder();

            ArrayList<String> classesIds = new ArrayList<>();
            for (String lb : docAsQuery.getLabels()) {
                classesIds.add(lb);
            }

            int nlin = 0;
            for (Map.Entry<String, TreeMap<Integer, Feature>> ent : docs.entrySet()) {
                if (this.mapClassToId.containsKey(ent.getKey())) {

                    String lbl = (classesIds.contains(ent.getKey())) ? "1" : "0";

                    String cID = ent.getKey();
                    String queryName = docAsQuery.getId();
                    StringBuilder tmpLine = new StringBuilder();

                    for (int i = 0; i < features.length; i++) {
                        int fnum = i + 1;
                        if (!ent.getValue().containsKey(fnum)) {
                            ent.getValue().put(fnum, new Feature(queryName, 0D, queryName, cID, lbl));
                        }
                    }

                    for (Map.Entry<Integer, Feature> ent2 : ent.getValue().entrySet()) {
                        tmpLine.append(ent2.getKey()).append(":").append(ent2.getValue().getFeatureValue().toString())
                                .append(" ");
                    }

                    String line = lbl + " " + "qid:" + queryID + " " + tmpLine + "# " + queryName + " " + cID + "\n";
                    lines.append(line);
                    nlin++;
                }
            }

            // FeaturesCalculator.logger.info("Escrevendo features do documento " + docAsQuery.getId());
            pw.write(lines.toString());
            // FeaturesCalculator.logger.info(nlin + " dados persistidos em disco!");
            pw.close();
        } catch (IOException e) {
            FeaturesCalculator.logger.error(e.getMessage());
        }

    }
}