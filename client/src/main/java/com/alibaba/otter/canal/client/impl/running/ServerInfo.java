package com.alibaba.otter.canal.client.impl.running;

import com.alibaba.otter.canal.common.zookeeper.running.ServerRunningData;

import java.util.List;

/**
 * Created by jiashiran on 2016/10/24.
 */
public class ServerInfo {

    private List<ServerRunningData> serverRunningDatas;


    public List<ServerRunningData> getServerRunningDatas() {
        return serverRunningDatas;
    }

    public void setServerRunningDatas(List<ServerRunningData> serverRunningDatas) {
        this.serverRunningDatas = serverRunningDatas;
    }
}
