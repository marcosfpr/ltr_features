package ltr.features;

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import static ltr.settings.Config.configFile;

/**
 * Classe responsável por aplicar o Score Propagation -> aplicar relações entre classes
 */
public class FeaturePropagator {
    public static void main(String[] args) {
        FeaturePropagator fp = new FeaturePropagator(
            configFile.getProperty("FEATURE_PATH"),
            configFile.getProperty("FEATURE_PROPAGATED_PATH"),
            configFile.getProperty("CONCEPT_GRAPH"),
            Integer.parseInt(configFile.getProperty("NUM_ITERATIONS")),
            Double.parseDouble(configFile.getProperty("LAMBDA")),
            configFile.getProperty("BLACK_LIST")
        );
        fp.run();
    }

    public static final Logger logger = Logger.getLogger(FeaturePropagator.class.getName());
    
    private HashSet<Integer> propagationBlackList;
    private Integer numIteration;
    private Double lambda;
    private String graphPath;
    private String featuresPath;
    private String featuresPropagationPath;

    public Map<String, Map<String, Double>> conceptGraph = new HashMap<>();
    public Map<String, Double> propagationGraph = new HashMap<>();

    public FeaturePropagator(String featuresPath,  String featuresPropagationPath, String graphPath,
    Integer numIteration, Double lambda, String blackList) {
        this.graphPath = graphPath;
        this.featuresPath = featuresPath;
        this.featuresPropagationPath = featuresPropagationPath;
        this.numIteration = numIteration;
        this.lambda = lambda;
        this.loadGraph();
        this.loadBlackList(blackList);
    }

    public void run(){
        try {

            resolveFeaturePropagatedDir();
            propagateFeatures();

        } catch (IOException ex) {
            logger.error(ex.getMessage());
        }
    }

    private void resolveFeaturePropagatedDir() throws IOException {
        File resDir = new File(featuresPropagationPath);
        if (resDir.exists()) {
            FileUtils.deleteDirectory(resDir);
            logger.info("Deletando diretório em: " + featuresPropagationPath);
        }

        resDir.mkdirs();
        File file = new File(featuresPropagationPath+"/all_folds.txt");
        file.createNewFile();
    }

    private void propagateFeatures() throws IOException {
        TreeMap<Integer, HashMap<String, HashMap<String, Feature>>> feature = new TreeMap<>();
        // organizando features por fnum -> qid -> classid

        String Kfold_path = featuresPath + "/all_folds.txt";
        BufferedReader br = new BufferedReader(new FileReader(Kfold_path));
        String line;

        while ((line = br.readLine()) != null) {
            String[] rawLine = line.split(" # ");
            String[] featuresLine = rawLine[0].split("\\s+");
            String[] metaLine = rawLine[1].split("\\s+");

            for (int i = 2; i < featuresLine.length; i++) {
                Integer fNum = Integer.parseInt(featuresLine[i].split(":")[0]);
                Double fVal = Double.parseDouble(featuresLine[i].split(":")[1]);

                String qId = featuresLine[1].split(":")[1];

                String docID = metaLine[0];
                String cId = metaLine[1];

                // recriando feature em memória
                Feature f = new Feature(fNum.toString(), fVal, docID, cId, featuresLine[0]);


                HashMap<String, HashMap<String, Feature>> queryTo_classToFeatures = feature.get(fNum);
                HashMap<String, Feature> classToFeatures;

                // primeiro sample irá cair aqui
                if (queryTo_classToFeatures == null) {
                    queryTo_classToFeatures = new HashMap<>();
                    classToFeatures = new HashMap<>();
                } else {
                    classToFeatures = queryTo_classToFeatures.get(qId);
                    if (classToFeatures == null){ // caso a consulta qid não possua nenhuma (classe ->features ) ainda
                        classToFeatures = new HashMap<>();
                        this.addFeatureToFile(this.propagateAndConcatFeatures(feature));
                        logger.info("Todas as features de " + qId + " foram propagadas...");
                        feature = new TreeMap<>(); // calculo as features do qid passado e recomeço :)
                        queryTo_classToFeatures = new HashMap<>();
                    }
                }
                classToFeatures.put(cId, f);
                queryTo_classToFeatures.put(qId, classToFeatures);
                feature.put(fNum, queryTo_classToFeatures);
            }
        }
        this.addFeatureToFile(this.propagateAndConcatFeatures(feature));
        logger.info("A propagação de features foi concluída com sucesso");
        br.close();
    }
    
