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
package org.fedoracommons.akubra.rmi.client;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.classextension.EasyMock.createMock;
import static org.easymock.classextension.EasyMock.makeThreadSafe;
import static org.easymock.classextension.EasyMock.replay;
import static org.easymock.classextension.EasyMock.reset;
import static org.easymock.classextension.EasyMock.verify;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.io.IOException;
import java.io.InputStream;

import java.net.URI;

import java.rmi.NoSuchObjectException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.fedoracommons.akubra.Blob;
import org.fedoracommons.akubra.BlobStoreConnection;
import org.fedoracommons.akubra.UnsupportedIdException;
import org.fedoracommons.akubra.impl.StreamManager;
import org.fedoracommons.akubra.rmi.remote.RemoteConnection;
import org.fedoracommons.akubra.rmi.server.Exporter;
import org.fedoracommons.akubra.rmi.server.ServerConnection;

import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

/**
 * Unit tests for ClientConnection.
 *
 * @author Pradeep Krishnan
  */
public class ClientConnectionTest {
  private Exporter            exporter;
  private BlobStoreConnection con;
  private ServerConnection    sc;
  private ClientConnection    cc;

  @BeforeSuite
  public void setUp() throws Exception {
    exporter = new Exporter(0);

    ClientStore store = createMock(ClientStore.class);
    expect(store.getExporter()).andStubReturn(exporter);
    replay(store);

    con   = createMock(BlobStoreConnection.class);
    sc    = new ServerConnection(con, exporter);
    cc    = new ClientConnection(store, new StreamManager(), (RemoteConnection) sc.getExported());
  }

  @AfterSuite
  public void tearDown() throws Exception {
    sc.unExport(false);
  }

  @Test
  public void testGetBlobInputStreamLongMapOfStringString()
                                                   throws IOException {
    InputStream         in    = createMock(InputStream.class);
    URI                 id    = URI.create("foo:bar");
    Map<String, String> hints = new HashMap<String, String>();
    hints.put("try harder?", "yes, of course!");

    Blob blob = createMock(Blob.class);
    expect(blob.getId()).andStubReturn(id);
    makeThreadSafe(blob, true);
    replay(blob);

    reset(con);
    expect(con.getBlob(isA(ClientInputStream.class), eq(42L), eq(hints))).andReturn(blob);
    expect(con.getBlob(isA(ClientInputStream.class), eq(-1L), eq(hints)))
       .andThrow(new UnsupportedOperationException());
    makeThreadSafe(con, true);
    replay(con);

    Blob cb = cc.getBlob(in, 42L, hints);
    assertTrue(cb instanceof ClientBlob);
    assertEquals(blob.getId(), cb.getId());

    try {
      cc.getBlob(in, -1L, hints);
      fail("Failed to rcv expected exception");
    } catch (UnsupportedOperationException e) {
    }

    verify(con);
  }

  @Test
  public void testClose() throws IOException {
    ServerConnection sc = new ServerConnection(con, exporter);
    ClientConnection cc =
      new ClientConnection(this.cc.getBlobStore(), new StreamManager(),
                           (RemoteConnection) sc.getExported());

    reset(con);
    con.close();
    expect(con.listBlobIds(null)).andStubReturn(null);
    replay(con);

    cc.listBlobIds(null);
    cc.close();
    verify(con);

    try {
      for (int i = 0; i < 10; i++) {
        try {
          Thread.sleep(Exporter.RETRY_DELAY * i * 2);
        } catch (InterruptedException e) {
        }

        cc.listBlobIds(null);
      }

      fail("Failed to get expected exception");
    } catch (NoSuchObjectException e) {
    }
  }

  @Test
  public void testGetBlobURIMapOfStringString() throws IOException {
    URI                 id    = URI.create("foo:bar");
    Map<String, String> hints = new HashMap<String, String>();
    hints.put("try harder?", "yes, of course!");

    Blob blob = createMock(Blob.class);
    expect(blob.getId()).andStubReturn(id);
    makeThreadSafe(blob, true);
    replay(blob);

    reset(con);
    expect(con.getBlob(id, hints)).andReturn(blob);
    expect(con.getBlob(id, hints)).andThrow(new UnsupportedIdException(id));
    makeThreadSafe(con, true);
    replay(con);

    Blob rb = cc.getBlob(id, hints);
    assertTrue(rb instanceof ClientBlob);
    assertEquals(blob.getId(), rb.getId());

    try {
      cc.getBlob(id, hints);
      fail("Failed to rcv expected exception");
    } catch (UnsupportedIdException e) {
      assertEquals(id, e.getBlobId());
    }

    verify(con);
  }

  @Test
  public void testListBlobIds() throws IOException {
    URI           id = URI.create("foo:bar");
    Iterator<URI> it = Arrays.asList(id).iterator();

    reset(con);
    expect(con.listBlobIds(null)).andReturn(it);
    replay(con);

    Iterator<URI> ri = cc.listBlobIds(null);
    assertTrue(ri instanceof ClientIterator);
    assertTrue(ri.hasNext());
    assertEquals(id, ri.next());
    assertFalse(ri.hasNext());
    verify(con);
  }
}
