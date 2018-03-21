/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.catalog.content.impl;

import com.google.common.io.ByteSource;
import ddf.catalog.content.StorageException;
import ddf.catalog.content.data.ContentItem;
import ddf.catalog.content.data.impl.ContentItemImpl;
import ddf.catalog.content.storageprovider.AbstractFileSystemStorageProvider;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** File system storage provider. */
public class FileSystemStorageProvider extends AbstractFileSystemStorageProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(FileSystemStorageProvider.class);

  /** Default constructor, invoked by blueprint. */
  public FileSystemStorageProvider() {
    LOGGER.debug("File System Provider initializing...");
  }

  @Override
  protected String getTempContentItemDir(String requestId, URI contentUri) {
    List<String> pathParts = new ArrayList<>();
    pathParts.add(requestId);
    pathParts.addAll(
        getContentFilePathParts(contentUri.getSchemeSpecificPart(), contentUri.getFragment()));

    return Paths.get(
            Paths.get(baseContentTmpDirectory).toAbsolutePath().toString(),
            pathParts.toArray(new String[pathParts.size()]))
        .toString();
  }

  @Override
  protected ContentItem generateContentFile(
      ContentItem item, String contentItemDirectory, String storeReference) throws IOException {
    LOGGER.trace("ENTERING: generateContentFile");
    Path contentDirectory = Paths.get(contentItemDirectory);
    if (!Files.exists(contentDirectory)) {
      contentDirectory = Files.createDirectories(contentDirectory);
    }

    Path contentItemPath =
        Paths.get(contentDirectory.toAbsolutePath().toString(), item.getFilename());
    ByteSource byteSource;

    long copy;

    if (storeReference != null) {
      copy = item.getSize();
      Files.write(
          Paths.get(contentItemPath.toString() + "." + REF_EXT),
          storeReference.getBytes(Charset.forName("UTF-8")));
      byteSource =
          new ByteSource() {
            @Override
            public InputStream openStream() throws IOException {
              return new URL(storeReference).openStream();
            }
          };
    } else {
      try (InputStream inputStream = item.getInputStream()) {
        copy = Files.copy(inputStream, contentItemPath);
      }
      byteSource = com.google.common.io.Files.asByteSource(contentItemPath.toFile());

      if (copy != item.getSize()) {
        LOGGER.warn(
            "Created content item {} size {} does not match expected size {}"
                + System.lineSeparator()
                + "Verify filesystem and/or network integrity.",
            item.getId(),
            copy,
            item.getSize());
      }
    }

    ContentItemImpl contentItem =
        new ContentItemImpl(
            item.getId(),
            item.getQualifier(),
            byteSource,
            item.getMimeType().toString(),
            contentItemPath.getFileName().toString(),
            copy,
            item.getMetacard());

    LOGGER.trace("EXITING: generateContentFile");

    return contentItem;
  }

  @Override
  protected ContentItem readContent(URI uri) throws StorageException {
    Path file = getContentFilePath(uri);

    if (file == null) {
      throw new StorageException(
          "Unable to find file for content ID: " + uri.getSchemeSpecificPart());
    }

    String filename = file.getFileName().toString();
    String extension = FilenameUtils.getExtension(filename);
    URI reference = null;
    // if the file is an external reference, remove the reference extension
    // so we can get the real extension
    if (REF_EXT.equals(extension)) {
      extension = FilenameUtils.getExtension(FilenameUtils.removeExtension(filename));
      try {
        reference = new URI(new String(Files.readAllBytes(file), Charset.forName("UTF-8")));

        if (reference.getScheme() == null) {
          file = Paths.get(reference.toASCIIString());
        } else if (reference.getScheme().equalsIgnoreCase("file")) {
          file = Paths.get(reference);
        } else {
          file = null;
        }
      } catch (IOException | URISyntaxException e) {
        throw new StorageException(e);
      }
    }

    if (reference != null && file != null && !file.toFile().exists()) {
      throw new StorageException("Cannot read " + uri + ".");
    }

    String mimeType = DEFAULT_MIME_TYPE;
    long size = 0;
    ByteSource byteSource;

    try (InputStream fileInputStream =
        file != null ? Files.newInputStream(file) : reference.toURL().openStream()) {
      mimeType = mimeTypeMapper.guessMimeType(fileInputStream, extension);
    } catch (Exception e) {
      LOGGER.info(
          "Could not determine mime type for file extension = {}; defaulting to {}",
          extension,
          DEFAULT_MIME_TYPE);
    }

    if (file != null) {
      if (mimeType == null || DEFAULT_MIME_TYPE.equals(mimeType)) {
        try {
          mimeType = Files.probeContentType(file);
        } catch (IOException e) {
          LOGGER.info("Unable to determine mime type using Java Files service.", e);
        }
      }

      LOGGER.debug("mimeType = {}", mimeType);
      try {
        size = Files.size(file);
      } catch (IOException e) {
        LOGGER.info("Unable to retrieve size of file: {}", file.toAbsolutePath().toString(), e);
      }
      byteSource = com.google.common.io.Files.asByteSource(file.toFile());
    } else {
      URI finalReference = reference;
      byteSource =
          new ByteSource() {
            @Override
            public InputStream openStream() throws IOException {
              return finalReference.toURL().openStream();
            }
          };
    }

    return new ContentItemImpl(
        uri.getSchemeSpecificPart(), uri.getFragment(), byteSource, mimeType, filename, size, null);
  }

  @Override
  @SuppressFBWarnings
  public void setBaseContentDirectory(final String baseDirectory) throws IOException {
    Path directory;
    if (!baseDirectory.isEmpty()) {
      String path = tryCanonicalizeDirectory(baseDirectory);
      try {
        directory = Paths.get(path, DEFAULT_CONTENT_REPOSITORY, DEFAULT_CONTENT_STORE);
      } catch (InvalidPathException e) {
        path = System.getProperty(KARAF_HOME);
        directory = Paths.get(path, DEFAULT_CONTENT_REPOSITORY, DEFAULT_CONTENT_STORE);
      }
    } else {
      String path = System.getProperty("karaf.home");
      directory = Paths.get(path, DEFAULT_CONTENT_REPOSITORY, DEFAULT_CONTENT_STORE);
    }

    Path directories;
    if (!Files.exists(directory)) {
      directories = Files.createDirectories(directory);
      LOGGER.debug(
          "Setting base content directory to: {}", directories.toAbsolutePath().toString());
    } else {
      directories = directory;
    }

    Path tmpDirectories;
    Path tmpDirectory = Paths.get(directories.toAbsolutePath().toString(), DEFAULT_TMP);
    if (!Files.exists(tmpDirectory)) {
      tmpDirectories = Files.createDirectories(tmpDirectory);
      LOGGER.debug(
          "Setting base content directory to: {}", tmpDirectory.toAbsolutePath().toString());
    } else {
      tmpDirectories = tmpDirectory;
    }

    this.baseContentDirectory = directories.toString();
    this.baseContentTmpDirectory = tmpDirectories.toString();
  }

  private String tryCanonicalizeDirectory(String directory) {
    String normalized = FilenameUtils.normalize(directory);
    try {
      return Paths.get(normalized).toFile().getCanonicalPath();
    } catch (IOException e) {
      LOGGER.debug("Could not get canonical path for ({})", directory, e);
    }
    return normalized;
  }

  private Path getContentFilePath(URI uri) throws StorageException {
    Path contentIdDir = getContentItemDir(uri);
    List<Path> contentFiles;
    if (contentIdDir != null && Files.exists(contentIdDir)) {
      try {
        contentFiles = listPaths(contentIdDir);
      } catch (IOException e) {
        throw new StorageException(e);
      }

      contentFiles.removeIf(Files::isDirectory);

      if (contentFiles.size() != 1) {
        throw new StorageException(
            "Content ID: " + uri.getSchemeSpecificPart() + " storage folder is corrupted.");
      }

      // there should only be one file
      return contentFiles.get(0);
    }
    return null;
  }
}
