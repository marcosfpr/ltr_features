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
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.SimpleFSDirectory;


import ltr.features.Feature;
import ltr.features.QueryDocument;
import ltr.fileutils.FileExtraction;
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
    private Map<String, String> mapClassToId;

    public static int queryID = 1;
    public static String featuresPath;
    public static ReentrantLock accessFile = new ReentrantLock();

    public static final Integer[] featureNumbers = new Integer[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
                                                                11, 12, 13, 14, 15, 16, 17, 18};

    public static final String[][] FEATURES = new String[][]{
        {"2000", "TEXT", "TEXT", "LMD"},
        {"0.6", "TEXT", "TEXT", "LMJ"},
        {"0.75", "TEXT", "TEXT", "BM25"},
        {"0.0", "TEXT", "TEXT", "VM"},
        {"2000", "TEXT", "TITLE", "LMD"},
        {"0.6", "TEXT", "TITLE", "LMJ"},
        {"0.75", "TEXT", "TITLE", "BM25"},
        {"0.0", "TEXT", "TITLE", "VM"},
        {"2000", "TITLE", "TEXT", "LMD"},
        {"0.6", "TITLE", "TEXT", "LMJ"},
        {"0.75", "TITLE", "TEXT", "BM25"},
        {"2000", "TITLE", "TITLE", "LMD"},
        {"0.6", "TITLE", "TITLE", "LMJ"},
        {"0.75", "TITLE", "TITLE", "BM25"},
        {"0.0", "TITLE", "TITLE", "VM"}
    };

    /**
     * @param featurePath configFile.getProperty("FEATURE_PATH")
     * @param corpusPath configFile.getProperty("CORPUS")
     * @param conceptIndexPath configFile.getProperty("CONCEPT")
     */
    public FeaturesCalculator(String featurePath, String corpusPath, String indexPath, 
    		String conceptIndexPath, String classesPath) {
        this.resolvePaths(featurePath);
        this.loadClasses(classesPath);
        this.corpusPath = corpusPath;
        this.conceptIndexPath = conceptIndexPath;
        this.indexPath = indexPath;
    }

    public abstract FeaturesDefinition getFeaturesDefinition(IndexReader conceptReader, IndexReader documentReader);
    public abstract Parser<QueryDocument> getParser();

    public void run() throws Exception {

        try {
            IndexReader conceptReader = IndexReader.open(new SimpleFSDirectory(
                    new File(this.conceptIndexPath)
            ));
            
            IndexReader docReader = IndexReader.open(new SimpleFSDirectory(
                    new File(this.indexPath)
            ));
            
            FeaturesDefinition featuresDefinition = getFeaturesDefinition(conceptReader, docReader);
            featuresDefinition.setMapClassToId(this.mapClassToId);

            Parser<QueryDocument> parser = getParser();

            ExecutorService executor = Executors.newFixedThreadPool(10);

            List<QueryDocument> docs = parser.parse(FileExtraction.getAllFiles(new File(corpusPath), "", ".csv"));
                        
            
            for(QueryDocument docAsQuery : docs) {
                executor.submit(new FeatureCalculatorCallable(docAsQuery, featuresDefinition, mapClassToId));
            }

            executor.shutdown();
            try {
                if(! executor.awaitTermination(480, TimeUnit.MINUTES)) {
                    executor.shutdownNow();
                }
            }
            catch (InterruptedException e) {
                executor.shutdownNow();
            }

            logger.info("Fim do processamento.");

        }
        catch (IOException | InterruptedException e) {
            logger.error(e.getMessage());
        }
    }

    private void resolvePaths(String featurePath){
        try{
            File foldDir = new File(featurePath);
            if (foldDir.exists()) {
                FileUtils.deleteDirectory(foldDir);
                logger.info("Deletando diretório dos folds em " + featurePath);
            }
            foldDir.mkdirs();
            File file = new File(featurePath + "/all_folds.txt");
            file.createNewFile();
            FeaturesCalculator.featuresPath = featurePath + "/all_folds.txt";
        }
        catch (IOException e) {
            logger.error(e.getMessage());
        }
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
}

class FeatureCalculatorCallable implements Runnable {

    private final Integer[] featureNumbers = FeaturesCalculator.featureNumbers;
    private final QueryDocument docAsQuery;
    private final FeaturesDefinition featuresDefinition;
    private Map<String, String> mapClassToId;


    public FeatureCalculatorCallable(QueryDocument docAsQuery,
                                     FeaturesDefinition featuresDefinition,
                                     Map<String, String> mapClassToId) {

        this.docAsQuery =  docAsQuery;
        this.featuresDefinition = featuresDefinition;
        this.mapClassToId = mapClassToId;
    }

