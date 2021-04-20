package ltr.extraction;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.NIOFSDirectory;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import javax.sound.sampled.SourceDataLine;

import org.apache.log4j.Logger;
import static ltr.settings.Config.configFile;

public class FoldCreator {

    private static int k = 5;
    public static final Logger logger = Logger.getLogger(FoldCreator.class.getName());
    public static void setK(int k) {
        FoldCreator.k = k;
    }


    public static ArrayList<String> loadClasses(String inDocsFileName)
    {
        ArrayList<String> classes = new ArrayList<>();
        try{
            BufferedReader bf = new BufferedReader(new FileReader(inDocsFileName));
            String str;
            while((str = bf.readLine()) != null)
            {
                str = str.trim();
                classes.addAll(Arrays.asList(str.split(" ")));
            }
        } catch (IOException ex) {
            System.err.println(ex);
        }
        return classes;
    }

    public static void run(String filePath, String inputFeatureFile, String outDir)
    {
        try {

            HashMap<Integer, ArrayList<String>> fileToFold = generateLogicFolds(filePath);

            File out = new File(outDir);
            FileUtils.forceMkdir(out);

            for(int i = 1; i <= k; i++)
            {
                File fold = new File(outDir + "/Fold" + i);
                FileUtils.forceMkdir(fold);
                File file = new File(outDir + "/Fold" + i + "/test.txt");
                FileWriter fw = new FileWriter(file);
                BufferedWriter bw = new BufferedWriter(fw);
                File foldTrain = new File(outDir + "/Fold" + i + "/train.txt");
                FileWriter fwTrain = new FileWriter(foldTrain);
                BufferedWriter bwTrain = new BufferedWriter(fwTrain);

                BufferedReader br = new BufferedReader(new FileReader(new File(inputFeatureFile)));

                String s;
                while((s = br.readLine()) != null)
                {
                    String[] raw = s.trim().split(" ");
                    String cId = raw[raw.length - 2];
                    if(fileToFold.get(i).contains(cId))
                        bw.write(s + "\n");
                    else
                        bwTrain.write(s + "\n");
                    
                }

                bw.close();
                bwTrain.close();

            }
        } catch (IOException ex) {
            logger.error(ex.getMessage());
        }

    }

    // path = conceptPath => dividir baseado nas classes
    //        indexPath => dividir baseado nos documentos
    //        classesPath => dividir baseado em lista de classes
    private static HashMap<Integer, ArrayList<String>> generateLogicFolds(String path) throws IOException {

       IndexReader reader = IndexReader.open(new NIOFSDirectory(new File(path)));

       ArrayList<String> classes = new ArrayList<>();
       for (int i = 0; i < reader.numDocs(); i++){
           classes.add(reader.document(i).get("ID"));
       }

        // ArrayList<String> classes = FoldCreator.loadClasses(path);

        HashMap<Integer, ArrayList<String>> blocks = new HashMap<>();

        int chunkSize =  (int) Math.ceil((double) classes.size() / (double) FoldCreator.k);

        for(int i = 0; i < FoldCreator.k; i++) {

            int arrayPosition = i * chunkSize;
            int length = Math.min(classes.size() - arrayPosition, chunkSize);

            ArrayList<String> chunk = new ArrayList<String>(classes.subList(arrayPosition, arrayPosition+length));
            blocks.put(i+1, chunk);
        }

        return blocks;
    }

    public static void main(String[] args) {
        FoldCreator.run(
                configFile.getProperty("INDEX"),
                configFile.getProperty("FEATURE_PATH")+"/all_folds.txt",
                configFile.getProperty("FEATURE_PATH")+"/cv"
        );
    }
}