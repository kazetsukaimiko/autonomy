package io.freedriver.autonomy.util;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class FestivalTest {

    private static final List<String> words = Stream.of(
            "Wet Noodle",
            "Pastel Marbles",
            "Maple Jack-O-Lantern",
            "Etch A Sketch",
            "Poker Face",
            "Rope Faces",
            "Depth Charge",
            "Eight Insignia",
            "Quarantine Demonstration",
            "Innovation Observation",
            "Terraforming Category"
    ).collect(Collectors.toList());

    /*
    @Test
    public void testFestival() {
        IntStream.range(0,10)
                .mapToObj(FestivalTest::feed)
                .peek(System.out::println)
                .forEach(Festival::speak);
    }

    public static String feed(int input) {
        try {
            Thread.sleep(250);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return randomMember() + "\n\n\n";
    }

    public static String randomMember() {
        Random r = new Random(System.currentTimeMillis());
        return Optional.of(words)
                .map(List::size)
                .map(r::nextInt)
                .map(words::get)
                .orElse("empty");
    }

     */
}
