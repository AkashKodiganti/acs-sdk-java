/*******************************************************************************
 * Copyright (c) 2018 Arrow Electronics, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License 2.0
 * which accompanies this distribution, and is available at
 * http://apache.org/licenses/LICENSE-2.0
 *
 * Contributors:
 *     Arrow Electronics, Inc.
 *******************************************************************************/
package com.arrow.acs.client.api;

import java.io.IOException;

import javax.annotation.PreDestroy;

import org.apache.http.config.SocketConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import com.arrow.acs.AcsUtils;
import com.arrow.acs.Loggable;

public class ConnectionManager extends Loggable {

    private static final int DEFAULT_MAX_TOTAL_CONNECTIONS = 500;
    private static final int DEFAULT_MAX_PER_ROUTE_CONNECTIONS = 500;
    private static final int DEFAULT_VALIDATE_AFTER_INACTIVITY_MS = 5000;
    private static final int DEFAULT_SOCKET_TIMEOUT_MS = 600000;

    private static final class SingletonHolder {
        static final ConnectionManager SINGLETON = new ConnectionManager();
    }

    public static ConnectionManager getInstance() {
        return SingletonHolder.SINGLETON;
    }

    private PoolingHttpClientConnectionManager connectionManager;

    private int maxTotalConnections = DEFAULT_MAX_TOTAL_CONNECTIONS;
    private int maxPerRouteConnections = DEFAULT_MAX_PER_ROUTE_CONNECTIONS;

    private ConnectionManager() {
        String method = "ConnectionManager";
        logInfo(method, "...");
        connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(maxTotalConnections);
        connectionManager.setDefaultMaxPerRoute(maxPerRouteConnections);
        connectionManager.setValidateAfterInactivity(DEFAULT_VALIDATE_AFTER_INACTIVITY_MS);
        connectionManager.setDefaultSocketConfig(
                SocketConfig.custom().setSoKeepAlive(true).setSoTimeout(DEFAULT_SOCKET_TIMEOUT_MS).build());

        // instantiate one
        try (CloseableHttpClient client = getConnection()) {
            logInfo(method, "got connection!");
        } catch (IOException e) {
            logError(method, e);
        }

        logInfo(method, "ready");
    }

    public CloseableHttpClient getConnection() {
        AcsUtils.notNull(connectionManager, "connection manager is not available");
        return HttpClients.custom().setConnectionManager(connectionManager).setConnectionManagerShared(true).build();
    }

    public int getMaxTotalConnections() {
        return maxTotalConnections;
    }

    public void setMaxTotalConnections(int maxTotalConnections) {
        this.maxTotalConnections = maxTotalConnections;
    }

    public int getMaxPerRouteConnections() {
        return maxPerRouteConnections;
    }

    public void setMaxPerRouteConnections(int maxPerRouteConnections) {
        this.maxPerRouteConnections = maxPerRouteConnections;
    }

    @PreDestroy
    public void shutdown() {
        String method = "shutdown";
        if (connectionManager != null) {
            try {
                logInfo(method, "shutting down connectionManager ...");
                connectionManager.close();
                connectionManager = null;
            } catch (Throwable t) {
            }
        }
    }
}
