package com.github.allinkdev.reflector.field;

import java.lang.reflect.Modifier;
import java.lang.reflect.Type;

public final class Field {
    private final String name;
    private final Object instance;
    private final Class<?> instanceClass;
    private final java.lang.reflect.Field handle;

    Field(final String name, final Object instance,
          final Class<?> instanceClass, final java.lang.reflect.Field handle) {
        this.name = name;
        this.instance = instance;
        this.instanceClass = instanceClass;
        this.handle = handle;
    }

    public static Field from(final Object instance, final java.lang.reflect.Field handle) {
        final String name = handle.getName();
        final Class<?> instanceClass = handle.getDeclaringClass();

        return new Field(name, instance, instanceClass, handle);
    }

    public static Field from(final java.lang.reflect.Field handle) {
        return from(null, handle);
    }


    public String name() {
        return name;
    }

    public boolean isStatic() {
        return Modifier.isStatic(this.modifiers());
    }

    @SuppressWarnings("unchecked")
    public <T> T value() {
        this.handle.setAccessible(true);

        try {
            return (T) this.handle.get(instance);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to get value of field!", e);
        }
    }

    public Object instance() {
        return instance;
    }

    public Class<?> instanceClass() {
        return instanceClass;
    }

    public int modifiers() {
        return this.handle.getModifiers();
    }

    public void value(final Object newValue) {
        final int modifiers = this.modifiers();

        if (Modifier.isFinal(modifiers)) {
            throw new UnsupportedOperationException("Tried to set final field!");
        }

        this.handle.setAccessible(true);

        try {
            this.handle.set(instance, newValue);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to set the value of field!", e);
        }
    }

    public java.lang.reflect.Field handle() {
        return handle;
    }

    public Type getFieldType() {
        return this.handle.getAnnotatedType()
                .getType();
    }

    public boolean isString() {
        return this.getFieldType() == String.class;
    }
}