    @Override
    public void run() {
        
        Map<Integer, Map<String, Feature>> allFeaturesOneDoc = new TreeMap<>();
        for (int fnum : featureNumbers) {
            try {
				allFeaturesOneDoc.put(fnum, calculateFeatures(docAsQuery, fnum, featuresDefinition));
			} catch (Exception e) {
				FeaturesCalculator.logger.error("Erro na extração da Feature " + fnum + "!");
			}
        }

        FeaturesCalculator.accessFile.lock();
        int qid = FeaturesCalculator.queryID++;
        FeaturesCalculator.logger.info("Todas features da consulta " + qid + " foram calculadas");
        writeOnDisk(docAsQuery, allFeaturesOneDoc, qid);
        FeaturesCalculator.accessFile.unlock();

    }


    public Map<String, Feature> calculateFeatures(QueryDocument docAsQuery, int fnum, FeaturesDefinition fdef) throws Exception {
        Map<String, Feature> allFeatures = new HashMap<>();

        Map<String, Feature> f;
        List<Float> params;

        switch (fnum) {
            case 1: // popularidade
                f = fdef.documentNumberFeatureBased();
                allFeatures.putAll(f);
                break;
            case 2:
            	f = fdef.idfFeature();
            	allFeatures.putAll(f);
            	break;
            case 3:
            	f = fdef.tfFeature(docAsQuery);
            	allFeatures.putAll(f);
            	break;
            case 4: 
            	f = fdef.booleanFeature(docAsQuery);
            	allFeatures.putAll(f);
            	break;
            default:
                params = Collections.singletonList(Float.valueOf(FeaturesCalculator.FEATURES[fnum - 1][0]));
                String queryField = FeaturesCalculator.FEATURES[fnum-1][1];
                String docField = FeaturesCalculator.FEATURES[fnum-1][2];
                String simFunc = FeaturesCalculator.FEATURES[fnum-1][3];
                f = fdef.documentFeatureBased(docAsQuery, queryField, docField, simFunc, params);
                allFeatures.putAll(f);
                break;
        }
        return allFeatures;
    }

    private void writeOnDisk(QueryDocument docAsQuery, Map<Integer, Map<String, Feature>> allFeatures,
                            int queryID) {

        HashMap<String, TreeMap<Integer,Feature>> docs = new HashMap<>();

        for(Map.Entry<Integer, Map<String, Feature>> feature: allFeatures.entrySet()) {
            Integer fnum = feature.getKey();
            for(Map.Entry<String, Feature> row: feature.getValue().entrySet()) {
                TreeMap<Integer, Feature> fs = docs.get(row.getKey());
                if(fs==null)
                    fs = new TreeMap<>();
                fs.put(fnum, row.getValue());
                docs.put(row.getKey(), fs);
            }
        }

        try {
            PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(FeaturesCalculator.featuresPath, true)));
   
            StringBuilder lines = new StringBuilder();

            int nlin = 0;
            for (Map.Entry<String, TreeMap<Integer, Feature>> ent : docs.entrySet()) {
            	
            	ArrayList<String> classesIds = new ArrayList<>();
            	for(String lb : docAsQuery.getLabels()) {
            		classesIds.add(this.mapClassToId.get(lb));
            	}

                String lbl = (classesIds.contains(ent.getKey())) ? "1" : "0";
                
                String cID = ent.getKey();
                String queryName = docAsQuery.getId();
                StringBuilder tmpLine = new StringBuilder();

                for (int fnum : this.featureNumbers) {
                    if (!ent.getValue().containsKey(fnum)) {
                        ent.getValue().put(fnum, new Feature(queryName, 0D, queryName, cID, lbl));
                    }
                }

                for (Map.Entry<Integer, Feature> ent2 : ent.getValue().entrySet()) {
                    tmpLine.append(ent2.getKey()).append(":").append(ent2.getValue()
                            .getFeatureValue().toString()).append(" ");
                }

                String line = lbl + " "
                        + "qid:" + queryID + " "
                        + tmpLine
                        + "# "
                        + queryName + " "
                        + cID
                        + "\n";
                lines.append(line);
                nlin++;

            }

            FeaturesCalculator.logger.info("Escrevendo features do documento " + docAsQuery.getId());
            pw.write(lines.toString());
            FeaturesCalculator.logger.info(nlin + " dados persistidos em disco!");
            pw.close();
        }
        catch (IOException e) {
            FeaturesCalculator.logger.error(e.getMessage());
        }

    }
}