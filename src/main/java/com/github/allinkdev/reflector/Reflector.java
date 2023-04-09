package com.github.allinkdev.reflector;

import com.github.allinkdev.reflector.field.Field;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLDecoder;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.*;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

public final class Reflector<O> {
    private final Class<? extends O> objectClass;
    private O object = null;

    Reflector(final O object, final Class<? extends O> objectClass) {
        this.object = object;
        this.objectClass = objectClass;
    }

    Reflector(final Class<? extends O> objectClass) {
        this.objectClass = objectClass;
    }

    public static Optional<Reflector<?>> createNew(final String className, final ClassLoader classLoader) {
        final Class<?> referencedClass;

        try {
            referencedClass = Class.forName(className, true, classLoader);
        } catch (ClassNotFoundException e) {
            return Optional.empty();
        }

        return Optional.of(new Reflector<>(referencedClass));
    }


    public static Optional<Reflector<?>> createNew(final String className, final Object instance, final ClassLoader classLoader) {
        final Class<?> referencedClass;

        try {
            referencedClass = classLoader.loadClass(className);
        } catch (ClassNotFoundException e) {
            return Optional.empty();
        }

        return Optional.of(new Reflector<>(instance, referencedClass));
    }

    public static <O> Reflector<O> createNew(final Class<? extends O> objectClass) {
        return new Reflector<>(objectClass);
    }

    public static <O> Reflector<O> createNew(final O object, final Class<? extends O> objectClass) {
        return new Reflector<>(object, objectClass);
    }

    private static Field[] getFieldsRecursive(final Object instance, final Class<?> currentClass) {
        final java.lang.reflect.Field[] myFieldHandles = currentClass.getDeclaredFields();
        final Class<?> superClass = currentClass.getSuperclass();

        final Field[] superClassFields = Objects.equals(superClass, currentClass) || superClass == null ? new Field[0] : getFieldsRecursive(instance, superClass);
        final int combinedLength = myFieldHandles.length + superClassFields.length;
        final Field[] fieldArray = new Field[combinedLength];
        final Field[] myFields = Arrays.stream(myFieldHandles)
                .map(h -> Field.from(instance, h))
                .toArray(Field[]::new);

        System.arraycopy(myFields, 0, fieldArray, 0, myFields.length);
        System.arraycopy(superClassFields, 0, fieldArray, myFields.length, superClassFields.length);

        return fieldArray;
    }

