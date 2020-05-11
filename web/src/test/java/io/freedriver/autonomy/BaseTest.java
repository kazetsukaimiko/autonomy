package io.freedriver.autonomy;


import io.freedriver.autonomy.jpa.entity.EntityBase;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class BaseTest {

    @Test
    public void testReduceBehavior() {
        List<Integer> order = new ArrayList<>();

        IntStream.range(0, 100)
                .boxed()
                .peek(order::add)
                .reduce(Stream.empty(), BaseTest::modTen, Stream::concat)
                .forEach(order::add);

        order.forEach(System.out::println);

        order = new ArrayList<>();
        IntStream.range(0, 100)
                .boxed()
                .peek(order::add)
                .filter(i -> i % 10 == 0)
                .forEach(order::add);
        order.forEach(System.out::println);

    }

    public static Stream<Integer> modTen(Stream<Integer> stream, Integer i) {
        if (i % 10 == 0) {
            return Stream.concat(stream, Stream.of(i));
        }
        return stream;
    }

    protected static final Random RANDOM = new Random(System.currentTimeMillis());
    private static final List<String> NAMES = Stream.of(
            "Donut",
            "Penguin",
            "Stumpy",
            "Whicker",
            "Shadow",
            "Howard",
            "Wilshire",
            "Darling",
            "Disco",
            "Jack",
            "The-Bear",
            "Sneak",
            "The-Big-L",
            "Whisp",
            "Wheezy",
            "Crazy",
            "Goat",
            "Pirate",
            "Saucy",
            "Hambone",
            "Butcher",
            "Walla-Walla",
            "Snake",
            "Caboose",
            "Sleepy",
            "Killer",
            "Stompy",
            "Mopey",
            "Dopey",
            "Weasel",
            "Ghost",
            "Dasher",
            "Grumpy",
            "Hollywood",
            "Tooth",
            "Noodle",
            "King",
            "Cupid",
            "Prancer"
    ).collect(Collectors.toList());

    protected static long randomLong() {
        return RANDOM.nextLong();
    }

    protected static String randomName(Class<? extends EntityBase> entityKlazz) {
        return entityKlazz.getSimpleName() + " " + randomBiName();
    }

    protected static String randomBiName() {
        return randomName() + " " + randomName();
    }

    protected static String randomName() {
        return randomElementFrom(NAMES);
    }

    protected static <T> T randomElementFrom(Stream<T> stream) {
        return randomElementFrom(stream.collect(Collectors.toList()));
    }

    protected static <T> T randomElementFrom(List<T> list) {
        return list == null || list.isEmpty() ?
                null : list.get(RANDOM.nextInt(list.size()));
    }


    protected static <T> Stream<T> randomNumberOf(int low, int high, Function<Integer, T> generator) {
        return IntStream.range(Math.min(low, high), Math.max(low, high)-RANDOM.nextInt(Math.abs(high - low)))
                .boxed()
                .map(generator);
    }

    /*
    protected List<PinEntity> generatePins(int digital, int analog) {
        return Stream.concat(
                IntStream.range(0, digital).mapToObj(this::generateDigitalPin),
                IntStream.range(0, analog).mapToObj(this::generateAnalogPin)
        ).collect(Collectors.toList());
    }

    protected DigitalPin generateDigitalPin(int i) {
        DigitalPin pin = new DigitalPin();
        pin.setPin(Identifier.of(i));
        pin.setName("DPin"+i);
        pin.setMode(Mode.OUTPUT);
        return pin;
    }

    protected AnalogPin generateAnalogPin(int i) {
        AnalogPin pin = new AnalogPin();
        pin.setPin(Identifier.of(i));
        pin.setName("APin"+i);
        pin.setResistance(1000);
        pin.setVoltage(5);
        pin.setMode(Mode.INPUT);
        return pin;
    }

    protected BoardEntity generateBoard(UUID targetBoard, int i) {
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

    protected List<PermutationEntity> generateTwoPermutations(List<PinEntity> groupPins) {

        PermutationEntity allOff = new PermutationEntity();
        allOff.setInitialState(true);
        allOff.setInactivePins(groupPins);

        PermutationEntity allOn = new PermutationEntity();
        allOn.setInitialState(false);
        allOn.setActivePins(groupPins);

        return Arrays.asList(allOff, allOn);
    }
    */

}
