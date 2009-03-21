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
package org.fedoracommons.akubra;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.net.URI;

import java.util.Map;

import org.apache.commons.io.IOUtils;

/**
 * An abstract base class for blob store connections.
 *
 * @author Pradeep Krishnan
 */
public abstract class AbstractBlobStoreConnection implements BlobStoreConnection {
  protected final BlobStore owner;
  protected boolean closed = false;

  protected AbstractBlobStoreConnection(BlobStore owner) {
    this.owner = owner;
  }

  //@Override
  public BlobStore getBlobStore() {
    return owner;
  }

  //@Override
  public Blob getBlob(InputStream content, long estimatedSize, Map<String, String> hints)
            throws IOException, UnsupportedOperationException {
    Blob blob = getBlob((URI)null, hints);
    if (!blob.exists())
      blob.create();

    OutputStream out = null;
    try {
      IOUtils.copyLarge(content, out = blob.openOutputStream(estimatedSize));
      out.close();
      out = null;
    } finally {
      if (out != null)
        IOUtils.closeQuietly(out);
    }

    return blob;
  }


  //@Override
  public void close() {
    closed = true;
  }

  //@Override
  public boolean isClosed() {
    return closed;
  }
}
