/* $HeadURL$
 * $Id$
 *
 * Copyright (c) 2007-2008 by Fedora Commons Inc.
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
package org.fedoracommons.akubra.fs;

import java.io.File;

import java.net.URI;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link FSBlobIdIterator}.
 *
 * @author Chris Wilper
 */
public class TestFSBlobIdIterator {
  private static File tmpDir;
  private static File emptyDir;
  private static File multiDir;

  /**
   * Sets up the test directories.
   *
   * @throws Exception if setup fails.
   */
  @BeforeClass
  public static void init() throws Exception {
    // create new tmpDir in java.io.tmpdir
    File tempFile = File.createTempFile("akubra-test", null);
    tmpDir = new File(tempFile.getPath());
    tempFile.delete();
    tmpDir.mkdir();
    // setup dirs within for tests
    emptyDir = new File(tmpDir, "empty");
    emptyDir.mkdir();
    multiDir = new File(tmpDir, "multi");
    multiDir.mkdir();
    add(multiDir, "file-1");
    add(multiDir, "file-2");
    add(multiDir, "dir-empty/");
    add(multiDir, "dir-nonempty/");
    add(multiDir, "dir-nonempty/file-3");
    add(multiDir, "dir-nonempty/file-4");
    add(multiDir, "dir-nonempty/subdir/");
    add(multiDir, "dir-nonempty/subdir/file-5");
    add(multiDir, "dir-nonempty/subdir/file-6");
  }

  /**
   * Removes the test directories.
   */
  @AfterClass
  public static void destroy() {
    rmdir(tmpDir);
  }

  /**
   * An empty dir should result in an empty iterator.
   */
  @Test
  public void testEmpty() {
    assertEquals(0, getSet(getIter(emptyDir, null)).size());
  }

  /**
   * A populated dir should result in an iterator with an item for each file.
   */
  @Test
  public void testMulti() {
    assertEquals(6, getSet(getIter(multiDir, null)).size());
  }

  /**
   * Prefix filters should be respected.
   */
  @Test
  public void testMultiWithFilter() {
    assertEquals(6, getSet(getIter(multiDir, "file:///")).size());
    String prefix = FSBlobStoreConnection.getBlobIdPrefix(multiDir);
    assertEquals(1, getSet(getIter(multiDir, prefix + "file-1")).size());
    assertEquals(0, getSet(getIter(multiDir, prefix + "dir-e")).size());
    assertEquals(4, getSet(getIter(multiDir, prefix + "dir-n")).size());
  }

  private static FSBlobIdIterator getIter(File dir, String filterPrefix) {
    return new FSBlobIdIterator(dir, filterPrefix);
  }

  private static Set<URI> getSet(Iterator<URI> iter) {
    HashSet<URI> set = new HashSet<URI>();
    while (iter.hasNext()) {
      set.add(iter.next());
    }
    return set;
  }

  // create an empty file or dir with the given name in the given dir
  private static void add(File dir, String name) throws Exception {
    File file = new File(dir, name);
    if (name.endsWith("/")) {
      file.mkdir();
    } else {
      file.createNewFile();
    }
  }

  // rm -rf dir
  private static void rmdir(File dir) {
    for (File file : dir.listFiles()) {
      if (file.isDirectory()) {
        rmdir(file);
      } else {
        file.delete();
      }
    }
    dir.delete();
  }

}
