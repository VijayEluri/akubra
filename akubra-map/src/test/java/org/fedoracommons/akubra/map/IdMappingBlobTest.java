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
package org.fedoracommons.akubra.map;

import java.io.OutputStream;
import java.io.StringReader;

import java.net.URI;

import org.apache.commons.io.IOUtils;

import org.fedoracommons.akubra.Blob;
import org.fedoracommons.akubra.BlobStore;
import org.fedoracommons.akubra.BlobStoreConnection;
import org.fedoracommons.akubra.impl.BlobWrapper;
import org.fedoracommons.akubra.mem.MemBlobStore;
import org.testng.annotations.Test;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

/**
 * Unit tests for {@link IdMappingBlob}.
 *
 * @author Chris Wilper
 */
public class IdMappingBlobTest {

  private static final URI blobId1 = URI.create("urn:blob:1");

  /**
   * Getting the canonical id should return a value when the delegate does.
   */
  @Test
  public void testGetCanonicalIdNotNull() throws Exception {
    assertNotNull(getTestBlob(blobId1, true).getCanonicalId());
  }

  /**
   * Getting the canonical id should return <code>null</code> when the delegate does.
   */
  @Test
  public void testGetCanonicalIdNull() throws Exception {
    assertNull(getTestBlob(blobId1, false).getCanonicalId());
  }

  private static Blob getTestBlob(URI blobId,
                                  boolean delegateCanCanonicalize)
      throws Exception {
    IdMapper mapper = new MockIdMapper();
    BlobStore store = new IdMappingBlobStore(URI.create("urn:test-store"),
        new MemBlobStore(), mapper);
    BlobStoreConnection connection = store.openConnection(null, null);
    Blob delegate = connection.getBlob(blobId, null);
    OutputStream out = delegate.openOutputStream(-1, false);
    IOUtils.copy(new StringReader(blobId.toString()), out);
    out.close();
    if (!delegateCanCanonicalize)
      delegate = new BlobWrapper(delegate) {
        @Override
        public URI getCanonicalId() {
          return null;
        }
      };
    return new IdMappingBlob(connection, delegate, mapper);
  }

}
