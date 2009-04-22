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

package org.fedoracommons.akubra.tck;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.apache.commons.io.IOUtils;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import org.fedoracommons.akubra.AkubraBlobException;
import org.fedoracommons.akubra.Blob;
import org.fedoracommons.akubra.BlobStore;
import org.fedoracommons.akubra.BlobStoreConnection;

/**
 * A set of helpers for the TCK tests. Most of these helpers are not static even though they could
 * be so that they can overriden if necessary.
 *
 * @author Ronald Tschalär
 */
@SuppressWarnings("PMD.TooManyStaticImports")
public abstract class AbstractTests {
  /** the store being tested */
  protected final BlobStore          store;
  /** whether the store is transactional or not */
  protected final boolean            isTransactional;
  /** a transaction-manager */
  protected final TransactionManager tm;

  /**
   * Create a new test-helper instance.
   *
   * @param store           the store to test
   * @param isTransactional if the store is transactional
   */
  protected AbstractTests(BlobStore store, boolean isTransactional) {
    this.store           = store;
    this.isTransactional = isTransactional;

    tm = BtmUtils.getTM();
  }

  /*
   * simple helpers that do an operation on an open connection and assert things went fine
   */

  protected Blob getBlob(BlobStoreConnection con, URI id, boolean exists) throws Exception {
    Blob b = con.getBlob(id, null);
    if (id != null)
      assertEquals(b.getId(), id);
    assertEquals(b.exists(), exists);
    return b;
  }

  protected Blob getBlob(BlobStoreConnection con, URI id, String body) throws Exception {
    Blob b = getBlob(con, id, body != null);

    if (body != null)
      assertEquals(getBody(b), body);

    return b;
  }

  protected void createBlob(BlobStoreConnection con, Blob b, String body) throws Exception {
    setBlob(con, b, (body != null) ? body : "");

    assertTrue(b.exists());
    assertTrue(con.getBlob(b.getId(), null).exists());
  }

  protected void setBlob(BlobStoreConnection con, Blob b, String body) throws Exception {
    setBody(b, body);
    assertEquals(getBody(b), body);
    assertEquals(getBody(con.getBlob(b.getId(), null)), body);
  }

  protected void deleteBlob(BlobStoreConnection con, Blob b) throws Exception {
    b.delete();
    assertFalse(b.exists());
    assertFalse(con.getBlob(b.getId(), null).exists());
  }

  protected void moveBlob(BlobStoreConnection con, Blob ob, Blob nb, String body) throws Exception {
    ob.moveTo(nb);
    assertFalse(ob.exists());
    assertFalse(con.getBlob(ob.getId(), null).exists());
    assertTrue(nb.exists());
    assertTrue(con.getBlob(nb.getId(), null).exists());

    if (body != null) {
      assertEquals(getBody(nb), body);
      assertEquals(getBody(con.getBlob(nb.getId(), null)), body);
    }
  }

  protected void listBlobs(BlobStoreConnection con, String prefix, URI[] expected)
      throws Exception {
    Set<URI> exp = new HashSet(Arrays.asList(expected));
    URI id;

    for (Iterator<URI> iter = con.listBlobIds(prefix); iter.hasNext(); )
      assertTrue(exp.remove(id = iter.next()), "unexpected blob '" + id + "' found;");

    assertTrue(exp.isEmpty(), "expected blobs not found for prefix '" + prefix + "': " + exp + ";");
  }

  /*
   * versions of the simple helpers that open a connection and run in a transaction if necessary.
   */

  protected void createBlob(final URI id, final String val, boolean commit) throws Exception {
    runTests(new ConAction() {
        public void run(BlobStoreConnection con) throws Exception {
          Blob b = getBlob(con, id, null);
          createBlob(con, b, val);
        }
    }, commit);
  }

  protected void deleteBlob(final URI id, final String body, final boolean commit)
      throws Exception {
    runTests(new ConAction() {
        public void run(BlobStoreConnection con) throws Exception {
          Blob b = getBlob(con, id, body);
          deleteBlob(con, b);
        }
    }, commit);
  }

  protected void getBlob(final URI id, final String val, boolean commit) throws Exception {
    runTests(new ConAction() {
        public void run(BlobStoreConnection con) throws Exception {
          getBlob(con, id, val);
        }
    }, commit);
  }

  protected void setBlob(final URI id, final String val, boolean commit) throws Exception {
    runTests(new ConAction() {
        public void run(BlobStoreConnection con) throws Exception {
          Blob b = getBlob(con, id, true);
          setBlob(con, b, val);
        }
    }, commit);
  }

  protected void renameBlob(final URI oldId, final URI newId, final String val, boolean commit)
      throws Exception {
    runTests(new ConAction() {
        public void run(BlobStoreConnection con) throws Exception {
          Blob ob = getBlob(con, oldId, val);
          Blob nb = getBlob(con, newId, null);
          moveBlob(con, ob, nb, val);
        }
    }, commit);
  }

