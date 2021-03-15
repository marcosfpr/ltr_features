package ltr.collections.jrc;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.Logger;


public class EuroVocConfig {

    static final Logger logger = Logger.getLogger(EuroVocConfig.class.getName());

    private final Map<String, String> conceptDescs = new HashMap<String, String>(); // classe, descricao da classe
    private final Map<String, ArrayList<String>> conceptUnDescs = new HashMap<String, ArrayList<String>>(); // classe, conceitos parecidos
    private final Map<String, ArrayList<String>> conceptHierarchyParents = new HashMap<String, ArrayList<String>>(); // pai, filhos
    private final Map<String, ArrayList<String>> conceptHierarchyChild = new HashMap<String, ArrayList<String>>(); // filho, pais

    public EuroVocConfig(String descPath, String unDescPath, String hierarchyPath) {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            org.w3c.dom.Document descDoc = dBuilder.parse(new File(descPath));
            NodeList nodeList = descDoc.getElementsByTagName("RECORD");
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node nNode = nodeList.item(i);
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) nNode;
                    String id = element.getElementsByTagName("DESCRIPTEUR_ID").item(0).getTextContent();
                    String desc = element.getElementsByTagName("LIBELLE").item(0).getTextContent();
                    conceptDescs.put(id, desc);
                }
            }
            logger.info("Descrições dos conceitos carregadas!");


            org.w3c.dom.Document unDescDoc = dBuilder.parse(new File(unDescPath));
            nodeList = unDescDoc.getElementsByTagName("RECORD");
            for (int i = 0; i < nodeList.getLength(); i++) {
                ArrayList<String> unDescs = new ArrayList<String>();
                Node nNode = nodeList.item(i);
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) nNode;
                    String id = element.getElementsByTagName("DESCRIPTEUR_ID").item(0).getTextContent();
                    Element elementNode = (Element) nNode;
                    NodeList nodeListUF = elementNode.getElementsByTagName("UF_EL");
                    for (int j = 0; j < nodeListUF.getLength(); j++) {
                        Node nNodeUF = nodeListUF.item(j);
                        String unDesc = nNodeUF.getTextContent();
                        unDescs.add(unDesc);
                    }
                    conceptUnDescs.put(id, unDescs);
                }
            }
            logger.info("Não descrições dos conceitos carregadas!");

            org.w3c.dom.Document hierarchyDoc = dBuilder.parse(new File(hierarchyPath));
            nodeList = hierarchyDoc.getElementsByTagName("RECORD");
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node nNode = nodeList.item(i);
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) nNode;
                    String p = element.getElementsByTagName("SOURCE_ID").item(0).getTextContent();
                    String c = element.getElementsByTagName("CIBLE_ID").item(0).getTextContent();

                    ArrayList<String> child = conceptHierarchyChild.get(p);
                    if(child ==null){
                        child = new ArrayList<String>();
                    }
                    child.add(c);
                    conceptHierarchyChild.put(p, child);

                    ArrayList<String> parents = conceptHierarchyParents.get(c);
                    if(parents ==null){
                        parents = new ArrayList<String>();
                    }
                    parents.add(p);
                    conceptHierarchyParents.put(c, parents);
                }
            }
            logger.info("Hierarquia de conceitos carregadas!");

        }
        catch (ParserConfigurationException | IOException | SAXException e) {
            logger.error(e.getMessage());
            System.exit(1);
        }
    }

    public ArrayList<String> conceptInfoExtractor(String cID) {
        ArrayList<String> fields = new ArrayList<String>();

        StringBuilder parents = new StringBuilder();
        if(this.conceptHierarchyParents.containsKey(cID)){
            for(String p : this.conceptHierarchyParents.get(cID)){
                parents.append(p).append(" ");
            }
        }
        fields.add(parents.toString().trim());
        StringBuilder children = new StringBuilder();
        if(this.conceptHierarchyChild.containsKey(cID)){
            for(String child : this.conceptHierarchyChild.get(cID)){
                children.append(child).append(" ");
            }
        }
        fields.add(children.toString().trim());

        String desc = this.conceptDescs.get(cID);
        if(desc == null)
            desc = "";
        fields.add(desc);
        StringBuilder unDescs = new StringBuilder();
        if(this.conceptUnDescs.containsKey(cID)){
            for(String undesc : this.conceptUnDescs.get(cID)){
                unDescs.append(undesc).append(" ");
            }
        }
        fields.add(unDescs.toString().trim());


        String cumDesc = this.recursiveDescParents(cID);
        fields.add(cumDesc.trim());
        String cumUnDescs = this.recursiveUnDescParents(cID);
        fields.add(cumUnDescs.trim());
        return fields;
    }

    private String recursiveDescParents(String docId){
        StringBuilder cumDesc = new StringBuilder();
        if(this.conceptHierarchyParents.containsKey(docId)){ // Tem filhos?
            for(String pId: this.conceptHierarchyParents.get(docId)){
                cumDesc.append(recursiveDescParents(pId)).append(" ");
            }
        }
        String tmp = this.conceptDescs.get(docId);
        if(tmp == null)
            tmp = "";
        return cumDesc + tmp ;
    }

    private String recursiveUnDescParents(String docId){
        StringBuilder cumDesc = new StringBuilder();
        if(this.conceptHierarchyParents.containsKey(docId)){
            for(String pId: this.conceptHierarchyParents.get(docId)){
                cumDesc.append(recursiveUnDescParents(pId)).append(" ");
            }
        }
        StringBuilder unDescs= new StringBuilder();
        if(this.conceptUnDescs.containsKey(docId)){
            for(String s: this.conceptUnDescs.get(docId)){
                unDescs.append(s).append(" ");
            }
        }
        return cumDesc + unDescs.toString().trim();
    }


    public Map<String, String> getConceptDescs() {
        return conceptDescs;
    }

    public Map<String, ArrayList<String>> getConceptUnDescs() {
        return conceptUnDescs;
    }

    public Map<String, ArrayList<String>> getConceptHierarchyParents() {
        return conceptHierarchyParents;
    }

    public Map<String, ArrayList<String>> getConceptHierarchyChild() {
        return conceptHierarchyChild;
    }
}
