package io.freedriver.autonomy.fx;

import io.freedriver.wmctrl.WMCtrlEntry;
import io.freedriver.wmctrl.WMCtrlReader;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.robot.Robot;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.OptionalDouble;

public class WatchWindow extends Application {
    private Robot robot;
    private ImageView iv;
    private Stage stage;
    private WMCtrlEntry entry;
    private Rectangle2D rectangle2D;
    private WritableImage writableImage;
    private long lastUpdate = 0;
    private List<Long> times = new ArrayList<>();
    private List<Long> frames = new ArrayList<>();

    public static void main(String[] args) {
        Application.launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        this.stage = stage;
        robot = new Robot();


        iv = new ImageView();
        iv.setPreserveRatio(true);
        iv.setSmooth(true);
        Group root = new Group();
        Scene scene = new Scene(root);
        scene.setFill(Color.BLACK);
        root.getChildren().add(iv);
        stage.setTitle("ImageView");
        //stage.setWidth(1280);
        //stage.setHeight(720);
        stage.setScene(scene);
        stage.sizeToScene();
        stage.show();


        Platform.runLater(this::refresh);
    }

    public double getFPS() {
        return 1000L / differences(times)
                .stream()
                .mapToLong(Long::valueOf)
                .average()
                .orElse(1000D);
    }

    public List<Long> differences(List<Long> input) {
        if (input.isEmpty() || input.size() == 1) {
            return Collections.emptyList();
        }
        List<Long> diffs = new ArrayList<>();
        Long current = input.get(0);
        for (int i=1; i<input.size();i++) {
            diffs.add(input.get(i)-current);
            current = input.get(i);
        }
        return diffs;
    }

    public synchronized void update() {
        if (System.currentTimeMillis()-lastUpdate >= 1000) {
            System.out.println("FPS: " + getFPS() + " / " + (1000L / frames.stream().mapToLong(Long::valueOf).average().orElse(1000D)));
            entry = WMCtrlReader.getActiveWindows().stream()
                    .filter(WMCtrlEntry::isWindow)
                    .filter(WMCtrlEntry::nonZero)
                    .filter(entry -> entry.getTitle().contains("GSdx"))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Can't find window"));
            rectangle2D = entry.getRectangle2D();
            if (writableImage == null || writableImage.getWidth() != entry.getWidth() || writableImage.getHeight() != entry.getHeight()) {
                writableImage = new WritableImage(entry.getWidth(), entry.getHeight());
                System.out.println("New writable image: " +
                                (writableImage == null ? "NULL" : "") +
                                (writableImage.getWidth() != entry.getWidth()? "WID ("+writableImage.getWidth()+"vs"+entry.getWidth()+")" : "") +
                            (!Objects.equals(writableImage.getHeight(), entry.getHeight())? "HEI" : "")
                        );

                iv.setImage(writableImage);
            }
            iv.setFitWidth(entry.getWidth());
            iv.setFitHeight(entry.getHeight());
            stage.setWidth(entry.getWidth());
            stage.setHeight(entry.getHeight());
            lastUpdate = System.currentTimeMillis();
        }
    }

    private void updateStage(WMCtrlEntry entry) {
        //iv.setImage(captureWindow(entry));
        long s = System.currentTimeMillis();
        captureWindow(entry);
        long e = System.currentTimeMillis() - s;
        frames.add(e);
        if (frames.size() > 10) {
            frames = frames.subList(frames.size()-10, frames.size());
        }
    }


    public void refresh() {
        times.add(System.currentTimeMillis());
        if (times.size() > 10) {
            times = times.subList(times.size() - 10, times.size());
        }
        update();
        updateStage(entry);
        Platform.runLater(this::refresh);
    }

    public void displayWindow(WMCtrlEntry entry) {
        ImageView iv = new ImageView();
    }

    public WritableImage captureWindow(WMCtrlEntry entry) {
        return robot
                .getScreenCapture(writableImage, rectangle2D);
    }

}
