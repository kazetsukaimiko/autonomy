package io.freedriver.autonomy;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BaseTest {
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

    protected String randomName() {
        return randomElementFrom(NAMES);
    }

    protected <T> T randomElementFrom(List<T> list) {
        return list == null || list.isEmpty() ?
                null : list.get(RANDOM.nextInt(list.size()));
    }
}
