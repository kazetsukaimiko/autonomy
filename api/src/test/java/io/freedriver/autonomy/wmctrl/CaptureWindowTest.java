package io.freedriver.autonomy.wmctrl;

import io.freedriver.wmctrl.WMCtrlEntry;
import io.freedriver.wmctrl.WMCtrlReader;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;



import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CaptureWindowTest {
    /*
    Robot robot;
    Path directory;

    @BeforeEach
    public void makeRobot() throws AWTException, IOException {
        robot = new Robot();
        directory = Paths.get("/tmp/captureTest");
        Files.createDirectories(directory);
    }


    @Test
    public void testImageViewUpdate() {


        ImageView iv = new ImageView();
//        iv.setImage(image);
        iv.setFitWidth(100);
        iv.setPreserveRatio(true);
        iv.setSmooth(true);
        Group root = new Group();
        Scene scene = new Scene(root);
        scene.setFill(Color.BLACK);
        root.getChildren().add(iv);


//        iv.setCache(true);

        WMCtrlReader.getActiveWindows().stream()
                .filter(WMCtrlEntry::isWindow)
                .filter(WMCtrlEntry::nonZero)
                .filter(entry -> entry.getTitle().contains("GSdx"))
                .findFirst()
                .map(this::toImage)
                .ifPresent(iv::setImage);
    }



*/
}
