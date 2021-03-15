package ltr.collections.jrc.parser;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import ltr.collections.jrc.EuroVocDoc;
import ltr.features.QueryDocument;
import ltr.parser.Parser;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import org.apache.log4j.Logger;


/***
 * Esta classe transforma arquivos .xml da coleção eurovoc em DOM objects.
 */
public class EuroVocParser implements Parser<QueryDocument> {

    static final Logger logger = Logger.getLogger(EuroVocParser.class.getName());

    @Override
    public List<QueryDocument> parse(List<File> files) throws Exception {
        List<Future<EuroVocDoc>> futures = new ArrayList<>();

        ExecutorService executor = Executors.newFixedThreadPool(10);

        for (File file : files) {
            final Future<EuroVocDoc> future = executor.submit(new EuroVocParserCallable(file));
            futures.add(future);
        }
        executor.awaitTermination(2, TimeUnit.MINUTES);

        ArrayList<QueryDocument> domDocs = new ArrayList<>();
        for (Future<EuroVocDoc> f : futures) {
            try {
                domDocs.add(f.get());
            } catch (InterruptedException | ExecutionException e) {
                logger.error(e.getMessage());
            }
        }

        executor.shutdown();

        return domDocs;
    }

}

class EuroVocParserCallable implements Callable<EuroVocDoc> {

    private final File file;

    public EuroVocParserCallable(File file) {
        this.file = file;
    }

    @Override
    public EuroVocDoc call()
            throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {

        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        docFactory.setNamespaceAware(true); // suportar xml

        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document domDoc = docBuilder.parse(file);

        XPathFactory xPathFactory = XPathFactory.newInstance();
        XPath xpath = xPathFactory.newXPath();

        XPathExpression idExpr = xpath.compile("/TEI.2");
        XPathExpression nExpr = xpath.compile("/TEI.2");
        XPathExpression langExpr = xpath.compile("/TEI.2");
        XPathExpression creationDateExpr = xpath.compile("//teiHeader");
        XPathExpression titleExpr = xpath.compile("//title[2]");
        XPathExpression urlExpr = xpath.compile("//xref");
        XPathExpression noteExpr = xpath.compile("//note");
        XPathExpression classExpr = xpath.compile("//classCode");
        XPathExpression textExpr = xpath.compile("//p");

        String id = getId(idExpr, domDoc);
        String n = getNumber(nExpr, domDoc);
        String lang = getLang(langExpr, domDoc);
        String creationDate = getCreationDate(creationDateExpr, domDoc);
        String title = getTitle(titleExpr, domDoc);
        String url = getUrl(urlExpr, domDoc);
        String note = getNote(noteExpr, domDoc);
        String text = getText(textExpr, domDoc);
        ArrayList<String> classes = getClasses(classExpr, domDoc);

        return new EuroVocDoc(id, n, lang, creationDate, title, url, note, text, classes);
    }

    private static String getId(XPathExpression idExpr, Document domDoc) throws XPathExpressionException {
        return ((Node) idExpr.evaluate(domDoc, XPathConstants.NODE)).getAttributes().getNamedItem("id").getTextContent();
    }

    private static String getNumber(XPathExpression nExpr, Document domDoc) throws XPathExpressionException {
        return ((Node) nExpr.evaluate(domDoc, XPathConstants.NODE)).getAttributes().getNamedItem("n").getTextContent();
    }

    private static String getLang(XPathExpression langExpr, Document domDoc) throws XPathExpressionException {
        return ((Node) langExpr.evaluate(domDoc, XPathConstants.NODE)).getAttributes()
                .getNamedItem("lang").getTextContent();
    }

    private static String getCreationDate(XPathExpression creationDateExpr, Document domDoc)
            throws XPathExpressionException {
        return ((Node) creationDateExpr.evaluate(domDoc, XPathConstants.NODE)).getAttributes()
                .getNamedItem("date.created").getTextContent();
    }

    private static String getTitle(XPathExpression titleExpr, Document domDoc) throws XPathExpressionException {
        return (String) titleExpr.evaluate(domDoc, XPathConstants.STRING);
    }

    private static String getUrl(XPathExpression urlExpr, Document domDoc) throws XPathExpressionException {
        return (String) urlExpr.evaluate(domDoc, XPathConstants.STRING);
    }

    private static String getNote(XPathExpression noteExpr, Document domDoc) throws XPathExpressionException {
        return (String) noteExpr.evaluate(domDoc, XPathConstants.STRING);
    }

    private static String getText(XPathExpression textExpr, Document domDoc)
            throws XPathExpressionException {
        NodeList textNodes = (NodeList)textExpr.evaluate(domDoc, XPathConstants.NODESET);
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < textNodes.getLength(); i++) {
            text.append(textNodes.item(i).getTextContent()).append("\n");
        }
        return text.toString();
    }

    private static ArrayList<String> getClasses(XPathExpression classExpr, Document domDoc)
            throws XPathExpressionException {
        ArrayList<String> classes = new ArrayList<>();
        NodeList classesNodes = (NodeList) classExpr.evaluate(domDoc, XPathConstants.NODESET);
        for (int i = 0; i < classesNodes.getLength(); i++) {
            classes.add(classesNodes.item(i).getTextContent());
        }
        return classes;
    }

}



