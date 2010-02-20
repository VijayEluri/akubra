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
import static org.easymock.classextension.EasyMock.createMock;
import static org.easymock.classextension.EasyMock.replay;
import static org.easymock.classextension.EasyMock.reset;
import static org.easymock.classextension.EasyMock.verify;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.net.URI;

import org.akubraproject.Blob;
import org.akubraproject.BlobStoreConnection;
import org.akubraproject.DuplicateBlobException;
import org.akubraproject.MissingBlobException;
import org.akubraproject.rmi.remote.RemoteBlob;
import org.akubraproject.rmi.remote.RemoteInputStream;
import org.akubraproject.rmi.remote.RemoteOutputStream;

import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

/**
 * Unit tests for ServerBlob.
 *
 * @author Pradeep Krishnan
  */
public class ServerBlobTest {
  private Exporter   exporter;
  private ServerBlob sb;
  private Blob       blob;

  @BeforeSuite
  public void setUp() throws Exception {
    exporter   = new Exporter(0);
    blob       = createMock(Blob.class);
    sb         = new ServerBlob(blob, exporter);
  }

  @AfterSuite
  public void tearDown() throws Exception {
    sb.unExport(false);
  }

  @Test
  public void testServerBlob() {
    assertTrue(sb.getExported() instanceof RemoteBlob);
  }

  @Test
  public void testGetId() {
    URI uri = URI.create("urn:test:blob:id");
    reset(blob);
    expect(blob.getId()).andReturn(uri);
    replay(blob);

    assertEquals(uri, sb.getId());
    verify(blob);
  }

  @Test
  public void testExists() throws IOException {
    reset(blob);
    expect(blob.exists()).andReturn(true);
    replay(blob);

    assertTrue(sb.exists());
    verify(blob);
  }

  @Test
  public void testGetSize() throws IOException {
    reset(blob);
    expect(blob.getSize()).andReturn(42L);
    replay(blob);

    assertEquals(42L, sb.getSize());
    verify(blob);
  }

  @Test
  public void testDelete() throws IOException {
    reset(blob);
    blob.delete();
    replay(blob);

    sb.delete();
    verify(blob);
  }

  @Test
  public void testMoveTo() throws IOException {
    URI                 id1   = URI.create("foo:1");
    URI                 id2   = URI.create("foo:2");
    BlobStoreConnection con   = createMock(BlobStoreConnection.class);
    Blob                blob2 = createMock(Blob.class);

    reset(blob);
    expect(blob.getConnection()).andStubReturn(con);
    expect(blob2.getConnection()).andStubReturn(con);

    expect(blob.getId()).andStubReturn(id1);
    expect(blob2.getId()).andStubReturn(id2);

    expect(con.getBlob(id1, null)).andStubReturn(blob);
    expect(con.getBlob(id2, null)).andStubReturn(blob2);

    expect(blob.moveTo(id1, null)).andStubThrow(new DuplicateBlobException(id1));
    expect(blob.moveTo(id2, null)).andStubReturn(blob2);
    expect(blob.moveTo(null, null)).andStubThrow(new UnsupportedOperationException());
    replay(blob);
    replay(blob2);
    replay(con);

    try {
      sb.moveTo(blob.getId(), null);
      fail("Failed to rcv expected exception");
    } catch (DuplicateBlobException e) {
    }

    sb.moveTo(blob2.getId(), null);

    try {
      sb.moveTo(null, null);
      fail("Failed to rcv expected exception");
    } catch (UnsupportedOperationException e) {
    }

    verify(blob);
  }

  @Test
  public void testOpenInputStream() throws IOException {
    URI         id = URI.create("foo:bar");
    InputStream in = createMock(InputStream.class);
    reset(blob);
    expect(blob.openInputStream()).andReturn(in);
    expect(blob.openInputStream()).andThrow(new MissingBlobException(id));
    replay(blob);

    RemoteInputStream ri = sb.openInputStream();
    assertTrue(ri instanceof ServerInputStream);
    assertEquals(((ServerInputStream) ri).getInputStream(), in);

    try {
      sb.openInputStream();
      fail("Failed to rcv expected exception");
    } catch (MissingBlobException e) {
      assertEquals(id, e.getBlobId());
    }

    verify(blob);
  }

  @Test
  public void testOpenOutputStream() throws IOException {
    URI          id  = URI.create("foo:bar");
    OutputStream out = createMock(OutputStream.class);
    reset(blob);
    expect(blob.openOutputStream(42L, true)).andReturn(out);
    expect(blob.openOutputStream(-1L, true)).andThrow(new MissingBlobException(id));
    replay(blob);

    RemoteOutputStream ro = sb.openOutputStream(42L, true);
    assertTrue(ro instanceof ServerOutputStream);
    assertEquals(((ServerOutputStream) ro).getOutputStream(), out);

    try {
      sb.openOutputStream(-1L, true);
      fail("Failed to rcv expected exception");
    } catch (MissingBlobException e) {
      assertEquals(id, e.getBlobId());
    }

    verify(blob);
  }
}
