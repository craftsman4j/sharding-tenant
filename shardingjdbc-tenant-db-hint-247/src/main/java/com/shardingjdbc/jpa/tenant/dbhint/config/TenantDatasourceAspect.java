package com.shardingjdbc.jpa.tenant.dbhint.config;

import cn.hutool.core.util.ReflectUtil;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.driver.jdbc.core.datasource.ShardingSphereDataSource;
import org.apache.shardingsphere.infra.datanode.DataNode;
import org.apache.shardingsphere.infra.hint.HintManager;
import org.apache.shardingsphere.infra.metadata.database.ShardingSphereDatabase;
import org.apache.shardingsphere.mode.manager.ContextManager;
import org.apache.shardingsphere.sharding.rule.ShardingRule;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

import static com.shardingjdbc.jpa.tenant.dbhint.config.CustomContextManagerLifecycleListener.CONTEXT_MANAGER_HOLDER;

/**
 * @author pdai
 */
@Aspect
@Order(1)
@Component
@Slf4j
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

    private void createShardingDataSource() {
        ContextManager contextManager = CONTEXT_MANAGER_HOLDER.get("logic_db");
        ShardingSphereDatabase database = contextManager.getDatabase("logic_db");
        database.getRuleMetaData().getRules().forEach(rule -> {
            if (rule instanceof ShardingRule) {
                ShardingRule shardingRule = ((ShardingRule) rule);
                shardingRule.getShardingTables().forEach((name, shardingTable) -> {
                    List<DataNode> actualDataNodes = shardingTable.getActualDataNodes();
                    actualDataNodes.add(new DataNode(actualDataNodes.get(0).getDataSourceName(), "order_new"));
                    actualDataNodes = new HashSet<>(actualDataNodes).stream().collect(Collectors.toList());
                    Set<String> actualTables = (Set<String>) ReflectUtil.getFieldValue(shardingTable, "actualTables");
                    actualTables.add("order_new");
                    Map<DataNode, Integer> dataNodeIndexMap = new HashMap<>();
                    Map<String, Collection<String>> dataSourceToTablesMap = shardingTable.getDataSourceToTablesMap();
                    for (int i = 0; i < actualDataNodes.size(); i++) {
                        dataNodeIndexMap.put(actualDataNodes.get(i), i);
                        dataSourceToTablesMap.get(actualDataNodes.get(i).getDataSourceName()).add(actualDataNodes.get(i).getTableName());
                    }
                    ReflectUtil.setFieldValue(shardingTable, "dataNodeIndexMap", dataNodeIndexMap);
                    ReflectUtil.setFieldValue(shardingTable, "dataSourceToTablesMap", dataSourceToTablesMap);
                    ReflectUtil.setFieldValue(shardingTable, "actualTables", actualTables);
                    ReflectUtil.setFieldValue(shardingTable, "actualDataNodes", actualDataNodes);
                });
                log.info("shardingRule:{}", shardingRule);
            }
        });
    }

}

