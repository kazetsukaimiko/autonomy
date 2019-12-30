package io.freedriver.autonomy.entity;

public interface ImmutablePerson {
    String getName();
    int getAge();
    String getEmailAddress();

    static ImmutablePerson person(final String name, final int age, final String emailAddress) {
        return new ImmutablePerson() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public int getAge() {
                return age;
            }

            @Override
            public String getEmailAddress() {
                return emailAddress;
            }
        };
    }
}
