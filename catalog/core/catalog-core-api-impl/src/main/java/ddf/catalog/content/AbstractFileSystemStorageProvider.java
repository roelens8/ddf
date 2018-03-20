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
package ddf.catalog.content;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.Constants;
import ddf.catalog.content.data.ContentItem;
import ddf.catalog.content.data.impl.ContentItemValidator;
import ddf.catalog.content.operation.CreateStorageRequest;
import ddf.catalog.content.operation.CreateStorageResponse;
import ddf.catalog.content.operation.impl.CreateStorageResponseImpl;
import ddf.catalog.data.Metacard;
import ddf.mime.MimeTypeMapper;

public abstract class AbstractFileSystemStorageProvider implements StorageProvider {

  public static final String DEFAULT_CONTENT_REPOSITORY = "content";

  public static final String DEFAULT_CONTENT_STORE = "store";

  public static final String DEFAULT_TMP = "tmp";

  public static final String KARAF_HOME = "karaf.home";

  private static final String DEFAULT_MIME_TYPE = "application/octet-stream";

  private static final Logger LOGGER =
      LoggerFactory.getLogger(AbstractFileSystemStorageProvider.class);

  public static final String REF_EXT = "external-reference";

  /** Mapper for file extensions-to-mime types (and vice versa) */
  private MimeTypeMapper mimeTypeMapper;

  /** Root directory for entire content repository */
  private Path baseContentDirectory;

  private Path baseContentTmpDirectory;

  private Map<String, List<Metacard>> deletionMap = new ConcurrentHashMap<>();

  private Map<String, Set<String>> updateMap = new ConcurrentHashMap<>();

  /** Default constructor, invoked by blueprint. */
  public AbstractFileSystemStorageProvider() {
    LOGGER.debug("Abstract File System Provider initializing...");
  }

  @Override
  public CreateStorageResponse create(CreateStorageRequest createRequest) throws StorageException {
    LOGGER.trace("ENTERING: create");

    List<ContentItem> contentItems = createRequest.getContentItems();

    List<ContentItem> createdContentItems = new ArrayList<>(createRequest.getContentItems().size());

    for (ContentItem contentItem : contentItems) {
      try {
        if (!ContentItemValidator.validate(contentItem)) {
          LOGGER.warn("Item is not valid: {}", contentItem);
          continue;
        }
        Path contentIdDir =
            getTempContentItemDir(createRequest.getId(), new URI(contentItem.getUri()));

        Path contentDirectory = Files.createDirectories(contentIdDir);

        createdContentItems.add(
            generateContentFile(
                contentItem,
                contentDirectory,
                (String) createRequest.getPropertyValue(Constants.STORE_REFERENCE_KEY)));
      } catch (IOException | URISyntaxException | IllegalArgumentException e) {
        throw new StorageException(e);
      }
    }

    CreateStorageResponse response =
        new CreateStorageResponseImpl(createRequest, createdContentItems);
    updateMap.put(
        createRequest.getId(),
        createdContentItems.stream().map(ContentItem::getUri).collect(Collectors.toSet()));

    LOGGER.trace("EXITING: create");

    return response;
  }

  protected Path getTempContentItemDir(String requestId, URI contentUri) {
    List<String> pathParts = new ArrayList<>();
    pathParts.add(requestId);
    pathParts.addAll(
        getContentFilePathParts(contentUri.getSchemeSpecificPart(), contentUri.getFragment()));

    return Paths.get(
        baseContentTmpDirectory.toAbsolutePath().toString(),
        pathParts.toArray(new String[pathParts.size()]));
  }

  protected Path getContentItemDir(URI contentUri) {
    List<String> pathParts =
        getContentFilePathParts(contentUri.getSchemeSpecificPart(), contentUri.getFragment());
    try {
      return Paths.get(
          baseContentDirectory.toString(), pathParts.toArray(new String[pathParts.size()]));
    } catch (InvalidPathException e) {
      LOGGER.debug(
          "Invalid path: [{}/{}]",
          baseContentDirectory.toString(),
          pathParts.stream().collect(Collectors.joining()));
      return null;
    }
  }

  // separating into 2 directories of 3 characters each allows us to
  // get to 361,000,000,000 records before we would run up against the
  // NTFS file system limits for a single directory
  public List<String> getContentFilePathParts(String id, String qualifier) {
    String partsId = id;
    if (id.length() < 6) {
      partsId = StringUtils.rightPad(id, 6, "0");
    }
    List<String> parts = new ArrayList<>();
    parts.add(partsId.substring(0, 3));
    parts.add(partsId.substring(3, 6));
    parts.add(partsId);
    if (StringUtils.isNotBlank(qualifier)) {
      parts.add(qualifier);
    }
    return parts;
  }

  protected abstract ContentItem generateContentFile(
      ContentItem item, Path contentDirectory, String storeReference)
      throws IOException, StorageException;
}
