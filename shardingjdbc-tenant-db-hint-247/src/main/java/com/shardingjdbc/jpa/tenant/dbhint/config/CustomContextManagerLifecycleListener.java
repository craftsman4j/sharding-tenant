package com.shardingjdbc.jpa.tenant.dbhint.config;

import org.apache.shardingsphere.infra.instance.metadata.InstanceType;
import org.apache.shardingsphere.mode.manager.ContextManager;
import org.apache.shardingsphere.mode.manager.listener.ContextManagerLifecycleListener;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CustomContextManagerLifecycleListener implements ContextManagerLifecycleListener {
	public static final Map<String, ContextManager> CONTEXT_MANAGER_HOLDER = new ConcurrentHashMap<>();
	@Override
	public void onInitialized(String databaseName, ContextManager contextManager) {
		CONTEXT_MANAGER_HOLDER.put(databaseName, contextManager);
	}

	@Override
	public void onDestroyed(String databaseName, InstanceType instanceType) {
		CONTEXT_MANAGER_HOLDER.remove(databaseName);
	}
}