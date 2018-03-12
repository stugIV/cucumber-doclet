package fr.pylsoft.doclet;

import com.sun.javadoc.Parameter;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

class Util {

    private final static Pattern PATTERN = Pattern.compile("^(.*)\\(\\?\\:([^\\)]*)\\)(.*)$");
    private final static String CUCUMBER_NAME_PACKAGE = "cucumber.api.java";
    private final static String PART_NAME_JAR_CUCUMBER = "cucumber-java";

    public static void main(String[] args) {
        System.out.println("----------");
        System.out.println("- Test 1 -");
        System.out.println("----------");
        String phrase = "le contexte (?:de|du|des|d'|le|la|les|l') (.*) de ta (?:mere|pere) (.*):";
        //String phrase = "un b�n�ficiaire ouvrant-droit avec le matricule '(.*)' et le num�ro de famille '(.*)'";
        System.out.println(phrase);
        System.out.println("-------");
        extractPhrasesList(phrase).forEach(System.out::println);

        System.out.println("----------");
        System.out.println("- Test 2 -");
        System.out.println("----------");
        String phraseAfterTreatment = "a beneficiary : opening-right with the roll number '(.*)' and the family number '(.*)':";
        System.out.println(phraseAfterTreatment);
        System.out.println("-------");
        {
            String[] parametres = new String[]{"PARAM1", "PARAM2", "PARAM3"};
            for (String parametre : parametres) {
                String nomParametre = "[" + parametre + "]";
                phraseAfterTreatment = phraseAfterTreatment.replaceFirst("\\([^\\)]*\\)|(:$)", "$1" + nomParametre);
            }

            System.out.println("phrase after treatment=" + phraseAfterTreatment);
        }
        System.out.println("----------");
        System.out.println("- Test 3 -");
        System.out.println("----------");
        try {
            List<String> annotationList = recoverCucumberAnnotationList();
            System.out.println("no annotation cucumber found =" + annotationList.size());

        } catch (DocletCucumberException e) {
            System.out.println(e.getMessage());
        }
    }

    static boolean isNotNullAndNotEmpty(String string) {
        return string != null && !string.isEmpty();
    }

    static boolean isNullOrEmpty(String string) {
        return string == null || string.isEmpty();
    }

    /**
     * This method retrieves all Cucumber annotations
     * from the jar cucumber-java
     *
     * @return The list of names of each annotation cucumber in all languages
     */
    static List<String> recoverCucumberAnnotationList() throws DocletCucumberException {
        List<String> classes = processJarByClassPath();

        if (classes.isEmpty()) {
            System.out.println("le jar " + PART_NAME_JAR_CUCUMBER + " was not found in classPath");
            classes = processJarByClassLoader();

            if (classes.isEmpty()) {
                throw new DocletCucumberException("the jar " + PART_NAME_JAR_CUCUMBER + " was not found in the classLoader");
            }
        }
        return classes;
    }

    /**
     * This method tries to find the jar cucumber-java via the classLoader
     *
     * @return The list of names of each annotation cucumber in all languages
     * If the jar was found in the classLoader
     */
    private static List<String> processJarByClassLoader() {
        List<String> classes = new ArrayList<>();
        String packageName = CUCUMBER_NAME_PACKAGE.replace('.', '/');

        Enumeration<URL> resource;
        try {
            resource = Util.class.getClassLoader().getResources(packageName);
        } catch (IOException e) {
            return classes;
        }

        Collections.list(resource).forEach(url -> {//
            System.out.println("url found = " + url.toString());
            try {
                URLConnection con = url.openConnection();
                JarFile jfile = null;

                if (con instanceof JarURLConnection) {
                    // Should usually be the case for traditional JAR files.
                    JarURLConnection jarCon = (JarURLConnection) con;
                    jfile = jarCon.getJarFile();

                    classes.addAll(processJar(jfile));
                }
            } catch (IOException e) {
                // if problem we do not do anything and we go to the next jar
            }
        });
        return classes;

    }

    /**
     * This method tries to find the jar cucumber-java via the classpath
     *
     * @return The list of names of each annotation cucumber in all languages
     * If the jar was found in the classpath system
     */
    private static List<String> processJarByClassPath() {
        List<String> classes = new ArrayList<>();

        // We retrieve all entries of CLASSPATH
        String[] entries = System.getProperty("java.class.path").split(System.getProperty("path.separator"));

        // For all these entries, we check if they contain a jar
        for (final String jar : entries) {
            if (jar.endsWith(".jar")) {
                System.out.println("jar from classpath :" + jar);
                if (isNotNullAndNotEmpty(jar) && jar.contains(PART_NAME_JAR_CUCUMBER)) {
                    System.out.println("Jar " + PART_NAME_JAR_CUCUMBER + " found:" + jar);
                    try (JarFile jfile = new JarFile(jar);) {

                        classes.addAll(processJar(jfile));
                    } catch (IOException e) {
                        // And we do not do anything because not the jar that interests us
                        // should not happen
                    }
                }
            }
        }
        return classes;
    }

    /**
     * This method returns the list of Cucumber annotations contained in the jarFile
     * and starting with
     * {@link fr.pylsoft.doclet.Util}
     *
     * @param jarFile - the jarFile containing the package
     *                {@link fr.pylsoft.doclet.Util}
     * @return the list of Cucumber annotations found
     */
    private static List<String> processJar(final JarFile jarFile) {

        return Collections.list(jarFile.entries()).stream() //
                .filter(element -> element.getName().matches("^" + CUCUMBER_NAME_PACKAGE.replace(".", "/") + "/(.*)/(.*).class$")) //
                .map(element -> element.getName().replace('/', '.').replaceAll(".class", "")) //
                .map(className -> {
                    try {
                        return Class.forName(className).getSimpleName();
                    } catch (ClassNotFoundException e) {
                        System.out.println("element not a class :" + className);
                        // we do not do anything and we move on to the next class
                        // should not happen
                        return "";
                    }
                }) //
                .collect(Collectors.toList());
    }

    static List<String> extractPhrasesList(final String phrase) {
        return extractPhrasesList(phrase, new ArrayList<>());
    }

    private static List<String> extractPhrasesList(final String phrase, final List<String> phrasesList) {

        List<String> resultList = phrasesList;

        Matcher matcher = PATTERN.matcher(phrase);

        if (matcher.matches()) {
            for (int index = 1; index <= matcher.groupCount(); index++) {
                resultList = extractPhrasesList(matcher.group(index), resultList);
            }
        } else resultList = Arrays.stream(phrase.split("\\|")) //
                .map((String lastPhrase) -> {
                    if (phrasesList.size() == 0) {
                        return Collections.singletonList(lastPhrase);
                    } else {
                        return phrasesList.stream().map(firstPhrase -> (firstPhrase + lastPhrase))
                                .collect(Collectors.toList());
                    }
                }).flatMap(List::stream).collect(Collectors.toList());

        return resultList;
    }

    static String additionParameterInPossiblePhrase(final String possiblePhrase, final Parameter[] parameters) {

        String phraseAfterProcess = possiblePhrase;
        for (Parameter parameter : parameters) {
            String parameterName = "[" + parameter.name() + "]";
            phraseAfterProcess = phraseAfterProcess.replaceFirst("\\([^\\)]*\\)|(:$)", "$1" + parameterName);
        }

        return phraseAfterProcess;
    }
}
