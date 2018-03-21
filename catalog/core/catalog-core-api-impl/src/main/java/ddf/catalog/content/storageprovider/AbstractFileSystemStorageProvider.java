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
package ddf.catalog.content.storageprovider;

import ddf.catalog.Constants;
import ddf.catalog.content.StorageException;
import ddf.catalog.content.StorageProvider;
import ddf.catalog.content.data.ContentItem;
import ddf.catalog.content.data.impl.ContentItemImpl;
import ddf.catalog.content.data.impl.ContentItemValidator;
import ddf.catalog.content.operation.CreateStorageRequest;
import ddf.catalog.content.operation.CreateStorageResponse;
import ddf.catalog.content.operation.DeleteStorageRequest;
import ddf.catalog.content.operation.DeleteStorageResponse;
import ddf.catalog.content.operation.ReadStorageRequest;
import ddf.catalog.content.operation.ReadStorageResponse;
import ddf.catalog.content.operation.StorageRequest;
import ddf.catalog.content.operation.UpdateStorageRequest;
import ddf.catalog.content.operation.UpdateStorageResponse;
import ddf.catalog.content.operation.impl.CreateStorageResponseImpl;
import ddf.catalog.content.operation.impl.DeleteStorageResponseImpl;
import ddf.catalog.content.operation.impl.ReadStorageResponseImpl;
import ddf.catalog.content.operation.impl.UpdateStorageResponseImpl;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.mime.MimeTypeMapper;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javax.activation.MimeType;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** File system storage provider. */
public abstract class AbstractFileSystemStorageProvider implements StorageProvider {

  public static final String DEFAULT_CONTENT_REPOSITORY = "content";

  public static final String DEFAULT_CONTENT_STORE = "store";

  public static final String DEFAULT_TMP = "tmp";

  public static final String KARAF_HOME = "karaf.home";

  protected static final String DEFAULT_MIME_TYPE = "application/octet-stream";

  private static final Logger LOGGER =
      LoggerFactory.getLogger(AbstractFileSystemStorageProvider.class);

  public static final String REF_EXT = "external-reference";

  /** Mapper for file extensions-to-mime types (and vice versa) */
  protected MimeTypeMapper mimeTypeMapper;

  private Map<String, List<Metacard>> deletionMap = new ConcurrentHashMap<>();

  private Map<String, Set<String>> updateMap = new ConcurrentHashMap<>();

  /** Root directory for entire content repository */
  protected String baseContentDirectory;

  protected String baseContentTmpDirectory;