  protected void listBlobs(final String prefix, final URI[] expected) throws Exception {
    runTests(new ConAction() {
        public void run(BlobStoreConnection con) throws Exception {
          listBlobs(con, prefix, expected);
        }
    }, true);
  }

  protected void assertNoBlobs(final String prefix) throws Exception {
    runTests(new ConAction() {
        public void run(BlobStoreConnection con) throws Exception {
          assertFalse(con.listBlobIds(prefix).hasNext(),
                      "unexpected blobs found for prefix '" + prefix + "';");
        }
    }, true);
  }

  /*
   * Other helpers
   */

  protected String getBody(Blob b) throws IOException {
    InputStream is = b.openInputStream();
    try {
      String res = IOUtils.toString(is, "UTF-8");
      long size = b.getSize();
      if (size != -1)
        assertEquals(res.length(), size);
      return res;
    } finally {
      is.close();
    }
  }

  protected void setBody(Blob b, String data) throws IOException {
    setBody(b, data, data.length(), true);
  }

  protected void setBody(Blob b, String data, long estSize, boolean overwrite) throws IOException {
    OutputStream os = b.openOutputStream(estSize, overwrite);
    try {
      os.write(data.getBytes("UTF-8"));
    } finally {
      os.close();
    }
  }

  protected void runTests(Action test) throws Exception {
    runTests(test, true);
  }

  protected void runTests(Action test, boolean commit) throws Exception {
    if (isTransactional) {
      try {
        tm.begin();

        test.run(tm.getTransaction());

        if (commit)
          tm.commit();
        else
          tm.rollback();
      } finally {
        if (tm.getTransaction() != null) {
          try {
            tm.rollback();
          } catch (Throwable t) {
            t.printStackTrace();
          }
        }
      }
    } else {
      test.run(null);
    }
  }

  protected void runTests(ConAction test) throws Exception {
    runTests(test, true);
  }

  protected void runTests(final ConAction test, boolean commit) throws Exception {
    runTests(new Action() {
      public void run(Transaction txn) throws Exception {
        BlobStoreConnection con = store.openConnection(txn);
        assertSame(con.getBlobStore(), store);
        assertFalse(con.isClosed());

        boolean success = false;
        try {
          test.run(con);
          success = true;
        } finally {
          try {
            con.close();
            assertTrue(con.isClosed());
          } catch (Throwable t) {
            if (success) {
              if (t instanceof Exception)
                throw (Exception) t;
              if (t instanceof Error)
                throw (Error) t;
              throw new Error("Who the heck subclassed Throwable directly?", t);
            }
            t.printStackTrace();
          }
        }
      }
    }, commit);
  }

  protected void shouldFail(ERunnable test, Class<? extends Throwable> expExc, URI id)
      throws Exception {
    try {
      test.erun();
      fail("Did not get expected " + expExc.getName());
    } catch (Throwable t) {
      if (expExc.isAssignableFrom(t.getClass())) {
        if (t instanceof AkubraBlobException)
          assertEquals(((AkubraBlobException) t).getBlobId(), id);
      } else {
        if (t instanceof Exception)
          throw (Exception) t;
        if (t instanceof Error)
          throw (Error) t;
        throw new Error("Who the heck subclassed Throwable directly?", t);
      }
    }
  }

  protected void shouldFail(final ConAction test, Class<? extends Throwable> expExc, URI id)
      throws Exception {
    shouldFail(new ERunnable() {
      public void erun() throws Exception {
        runTests(test);
      }
    }, expExc, id);
  }

  protected static interface Action {
    public void run(Transaction txn) throws Exception;
  }

  protected static interface ConAction {
    public void run(BlobStoreConnection con) throws Exception;
  }

  protected static abstract class ERunnable implements Runnable {
    public void run() {
      try {
        erun();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    public abstract void erun() throws Exception;
  }

  protected Thread doInThread(Runnable r) throws Exception {
    return doInThread(r, null);
  }

  protected Thread doInThread(Runnable r, final boolean[] failed) throws Exception {
    Thread t = new Thread(r, "TCKTest");

    t.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
      public void uncaughtException(Thread t, Throwable e) {
        if (failed != null) {
          synchronized (failed) {
            failed[0] = true;
          }
        }

        e.printStackTrace();
      }
    });

    t.start();
    return t;
  }

  protected static void waitFor(boolean[] cv, boolean val, long to) throws InterruptedException {
    long t0 = System.currentTimeMillis();
    synchronized (cv) {
      while (cv[0] != val && (to == 0 || (System.currentTimeMillis() - t0) < to))
        cv.wait(to);
    }
  }

  protected static void notify(boolean[] cv, boolean val) {
    synchronized (cv) {
      cv[0] = val;
      cv.notifyAll();
    }
  }

  protected static void notifyAndWait(boolean[] cv, boolean val) throws InterruptedException {
    synchronized (cv) {
      notify(cv, val);
      waitFor(cv, !val, 0);
    }
  }
}
