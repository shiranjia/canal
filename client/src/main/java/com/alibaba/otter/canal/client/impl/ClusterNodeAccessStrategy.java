package com.alibaba.otter.canal.client.impl;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.alibaba.fastjson.JSON;
import com.alibaba.otter.canal.client.impl.running.ServerInfo;
import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.IZkDataListener;
import org.apache.commons.lang.StringUtils;

import com.alibaba.otter.canal.client.CanalNodeAccessStrategy;
import com.alibaba.otter.canal.common.utils.JsonUtils;
import com.alibaba.otter.canal.common.zookeeper.ZkClientx;
import com.alibaba.otter.canal.common.zookeeper.ZookeeperPathUtils;
import com.alibaba.otter.canal.common.zookeeper.running.ServerRunningData;
import com.alibaba.otter.canal.protocol.exception.CanalClientException;

/**
 * 集群模式的调度策略
 * 
 * @author jianghang 2012-12-3 下午10:01:04
 * @version 1.0.0
 */
public class ClusterNodeAccessStrategy implements CanalNodeAccessStrategy {

    private IZkChildListener                 childListener;                                      // 监听所有的服务器列表
    private IZkDataListener                  dataListener;                                       // 监听当前的工作节点
    private ZkClientx                        zkClient;
    private volatile List<InetSocketAddress> currentAddress = new ArrayList<InetSocketAddress>();
    private volatile InetSocketAddress       runningAddress = null;

    public ClusterNodeAccessStrategy(String destination, ZkClientx zkClient){
        this.zkClient = zkClient;
        childListener = new IZkChildListener() {

            public void handleChildChange(String parentPath, List<String> currentChilds) throws Exception {
                initClusters(currentChilds);
            }

        };

        dataListener = new IZkDataListener() {

            public void handleDataDeleted(String dataPath) throws Exception {
                runningAddress = null;
            }

            public void handleDataChange(String dataPath, Object data) throws Exception {
                initRunning(data);
            }

        };

        String clusterPath = ZookeeperPathUtils.getDestinationClusterRoot(destination);
        this.zkClient.subscribeChildChanges(clusterPath, childListener);
        initClusters(this.zkClient.getChildren(clusterPath));

        String runningPath = ZookeeperPathUtils.getDestinationServerRunning(destination);
        this.zkClient.subscribeDataChanges(runningPath, dataListener);
        initRunning(this.zkClient.readData(runningPath, true));
    }

    public SocketAddress nextNode() {
        if (runningAddress != null) {// 如果服务已经启动，直接选择当前正在工作的节点
            return runningAddress;
        } else if (!currentAddress.isEmpty()) { // 如果不存在已经启动的服务，可能服务是一种lazy启动，随机选择一台触发服务器进行启动
            return currentAddress.get(0);// 默认返回第一个节点，之前已经做过shuffle
        } else {
            throw new CanalClientException("no alive canal server");
        }
    }

    @Override
    public ServerInfo getServerInfo() {
        ServerInfo serverData = new ServerInfo();
        List<ServerRunningData> serverRunningDatas = new ArrayList<ServerRunningData>();
        if(zkClient.exists(ZookeeperPathUtils.DESTINATION_ROOT_NODE)){
            List<String> destinations = zkClient.getChildren(ZookeeperPathUtils.DESTINATION_ROOT_NODE);
            for (String d : destinations){
                d = ZookeeperPathUtils.DESTINATION_ROOT_NODE + "/" + d ;
                ServerRunningData serverRunningData = getObject(zkClient , d + "/running", ServerRunningData.class);
                if(serverRunningData != null){
                    serverRunningDatas.add(serverRunningData);
                }
                //ClientRunningData clientRunningData = getObject(zkClient, d + "/1001/running", ClientRunningData.class);

                //LogPosition logPosition = getObject(zkClient, d + "/1001/cursor", LogPosition.class);
            }
        }
        serverData.setServerRunningDatas(serverRunningDatas);

        return serverData;
    }

    public static <T> T getObject(ZkClientx zkClientx, String path, Class<T> cla) {
        String canalRun = getData(zkClientx, path);
        if (canalRun != null && canalRun.length() > 0) {
            try {
                return JSON.parseObject(canalRun, cla);
            } catch (Exception e) {
            }
        }
        return null;
    }

    public static String getData(ZkClientx zkClientx, String path) {
        byte[] ds = zkClientx.readData(path, true);
        if (null != ds && ds.length > 0) {
            return new String(ds);
        }
        return "";
    }

    private void initClusters(List<String> currentChilds) {
        if (currentChilds == null || currentChilds.isEmpty()) {
            currentAddress = new ArrayList<InetSocketAddress>();
        } else {
            List<InetSocketAddress> addresses = new ArrayList<InetSocketAddress>();
            for (String address : currentChilds) {
                String[] strs = StringUtils.split(address, ":");
                if (strs != null && strs.length == 2) {
                    addresses.add(new InetSocketAddress(strs[0], Integer.valueOf(strs[1])));
                }
            }

            Collections.shuffle(addresses);
            currentAddress = addresses;// 直接切换引用
        }
    }

    private void initRunning(Object data) {
        if (data == null) {
            return;
        }

        ServerRunningData runningData = JsonUtils.unmarshalFromByte((byte[]) data, ServerRunningData.class);
        String[] strs = StringUtils.split(runningData.getAddress(), ':');
        if (strs.length == 2) {
            runningAddress = new InetSocketAddress(strs[0], Integer.valueOf(strs[1]));
        }
    }

    public void setZkClient(ZkClientx zkClient) {
        this.zkClient = zkClient;
    }

    public ZkClientx getZkClient() {
        return zkClient;
    }

}
