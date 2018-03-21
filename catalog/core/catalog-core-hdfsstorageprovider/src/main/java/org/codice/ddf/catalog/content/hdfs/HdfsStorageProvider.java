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
package org.codice.ddf.catalog.content.hdfs;

import ddf.catalog.content.StorageException;
import ddf.catalog.content.data.ContentItem;
import ddf.catalog.content.data.impl.ContentItemImpl;
import ddf.catalog.content.operation.DeleteStorageRequest;
import ddf.catalog.content.operation.DeleteStorageResponse;
import ddf.catalog.content.operation.StorageRequest;
import ddf.catalog.content.operation.UpdateStorageRequest;
import ddf.catalog.content.operation.UpdateStorageResponse;
import ddf.catalog.content.storageprovider.AbstractFileSystemStorageProvider;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.codice.ddf.configuration.PropertyResolver;

public class HdfsStorageProvider extends AbstractFileSystemStorageProvider {
  private String hdfsRootDirectory;

  private FileSystem fileSystem;

  @Override
  public UpdateStorageResponse update(UpdateStorageRequest updateStorageRequest)
      throws StorageException {
    return null;
  }

  @Override
  public DeleteStorageResponse delete(DeleteStorageRequest deleteStorageRequest)
      throws StorageException {
    /*Configuration conf = new Configuration();
    conf.set("fileSystem.defaultFS", "hdfs://localhost:9000");

    FileSystem fileSystem = FileSystem.newInstance(conf);

    boolean didDelete = fileSystem.delete(new Path(
            "/User/armandroelens/test/testFile.txt"));
    System.out.println(didDelete);
    fileSystem.close();*/
    return null;
  }

  @Override
  public void commit(StorageRequest storageRequest) throws StorageException {}

  @Override
  public void rollback(StorageRequest storageRequest) throws StorageException {}

  @Override
  protected ContentItem generateContentFile(
      ContentItem contentItem, String contentDirectory, String storeReference) throws IOException {
    Path contentPath = new Path(contentDirectory);
    if (!fileSystem.exists(contentPath)) {
      fileSystem.mkdirs(contentPath);
    }
    Path contentItemFilePath = new Path(contentDirectory + "/" + contentItem.getFilename());
    try (FSDataOutputStream fsDataOutputStream = fileSystem.create(contentItemFilePath)) {
      fsDataOutputStream.write(IOUtils.toByteArray(contentItem.getInputStream()));
    }
    FSDataInputStream fsDataInputStream = fileSystem.open(contentItemFilePath);
    InputStream copyInputStream = new ByteArrayInputStream(IOUtils.toByteArray(fsDataInputStream));

    return new ContentItemImpl(
        contentItem.getId(),
        contentItem.getQualifier(),
        new com.google.common.io.ByteSource() {
          @Override
          public InputStream openStream() {
            return copyInputStream;
          }
        },
        contentItem.getMimeType().toString(),
        contentItem.getFilename(),
        contentItem.getSize(),
        contentItem.getMetacard());
  }

  @Override
  protected ContentItem readContent(URI uri) throws IOException, StorageException {
    Path file = new Path(uri);
    if (file == null || !fileSystem.exists(file)) {
      throw new StorageException(
          "Unable to find file for content ID: " + uri.getSchemeSpecificPart());
    }

    FSDataInputStream fsDataInputStream = fileSystem.open(file);
    System.out.println(IOUtils.toString(fsDataInputStream, "UTF-8"));
    fsDataInputStream.close();
    fileSystem.close();
    return null;
  }

  protected String getTempContentItemDir(String requestId, URI contentUri) {
    List<String> pathParts = new ArrayList<>();
    pathParts.add(requestId);
    pathParts.addAll(
        getContentFilePathParts(contentUri.getSchemeSpecificPart(), contentUri.getFragment()));
    return hdfsRootDirectory
        + baseContentTmpDirectory.toString()
        + StringUtils.join(pathParts, "/");
  }

  @Override
  public void setBaseContentDirectory(final String baseContentDirectory) throws StorageException {
    if (StringUtils.isNotBlank(baseContentDirectory)) {
      Path baseContentPath = new Path(baseContentDirectory);
      try {
        if (!fileSystem.isDirectory(baseContentPath)) {
          fileSystem.mkdirs(baseContentPath);
        }
        this.baseContentDirectory = baseContentDirectory;
        this.baseContentTmpDirectory = baseContentDirectory;
      } catch (IOException e) {
        throw new StorageException(
            "There was problem processing the currently configured \"Base Content Directory\"", e);
      }
    }
  }

  public void setHdfs(String hdfsRootDirectory) throws StorageException {
    if (StringUtils.isNotBlank(hdfsRootDirectory)) {
      ClassLoader threadLoader = null;
      try {
        threadLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(DistributedFileSystem.class.getClassLoader());
        this.hdfsRootDirectory = new PropertyResolver(hdfsRootDirectory).toString();
        fileSystem = new DistributedFileSystem();
        fileSystem.initialize(new URI(this.hdfsRootDirectory), new Configuration());
      } catch (Exception e) {
        throw new StorageException(
            "Couldn't not create the HDFS filesystem for the storage provider with the currently configured \"HDFS Root Directory\".",
            e);
      } finally {
        if (threadLoader != null) {
          Thread.currentThread().setContextClassLoader(threadLoader);
        }
      }
    } else {
      throw new StorageException(
          "The HDFS root directory is empty. Couldn't not create the HDFS filesystem for the storage provider.");
    }
  }
}
