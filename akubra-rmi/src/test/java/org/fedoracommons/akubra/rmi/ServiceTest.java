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
package org.fedoracommons.akubra.rmi;

import static org.testng.Assert.assertEquals;

import java.io.IOException;

import java.net.ServerSocket;
import java.net.URI;
import java.net.URISyntaxException;

import java.rmi.NotBoundException;

import org.fedoracommons.akubra.BlobStore;
import org.fedoracommons.akubra.mem.MemBlobStore;

import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

/**
 * Tests export, locate and un-export of akubra-rmi server and client.
 *
 * @author Pradeep Krishnan
 */
public class ServiceTest {
  private MemBlobStore mem;
  private int          reg;

  /**
   * Setup before all tests.
   *
   */
  @BeforeSuite
  public void setUp() throws Exception {
    mem   = new MemBlobStore();
    reg   = freePort();
  }

  /**
   * Tear down after all tests.
   *
   */
  @AfterSuite
  public void tearDown() throws Exception {
  }

  /**
   * Tests export, access and unexport with defaults.
   *
   */
  @Test
  public void testDefault() throws NotBoundException, IOException {
    AkubraRMIServer.export(mem);

    try {
      BlobStore store = AkubraRMIClient.create();
      assertEquals(mem.getCapabilities(), store.getCapabilities());
    } finally {
      AkubraRMIServer.unExport();
    }
  }

  /**
   * Tests export, access and unexport with a specific registry port.
   *
   */
  @Test
  public void testWithPort() throws NotBoundException, IOException {
    AkubraRMIServer.export(mem, reg);

    try {
      BlobStore store = AkubraRMIClient.create(reg);
      assertEquals(mem.getCapabilities(), store.getCapabilities());
    } finally {
      AkubraRMIServer.unExport(reg);
    }
  }

  /**
   * Tests export, access and unexport with a specific registry port and a specific service
   * name.
   *
   */
  @Test
  public void testWithNameAndPort() throws NotBoundException, IOException, URISyntaxException {
    AkubraRMIServer.export(mem, "service-test-with-name-and-port", reg);

    try {
      BlobStore store = AkubraRMIClient.create("service-test-with-name-and-port", reg);
      assertEquals(mem.getCapabilities(), store.getCapabilities());
    } finally {
      AkubraRMIServer.unExport("service-test-with-name-and-port", reg);
    }
  }

  /**
   * Tests export, access and unexport with a specific registry port and a specific service
   * name and with different ports for RMI registry and akubra-rmi-server.
   *
   */
  @Test
  public void testWithNameAndAlternatePorts()
                                     throws NotBoundException, IOException, URISyntaxException {
    AkubraRMIServer.export(mem, "test-with-name-and-alternate-ports", reg, freePort());

    try {
      BlobStore store = AkubraRMIClient.create("test-with-name-and-alternate-ports", reg);
      assertEquals(mem.getCapabilities(), store.getCapabilities());
    } finally {
      AkubraRMIServer.unExport("test-with-name-and-alternate-ports", reg);
    }
  }

  /**
   * Tests export, access and unexport with a specific registry port and a specific service
   * name. and with different ports for RMI registry and akubra-rmi-server.
   *
   */
  @Test
  public void testLookupByUri() throws NotBoundException, IOException, URISyntaxException {
    AkubraRMIServer.export(mem, "test-uri-lookup", reg, freePort());

    try {
      BlobStore store =
        AkubraRMIClient.create(URI.create("rmi://localhost:" + reg + "/test-uri-lookup"));
      assertEquals(mem.getCapabilities(), store.getCapabilities());
    } finally {
      AkubraRMIServer.unExport("test-uri-lookup", reg);
    }
  }

  private int freePort() throws IOException {
    ServerSocket s    = new ServerSocket(0);
    int          port = s.getLocalPort();
    s.close();

    return port;
  }
}
