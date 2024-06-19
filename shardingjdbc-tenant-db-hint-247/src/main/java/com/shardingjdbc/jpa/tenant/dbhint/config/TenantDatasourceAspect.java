package com.shardingjdbc.jpa.tenant.dbhint.config;

import com.zaxxer.hikari.HikariDataSource;
import org.apache.shardingsphere.driver.jdbc.core.datasource.ShardingSphereDataSource;
import org.apache.shardingsphere.infra.hint.HintManager;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Map;

/**
 * @author pdai
 */
@Aspect
@Order(1)
@Component
public class TenantDatasourceAspect {

    @Resource
    private DataSource dataSource;

    /**
     * point cut.
     */
    @Pointcut("execution(* com.shardingjdbc.jpa.tenant.dbhint.service.*.*(..))")
    public void useTenantDSPointCut() {
        // no impl
    }

    @Before("useTenantDSPointCut()")
    public void doDs0Before() {
        createShardingDataSource("tenant-c");

        HintManager hintManager = HintManager.getInstance();
        // pdai: 实际环境将 client 信息放在 xxxContext 中（由ThreadLocal承接），并通过 client-id 来获取 tenant.
        // 这里为了方便演示，只是使用了 tenant-c
        hintManager.setDatabaseShardingValue("tenant-c");
    }

    @After("useTenantDSPointCut()")
    public void doDs0after() {
        HintManager.clear();
    }

    private void createShardingDataSource(String tenantCode) {
        DynamicDataSource dynamicDataSource = (DynamicDataSource) dataSource;
        if (dynamicDataSource.existsTenantDataSource(tenantCode)) {
            return;
        }

        Map<String, DataSource> dataSourceMap = ShardingDataSourceConfig.createDataSourceMap();
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName("com.mysql.jdbc.Driver");
        dataSource.setJdbcUrl("jdbc:mysql://43.142.4.174:3306/test_db_tenant_c");
        dataSource.setUsername("dev");
        dataSource.setPassword("123456");
        dataSourceMap.put(tenantCode, dataSource);
        try {
            ShardingSphereDataSource shardingSphereDataSource =
                    ShardingDataSourceConfig.createShardingSphereDataSource(dataSourceMap,
                            Collections.singleton(ShardingDataSourceConfig.createShardingRuleConfiguration(dataSourceMap.keySet())));
            // 先关闭之前的 shardingDT
            ShardingSphereDataSource shardingDT = (ShardingSphereDataSource) dynamicDataSource.removeTargetDataSource("shardingDT");
            shardingDT.close();
            dynamicDataSource.addTargetDataSources("shardingDT", shardingSphereDataSource);
            // 缓存租户数据源
            dynamicDataSource.setTenantDataSource(tenantCode);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}