  /** Default constructor, invoked by blueprint. */
  public AbstractFileSystemStorageProvider() {
    LOGGER.debug("Generic File System Provider initializing...");
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
        String contentIdDir =
            getTempContentItemDir(createRequest.getId(), new URI(contentItem.getUri()));

        createdContentItems.add(
            generateContentFile(
                contentItem,
                contentIdDir,
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

  @Override
  public ReadStorageResponse read(ReadStorageRequest readRequest)
      throws StorageException, IOException {
    LOGGER.trace("ENTERING: read");

    if (readRequest.getResourceUri() == null) {
      return new ReadStorageResponseImpl(readRequest);
    }

    URI uri = readRequest.getResourceUri();
    ContentItem returnItem = readContent(uri);
    return new ReadStorageResponseImpl(readRequest, returnItem);
  }

  @Override
  public UpdateStorageResponse update(UpdateStorageRequest updateRequest) throws StorageException {
    LOGGER.trace("ENTERING: update");

    List<ContentItem> contentItems = updateRequest.getContentItems();

    List<ContentItem> updatedItems = new ArrayList<>(updateRequest.getContentItems().size());

    for (ContentItem contentItem : contentItems) {
      try {
        if (!ContentItemValidator.validate(contentItem)) {
          LOGGER.warn("Item is not valid: {}", contentItem);
          continue;
        }

        ContentItem updateItem = contentItem;
        if (StringUtils.isBlank(contentItem.getFilename())
            || StringUtils.equals(contentItem.getFilename(), ContentItem.DEFAULT_FILE_NAME)) {
          ContentItem existingItem = readContent(new URI(contentItem.getUri()));
          updateItem = new ContentItemDecorator(contentItem, existingItem);
        }

        String contentIdDir =
            getTempContentItemDir(updateRequest.getId(), new URI(updateItem.getUri()));

        updatedItems.add(
            generateContentFile(
                updateItem,
                contentIdDir,
                (String) updateRequest.getPropertyValue(Constants.STORE_REFERENCE_KEY)));
      } catch (IOException | URISyntaxException | IllegalArgumentException e) {
        throw new StorageException(e);
      }
    }

    for (ContentItem contentItem : updatedItems) {
      if (contentItem.getMetacard().getResourceURI() == null
          && StringUtils.isBlank(contentItem.getQualifier())) {
        contentItem
            .getMetacard()
            .setAttribute(new AttributeImpl(Metacard.RESOURCE_URI, contentItem.getUri()));
        try {
          contentItem
              .getMetacard()
              .setAttribute(new AttributeImpl(Metacard.RESOURCE_SIZE, contentItem.getSize()));
        } catch (IOException e) {
          LOGGER.info(
              "Could not set size of content item [{}] on metacard [{}]",
              contentItem.getId(),
              contentItem.getMetacard().getId(),
              e);
        }
      }
    }

    UpdateStorageResponse response = new UpdateStorageResponseImpl(updateRequest, updatedItems);
    updateMap.put(
        updateRequest.getId(),
        updatedItems.stream().map(ContentItem::getUri).collect(Collectors.toSet()));

    LOGGER.trace("EXITING: update");

    return response;
  }

  @Override
  public DeleteStorageResponse delete(DeleteStorageRequest deleteRequest) throws StorageException {
    LOGGER.trace("ENTERING: delete");

    List<Metacard> itemsToBeDeleted = new ArrayList<>();

    List<ContentItem> deletedContentItems = new ArrayList<>(deleteRequest.getMetacards().size());
    for (Metacard metacard : deleteRequest.getMetacards()) {
      LOGGER.debug("File to be deleted: {}", metacard.getId());

      ContentItem deletedContentItem =
          new ContentItemImpl(metacard.getId(), "", null, "", "", 0, metacard);

      if (!ContentItemValidator.validate(deletedContentItem)) {
        LOGGER.warn("Cannot delete invalid content item ({})", deletedContentItem);
        continue;
      }
      try {
        // For deletion we can ignore the qualifier and assume everything under a given ID is
        // to be removed.
        Path contentIdDir = getContentItemDir(new URI(deletedContentItem.getUri()));
        if (Files.exists(contentIdDir)) {
          List<Path> paths = new ArrayList<>();
          if (Files.isDirectory(contentIdDir)) {
            paths = listPaths(contentIdDir);
          } else {
            paths.add(contentIdDir);
          }

          for (Path path : paths) {
            if (Files.exists(path)) {
              deletedContentItems.add(deletedContentItem);
            }
          }
          itemsToBeDeleted.add(metacard);
        }
      } catch (IOException | URISyntaxException e) {
        throw new StorageException("Could not delete file: " + metacard.getId(), e);
      }
    }

    deletionMap.put(deleteRequest.getId(), itemsToBeDeleted);

    DeleteStorageResponse response =
        new DeleteStorageResponseImpl(deleteRequest, deletedContentItems);
    LOGGER.trace("EXITING: delete");

    return response;
  }

  @Override
  public void commit(StorageRequest request) throws StorageException {
    if (deletionMap.containsKey(request.getId())) {
      commitDeletes(request);
    } else if (updateMap.containsKey(request.getId())) {
      commitUpdates(request);
    } else {
      LOGGER.info("Nothing to commit for request: {}", request.getId());
    }
  }

  private void commitDeletes(StorageRequest request) throws StorageException {
    List<Metacard> itemsToBeDeleted = deletionMap.get(request.getId());
    try {
      for (Metacard metacard : itemsToBeDeleted) {
        LOGGER.debug("File to be deleted: {}", metacard.getId());

        String metacardId = metacard.getId();

        List<String> parts = getContentFilePathParts(metacardId, "");

        Path contentIdDir =
            Paths.get(baseContentDirectory.toString(), parts.toArray(new String[parts.size()]));

        if (!Files.exists(contentIdDir)) {
          throw new StorageException("File doesn't exist for id: " + metacard.getId());
        }

        try {
          FileUtils.deleteDirectory(contentIdDir.toFile());

          Path part1 = contentIdDir.getParent();
          if (Files.isDirectory(part1) && isDirectoryEmpty(part1)) {
            FileUtils.deleteDirectory(part1.toFile());
            Path part0 = part1.getParent();
            if (Files.isDirectory(part0) && isDirectoryEmpty(part0)) {
              FileUtils.deleteDirectory(part0.toFile());
            }
          }

        } catch (IOException e) {
          throw new StorageException("Could not delete file: " + metacard.getId(), e);
        }
      }
    } finally {
      rollback(request);
    }
  }

  private boolean isDirectoryEmpty(Path dir) throws IOException {
    try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(dir)) {
      return !dirStream.iterator().hasNext();
    } catch (IOException e) {
      LOGGER.debug("Unable to open directory stream for {}", dir.toString(), e);
      throw e;
    }
  }

  private void commitUpdates(StorageRequest request) throws StorageException {
    try {
      for (String contentUri : updateMap.get(request.getId())) {
        Path contentIdDir = Paths.get(getTempContentItemDir(request.getId(), new URI(contentUri)));
        Path target = getContentItemDir(new URI(contentUri));
        try {
          if (Files.exists(contentIdDir)) {
            if (Files.exists(target)) {
              List<Path> files = listPaths(target);
              for (Path file : files) {
                if (!Files.isDirectory(file)) {
                  Files.deleteIfExists(file);
                }
              }
            }
            Files.createDirectories(target.getParent());
            Files.move(contentIdDir, target, StandardCopyOption.REPLACE_EXISTING);
          }
        } catch (IOException e) {
          LOGGER.debug(
              "Unable to move files by simple rename, resorting to copy. This will impact performance.",
              e);
          try {
            Path createdTarget = Files.createDirectories(target);
            List<Path> files = listPaths(contentIdDir);
            Files.copy(
                files.get(0),
                Paths.get(
                    createdTarget.toAbsolutePath().toString(),
                    files.get(0).getFileName().toString()));
          } catch (IOException e1) {
            throw new StorageException(
                "Unable to commit changes for request: " + request.getId(), e1);
          }
        }
      }
    } catch (URISyntaxException e) {
      throw new StorageException(e);
    } finally {
      rollback(request);
    }
  }

  @Override
  public void rollback(StorageRequest request) throws StorageException {
    String id = request.getId();
    Path requestIdDir =
        Paths.get(Paths.get(baseContentTmpDirectory).toAbsolutePath().toString(), id);
    deletionMap.remove(id);
    updateMap.remove(id);
    try {
      FileUtils.deleteDirectory(requestIdDir.toFile());
    } catch (IOException e) {
      throw new StorageException(
          "Unable to remove temporary content storage for request: " + id, e);
    }
  }

  protected List<Path> listPaths(Path dir) throws IOException {
    List<Path> result = new ArrayList<>();
    if (Files.exists(dir)) {
      try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
        for (Path entry : stream) {
          result.add(entry);
        }
      } catch (DirectoryIteratorException ex) {
        // I/O error encounted during the iteration, the cause is an IOException
        throw ex.getCause();
      }
    }
    return result;
  }

  protected abstract ContentItem readContent(URI uri) throws StorageException, IOException;

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

  protected abstract String getTempContentItemDir(String requestId, URI contentUri);

  protected abstract ContentItem generateContentFile(
      ContentItem item, String contentItemDirectory, String storeReference)
      throws IOException, StorageException;

  public abstract void setBaseContentDirectory(final String baseDirectory)
      throws IOException, StorageException;

  public MimeTypeMapper getMimeTypeMapper() {
    return mimeTypeMapper;
  }

  public void setMimeTypeMapper(MimeTypeMapper mimeTypeMapper) {
    this.mimeTypeMapper = mimeTypeMapper;
  }

  private static class ContentItemDecorator implements ContentItem {

    private final ContentItem updateContentItem;

    private final ContentItem existingItem;

    public ContentItemDecorator(ContentItem contentItem, ContentItem existingItem) {
      this.updateContentItem = contentItem;
      this.existingItem = existingItem;
    }

    @Override
    public String getId() {
      return updateContentItem.getId();
    }

    @Override
    public String getUri() {
      return updateContentItem.getUri();
    }

    @Override
    public String getQualifier() {
      return updateContentItem.getQualifier();
    }

    @Override
    public String getFilename() {
      return existingItem.getFilename();
    }

    @Override
    public MimeType getMimeType() {
      return existingItem.getMimeType();
    }

    @Override
    public String getMimeTypeRawData() {
      return existingItem.getMimeTypeRawData();
    }

    @Override
    public InputStream getInputStream() throws IOException {
      return updateContentItem.getInputStream();
    }

    @Override
    public long getSize() throws IOException {
      return updateContentItem.getSize();
    }

    @Override
    public Metacard getMetacard() {
      return updateContentItem.getMetacard();
    }
  }
}
