package io.freedriver.autonomy.deployments.common.util;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Random;
import java.util.function.BiPredicate;

import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class MessageConverterTest {
    private static final Random RANDOM = new Random();

    protected abstract MessageConverter getConverter();

    public <T> void testType(T value, BiPredicate<T, T> equivalence) throws IOException {
        byte[] data = getConverter().toMessage(value);
        T read = getConverter().fromMessage(data);
        assertTrue(equivalence.test(value, read), String.format("Input %s should equal output %s", value, read));
    }


    public <T> void testType(T value) throws IOException {
        testType(value, Objects::equals);
    }

    @Test
    public void testInteger() throws IOException {
        testType(RANDOM.nextInt());
    }

    @Test
    public void testLong() throws IOException {
        testType(RANDOM.nextLong());
    }

    @Test
    public void testDouble() throws IOException {
        testType(RANDOM.nextDouble());
    }

    @Test
    public void testBoolean() throws IOException {
        testType(RANDOM.nextBoolean());
    }

    @Test
    public void testByteArray() throws IOException {
        byte[] array = new byte[RANDOM.nextInt(1024)+1024];
        RANDOM.nextBytes(array);
        testType(array, Arrays::equals);
    }

    @Test
    public void testPolymorphicPOJOs() throws IOException {
        Cat c = new Cat();
        c.setName("Tony");
        c.setBreed("Siamese");
        testType(c);

        Dog d = new Dog();
        c.setName("Max");
        c.setBreed("Akita");
        testType(d);

        testType((Animal) c);
    }


    private static class Animal {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Animal animal = (Animal) o;
            return Objects.equals(name, animal.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }
    }

    private static class Cat extends Animal {
        private String breed;

        public String getBreed() {
            return breed;
        }

        public void setBreed(String breed) {
            this.breed = breed;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;
            Cat cat = (Cat) o;
            return Objects.equals(breed, cat.breed);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), breed);
        }
    }

    private static class Dog extends Animal {
        private String breed;

        public String getBreed() {
            return breed;
        }

        public void setBreed(String breed) {
            this.breed = breed;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;
            Dog dog = (Dog) o;
            return Objects.equals(breed, dog.breed);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), breed);
        }
    }
}
