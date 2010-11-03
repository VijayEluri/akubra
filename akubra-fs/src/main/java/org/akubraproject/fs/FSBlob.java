/* $HeadURL$
 * $Id$
 *
 * Copyright (c) 2009-2010 DuraSpace
 * http://duraspace.org
 *
 * In collaboration with Topaz Inc.
 * http://www.topazproject.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.akubraproject.fs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.net.URI;
import java.net.URISyntaxException;

import java.nio.channels.FileChannel;

import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.akubraproject.Blob;
import org.akubraproject.DuplicateBlobException;
import org.akubraproject.MissingBlobException;
import org.akubraproject.UnsupportedIdException;
import org.akubraproject.impl.AbstractBlob;
import org.akubraproject.impl.StreamManager;

/**
 * Filesystem-backed Blob implementation.
 *
 * <p>A note on syncing: in order for a newly created, deleted, or moved file to be properly
 * sync'd the directory has to be fsync'd too; however, Java does not provide a way to do this.
 * Hence it is possible to loose a complete file despite having sync'd.
 *
 * @author Chris Wilper
 */
class FSBlob extends AbstractBlob {
  private static final Logger log = LoggerFactory.getLogger(FSBlob.class);

  static final String scheme = "file";
  private final URI canonicalId;
  private final File file;
  private final StreamManager manager;
  private final Set<File>     modified;

  /**
   * Create a file based blob
   *
   * @param connection the blob store connection
   * @param baseDir the baseDir of the store
   * @param blobId the identifier for the blob
   * @param manager the stream manager
   * @param modified the set of modified files in the connection; may be null
   */
  FSBlob(FSBlobStoreConnection connection, File baseDir, URI blobId, StreamManager manager,
         Set<File> modified) throws UnsupportedIdException {
    super(connection, blobId);
    this.canonicalId = validateId(blobId);
    this.file = new File(baseDir, canonicalId.getRawSchemeSpecificPart());
    this.manager = manager;
    this.modified = modified;
  }

  @Override
  public URI getCanonicalId() {
    return canonicalId;
  }

  @Override
  public InputStream openInputStream() throws IOException {
    ensureOpen();

    if (!file.exists())
      throw new MissingBlobException(getId());

    return manager.manageInputStream(getConnection(), new FileInputStream(file));
  }

  @Override
  public OutputStream openOutputStream(long estimatedSize, boolean overwrite) throws IOException {
    ensureOpen();

    if (!overwrite && file.exists())
      throw new DuplicateBlobException(getId());

    makeParentDirs(file);

    if (modified != null)
      modified.add(file);

    return manager.manageOutputStream(getConnection(), new FileOutputStream(file));
  }

  @Override
  public long getSize() throws IOException {
    ensureOpen();

    if (!file.exists())
      throw new MissingBlobException(getId());

    return file.length();
  }

  @Override
  public boolean exists() throws IOException {
    ensureOpen();

    return file.exists();
  }

  @Override
  public void delete() throws IOException {
    ensureOpen();

    if (!file.delete() && file.exists())
      throw new IOException("Failed to delete file: " + file);

    if (modified != null)
      modified.remove(file);
  }

  @Override
  public Blob moveTo(URI blobId, Map<String, String> hints) throws IOException {
    ensureOpen();
    FSBlob dest = (FSBlob) getConnection().getBlob(blobId, hints);

    File other = dest.file;

    if (other.exists())
      throw new DuplicateBlobException(blobId);

    makeParentDirs(other);

    if (!file.renameTo(other)) {
        if (!file.exists())
            throw new MissingBlobException(getId());

        boolean success = false;
        try {
            nioCopy(file, other);

            if (file.length() != other.length()) {
                throw new IOException("Source and destination file sizes do not match: source '" + file + "' is " + file.length() + " and destination '" + other + "' is " + other.length());
            }

            if (!file.delete() && file.exists())
                throw new IOException("Failed to delete file: " + file);

            success = true;
        }  finally {
            if (!success) {
                if (other.exists() && !other.delete())
                    log.warn("Error deleting destination file '" +  other + "' after source file '" + file + "' copy failure");
            }
        }
    }

    if (modified != null && modified.remove(file))
      modified.add(other);

    return dest;
  }

  static URI validateId(URI blobId) throws UnsupportedIdException {
    if (blobId == null)
      throw new NullPointerException("Id cannot be null");
    if (!blobId.getScheme().equalsIgnoreCase(scheme))
      throw new UnsupportedIdException(blobId, "Id must be in " + scheme + " scheme");
    String path = blobId.getRawSchemeSpecificPart();
    if (path.startsWith("/"))
      throw new UnsupportedIdException(blobId, "Id must specify a relative path");
    try {
      // insert a '/' so java.net.URI normalization works
      URI tmp = new URI(scheme + ":/" + path);
      String nPath = tmp.normalize().getRawSchemeSpecificPart().substring(1);
      if (nPath.equals("..") || nPath.startsWith("../"))
        throw new UnsupportedIdException(blobId, "Id cannot be outside top-level directory");
      if (nPath.endsWith("/"))
        throw new UnsupportedIdException(blobId, "Id cannot specify a directory");
      return new URI(scheme + ":" + nPath);
    } catch (URISyntaxException wontHappen) {
      throw new Error(wontHappen);
    }
  }

  private static void makeParentDirs(File file) throws IOException {
    File parent = file.getParentFile();

    if (parent != null && !parent.exists() && !parent.mkdirs())
      throw new IOException("Unable to create parent directory: " + parent.getPath());
  }

  private static void nioCopy(File source, File dest) throws IOException {
      FileChannel in = (new FileInputStream(source)).getChannel();
      FileChannel out = (new FileOutputStream(dest)).getChannel();
      in.transferTo(0, source.length(), out);
      in.close();
      out.close();

      if (!dest.exists())
          throw new IOException("Failed to copy file to new location: " + dest);
    }
}