    private void addFeatureToFile(TreeMap<Integer, HashMap<String, HashMap<String, Feature>>> allFeatures) {
        TreeMap<String,HashMap<String, TreeMap<Integer,Feature>>> lines = new TreeMap<>();
        Set<String> qIds =  allFeatures.firstEntry().getValue().keySet();

        for(String q: qIds){

            HashMap<String, TreeMap<Integer,Feature>> docs = new HashMap<>();
            for(Map.Entry<Integer, HashMap<String, HashMap<String, Feature>>> ent: allFeatures.entrySet()){
                Integer fnum = ent.getKey();
                for(Map.Entry<String, Feature> ent2: ent.getValue().get(q).entrySet()){
                    TreeMap<Integer,Feature> fs = docs.get(ent2.getKey());
                    if(fs==null)
                        fs = new TreeMap<>();
                    fs.put(fnum, ent2.getValue());
                    docs.put(ent2.getKey(), fs);
                }
            }
            lines.put(q, docs);

        }
        try{
            PrintWriter pw = new PrintWriter(new FileWriter(featuresPropagationPath+"/all_folds.txt",true));
            for(Map.Entry<String,HashMap<String, TreeMap<Integer,Feature>>> ent: lines.entrySet()){
                for(Map.Entry<String, TreeMap<Integer,Feature>> ent2: ent.getValue().entrySet()){
                    String lbl = "";
                    String docId = "";
                    String qName = "";
                    String tmpLine = "";
                    for(Map.Entry<Integer,Feature> ent3: ent2.getValue().entrySet()){
                        lbl = ent3.getValue().getLabel();
                        docId = ent3.getValue().getDocID();
                        qName = ent3.getValue().getClassID();
                        tmpLine += ent3.getKey() + ":" + ent3.getValue().getFeatureValue().toString() + " ";
                    }
                    String line =  lbl+ " "
                            + "qid:" + ent.getKey() + " "
                            + tmpLine
                            + "# "
                            + qName + " "
                            + docId
                            +"\n";
                    pw.write(line);
                }
            }
            pw.close();
        }catch(IOException ex){
            logger.error(ex.getMessage());
        }
    }

    private TreeMap<Integer, HashMap<String, HashMap<String, Feature>>> propagateAndConcatFeatures(TreeMap<Integer,
        HashMap<String, HashMap<String, Feature>>> rawFeatures) {

        TreeMap<Integer, HashMap<String, HashMap<String, Feature>>> tempRawFeatures = new TreeMap<>();
        TreeMap<Integer, HashMap<String, HashMap<String, Feature>>> finalTempRawFeatures;

        int fnumber = rawFeatures.size();

        for (Map.Entry<Integer, HashMap<String, HashMap<String, Feature>>> fnum_ent : rawFeatures.entrySet()) {
            if(this.propagationBlackList.contains(fnum_ent.getKey()))
                continue;

            HashMap<String, HashMap<String, Feature>> propagatedFeature = new HashMap<>();
            for (Map.Entry<String, HashMap<String, Feature>> qid_ent : fnum_ent.getValue().entrySet()) {
                HashMap<String, Feature> proFeatures = this.propagator(qid_ent.getValue());
                propagatedFeature.put(qid_ent.getKey(), proFeatures);
            }
            tempRawFeatures.put(++fnumber, propagatedFeature);
        }

        finalTempRawFeatures = tempRawFeatures;
        TreeMap<Integer, HashMap<String, HashMap<String, Feature>>> finalFeatures = new TreeMap<>();

        // sobrepoe raw antigo
        for (Map.Entry<Integer, HashMap<String, HashMap<String, Feature>>> ent : rawFeatures.entrySet()) {
            finalFeatures.put(ent.getKey(), ent.getValue());
        }

        for (Map.Entry<Integer, HashMap<String, HashMap<String, Feature>>> ent : finalTempRawFeatures.entrySet()) {
            finalFeatures.put(ent.getKey(), ent.getValue());
        }

        return finalFeatures;
    }


