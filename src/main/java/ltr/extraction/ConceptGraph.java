package ltr.extraction;

/**
 * Gera um grafo de co-ocorrencias entre os conceitos
 */

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import ltr.collections.jrc.parser.EuroVocParser;
import ltr.collections.rotten.parser.RottenParser;
import ltr.features.QueryDocument;
import ltr.index.Partitioner;
import ltr.parser.Parser;

import static ltr.settings.Config.configFile;


public class ConceptGraph {
    public static void main(String[] args) throws Exception {
        ConceptGraph graph = new ConceptGraph(
                configFile.getProperty("CORPUS_JRC"),
                configFile.getProperty("CONCEPT_GRAPH_JRC"),
                new EuroVocParser(), // RottenParser()
                Integer.parseInt(configFile.getProperty("CHUNK_SIZE")),
                "jrc", ".xml"
        );
        graph.makeGraph();
    }

    static final Logger logger = Logger.getLogger(ConceptGraph.class.getName());
    private Map<String, Double> edgeList = new HashMap<>(); // classe => numero de ocorrencias
    private final String corpusPath;
    private final String conceptGraph;
    private final String prefix;
    private final String extension;
    private final Integer chunkSize;
    private Parser<QueryDocument> parser;

    public ConceptGraph(String corpusPath, String conceptGraph, Parser<QueryDocument> parser, Integer chunkSize, String prefix, String extension) throws IOException {
        this.corpusPath = corpusPath;
        this.chunkSize = chunkSize;
        this.conceptGraph = conceptGraph;
        this.prefix = prefix;
        this.extension = extension;
        this.parser = parser;

        FileUtils.touch(new File(conceptGraph));
    }

    public void makeGraph() throws Exception {
        Partitioner partitioner = new Partitioner(corpusPath, prefix, chunkSize, extension);

        for (List<File> block : partitioner) {

            List<QueryDocument> doms = this.parser.parse(block);
            conceptDoc(doms);
            this.writeToFile();
        }
    }

    private void conceptDoc(List<QueryDocument> docs) {
        for (QueryDocument doc: docs ) {
            logger.info("Doc " + doc.getId() + " processed");
            List<String> classes = doc.getLabels();

            for (String c1 : classes){
                c1 = c1.trim().toLowerCase();
                for (String c2 : classes){
                    c2 = c2.trim().toLowerCase();
                    if (!c1.equals(c2)) {
                        if (edgeList.containsKey(c1 + "-" + c2)) {
                            edgeList.put(c1 + "-" + c2, edgeList.get(c1 + "-" + c2) + 1);
                        } else if (edgeList.containsKey(c2 + "-" + c1)) {
                            edgeList.put(c2 + "-" + c1, edgeList.get(c2 + "-" + c1) + 1);
                        } else {
                            edgeList.put(c1 + "-" + c2, 1.);
                        }

                    }
                }
            }
        }
    }

    private void writeToFile() throws IOException {
        File file = new File(conceptGraph);
        FileWriter fw = new FileWriter(file, true);

        BufferedWriter bw = new BufferedWriter(fw);

        for(Map.Entry<String, Double> ent: edgeList.entrySet()) {
            String[] s = ent.getKey().split("-");
            String class1 = s[0].trim().toLowerCase();
            String class2 = s[1].trim().toLowerCase();
            bw.write(class1 + "\t" + class2 + "\t" + ent.getValue() + "\n");
        }

        bw.close();
    }
    
}
