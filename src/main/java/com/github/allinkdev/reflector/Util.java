package com.github.allinkdev.reflector;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;

final class Util {
    static <E> List<E> enumerationToList(final Enumeration<E> enumeration) {
        final List<E> list = new ArrayList<>();

        while (enumeration.hasMoreElements()) {
            list.add(enumeration.nextElement());
        }

        return Collections.unmodifiableList(list);
    }

    static List<File> listFilesRecursively(final File directory, final FileFilter fileFilter) {
        final List<File> fileList = new ArrayList<>();
        final File[] files = directory.listFiles();

        if (files == null) {
            return Collections.unmodifiableList(fileList);
        }

        for (final File subFile : files) {
            if (subFile.isFile()) {
                fileList.add(subFile);
                continue;
            }

            fileList.addAll(listFilesRecursively(subFile, fileFilter));
        }

        return fileList.stream()
                .filter(fileFilter::accept)
                .collect(Collectors.toList());
    }
}
