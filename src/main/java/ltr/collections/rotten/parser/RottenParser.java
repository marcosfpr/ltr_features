package ltr.collections.rotten.parser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;

import org.apache.log4j.Logger;

import ltr.collections.rotten.RottenCritic;
import ltr.collections.rotten.RottenDoc;
import ltr.features.QueryDocument;
import ltr.parser.Parser;

/**
 * Transforma arquivos da rotten em objetos na memória
 */
public class RottenParser implements Parser<QueryDocument> {

    static final Logger logger = Logger.getLogger(RottenParser.class.getName());

    public List<QueryDocument> parse(List<File> files)
            throws InterruptedException, CsvValidationException, FileNotFoundException, IOException {

        List<QueryDocument> docs = new ArrayList<>();
        
      
        if(files.size() == 2) {
        	logger.info("Transformando documentos Rotten em DOM's...");
        	
        	Collections.sort(files);
        	
	        docs.addAll(createDom(files.get(1), files.get(0))); // considera ordem alfabética dos arquivos (info=movie, rotten=critics)
	        
        	logger.info("Transformação concluída dos documentos csv...");

        }

        return docs;
    }

    private List<RottenDoc> createDom(File criticsFile, File movieFile) throws FileNotFoundException, IOException  {
        Map<String, RottenDoc> treeDocs = new TreeMap<>();
        ArrayList<RottenDoc> docs = new ArrayList<>(); 
        
        try (CSVReader reader = new CSVReaderBuilder(new FileReader(movieFile)).withSkipLines(1).build()) {
            String[] lineInArray;
            while ((lineInArray = reader.readNext()) != null) {
            	String movieId = lineInArray[0];
                String movieTitle = lineInArray[1];
                String movieInfo = lineInArray[2];
                ArrayList<String> labels = new ArrayList<>(Arrays.asList(lineInArray[5].split(",")));
                
                if(labels != null) {
	                RottenDoc doc = new RottenDoc(movieId, movieTitle, movieInfo, labels);
	                treeDocs.put(movieId, doc);
	        		docs.add(doc);
	
	        		logger.info("Documento transformado: " + doc);
                }
            	
            }
        } catch (CsvValidationException e) {
			logger.error(e.getMessage());
		}
        
        try (CSVReader reader = new CSVReaderBuilder(new FileReader(criticsFile)).withSkipLines(1).build()) {
            String[] lineInArray;
            while ((lineInArray = reader.readNext()) != null) {
            	
            	String movieId = lineInArray[0];
                String criticName = lineInArray[1];
                Boolean topCritic = Boolean.parseBoolean(lineInArray[2]);
                String pubName = lineInArray[3];
                String date = lineInArray[6];
                String content = lineInArray[7];
                
            	if(treeDocs.containsKey(movieId)) {
            		RottenDoc doc = treeDocs.get(movieId);
            		
            		RottenCritic critic = new RottenCritic(criticName, topCritic, pubName, date, content);
            		
            		doc.addCritic(critic);
            		
            	}
            	else 
            		continue;
            	
            }
        } catch (CsvValidationException e) {
			logger.error(e.getMessage());
		}
        
        
		return docs;

    }
}
