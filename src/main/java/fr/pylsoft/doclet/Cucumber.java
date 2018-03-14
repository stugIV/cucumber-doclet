package fr.pylsoft.doclet;

import com.sun.javadoc.*;
import com.sun.javadoc.AnnotationDesc.ElementValuePair;
import fr.pylsoft.doclet.DocletTransformer.ATTRIBUTE_XML;
import fr.pylsoft.doclet.DocletTransformer.TAG_XML;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class Cucumber {
    private final static String LINE_BREAK = "\\n";
    private final static String SEPARATOR = ",";

    private final static String DEPRECATED = "Deprecated";
    private final static String EXAMPLE = "example";

    private static final String FILE_XSL_XML_VERS_HTML_DEFAULT = "DocCucumberToHtml.xsl";
    private static final String FILE_XSL_XML_VERS_TXT_DEFAULT = "DocCucumberToText.xsl";
    private static final String DEFAULT_OUTPUT_FILE_NAME = "CataloguePhraseCucumber";

    private static final String DEFAULT_GROUP_NAME = "Other";

    private final static List<String> ANNOTATIONS_INCLUDED = new ArrayList<>();

    private final static Map<String, Integer> mapAnnotationsFound = new HashMap<>();

    private final static Map<String, String> listOptions = new HashMap<>();

    private final static Map<String, List<String>> groups = new HashMap<>();

    private static Logger logger = LoggerFactory.getLogger(Cucumber.class);

    public static int optionLength(String option) {
        Integer length = Option.OPTIONS_LENGTH.get(option);
        return length != null ? length : 0;
    }

    /**
     * @param root Root document containing the result of javadoc.exe processing
     * @return true if treatment OK otherwise false
     */
    public static boolean start(RootDoc root) {
        try {
            ANNOTATIONS_INCLUDED.addAll(Util.recoverCucumberAnnotationList());
            ANNOTATIONS_INCLUDED.addAll(Collections.singletonList(DEPRECATED));

            // Update options
            for (String[] option : root.options()) {
                listOptions.put(option[0], option.length > 1 ? option[1] : "");
                if (option[0].equals(Option.GROUP)) {
                    Arrays.stream(option[2].split(":")).map(packageName -> {
                        if(groups.get(packageName) == null)
                          groups.put(packageName, new LinkedList<>(Collections.singletonList(option[1])));
                        else
                          groups.get(packageName).add(option[1]);
                        return packageName;
                    }).collect(Collectors.toList());
                }
            }

            Document document = createXMLDocument(root);

            createOutputFileViaTransformers(listOptions.get(Option.TRANSFORMERS), document);

            createOutputFile(document);

        } catch (DocletCucumberException e) {
            System.out.println(e.getMessage());
            return false;
        }

        return true;
    }

    private static Document createXMLDocument(final RootDoc root) throws DocletCucumberException {

        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            final DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.newDocument();
            Element rootElement = document.createElement(TAG_XML.ROOT);
            rootElement.setAttribute(ATTRIBUTE_XML.DATE, LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            rootElement.setAttribute(ATTRIBUTE_XML.VERSION, Cucumber.class.getPackage().getImplementationVersion());
            document.appendChild(rootElement);

            if (!listOptions.containsKey(Option.GROUP)) {
                ClassDoc[] classes = root.classes();
                for (ClassDoc classDoc : classes) {
                    Element elm = docByClass(document, classDoc);
                    if (elm != null) {
                        rootElement.appendChild(elm);
                    }
                }
                informAboutAnnotationsFoundInDocument(document, rootElement);
            }
            else {
                Map<String, Element> groups = new HashMap<>();
                ClassDoc[] classes = root.classes();
                for (ClassDoc classDoc : classes) {
                    Element elm = docByClass(document, classDoc);
                    if (elm != null) {
                        String groupName = elm.getAttribute(ATTRIBUTE_XML.GROUP);
                        Element group = groups.get(groupName);
                        if (group == null) {
                            group = document.createElement(TAG_XML.GROUP);
                            group.setAttribute(ATTRIBUTE_XML.NAME, groupName);
                            groups.put(groupName, group);
                            rootElement.appendChild(group);
                        }
                        group.appendChild(elm);
                    }
                }
                informAboutAnnotationsFoundInDocument(document, rootElement);
            }
            return document;
        } catch (ParserConfigurationException e) {
            throw new DocletCucumberException("Error while retrieving Doclet configuration", e);
        }
    }

    private static void informAboutAnnotationsFoundInDocument(final Document document, final Element rootElement) {
        if (mapAnnotationsFound.isEmpty()) {
            return;
        }

        Element elementResume = document.createElement(TAG_XML.RESUME);
        rootElement.appendChild(elementResume);

        mapAnnotationsFound.forEach((annotation, number) -> {
            System.out.println(annotation + "=" + number + " phrases");
            Element elementAnnotation = document.createElement(TAG_XML.ANNOTATION);
            elementAnnotation.setAttribute(ATTRIBUTE_XML.NAME, annotation);
            elementAnnotation.setAttribute(ATTRIBUTE_XML.NAME_PHRASE, number.toString());
            elementResume.appendChild(elementAnnotation);
        });

    }

    private static void addNewPhraseInMapAnnotation(String annotationName) {
        Integer number = mapAnnotationsFound.get(annotationName);
        if (number == null) {
            number = 1;
        } else {
            number = Integer.sum(number, 1);
        }
        mapAnnotationsFound.put(annotationName, number);
    }

    private static Element docByClass(Document document, ClassDoc classDoc) throws DocletCucumberException {
        Element elementClass = document.createElement(TAG_XML.CLASS);
        elementClass.setAttribute(ATTRIBUTE_XML.NAME, classDoc.name());
        elementClass.setAttribute(ATTRIBUTE_XML.PACKAGE, classDoc.qualifiedName());
        //append to group
        if (listOptions.containsKey(Option.GROUP)) {
            String groupName = DEFAULT_GROUP_NAME;
            if (groups.get(classDoc.qualifiedName()) != null)
                groupName = groups.get(classDoc.qualifiedName()).get(0);
            elementClass.setAttribute(ATTRIBUTE_XML.GROUP, groupName);
        }

        elementClass.setAttribute(ATTRIBUTE_XML.DESCRIPTION, classDoc.commentText());
        if (classDoc.superclassType().typeName().equals("Enum"))
            elementClass.appendChild(processEnum(document, classDoc));
        Arrays.stream(classDoc.methods()) //
                .map(methodDoc -> docByMethod(document, methodDoc)) //
                .filter(Objects::nonNull) //
                .forEach(elementClass::appendChild);

        return elementClass;//elementClass.getChildNodes().getLength() > 0 ? elementClass : null;
    }

    private static Element processEnum(final Document document, final ClassDoc classDoc) {
        Element elementClass = document.createElement(TAG_XML.ENUM);
        for(FieldDoc field: classDoc.fields()){
            Element fieldEl = document.createElement(TAG_XML.FIELD);
            fieldEl.setAttribute(ATTRIBUTE_XML.NAME, field.name());
            fieldEl.setAttribute(ATTRIBUTE_XML.DESCRIPTION, field.commentText());
            elementClass.appendChild(fieldEl);
        }
        return elementClass;
    }

    private static Element docByMethod(final Document document, final MethodDoc method) {
        final Element elementMethod = document.createElement(TAG_XML.FUNCTION);
        if (method.annotations() != null && method.annotations().length > 0) {
            elementMethod.setAttribute(ATTRIBUTE_XML.NAME, method.name());

            Arrays.stream(method.annotations()) //
                    .map(annotationDesc -> docByAnnotation(document, elementMethod, annotationDesc, method.parameters())) //
                    .filter(Objects::nonNull) //
                    .forEach(elementMethod::appendChild);

            if (elementMethod.getChildNodes().getLength() > 0) {
                docByParameter(document, elementMethod, method.parameters());
                docByComments(document, elementMethod, method.commentText());
                docByParameterTag(document, elementMethod, method.paramTags());
                docByTag(document, elementMethod, method.tags(EXAMPLE));
                return elementMethod;
            }
        }

        return null;
    }

    private static void docByParameter(Document document, Element elementMethod, Parameter[] parameters) {
        for (Parameter parameter : parameters) {
            Element elmTag = document.createElement(TAG_XML.PARAM);
            elmTag.setAttribute(ATTRIBUTE_XML.NAME, parameter.name());
            elmTag.setAttribute(ATTRIBUTE_XML.TYPE, parameter.typeName());
            elementMethod.appendChild(elmTag);
        }
    }

    private static Element docByComments(Document document, final Element elmMethod, final String comment) {
        if (Util.isNotNullAndNotEmpty(comment)) {
            Element elementComment = document.createElement(TAG_XML.COMMENT);
            String[] commentLines = comment.split(LINE_BREAK);
            for (String line : commentLines) {
                Element elmLine = document.createElement(TAG_XML.LINE);
                elmLine.setTextContent(line.replace("\"", "\\\""));
                elementComment.appendChild(elmLine);
            }
            elmMethod.appendChild(elementComment);
        }

        return elmMethod;
    }

    private static void docByParameterTag(Document document, Element elmMethod, ParamTag[] paramTags) {
        for (ParamTag tag : paramTags) {
            Element elmTag = document.createElement(TAG_XML.TAG);
            elmTag.setAttribute(ATTRIBUTE_XML.NAME, tag.name().replaceAll("@", ""));
            elmTag.setAttribute(ATTRIBUTE_XML.NAME_PARAMETER, tag.parameterName());

            String[] comment = tag.parameterComment().split(LINE_BREAK);
            for (String line : comment) {
                Element elmLine = document.createElement(TAG_XML.LINE);
                elmLine.setTextContent(line);
                elmTag.appendChild(elmLine);
            }
            elmMethod.appendChild(elmTag);
        }
    }

    private static void docByTag(Document document, Element elmMethod, Tag[] tags) {
        for (Tag tag : tags) {
            Element elmTag = document.createElement(TAG_XML.TAG);
            elmTag.setAttribute(ATTRIBUTE_XML.NAME, tag.name().replace("@", ""));

            String[] comment = tag.text().split(LINE_BREAK);
            for (String line : comment) {
                Element elmLine = document.createElement(TAG_XML.LINE);
                elmLine.setTextContent(line);
                elmTag.appendChild(elmLine);
            }
            elmMethod.appendChild(elmTag);
        }
    }

    private static Element docByAnnotation(final Document document, final Element elmFunction,
                                           final AnnotationDesc annotation, final Parameter[] parameters) {
        String annotationName = annotation.annotationType().simpleTypeName();

        if (!ANNOTATIONS_INCLUDED.contains(annotationName)) {
            return null;
        }
        if (Objects.equals(annotationName, DEPRECATED)) {
            elmFunction.setAttribute(ATTRIBUTE_XML.DEPRECATED, "true");
            addNewPhraseInMapAnnotation(DEPRECATED);
            return null;
        } else {
            final Element elmAnnotation = document.createElement(TAG_XML.ANNOTATION);
            elmAnnotation.setAttribute(ATTRIBUTE_XML.NAME, annotationName);
            Arrays.stream(annotation.elementValues()) //
                    .map(Cucumber::docByAnnotationContent).filter(Util::isNotNullAndNotEmpty) //
                    .forEach(phrase -> {
                        phrase = phrase.replace("\"", "\\\"");
                        elmAnnotation.setAttribute(ATTRIBUTE_XML.PHRASE, phrase);
                        createListOfPossiblePhrases(document, elmAnnotation, phrase, parameters);
                    });

            if (elmAnnotation.getAttribute(ATTRIBUTE_XML.PHRASE) != null) {
                addNewPhraseInMapAnnotation(annotationName);
                return elmAnnotation;
            }
            return null;
        }
    }

    private static void createListOfPossiblePhrases(Document document, Element elmAnnotation, String phrase,
                                                    Parameter[] parameters) {
        final List<String> possiblePhrases = Util.extractPhrasesList(phrase);
        if (!possiblePhrases.isEmpty()) {
            for (final String possiblePhrase : possiblePhrases) {
                Element elm = document.createElement(TAG_XML.PHRASE);
                elm.setTextContent(Util.additionParameterInPossiblePhrase(possiblePhrase, parameters));
                elmAnnotation.appendChild(elm);
            }
        }
    }

    private static String docByAnnotationContent(ElementValuePair elementValuePair) {
        String phrase = null;
        if (ATTRIBUTE_XML.VALUE.equals(elementValuePair.element().name())) {
            AnnotationValue annotationValue = elementValuePair.value();
            if (annotationValue != null) {
                phrase = annotationValue.value().toString();
                phrase = phrase.replaceAll("[\\^$]", "");
            }
        }

        return phrase;
    }

    private static void createOutputFileViaTransformers(final String transformers, final Document document) throws DocletCucumberException {
        if (transformers != null) {
            String[] transformersList = transformers.split(SEPARATOR);
            for (String transformer : transformersList) {
                System.out.println("Beginning of the generation since the Transformer : " + transformer);
                try {
                    Class classTransformer = Class.forName(transformer);
                    if (DocletTransformer.class.isAssignableFrom(classTransformer)) {
                        DocletTransformer transformerInstance = DocletTransformer.class.cast(classTransformer.newInstance());

                        String outputDirectory = listOptions.get(Option.OUT);
                        outputDirectory = outputDirectory == null ? "" : outputDirectory + File.separator;
                        transformerInstance.setOutputDirectory(outputDirectory);

                        transformerInstance.generateCucumberDoc(document);
                    }
                } catch (ClassNotFoundException e) {
                    System.out.println("the transformation class '" + transformer + "' was not found.");
                } catch (IllegalAccessException | InstantiationException e) {
                    System.out.println("Can not instantiate transformation class '" + transformer + "'.");
                }
            }
        }
    }

    private static void createOutputFile(Document document) throws DocletCucumberException {
        try {
            final TransformerFactory transformerFactory = TransformerFactory.newInstance();
            final Transformer transformerXML = transformerFactory.newTransformer();

            final boolean html = listOptions.containsKey(Option.HTML);
            final boolean txt = listOptions.containsKey(Option.TXT);
            final boolean xml = listOptions.containsKey(Option.XML);

            String completePathXslToHtml = listOptions.get(Option.XSL_HTML);
            if (Util.isNullOrEmpty(completePathXslToHtml)) {
                completePathXslToHtml = FILE_XSL_XML_VERS_HTML_DEFAULT;
            }
            String completePathXslToText = listOptions.get(Option.XSL_TXT);
            if (Util.isNullOrEmpty(completePathXslToText)) {
                completePathXslToText = FILE_XSL_XML_VERS_TXT_DEFAULT;
            }

            final DOMSource source = new DOMSource(document);

            String outputFileName = listOptions.get(Option.NAME);
            if (Util.isNullOrEmpty(outputFileName)) {
                outputFileName = DEFAULT_OUTPUT_FILE_NAME;
            }
            String fullPath = listOptions.get(Option.OUT);
            if (fullPath == null) {
                fullPath = "";
            }

            if (xml || (!html && !txt)) {
                Path xmlFilePath = Paths.get(fullPath, outputFileName + ".xml");
                final StreamResult xmlOutput = new StreamResult(xmlFilePath.toFile());
                transformerXML.setOutputProperty(OutputKeys.INDENT, "yes");
                transformerXML.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
                transformerXML.transform(source, xmlOutput);
                System.out.println("File '" + xmlFilePath + "' created.");
            }

            if (html) {
                StreamSource stylesource = new StreamSource(new File(completePathXslToHtml));
                final Transformer transformerHTML = transformerFactory.newTransformer(stylesource);
                final Path pathToHtmlFile = Paths.get(fullPath, outputFileName + ".html");
                final StreamResult htmlOutput = new StreamResult(pathToHtmlFile.toFile());
                transformerHTML.transform(source, htmlOutput);
                System.out.println("File '" + pathToHtmlFile + "' created.");
            }
            if (txt) {
                StreamSource stylesource = new StreamSource(new File(completePathXslToText));
                final Transformer transformerTXT = transformerFactory.newTransformer(stylesource);
                final Path txtFilePath = Paths.get(fullPath, outputFileName + ".txt");
                final StreamResult txtOutput = new StreamResult(txtFilePath.toFile());
                transformerTXT.transform(source, txtOutput);
                System.out.println("File '" + txtFilePath + "' created.");
            }
        } catch (TransformerException e) {
            throw new DocletCucumberException("Error preparing the output file", e);
        }
    }
}
