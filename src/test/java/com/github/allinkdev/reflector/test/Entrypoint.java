package com.github.allinkdev.reflector.test;

import com.github.allinkdev.reflector.Reflector;

import java.util.List;
import java.util.stream.Collectors;

public final class Entrypoint {
    public static void main(final String... args) {
        final Reflector<TestClass> reflector = Reflector.createNew(TestClass.class);

        final List<Class<?>> staticFields = reflector
                .allSubClassesInCurrentPackage()
                .peek(c -> System.out.println(c))
                .collect(Collectors.toList());

        //System.out.printf("[%s]", String.join(", ", staticFields));
    }
}
