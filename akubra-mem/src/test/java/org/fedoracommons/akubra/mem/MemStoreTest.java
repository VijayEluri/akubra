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

import java.net.URI;

import org.testng.annotations.Factory;
import org.testng.annotations.Test;

import org.fedoracommons.akubra.BlobStore;
import org.fedoracommons.akubra.tck.TCKTestSuite;

/**
 * TCK test suite for {@link MemBlobStore}.
 *
 * @author Ronald Tschalär
 */
public class MemStoreTest {
  @Factory
  public Object[] createTests() {
    URI storeId1 = URI.create("urn:mem-test:42");
    MemBlobStore bs2 = new MemBlobStore();

    return new Object[] {
      new MemStoreTestSuite(new MemBlobStore(storeId1), storeId1),
      new MemStoreTestSuite(bs2, bs2.getId()),
    };
  }

  public static class MemStoreTestSuite extends TCKTestSuite {
    public MemStoreTestSuite(BlobStore store, URI storeId) {
      super(store, storeId, false, true);
    }

    /** all URI's are valid */
    protected URI getInvalidId() {
      return null;
    }

    /** all URI's are distinct */
    protected URI[] getAliases(URI uri) {
      return new URI[] { uri };
    }

    /** test expansion of data buffer */
    @Test(groups={ "blob", "manipulatesBlobs" }, dependsOnGroups={ "init" })
    public void testBufferExpansion() throws Exception {
      // basic buffer
      URI id = createId("blobBufferExpansion1");
      createBlob(id, "Abandon all hope ye who enter here!", true);

      // first expansion
      StringBuilder sb = new StringBuilder(4000);
      sb.append("A tale told by an idiot, full of sound and fury, signifying nothing. ");
      for (int idx = 0; idx < 4; idx++)
        sb.append(sb.toString());

      setBlob(id, sb.toString(), true);

      // second expansion
      sb.append(sb.toString());
      setBlob(id, sb.toString(), true);

      // clean up
      deleteBlob(id, sb.toString(), true);
      assertNoBlobs(getPrefixFor("blobBufferExpansion"));
    }
  }
}
