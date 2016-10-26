package com.alibaba.otter.canal.client.impl.running;

import com.alibaba.otter.canal.common.zookeeper.running.ServerRunningData;

import java.util.List;

/**
 * Created by jiashiran on 2016/10/24.
 */
public class ServerInfo {

    private ServerRunningData serverRunningData;

    private List<String> activeServer;



    public List<String> getActiveServer() {
        return activeServer;
    }

    public void setActiveServer(List<String> activeServer) {
        this.activeServer = activeServer;
    }

    public ServerRunningData getServerRunningData() {
        return serverRunningData;
    }

    public void setServerRunningData(ServerRunningData serverRunningData) {
        this.serverRunningData = serverRunningData;
    }
}