    private static List<String> getAllClassPaths(final Class<?> caller) {
        final ProtectionDomain protectionDomain = caller.getProtectionDomain();
        final CodeSource codeSource = protectionDomain.getCodeSource();
        final URL codeSourceLocation = codeSource.getLocation();
        final String codePath;

        try {
            codePath = URLDecoder.decode(codeSourceLocation.getPath(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return Collections.emptyList();
        }

        if (codePath.endsWith(".jar")) {
            final JarFile jarFile;

            try {
                jarFile = new JarFile(codePath);
            } catch (IOException e) {
                e.printStackTrace();
                return Collections.emptyList();
            }

            final List<String> mappedList = Util.enumerationToList(jarFile.entries())
                    .stream()
                    .map(ZipEntry::getName)
                    .map(s -> s.replace(System.lineSeparator(), "/"))
                    .filter(f -> f.endsWith(".class"))
                    .collect(Collectors.toList());

            try {
                jarFile.close();
            } catch (IOException e) {
                return Collections.emptyList();
            }

            return mappedList;
        } else {
            final File directory = new File(codePath);
            final List<File> files = Util.listFilesRecursively(directory, f -> f.getName().endsWith(".class"));

            return files.stream()
                    .map(File::getPath)
                    .map(s -> s.replace(System.lineSeparator(), "/"))
                    .map(f -> f.replace(codePath, ""))
                    .collect(Collectors.toList());
        }
    }

    private static Stream<Class<?>> allClassesInPackage(final String packageName, final ClassLoader classLoader, final Class<?> caller) {
        final String packagePath = packageName.replace(".", "/");
        final List<String> classPaths = getAllClassPaths(caller);
        final List<Class<?>> classes = new ArrayList<>();

        for (final String classPath : classPaths) {
            if (!classPath.startsWith(packagePath)) {
                continue;
            }

            final String className = classPath
                    .replace(".class", "")
                    .replace("/", ".");

            final Class<?> fileClass;

            try {
                fileClass = Class.forName(className);
            } catch (ClassNotFoundException e) {
                continue;
            }

            classes.add(fileClass);
        }

        return classes.stream();
    }

    public Stream<Field> staticFields() {
        return this.allFields()
                .filter(Field::isStatic);
    }

    public Stream<Field> objectFields() {
        return this.allFields()
                .filter(f -> !f.isStatic());
    }

    public Stream<Field> allFields() {
        final Field[] fields = getFieldsRecursive(this.object, this.objectClass);

        return Arrays.stream(fields);
    }

    public Stream<Field> fieldsWithName(final String name) {
        final Stream<Field> fields = this.allFields();

        return fields.filter(f -> f.name().equals(name));
    }

    public String packageName() {
        return this.objectClass.getPackage()
                .getName();
    }

    public ClassLoader getClassLoader() {
        return objectClass.getClassLoader();
    }

    public Stream<Class<?>> allClassesInCurrentPackage() {
        return allClassesInPackage(this.packageName(), this.getClassLoader(), objectClass);
    }

    public Stream<Class<? extends O>> allSubClassesInCurrentPackage() {
        return this.allClassesInCurrentPackage()
                .filter(objectClass::isAssignableFrom)
                .map(c -> c.asSubclass(objectClass));
    }

    public Stream<Class<? extends O>> allSubClassesInPackage(final String packageName) {
        return allClassesInPackage(packageName, this.getClassLoader(), objectClass)
                .filter(objectClass::isAssignableFrom)
                .map(c -> c.asSubclass(objectClass));
    }

    public Stream<Class<? extends O>> allSubClassesInSubPackage(final String subPackageName) {
        final String packageName = this.packageName();
        final StringBuilder fullSubPackageNameBuilder = new StringBuilder(packageName);

        if (!subPackageName.startsWith(".")) {
            fullSubPackageNameBuilder.append('.');
        }

        fullSubPackageNameBuilder.append(subPackageName);

        final String fullSubPackageName = fullSubPackageNameBuilder.toString();

        return allSubClassesInPackage(fullSubPackageName);
    }

    public Optional<Method> method(final String name, final Class<?>... params) {
        try {
            return Optional.of(objectClass.getDeclaredMethod(name, params));
        } catch (NoSuchMethodException e) {
            return Optional.empty();
        }
    }

    public Stream<Method> methods() {
        return Arrays.stream(objectClass.getDeclaredMethods());
    }

    public Optional<Method> mainMethod() {
        return this.method("main", String[].class);
    }

    public void invokeMain(final String[] args) throws InvocationTargetException, IllegalAccessException {
        final Optional<Method> mainMethodOptional = this.mainMethod();

        if (!mainMethodOptional.isPresent()) {
            return;
        }

        final Method mainMethod = mainMethodOptional.get();
        mainMethod.invoke(null, new Object[]{args});
    }

    public Optional<Constructor<? extends O>> constructor(final Class<?>... types) {
        try {
            return Optional.of(objectClass.getDeclaredConstructor(types));
        } catch (NoSuchMethodException e) {
            return Optional.empty();
        }
    }

    public Optional<O> create(final Object... args) {
        final Class<?>[] argumentTypes = Arrays.stream(args)
                .map(Object::getClass)
                .toArray(Class<?>[]::new);
        final Optional<Constructor<? extends O>> constructorOptional = this.constructor(argumentTypes);

        if (!constructorOptional.isPresent()) {
            return Optional.empty();
        }

        final Constructor<? extends O> constructor = constructorOptional.get();

        try {
            return Optional.of(constructor.newInstance(args));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
