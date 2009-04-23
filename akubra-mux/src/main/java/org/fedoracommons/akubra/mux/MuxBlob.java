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
import java.io.InputStream;
import java.io.OutputStream;

import java.net.URI;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.fedoracommons.akubra.Blob;
import org.fedoracommons.akubra.DuplicateBlobException;
import org.fedoracommons.akubra.MissingBlobException;
import org.fedoracommons.akubra.impl.BlobWrapper;

/**
 * A wrapped blob for use by implementations of {@link AbstractMuxConnection}. This ensures
 * that {@link #getConnection()} returns the connection from the mux store layer rather than the
 * backing store. Additionally this supports {@link #moveTo(Blob)} across blob stores using {@link
 * #moveByCopy(Blob, URI, URI)}.
 *
 * @author Pradeep Krishnan
 */
public class MuxBlob extends BlobWrapper {
  private static final Log log = LogFactory.getLog(MuxBlob.class);

  /**
   * Creates a new MuxBlob instance.
   *
   * @param delegate the Blob instance returned by a backing store
   * @param con the mux store connection
   */
  public MuxBlob(Blob delegate, AbstractMuxConnection con) {
    super(delegate, con);
  }

  @Override
  public void moveTo(Blob blob) throws IOException {
    if (blob == null)
      throw new NullPointerException("blob cannot be null");

    URI thisStore  = delegate.getConnection().getBlobStore().getId();
    URI otherStore =
      (blob instanceof MuxBlob) ? ((MuxBlob) blob).delegate.getConnection().getBlobStore().getId()
      : ((AbstractMuxConnection) getConnection()).getStore(blob.getId(), null).getId();

    if (!thisStore.equals(otherStore))
      moveByCopy(blob, thisStore, otherStore);
    else if (blob instanceof MuxBlob)
      delegate.moveTo(((MuxBlob) blob).delegate);
    else {
      // Unrecognized Blob class. Let the backing store deal with it.
      delegate.moveTo(blob);
    }
  }

  /**
   * Performs a {@link #moveTo(Blob)} operation by copy since the blobs are in different stores.
   *
   * @param blob the destination
   * @param thisStore the store where this Blob exists
   * @param otherStore the store where the destination Blob should exist
   *
   * @throws IOException on an error in copy
   * @throws DuplicateBlobException if a blob with the same id as the destination blob exists in
   *         the destination blob store
   * @throws MissingBlobException if this blob does not exist
   */
  protected void moveByCopy(Blob blob, URI thisStore, URI otherStore)
                     throws IOException, DuplicateBlobException, MissingBlobException {
    log.warn("Performing moveTo() by copy for '" + getId() + "' to '" + blob.getId()
             + "' from store '" + thisStore + "' to store '" + otherStore + "'");

    InputStream  in      = openInputStream();
    OutputStream out     = null;
    boolean      created = false;

    try {
      out = blob.openOutputStream(getSize(), false);
      created = true;
      IOUtils.copy(in, out);
      out.close();
      out = null;
      in.close();
      in = null;

      delete();
      created = false;
    } finally {
      if (in != null)
        IOUtils.closeQuietly(in);

      if (out != null)
        IOUtils.closeQuietly(out);

      try {
        if (created)
          blob.delete();
      } catch (Exception de) {
        log.warn("Ignored deletion failure for " + blob.getId());
      }
    }
  }
}
