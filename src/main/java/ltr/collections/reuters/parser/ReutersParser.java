package ltr.collections.reuters.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import ltr.collections.reuters.ReutersDoc;
import ltr.features.QueryDocument;
import ltr.parser.Parser;

import static ltr.settings.Config.configFile;

public class ReutersParser implements Parser<QueryDocument>{
    
    static final Logger logger = Logger.getLogger(ReutersParser.class.getName());

    public List<QueryDocument> parse(List<File> files)
            throws InterruptedException, FileNotFoundException, IOException {

        List<QueryDocument> docs = new ArrayList<>();
        
    
        logger.info("Transformando documentos Reuters em DOM's...");
        
        Collections.sort(files);
        
        docs.addAll(createDom(new File(configFile.getProperty("CLASSES_REUTERS")), files)); // considera ordem alfabética dos arquivos (info=movie, rotten=critics)
        
        logger.info("Transformação concluída dos documentos txt...");

        

        return docs;
    }

    private List<ReutersDoc> createDom(File catsFile, List<File> files) throws FileNotFoundException, IOException  {

        Map<String, ReutersDoc> treeDocs = new TreeMap<>();
        ArrayList<ReutersDoc> docs = new ArrayList<>(); 
        
        for (File docFile : files) {

            FileInputStream fisTargetFile = new FileInputStream(docFile);

            String text = IOUtils.toString(fisTargetFile, "UTF-8");

            ReutersDoc doc = new ReutersDoc(docFile.getName(), text);
            
            treeDocs.put(docFile.getName(), doc);
            docs.add(doc);

        }
       
        String dataset = configFile.getProperty("DATASET_REUTERS");

        FileReader fr = new FileReader(catsFile);   //reads the file  
        BufferedReader br = new BufferedReader(fr);  //creates a buffering character input stream  
        String line;  

        while((line=br.readLine()) != null)  
        {  
            String[] tokens = line.split("/");

            if(tokens[0].equals(dataset)) {
                String[] parameters = tokens[1].split(" ");

                String document = parameters[0];

                ArrayList<String> classes = new ArrayList<>(Arrays.asList(parameters));
                classes.remove(0);

                treeDocs.get(document).setClasses(classes);
            }

        }

        fr.close();    //closes the stream and release the resources  

		return docs;

    }
}
