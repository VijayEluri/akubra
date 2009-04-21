/* $HeadURL::                                                                            $
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
package org.fedoracommons.akubra.mux;

import java.io.IOException;

import java.net.URI;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.transaction.Transaction;

import org.fedoracommons.akubra.Blob;
import org.fedoracommons.akubra.BlobStore;
import org.fedoracommons.akubra.BlobStoreConnection;
import org.fedoracommons.akubra.UnsupportedIdException;
import org.fedoracommons.akubra.impl.AbstractBlobStoreConnection;

/**
 * An abstract base class for connections returned by multiplexing BlobStores. Sub-class
 * implementations must provide a {@link #getStore} implementation. Additionally for multiplexing
 * content addressable stores, sub-classes must override the default implementation of {@link
 * org.fedoracommons.akubra.impl.AbstractBlobStoreConnection#getBlob(java.io.InputStream, long,
 * Map)} and direct the call to the appropriate backing store. Note that the returned Blob from
 * the backing store must be wrapped so that the {@link
 * org.fedoracommons.akubra.Blob#getConnection()} of the returned Blob will return this connection
 * object.
 *
 * @author Pradeep Krishnan
 *
 * @see #getStore(URI, Map)
 * @see #getStores(String)
 * @see #getConnection(BlobStore)
 * @see MuxBlob
 */
public abstract class AbstractMuxConnection extends AbstractBlobStoreConnection {
  /**
   * The transaction to pass to while opening connections to the backing stores. Note that
   * this transaction is only passed to transaction capable stores. A null is passed for
   * non-transactional stores.
   *
   * @see #getConnection(BlobStore)
   */
  protected final Transaction txn;

  /**
   * A map of store-ids to connections.
   *
   * @see #getConnection(BlobStore)
   * @see #close()
   */
  protected Map<URI, BlobStoreConnection> cons = new HashMap<URI, BlobStoreConnection>();

  /**
   * Creates a new AbstractMuxConnection object.
   *
   * @param store the mux store
   * @param txn the txn passed in by the user
   */
  public AbstractMuxConnection(BlobStore store, Transaction txn) {
    super(store);
    this.txn = txn;
  }

  /**
   * Gets the backing store where the given blob is persisted.
   *
   * @param blobId the blob id (can be null)
   * @param hints A set of hints to allow the implementation to optimize the operation (can be
   *        null)
   *
   * @return the store. Cannot be null and must be one of the backing stores.
   *
   * @throws IOException for IO errors
   * @throws UnsupportedIdException if blobId is not in a recognized/usable pattern by any of the
   *         backing stores
   */
  public abstract BlobStore getStore(URI blobId, Map<String, String> hints)
                              throws IOException, UnsupportedIdException;

  /**
   * Gets the set of a backing stores matching the given blobId prefix. By default the
   * returned set is the set of all backing stores. Override in sub-classes to narrow down the
   * set.
   *
   * @param prefix the blobId prefix or null
   *
   * @return the set of stores matching the blobId prefix
   */
  public Set<BlobStore> getStores(String prefix) {
    return new HashSet<BlobStore>(getBlobStore().getBackingStores());
  }

  /**
   * Closes this connection and all underlying connections created using {@link
   * #getConnection(BlobStore)}.
   */
  @Override
  public void close() {
    super.close();

    if (cons != null) {
      for (BlobStoreConnection con : cons.values())
        con.close();

      cons.clear();
      cons = null;
    }
  }

  /**
   * Lookup/Create a connection to the given backing store.
   *
   * @param store the backing store
   *
   * @return the backing store connection or null
   *
   * @throws IOException on an error in opening a connection to the backing store
   */
  protected BlobStoreConnection getConnection(BlobStore store)
                                       throws IOException {
    if (store == null)
      return null;

    if (cons == null)
      throw new IllegalStateException("Connection closed.");

    BlobStoreConnection con = cons.get(store.getId());

    if (con == null) {
      Transaction t = store.getCapabilities().contains(BlobStore.TXN_CAPABILITY) ? txn : null;

      try {
        con = store.openConnection(t);
      } catch (UnsupportedOperationException e) {
        IOException ioe = new IOException("Failed to openConnection to " + store.getId());
        ioe.initCause(e);
        throw ioe;
      }

      cons.put(store.getId(), con);
    }

    return con;
  }

  public Blob getBlob(URI blobId, Map<String, String> hints)
               throws IOException {
    return new MuxBlob(getConnection(getStore(blobId, hints)).getBlob(blobId, hints), this);
  }

  public Iterator<URI> listBlobIds(String filterPrefix)
                            throws IOException {
    List<Iterator<URI>> iterators = new ArrayList<Iterator<URI>>();

    for (BlobStore store : getStores(filterPrefix))
      iterators.add(getConnection(store).listBlobIds(filterPrefix));

    final Iterator<Iterator<URI>> it = iterators.iterator();

    return new Iterator<URI>() {
        private Iterator<URI> cur = null;

        public boolean hasNext() {
          while ((cur == null) || (cur.hasNext() == false)) {
            if (!it.hasNext())
              return false;

            cur = it.next();
          }

          return true;
        }

        public URI next() {
          return hasNext() ? cur.next() : null;
        }

        public void remove() {
          throw new UnsupportedOperationException("remove() not supported");
        }
      };
  }
}
