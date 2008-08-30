/* $HeadURL::                                                                            $
 * $Id$
 *
 * Copyright (c) 2007-2008 by Fedora Commons Inc.
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
package org.fedoracommons.akubra;

import java.net.URI;
import java.util.List;

import javax.transaction.Transaction;

/**
 * Interface to abstract the idea of a general transaction based blob store
 *
 * @author Chris Wilper
 * @author Pradeep Krishnan
 * @author Ronald Tschalär
 */
public interface BlobStore {
  /**
   * Return the identifier associated with the store
   *
   * @return the URI identifying the blob store
   */
  URI getId();

  /**
   * Set the identifier for the store
   *
   * @param id the URI identifying the blob store
   */
  void setId(URI id);

  /**
   * Return the list of stores underlying this store
   *
   * @return list of underlying blob stores
   */
  List<BlobStore> getBackingStores();

  /**
   * Set the list of back store
   *
   * @param stores the list of backing store
   */
  void setBackingStores(List<BlobStore> stores)
    throws UnsupportedOperationException, IllegalStateException;

  /**
   * Open a connection to the blob store. The connection is valid for exactly one transaction, at
   * the end of which it will get {@link BlobStoreConnection#close close()}'d. Implementations are
   * expected to do any desired pooling of underlying connections themselves.
   *
   * @param tx the transaction associated with this connection
   * @return the connection to the blob store
   */
  BlobStoreConnection openConnection(Transaction tx);

  /**
   * Get capabilities of this blob store instance only
   *
   * @return array of Capability
   */
  Capability[] getDeclaredCapabilities();

  /**
   * Return capabilities of this store plus underlying blob stores, that is of the blob stack
   * starting at this level.
   *
   * @return array of Capability
   */
  Capability[] getCapabilities();
}
