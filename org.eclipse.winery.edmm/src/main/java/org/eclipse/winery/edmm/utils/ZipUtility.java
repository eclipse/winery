/*******************************************************************************
 * Copyright (c) 2021 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache Software License 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *******************************************************************************/
package org.eclipse.winery.edmm.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import io.github.edmm.utils.Compress;

public class ZipUtility {

    public static void pack(Path sourcePath, Path destPath) {
        Compress.zip(sourcePath, destPath);
    }

    /**
     * @param zipFilePath the Path of the zip file to unpack
     * @param destDirPath the Path of the directory where we want the unzipped files, if the direcory exists it will be
     *                    overwritten
     * @return it returns the destination file path
     */
    public static Path unpack(Path zipFilePath, Path destDirPath) throws IOException {
        if (Files.exists(destDirPath)) {
            Files.delete(destDirPath);
        }
        Files.createDirectory(destDirPath);

        try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(zipFilePath.toFile().toPath()))) {
            ZipEntry zipEntry = zipInputStream.getNextEntry();
            while (zipEntry != null) {
                Path newFile = destDirPath.resolve(zipEntry.getName());
                if (!newFile.normalize().startsWith(destDirPath.normalize())) {
                    throw new IOException("Bad zip entry");
                }
                Files.createDirectories(newFile.getParent());
                Files.copy(zipInputStream, newFile);
                zipEntry = zipInputStream.getNextEntry();
            }
            zipInputStream.closeEntry();
        }

        return destDirPath;
    }
}
