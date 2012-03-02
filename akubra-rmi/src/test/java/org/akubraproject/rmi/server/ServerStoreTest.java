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
package org.akubraproject.rmi.server;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.isNull;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.util.Map;

import javax.transaction.Transaction;

import org.akubraproject.BlobStore;
import org.akubraproject.BlobStoreConnection;
import org.akubraproject.rmi.remote.RemoteConnection;
import org.akubraproject.rmi.remote.RemoteStore;
import org.akubraproject.rmi.remote.RemoteTransactionListener;
import org.akubraproject.rmi.remote.RemoteCallListener.Operation;
import org.akubraproject.rmi.remote.RemoteCallListener.Result;

import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

/**
 * Unit tests for ServerStore.
 *
 * @author Pradeep Krishnan
  */
public class ServerStoreTest {
  private Exporter    exporter;
  private ServerStore ss;
  private BlobStore   store;

  @BeforeSuite
  public void setUp() throws Exception {
    exporter   = new Exporter(0);
    store      = createMock(BlobStore.class);
    ss         = new ServerStore(store, exporter);
  }

  @AfterSuite
  public void tearDown() throws Exception {
    ss.unExport(false);
  }

  @Test
  public void testServerStore() {
    assertTrue(ss.getExported() instanceof RemoteStore);
  }

  @Test
  public void testOpenConnection() throws Exception {
    BlobStoreConnection con = createMock(BlobStoreConnection.class);
    reset(store);
    expect(store.openConnection(null, null)).andReturn(con);
    expect(store.openConnection(null, null)).andThrow(new UnsupportedOperationException());
    replay(store);

    RemoteConnection rc = ss.openConnection(null);
    assertTrue(rc instanceof ServerConnection);
    assertEquals(con, ((ServerConnection)rc).getConnection());

    try {
      ss.openConnection(null);
      fail("Failed to rcv expected exception");
    } catch (UnsupportedOperationException e) {
    }

    verify(store);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testStartTransaction() throws Exception {
    BlobStoreConnection con = createMock(BlobStoreConnection.class);

    reset(store);
    expect(store.openConnection(isA(Transaction.class), (Map) isNull())).andReturn(con);
    replay(store);

    RemoteTransactionListener rtl = ss.startTransactionListener(null);
    Operation<?> op = rtl.getNextOperation();
    assertTrue(op instanceof Result);

    RemoteConnection rc = ((Result<RemoteConnection>) op).get();
    assertTrue(rc instanceof ServerConnection);
    assertEquals(con, ((ServerConnection)rc).getConnection());

    verify(store);
  }
}
