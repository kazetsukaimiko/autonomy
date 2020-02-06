package io.freedriver.autonomy.javafx;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.freedriver.autonomy.entity.jsonlink.BoardEntity;
import io.freedriver.autonomy.entity.jsonlink.DigitalPin;
import io.freedriver.autonomy.entity.jsonlink.GroupEntity;
import io.freedriver.autonomy.entity.jsonlink.PermutationEntity;
import io.freedriver.autonomy.entity.jsonlink.PinEntity;
import io.freedriver.autonomy.entity.jsonlink.WorkspaceEntity;
import io.freedriver.jsonlink.jackson.schema.v1.Identifier;
import io.freedriver.jsonlink.jackson.schema.v1.Mode;
import javafx.application.Application;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class HelloFX { //extends Application {

    /*
    private static ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    @Override
    public void start(Stage stage) {

        Scene scene = new Scene(mainView(mock()), 1680, 1050);

        stage.setScene(scene);
        stage.show();
        //Platform.runLater(() -> vb.getChildren().add(l2));
    }

    public static TabPane mainView(WorkspaceEntity workspace) {
        return workspace.getBoards()
                .stream()
                .map(HelloFX::boardPane)
                .reduce(new TabPane(), HelloFX::addTab, (a, b) -> a);
    }

    public static TabPane addTab(TabPane tabPane, Tab tab) {
        tabPane.getTabs().add(tab);
        return tabPane;
    }

    public static Tab boardPane(BoardEntity board) {
        BorderPane borderPane = new BorderPane();
        borderPane.setTop(new Text(board.getName() + " : " + board.getPins().size()));
        System.out.println("Board " + board.getBoardId().toString() + " has " + board.getPins().size());
        borderPane.setLeft(listOf(board.getPins(), pin -> new Text(pin.getName())));
        borderPane.setRight(new Text("Right"));
        borderPane.setBottom(new Text(board.getBoardId().toString()));

        Tab t = new Tab();
        t.setContent(borderPane);
        t.setText(board.getName());
        return t;
    }

    public static <T> VBox listOf(List<T> ts, Function<T, Node> nodeFunction) {
        VBox vBox = new VBox();
        List<HBox> removables = ts.stream()
                .peek(System.out::println)
                .map(t -> removable(ts, t, nodeFunction, vBox.getChildren()::remove))
                .collect(Collectors.toList());
        removables.forEach(hbox -> vBox.getChildren().add(hbox));

        return vBox;
    }

    public static <T> HBox removable(List<T> ts, T item, Function<T, Node> nodeFunction, Consumer<HBox> remover) {
        System.out.println("Removable ! ");
        HBox box = new HBox();
        Node removeable = removable(item, t -> {
            remover.accept(box);
            ts.remove(t);
            try {
                MAPPER.writeValue(System.out, ts);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        box.getChildren().addAll(removeable, nodeFunction.apply(item));
        return box;
    }

    public static <T> Node removable(T item, Consumer<T> remover) {
        Button remove = new Button("Remove");
        remove.setOnMouseClicked(mouseEvent -> {

            remover.accept(item);
        });
        return remove;
    }


    public static void main(String[] args) {
        launch();
    }








    public static WorkspaceEntity mock() {
        WorkspaceEntity workspaceEntity = new WorkspaceEntity();
        workspaceEntity.setCurrent(true);
        workspaceEntity.setBoards(IntStream.range(0, 5)
                .mapToObj(HelloFX::mockBoard)
                .collect(Collectors.toList()));
        try {
            MAPPER.writeValue(System.out, workspaceEntity);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return workspaceEntity;
    }

    public static BoardEntity mockBoard(int boardId) {
        BoardEntity boardEntity = new BoardEntity();
        boardEntity.setName("mockBoard"+boardId);
        boardEntity.setBoardId(UUID.randomUUID());

        List<PinEntity> pins = IntStream.range(0, 10)
                .mapToObj(i -> new DigitalPin(Identifier.of(i), "digital"+i, Mode.OUTPUT))
                .collect(Collectors.toList());

        boardEntity.setGroups(Arrays.asList(
                mockGroup("Evens", pins.stream()
                        .filter(pin -> pin.getPin().getPin() % 2 == 0)
                        .collect(Collectors.toList())),
                mockGroup("Odds", pins.stream()
                    .filter(pin -> pin.getPin().getPin() % 2 != 0)
                    .collect(Collectors.toList()))));

        boardEntity.setPins(pins);

        return boardEntity;
    }

    public static GroupEntity mockGroup(String groupName, List<PinEntity> pins) {
        GroupEntity groupEntity = new GroupEntity();
        groupEntity.setName(groupName);
        PermutationEntity allOff = new PermutationEntity();
        allOff.setInitialState(true);
        allOff.setInactivePins(pins);
        PermutationEntity allOn = new PermutationEntity();
        allOn.setInitialState(false);
        allOn.setActivePins(pins);
        groupEntity.setPins(pins);
        groupEntity.setPermutations(Arrays.asList(allOff, allOn));
        return groupEntity;
    }
*/
}