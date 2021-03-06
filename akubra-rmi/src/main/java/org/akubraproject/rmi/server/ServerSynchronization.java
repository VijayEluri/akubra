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

import java.rmi.RemoteException;

import javax.transaction.Synchronization;

import org.akubraproject.rmi.remote.RemoteSynchronization;

/**
 * Server side implementation of a Transaction Synchronization listener.
 *
 * @author Pradeep Krishnan
  */
public class ServerSynchronization extends UnicastExportable implements RemoteSynchronization {
  private static final long     serialVersionUID = 1L;
  private final Synchronization sync;

  /**
   * Creates a new ServerSynchronization object.
   *
   * @param sync the real listener to forward events to
   * @param exporter the exporter to use
   *
   * @throws RemoteException on an error in export
   */
  public ServerSynchronization(Synchronization sync, Exporter exporter)
                        throws RemoteException {
    super(exporter);
    this.sync = sync;
  }

  public void afterCompletion(int status) {
    sync.afterCompletion(status);
  }

  public void beforeCompletion() {
    sync.beforeCompletion();
  }

  // for testing
  Synchronization getSynchronization() {
    return sync;
  }
}
