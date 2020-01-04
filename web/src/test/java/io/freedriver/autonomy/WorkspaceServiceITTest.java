package io.freedriver.autonomy;

import io.freedriver.autonomy.entity.jsonlink.AnalogPin;
import io.freedriver.autonomy.entity.jsonlink.BoardEntity;
import io.freedriver.autonomy.entity.jsonlink.DigitalPin;
import io.freedriver.autonomy.entity.jsonlink.GroupEntity;
import io.freedriver.autonomy.entity.jsonlink.PermutationEntity;
import io.freedriver.autonomy.entity.jsonlink.PinEntity;
import io.freedriver.autonomy.entity.jsonlink.VersionEntity;
import io.freedriver.autonomy.entity.jsonlink.WorkspaceEntity;
import io.freedriver.autonomy.service.crud.WorkspaceService;
import io.freedriver.jsonlink.Connector;
import io.freedriver.jsonlink.Connectors;
import io.freedriver.jsonlink.jackson.schema.v1.Identifier;
import io.freedriver.jsonlink.jackson.schema.v1.Mode;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.runner.RunWith;
import org.wildfly.swarm.arquillian.DefaultDeployment;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@RunWith(Arquillian.class)
@DefaultDeployment
public class WorkspaceServiceITTest extends NitriteCRUDServiceITTest<WorkspaceEntity, WorkspaceService> {
    @Inject
    private WorkspaceService victim;

    @Override
    WorkspaceService getVictim() {
        return victim;
    }

    @Override
    WorkspaceEntity generate(UUID boardId, int i) {
        WorkspaceEntity entity = new WorkspaceEntity();
        entity.setVersion(VersionEntity.generate("generated"+i));
        entity.getBoards().add(generateBoard(boardId, 0));
        return entity;
    }

    List<PinEntity> generatePins(int digital, int analog) {
        return Stream.concat(
                IntStream.range(0, digital).mapToObj(this::generateDigitalPin),
                IntStream.range(0, analog).mapToObj(this::generateAnalogPin)
        ).collect(Collectors.toList());
    }

    DigitalPin generateDigitalPin(int i) {
        DigitalPin pin = new DigitalPin();
        pin.setPin(Identifier.of(i));
        pin.setName("DPin"+i);
        pin.setMode(Mode.OUTPUT);
        return pin;
    }

    AnalogPin generateAnalogPin(int i) {
        AnalogPin pin = new AnalogPin();
        pin.setPin(Identifier.of(i));
        pin.setName("APin"+i);
        pin.setResistance(1000);
        pin.setVoltage(5);
        pin.setMode(Mode.INPUT);
        return pin;
    }

    BoardEntity generateBoard(UUID targetBoard, int i) {
        List<PinEntity> pins = generatePins(30, 10);
        BoardEntity entity = new BoardEntity();
        entity.setBoardId(targetBoard);
        entity.setPins(pins);

        List<PinEntity> digitalPinsInGroups = pins.stream()
                .filter(DigitalPin.class::isInstance)
                .limit(12)
                .collect(Collectors.toList());
        IntStream.range(0, 3)
                .forEach(start -> {
                    GroupEntity group = new GroupEntity();
                    group.setPins(digitalPinsInGroups.subList(start*3, (start*3)+3));
                    group.setPermutations(generateTwoPermutations(group.getPins()));
                    entity.getGroups().add(group);
                });
        return entity;
    }

    List<PermutationEntity> generateTwoPermutations(List<PinEntity> groupPins) {

        PermutationEntity allOff = new PermutationEntity();
        allOff.setInitialState(true);
        allOff.setInactivePins(groupPins);

        PermutationEntity allOn = new PermutationEntity();
        allOn.setInitialState(false);
        allOn.setActivePins(groupPins);

        return Arrays.asList(allOff, allOn);
    }
}
