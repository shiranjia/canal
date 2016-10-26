package com.alibaba.otter.canal.client;


import com.alibaba.otter.canal.client.impl.running.ServerInfo;

import java.net.SocketAddress;
import java.util.Map;

/**
 * 集群节点访问控制接口
 * 
 * @author jianghang 2012-10-29 下午07:55:41
 * @version 1.0.0
 */
public interface CanalNodeAccessStrategy {

    SocketAddress nextNode();

    Map<String,ServerInfo> getServerInfo();
}
