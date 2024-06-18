package com.shardingjdbc.jpa.tenant.dbhint.config;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DynamicDataSource extends AbstractRoutingDataSource {

    private final Map<Object, Object> TARGET_DATA_SOURCES = new HashMap<>();

    private final Map<String, Object> TENANT_DATA_SOURCE = new ConcurrentHashMap<>();

    private static final ThreadLocal<String> DATA_SOURCE_NAME = new ThreadLocal<>();

    public static void setDataSource(String name) {
        DATA_SOURCE_NAME.set(name);
    }

    public static void clear() {
        DATA_SOURCE_NAME.remove();
    }

    public DynamicDataSource() {
    }

    private static final class DynamicDataSourceHolder {
        static final DynamicDataSource DYNAMIC_DATA_SOURCE = new DynamicDataSource();
    }

    public static DynamicDataSource getInstance() {
        return DynamicDataSourceHolder.DYNAMIC_DATA_SOURCE;
    }

    /**
     * determineCurrentLookupKey() 方法决定使用哪个数据源
     */
    @Override
    protected Object determineCurrentLookupKey() {
        return DATA_SOURCE_NAME.get();
    }

    public void setTargetDataSources(Map<Object, Object> targetDataSources) {
        // 设置默认数据源
        //super.setDefaultTargetDataSource(targetDataSources.get("default"));
        this.TARGET_DATA_SOURCES.putAll(targetDataSources);
        // 设置数据源
        super.setTargetDataSources(this.TARGET_DATA_SOURCES);
        super.afterPropertiesSet();
    }

    public Map<Object, Object> getTargetDataSources() {
        return this.TARGET_DATA_SOURCES;
    }

    public void addTargetDataSources(String name, DataSource dataSource) {
        // 设置默认数据源
        this.TARGET_DATA_SOURCES.put(name, dataSource);
        // 设置数据源
        super.setTargetDataSources(this.TARGET_DATA_SOURCES);
        super.afterPropertiesSet();
    }

    public DataSource removeTargetDataSource(String name) {
        DataSource dataSource = (DataSource) this.TARGET_DATA_SOURCES.get(name);
        if (dataSource != null) {
            this.TARGET_DATA_SOURCES.remove(name);
            // 重新设置数据源
            super.setTargetDataSources(this.TARGET_DATA_SOURCES);
            super.afterPropertiesSet();
        }
        return dataSource;
    }

    /************************ 存储租户数据源 ***************************/
    public void setTenantDataSource(String name) {
        TENANT_DATA_SOURCE.put(name, "1");
    }

    public void removeTenantDataSource(String name) {
        TENANT_DATA_SOURCE.remove(name);
    }

    public boolean existsTenantDataSource(String name) {
        return TENANT_DATA_SOURCE.containsKey(name);
    }

}
