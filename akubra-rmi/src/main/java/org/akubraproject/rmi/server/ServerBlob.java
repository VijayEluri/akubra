/* $HeadURL::                                                                            $
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

import java.io.IOException;

import java.net.URI;

import java.rmi.RemoteException;

import java.util.Map;

import org.akubraproject.Blob;
import org.akubraproject.rmi.remote.RemoteBlob;
import org.akubraproject.rmi.remote.RemoteInputStream;
import org.akubraproject.rmi.remote.RemoteOutputStream;

/**
 * Server side implementation of the RemoteBlob.
 *
 * @author Pradeep Krishnan
 */
public class ServerBlob extends UnicastExportable implements RemoteBlob {
  private static final long serialVersionUID = 1L;
  private final Blob        blob;

  /**
   * Creates a new ServerBlob object.
   *
   * @param blob the real blob
   * @param exporter the exporter to use
   *
   * @throws RemoteException on an error in export
   */
  public ServerBlob(Blob blob, Exporter exporter) throws RemoteException {
    super(exporter);
    this.blob = blob;
  }

  @Override
  public URI getId() {
    return blob.getId();
  }

  @Override
  public URI getCanonicalId() throws IOException {
    return blob.getCanonicalId();
  }

  @Override
  public boolean exists() throws IOException {
    return blob.exists();
  }

  @Override
  public long getSize() throws IOException {
    return blob.getSize();
  }

  @Override
  public void delete() throws IOException {
    blob.delete();
  }

  @Override
  public RemoteBlob moveTo(URI other, Map<Object, Object> hints) throws IOException {
    return new ServerBlob(blob.moveTo(other, hints), getExporter());
  }

  @Override
  public RemoteInputStream openInputStream() throws IOException {
    return new ServerInputStream(blob.openInputStream(), getExporter());
  }

  @Override
  public RemoteOutputStream openOutputStream(long estimatedSize, boolean overwrite)
      throws IOException {
    return new ServerOutputStream(blob.openOutputStream(estimatedSize, overwrite), getExporter());
  }

  // for tests
  Blob getBlob() {
    return blob;
  }
}
