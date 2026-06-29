package io.freedriver.autonomy.async;

import java.io.File;
import java.util.Optional;
import javax.xml.parsers.DocumentBuilderFactory;

import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Document;

@Slf4j
public class XMLVoid {
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
            log.error("Couldn't open file", e);
            return Optional.empty();
        }
    }

}