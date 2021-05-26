package ltr.extraction;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import ltr.collections.jrc.parser.EuroVocParser;
import ltr.collections.reuters.parser.ReutersParser;
import ltr.collections.rotten.parser.RottenParser;
import ltr.features.QueryDocument;
import ltr.fileutils.FileExtraction;
import ltr.parser.Parser;

import static ltr.settings.Config.configFile;

/**
 * Extrair as classes e atrelá-las a um único ID
 */
public class ClassesExtractor {
    public static final Logger logger = Logger.getLogger(ClassesExtractor.class.getName());

    public static void main(String[] args) throws Exception {
        ClassesExtractor.run(
            configFile.getProperty("TRAIN_JRC"),
            configFile.getProperty("TEST_JRC"),
            configFile.getProperty("CLASSES_PATH_JRC"), // _JRC
            new EuroVocParser(), "jrc", ".xml" // basta mudar esta linha para JRC
        );
    }

    private static void run(String trainDocs, String testDocs, String classes_path,
                             Parser<QueryDocument> parser, String prefix, String suffix) throws Exception {

        List<QueryDocument> docs = parser.parse(FileExtraction.getAllFiles(new File(trainDocs), prefix, suffix));
        List<QueryDocument> docsTest = parser.parse(FileExtraction.getAllFiles(new File(testDocs), prefix, suffix));
        
        Set<String> trainClasses = new TreeSet<>();
        Set<String> testClasses = new TreeSet<>();

        for(QueryDocument doc: docs) {
            trainClasses.addAll(doc.getLabels());
        }

        for(QueryDocument doc: docsTest) {
            testClasses.addAll(doc.getLabels());
        }

        Set<String> availableClasses = new TreeSet<>(trainClasses);
        availableClasses.retainAll(testClasses);

        BufferedWriter bw = new BufferedWriter(new FileWriter(classes_path));
        int i = 0;
        for(String usedClass : availableClasses) {
            if(usedClass.length() > 1) {
                i++;	
            	bw.write(usedClass.trim() + " => " + i + "\n");
                System.out.println(usedClass.trim() + " => " + i);
            }
        }

        bw.close();
        logger.info(docs.size() + " documentos serão utilizados.");
        logger.info(i + " classes serão utilizadas.");
    }
}
