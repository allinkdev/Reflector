package com.github.allinkdev.reflector.test;

public final class OtherEntrypoint {
    private static final String sex = "blabla";

    public static void main(final String... args) {
        System.out.printf("Other entrypoint called with args: [%s]%n", String.join(", ", args));
    }
}
