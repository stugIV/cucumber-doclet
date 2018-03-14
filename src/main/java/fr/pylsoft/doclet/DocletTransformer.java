package fr.pylsoft.doclet;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Create class inherit from this interface to generate your own report.
 * <p>
 * Don't forget to put the '-t yourTransformerClass' when you call javadoc exe
 *
 * @author pylsoft
 */
public abstract class DocletTransformer {

    private String outputDirectory;

    private final XPath xpath = XPathFactory.newInstance().newXPath();

    protected static class TAG_XML {
        public static final String ROOT = "JAVADOC";
        public static final String GROUP = "GROUP";
        public static final String CLASS = "CLASS";
        public static final String ENUM = "ENUM";
        public static final String FIELD = "FIELD";
        public static final String FUNCTION = "FUNCTION";
        public static final String ANNOTATION = "ANNOTATION";
        public static final String COMMENT = "COMMENT";
        public static final String PARAM = "PARAM";
        public static final String PHRASE = "PHRASE";
        public static final String LINE = "LINE";
        public static final String TAG = "TAG";
        public static final String RESUME = "RESUME";
    }

    public static class ATTRIBUTE_XML {
        public static final String VERSION = "docletVersion";
        public static final String DATE = "date";
        public static final String NAME = "name";
        public static final String PACKAGE = "package";
        public static final String GROUP = "group";
        public static final String DESCRIPTION = "description";
        public static final String PHRASE = "phrase";
        public static final String VALUE = "value";
        public static final String TYPE = "type";
        public static final String NAME_PARAMETER = "nameParameter";
        public static final String DEPRECATED = "Deprecated";
        public static final String NAME_PHRASE = "namePhrases";
    }

    protected void setOutputDirectory(String outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    protected String getOutputDirectory() {
        return outputDirectory;
    }

    /**
     * Define the name of the output file
     *
     * @return the name of the output file with its extension
     */
    public abstract String getFileName();

    protected void generateCucumberDoc(Document document) throws DocletCucumberException {


        File file = new File(getOutputDirectory() + getFileName());
        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file));
             PrintWriter printer = new PrintWriter(bufferedWriter)) {
            generateCucumberDoc(document.getDocumentElement(), printer);
        } catch (IOException e) {
            throw new DocletCucumberException("Error during file creation : " + e.getMessage(), e);
        }
        System.out.println("Generated file via " + this.getClass().getSimpleName() + " : " + getFileName() + " has been created.");
    }

    /**
     * This method is overloaded to create the output file from the Document dom xml
     *
     * @param rootElement - the root element of the XML feed containing the cucumber doc
     * @param printer       - the printer to write to the file
     * @throws DocletCucumberException in case of error during file generation
     */
    public abstract void generateCucumberDoc(final Element rootElement, final PrintWriter printer) throws DocletCucumberException;

    /**
     * This method starts an evaluation of an Xpath expression on the current node
     *
     * @param expression  - expression Xpath
     * @param currentNode - the current Xml node
     * @return the list of the elements resulting from the expression
     */
    public List<Element> evaluateExpressionXpath(final String expression, final Node currentNode) {
        List<Element> listeElements = new ArrayList<>();
        try {
            NodeList resultats = (NodeList) xpath.evaluate(expression, currentNode, XPathConstants.NODESET);
            for (int index = 0; index < resultats.getLength(); index++) {
                Node item = resultats.item(index);
                if (Element.class.isAssignableFrom(item.getClass())) {
                    listeElements.add(Element.class.cast(item));
                }
            }
        } catch (XPathExpressionException e) {
            System.out.println("ERREUR durant l'évalution de l'expression :" + expression);
        }
        return listeElements;
    }
}
