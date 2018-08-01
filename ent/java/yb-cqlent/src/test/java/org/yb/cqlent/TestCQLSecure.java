// Copyright (c) YugaByte, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software distributed under the License
// is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
// or implied.  See the License for the specific language governing permissions and limitations
// under the License.
//
package org.yb.cqlent;

import com.datastax.driver.core.*;
import com.datastax.driver.core.exceptions.TransportException;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.yb.client.TestUtils;
import org.yb.cql.*;

import java.io.*;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.security.*;
import java.util.*;

import javax.net.ssl.*;

import io.netty.handler.ssl.*;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

public class TestCQLSecure extends BaseCQLTest {
  public TestCQLSecure() {
    tserverArgs = new ArrayList<String>();
    tserverArgs.add("--use_client_to_server_encryption=true");
    tserverArgs.add(String.format("--certs_for_client_dir=%s", certsDir()));
  }

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    LOG.info("TestCQLSecure.setUpBeforeClass is running");

    System.setProperty("javax.net.ssl.trustStore", certsDir() + "/client.truststore");
    System.setProperty("javax.net.ssl.trustStorePassword", "password");
  }

  @Override
  public int getTestMethodTimeoutSec() {
    // No need to adjust for TSAN vs. non-TSAN here, it will be done automatically.
    return 240;
  }

  @Test
  public void testInsert() throws Exception {
    final int STRING_SIZE = 64;
    final String errorMessage = "YQL value too long";
    String tableName = "test_insert";
    String create_stmt = String.format(
        "CREATE TABLE %s (h int PRIMARY KEY, c varchar);", tableName);
    String exceptionString = null;
    session.execute(create_stmt);
    String ins_stmt = String.format("INSERT INTO %s (h, c) VALUES (1, ?);", tableName);
    String value = RandomStringUtils.randomAscii(STRING_SIZE);
    session.execute(ins_stmt, value);
    String sel_stmt = String.format("SELECT h, c FROM %s WHERE h = 1 ;", tableName);
    Row row = runSelect(sel_stmt).next();
    assertEquals(1, row.getInt(0));
    assertEquals(value, row.getString(1));
  }

  @Override
  public Cluster.Builder getDefaultClusterBuilder() {
    return super.getDefaultClusterBuilder().withSSL();
  }

  private static String certsDir() {
    return TestUtils.findYbRootDir() + "/build/latest/ent/test_certs";
  }
}