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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.transaction.Transaction;

import org.fedoracommons.akubra.Blob;
import org.fedoracommons.akubra.BlobStore;
import org.fedoracommons.akubra.BlobStoreConnection;
import org.fedoracommons.akubra.UnsupportedIdException;
import org.fedoracommons.akubra.mem.MemBlobStore;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

/**
 * Tests for AbstractMuxConnection.
 *
 * @author Pradeep Krishnan
 */
public class AbstractMuxConnectionTest {
  private MemBlobStore        store1;
  private MemBlobStore        store2;
  private AbstractMuxStore    store;
  private TestConnection      con;
  private Map<String, String> s1Hint;
  private Map<String, String> s2Hint;

  /**
   * Set things up for all tests.
   */
  @BeforeSuite
  public void setUp() throws Exception {
    store1   = new MemBlobStore(URI.create("urn:store:1"));
    store2   = new MemBlobStore(URI.create("urn:store:2"));

    store =
      new AbstractMuxStore(URI.create("urn:store")) {
          public BlobStoreConnection openConnection(Transaction tx)
                                             throws UnsupportedOperationException, IOException {
            return new TestConnection(this, tx);
          }
        };

    store.setBackingStores(Arrays.asList(store1, store2));
    s1Hint   = new HashMap<String, String>();
    s2Hint   = new HashMap<String, String>();
    s1Hint.put("store1", "store1");
    s2Hint.put("store2", "store2");

    con = (TestConnection) store.openConnection(null);
  }

  @AfterSuite
  public void tearDown() throws Exception {
  }

  /**
   * Test to see if close() closes all underlying stores.
   */
  @Test
  public void testClose() throws IOException {
    TestConnection con = (TestConnection) store.openConnection(null);
    assertFalse(con.isClosed());
    con.getConnection(store1);
    con.getConnection(store2);
    assertEquals(2, con.getCons().size());

    Collection<BlobStoreConnection> cons = con.getCons().values();
    con.close();
    assertTrue(con.isClosed());

    for (BlobStoreConnection c : cons)
      assertTrue(c.isClosed());
  }

  /**
   * Test for constructor.
   */
  @Test
  public void testAbstractMuxConnection() {
    assertEquals(store, con.getBlobStore());
  }

  /**
   * Tests that getStores() return the backing stores.
   */
  @Test
  public void testGetStores() {
    Set<BlobStore> s = con.getStores(null);
    assertEquals(2, s.size());
    assertTrue(s.contains(store1));
    assertTrue(s.contains(store2));
  }

  /**
   * Tests that the connection cache works as expected.
   */
  @Test
  public void testGetConnection() throws IOException {
    BlobStoreConnection con1 = con.getConnection(store1);
    BlobStoreConnection con2 = con.getConnection(store2);
    assertEquals(2, con.getCons().size());
    assertEquals(con.getCons().get(store1.getId()), con1);
    assertEquals(con.getCons().get(store2.getId()), con2);
    assertEquals(con1, con.getConnection(store1));
    assertEquals(con2, con.getConnection(store2));
  }

  /**
   * Cross checks getBlob returned with backing stores.
   */
  @Test
  public void testGetBlobURIMapOfStringString() throws IOException {
    Blob b1 = con.getBlob(null, s1Hint);
    Blob b2 = con.getBlob(null, s2Hint);
    Blob b3 = store1.openConnection(null).getBlob(b1.getId(), s1Hint);
    Blob b4 = store2.openConnection(null).getBlob(b2.getId(), s2Hint);

    if (!b1.exists())
      b1.create();

    if (!b2.exists())
      b2.create();

    assertTrue(b3.exists());
    assertTrue(b4.exists());
    b1.delete();
    b2.delete();
    assertFalse(b3.exists());
    assertFalse(b4.exists());
  }

  /**
   * Tests iteration across blob stores.
   */
  @Test
  public void testListBlobIds() throws IOException {
    for (Iterator<URI> it = con.listBlobIds(null); it.hasNext();)
      con.getBlob(it.next(), null).delete();

    assertFalse(con.listBlobIds(null).hasNext());

    Blob b1 = con.getBlob(null, s1Hint);
    Blob b2 = con.getBlob(null, s2Hint);

    if (!b1.exists())
      b1.create();

    if (!b2.exists())
      b2.create();

    Set<Blob> blobs = new HashSet<Blob>();

    for (Iterator<URI> it = con.listBlobIds(null); it.hasNext();)
      blobs.add(con.getBlob(it.next(), null));

    assertEquals(2, blobs.size());
    assertTrue(blobs.contains(b1));
    assertTrue(blobs.contains(b2));

    for (Blob b : new Blob[] { b1, b2 }) {
      blobs = new HashSet<Blob>();

      for (Iterator<URI> it = con.listBlobIds(b.getId().toString()); it.hasNext();)
        blobs.add(con.getBlob(it.next(), null));

      assertEquals(1, blobs.size());
      assertTrue(blobs.contains(b));
    }
  }

  /**
   * Tests renames across blob stores.
   */
  @Test
  public void testMoveTo() throws IOException {
    Blob b1 = con.getBlob(null, s1Hint);
    Blob b2 = con.getBlob(null, s2Hint);
    Blob b3 = store1.openConnection(null).getBlob(b1.getId(), s1Hint);
    Blob b4 = store2.openConnection(null).getBlob(b2.getId(), s2Hint);

    if (!b1.exists())
      b1.create();

    if (!b2.exists())
      b2.create();

    assertTrue(b3.exists());
    assertTrue(b4.exists());
    b2.delete();
    assertFalse(b2.exists());
    assertFalse(b4.exists());

    b1.moveTo(b2);

    assertFalse(b1.exists());
    assertFalse(b3.exists());
    assertTrue(b2.exists());
    assertTrue(b4.exists());
  }

  private class TestConnection extends AbstractMuxConnection {
    private TestConnection(BlobStore store, Transaction txn) {
      super(store, txn);
    }

    @Override
    public BlobStore getStore(URI blobId, Map<String, String> hints)
                       throws IOException, UnsupportedIdException {
      if (hints != null) {
        if (hints.keySet().containsAll(s1Hint.keySet()))
          return store1;

        if (hints.keySet().containsAll(s2Hint.keySet()))
          return store2;
      }

      if (blobId != null) {
        for (BlobStore s : getBlobStore().getBackingStores())
          if (getConnection(s).listBlobIds(blobId.toString()).hasNext()
               && (getConnection(s).getBlob(blobId, hints) != null))
            return s;
      }

      for (BlobStore s : getBlobStore().getBackingStores()) {
        try {
          if (getConnection(s).getBlob(blobId, hints) != null)
            return s;
        } catch (UnsupportedIdException e) {
          // skip this one
        }
      }

      throw new UnsupportedIdException(blobId);
    }

    public Map<URI, BlobStoreConnection> getCons() {
      return cons;
    }
  }
}
