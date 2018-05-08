// Copyright (c) YugaByte, Inc.

package com.yugabyte.yw.commissioner.tasks.subtasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.yugabyte.yw.commissioner.tasks.UniverseTaskBase;
import com.yugabyte.yw.common.DnsManager;
import com.yugabyte.yw.common.ShellProcessHandler;
import com.yugabyte.yw.forms.ITaskParams;
import com.yugabyte.yw.forms.UniverseTaskParams;
import com.yugabyte.yw.models.Universe;
import com.yugabyte.yw.models.helpers.NodeDetails;

import play.api.Play;

public class ManipulateDnsRecordTask extends UniverseTaskBase {
  public static final Logger LOG = LoggerFactory.getLogger(ManipulateDnsRecordTask.class);

  private DnsManager dnsManager;

  public static class Params extends UniverseTaskParams {
    public DnsManager.DnsCommandType type;
    public UUID providerUUID;
    public String hostedZoneId;
    public String domainNamePrefix;
    public Boolean isForceDelete;
  }

  @Override
  public void initialize(ITaskParams params) {
    super.initialize(params);
    this.dnsManager = Play.current().injector().instanceOf(DnsManager.class);
  }

  @Override
  protected Params taskParams() {
    return (Params)taskParams;
  }

  @Override
  public void run() {
    try {
      List<NodeDetails> tserverNodes = Universe.get(taskParams().universeUUID).getTServers();
      String nodeIpCsv = tserverNodes.stream()
          .map(nd -> nd.cloudInfo.private_ip)
          .collect(Collectors.joining(","));
      // Create the process to fetch information about the node from the cloud provider.
      ShellProcessHandler.ShellResponse response = dnsManager.manipulateDnsRecord(
          taskParams().type,
          taskParams().providerUUID,
          taskParams().hostedZoneId,
          taskParams().domainNamePrefix,
          nodeIpCsv);
      logShellResponse(response);
    } catch (Exception e) {
      if (taskParams().type != DnsManager.DnsCommandType.Delete || !taskParams().isForceDelete) {
        throw e;
      }
    }
  }
}