/* $HeadURL$
 * $Id$
 *
 * Copyright (c) 2008,2009 by Fedora Commons Inc.
 * http://www.fedoracommons.org
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

package org.fedoracommons.akubra.mem;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.iterators.FilterIterator;

import org.fedoracommons.akubra.AbstractBlobStoreConnection;
import org.fedoracommons.akubra.Blob;
import org.fedoracommons.akubra.BlobStore;
import org.fedoracommons.akubra.BlobWrapper;
import org.fedoracommons.akubra.DuplicateBlobException;
import org.fedoracommons.akubra.util.StreamManager;

/**
 * Connection implementation for in-memory blob store.
 *
 * @author Ronald Tschalär
 */
class MemConnection extends AbstractBlobStoreConnection {
  private final Map<URI, MemData> blobs;
  private final StreamManager     streamMgr;

  /**
   * Create a new connection.
   *
   * @param owner     the owning blob-store
   * @param blobs     the blob-map to use (shared, hence needs to be synchronized)
   * @param streamMgr the stream-manager to use
   */
  MemConnection(MemBlobStore owner, Map<URI, MemData> blobs, StreamManager streamMgr) {
    super(owner);
    this.blobs     = blobs;
    this.streamMgr = streamMgr;
  }

  //@Override
  public Blob getBlob(URI blobId, Map<String, String> hints) {
    if (blobId == null) {
      synchronized (blobs) {
        do {
          blobId = MemBlobStore.getRandomId("urn:mem-store:gen-id:");
        } while (blobs.containsKey(blobId));
      }
    }

    return new MemBlob(blobId, blobs, streamMgr, this);
  }

  //@Override
  public Iterator<URI> listBlobIds(final String filterPrefix) {
    synchronized (blobs) {
      return new FilterIterator(new ArrayList(blobs.keySet()).iterator(), new Predicate() {
         public boolean evaluate(Object object) {
           return ((filterPrefix == null) || object.toString().startsWith(filterPrefix));
         }
      });
    }
  }
}