    private HashMap<String, Feature> propagator(HashMap<String, Feature> features) {
        // classe -> features

        int itr = 0;
        HashMap<String, Feature> oldValues = features;
        while (itr < numIteration) {
            HashMap<String, Feature> newValues = new HashMap<>();

            for (Map.Entry<String, Feature> class_ent : oldValues.entrySet()) {
                double newValue = lambda * class_ent.getValue().getFeatureValue(); // S^t = lambda * S^{t-1}

                if(conceptGraph.get(class_ent.getKey()) != null){
                    for (Map.Entry<String, Double> ent : conceptGraph.get(class_ent.getKey()).entrySet()) {
                        if (oldValues.containsKey(ent.getKey())) {
                            // S^t += (1 - lambda) * PS^{t-1}
                            newValue += (1 - lambda) * oldValues.get(ent.getKey()).getFeatureValue()
                                    * (ent.getValue() / propagationGraph.get(ent.getKey())); // P = n° ocorrencias mutuas com C / Somatorio de ocorrencias mutuas de C
                        }
                    }
                }
                // nova feature
                Feature f = new Feature(class_ent.getValue().getFeatureName(), newValue, class_ent.getValue().getClassID(),
                        class_ent.getValue().getDocID(), class_ent.getValue().getLabel());

                f.setFeatureValue(newValue);
                newValues.put(class_ent.getKey(), f);
            }
            oldValues = FeatureNormalizer.normalize(newValues); // atualizando os valores antigos
            itr++;
        }
        return oldValues;
    }


    public void setNumIteration(Integer numIteration) {
        this.numIteration = numIteration;
    }

    public void setLambda(Double lambda) {
        this.lambda = lambda;
    }

    private void loadBlackList(String blackList) {
        this.propagationBlackList = new HashSet<Integer>();
        for(String s: blackList.split(",")){
            this.propagationBlackList.add(Integer.parseInt(s));
        }
    }

    private void loadGraph() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(new File(graphPath)));

            String str = "";
            while ((str = reader.readLine()) != null) {

                // class1   class2  co-occurrences
                String[] parts = str.split("\t");

                String label1 = parts[0];
                String label2 = parts[1];
                Double co_occurrences = Double.valueOf(parts[2]);

                if (conceptGraph.containsKey(label1)) {
                    Map<String, Double> temp = conceptGraph.get(label1);
                    temp.put(label2, Double.valueOf(co_occurrences));
                    conceptGraph.put(label1, temp);
                } else {
                    Map<String, Double> temp = new HashMap<>();
                    temp.put(label2, Double.valueOf(co_occurrences));
                    conceptGraph.put(label1, temp);
                }

                if (conceptGraph.containsKey(label2)) {
                    Map<String, Double> temp = conceptGraph.get(label2);
                    temp.put(label1, Double.valueOf(co_occurrences));
                    conceptGraph.put(label2, temp);
                } else {
                    Map<String, Double> temp = new HashMap<>();
                    temp.put(label1, Double.valueOf(co_occurrences));
                    conceptGraph.put(label2, temp);
                }
            }

            for(Map.Entry<String, Map<String, Double>> ent : conceptGraph.entrySet())
            {
                double sum = 0;
                for(Map.Entry<String, Double> ent2 : ent.getValue().entrySet())
                    sum += ent2.getValue();
                propagationGraph.put(ent.getKey(), sum);     // para cada classe, o total de ocorrências mutuas com outras classes
            }

        }
        catch (IOException e) {
            logger.error(e.getMessage());
        }
    }
}
