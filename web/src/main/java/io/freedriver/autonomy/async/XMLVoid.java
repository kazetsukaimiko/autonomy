package io.freedriver.autonomy.async;

import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class XMLVoid {
    public static final Logger LOGGER = Logger.getLogger(XMLVoid.class.getName());
    public static void main(String[] args) {
        if (args.length >= 2) {
            openFile(new File(args[0]))
                    .ifPresent(doc -> {
                        java.util.Arrays.stream(args, 1, args.length - 1)
                                .forEach(element -> remove(doc, element));
                    });
        }
    }


    public static boolean remove(Document doc, String element) {
        return false;
    }


    public static Optional<Document> openFile(File xmlFile) {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            return Optional.of(dbf.newDocumentBuilder().parse(xmlFile));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e, () -> "Couldn't open file");
            return Optional.empty();
        }
    }

}
