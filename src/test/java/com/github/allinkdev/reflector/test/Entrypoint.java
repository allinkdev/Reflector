package com.github.allinkdev.reflector.test;

import com.github.allinkdev.reflector.Reflector;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public final class Entrypoint {
    public static void main(final String... args) {
        final Optional<Reflector<?>> reflectorOptional = Reflector.createNew("com.github.allinkdev.reflector.test.TestClass", Entrypoint.class.getClassLoader());

        if (!reflectorOptional.isPresent()) {
            throw new IllegalStateException("Reflector optional was not present!");
        }

        final Reflector<?> reflector = reflectorOptional.get();
        final List<String> classNames = reflector
                .allSubClassesInCurrentPackage()
                .map(Class::getTypeName)
                .collect(Collectors.toList());

        System.out.printf("[%s]", String.join(", ", classNames));
    }
}
