/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.sharding.yaml.swapper.rule;

import org.apache.shardingsphere.infra.yaml.config.swapper.YamlConfigurationSwapper;
import org.apache.shardingsphere.sharding.api.config.rule.ShardingTableRuleConfiguration;
import org.apache.shardingsphere.sharding.api.config.strategy.audit.ShardingAuditStrategyConfiguration;
import org.apache.shardingsphere.sharding.api.config.strategy.keygen.KeyGenerateStrategyConfiguration;
import org.apache.shardingsphere.sharding.api.config.strategy.sharding.ShardingStrategyConfiguration;
import org.apache.shardingsphere.sharding.api.config.strategy.sharding.StandardShardingStrategyConfiguration;
import org.apache.shardingsphere.sharding.yaml.config.rule.YamlTableRuleConfiguration;
import org.apache.shardingsphere.sharding.yaml.config.strategy.audit.YamlShardingAuditStrategyConfiguration;
import org.apache.shardingsphere.sharding.yaml.config.strategy.keygen.YamlKeyGenerateStrategyConfiguration;
import org.apache.shardingsphere.sharding.yaml.config.strategy.sharding.YamlShardingStrategyConfiguration;
import org.apache.shardingsphere.sharding.yaml.swapper.strategy.YamlKeyGenerateStrategyConfigurationSwapper;
import org.apache.shardingsphere.sharding.yaml.swapper.strategy.YamlShardingStrategyConfigurationSwapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.reflect.Field;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public final class YamlShardingTableRuleConfigurationSwapperTest {
    
    private final YamlShardingTableRuleConfigurationSwapper swapper = new YamlShardingTableRuleConfigurationSwapper();
    
    @Mock
    private YamlShardingStrategyConfigurationSwapper shardingStrategySwapper;
    
    @Mock
    private YamlKeyGenerateStrategyConfigurationSwapper keyGenerateStrategySwapper;
    
    @Before
    public void setUp() throws ReflectiveOperationException {
        setSwapper("shardingStrategySwapper", shardingStrategySwapper);
        when(shardingStrategySwapper.swapToYamlConfiguration(ArgumentMatchers.any())).thenReturn(mock(YamlShardingStrategyConfiguration.class));
        when(shardingStrategySwapper.swapToObject(ArgumentMatchers.any())).thenReturn(mock(ShardingStrategyConfiguration.class));
        setSwapper("keyGenerateStrategySwapper", keyGenerateStrategySwapper);
        when(keyGenerateStrategySwapper.swapToYamlConfiguration(ArgumentMatchers.any())).thenReturn(mock(YamlKeyGenerateStrategyConfiguration.class));
        when(keyGenerateStrategySwapper.swapToObject(ArgumentMatchers.any())).thenReturn(mock(KeyGenerateStrategyConfiguration.class));
    }
    
    private void setSwapper(final String swapperFieldName, final YamlConfigurationSwapper swapperFieldValue) throws ReflectiveOperationException {
        Field field = YamlShardingTableRuleConfigurationSwapper.class.getDeclaredField(swapperFieldName);
        field.setAccessible(true);
        field.set(swapper, swapperFieldValue);
    }
    
    @Test
    public void assertSwapToYamlWithMinProperties() {
        YamlTableRuleConfiguration actual = swapper.swapToYamlConfiguration(new ShardingTableRuleConfiguration("tbl", "ds_$->{0..1}.tbl_$->{0..1}"));
        assertThat(actual.getLogicTable(), is("tbl"));
        assertThat(actual.getActualDataNodes(), is("ds_$->{0..1}.tbl_$->{0..1}"));
        assertNull(actual.getDatabaseStrategy());
        assertNull(actual.getTableStrategy());
        assertNull(actual.getKeyGenerateStrategy());
        assertNull(actual.getAuditStrategy());
    }
    
    @Test
    public void assertSwapToYamlWithMaxProperties() {
        ShardingTableRuleConfiguration shardingTableRuleConfig = new ShardingTableRuleConfiguration("tbl", "ds_$->{0..1}.tbl_$->{0..1}");
        shardingTableRuleConfig.setDatabaseShardingStrategy(mock(StandardShardingStrategyConfiguration.class));
        shardingTableRuleConfig.setTableShardingStrategy(mock(StandardShardingStrategyConfiguration.class));
        shardingTableRuleConfig.setKeyGenerateStrategy(mock(KeyGenerateStrategyConfiguration.class));
        shardingTableRuleConfig.setAuditStrategy(mock(ShardingAuditStrategyConfiguration.class));
        YamlTableRuleConfiguration actual = swapper.swapToYamlConfiguration(shardingTableRuleConfig);
        assertThat(actual.getLogicTable(), is("tbl"));
        assertThat(actual.getActualDataNodes(), is("ds_$->{0..1}.tbl_$->{0..1}"));
        assertNotNull(actual.getDatabaseStrategy());
        assertNotNull(actual.getTableStrategy());
        assertNotNull(actual.getKeyGenerateStrategy());
        assertNotNull(actual.getAuditStrategy());
    }
    
    @Test(expected = NullPointerException.class)
    public void assertSwapToObjectWithoutLogicTable() {
        new YamlShardingTableRuleConfigurationSwapper().swapToObject(new YamlTableRuleConfiguration());
    }
    
    @Test
    public void assertSwapToObjectWithMinProperties() {
        YamlTableRuleConfiguration yamlConfig = new YamlTableRuleConfiguration();
        yamlConfig.setLogicTable("tbl");
        yamlConfig.setActualDataNodes("ds_$->{0..1}.tbl_$->{0..1}");
        ShardingTableRuleConfiguration actual = swapper.swapToObject(yamlConfig);
        assertThat(actual.getLogicTable(), is("tbl"));
        assertThat(actual.getActualDataNodes(), is("ds_$->{0..1}.tbl_$->{0..1}"));
        assertNull(actual.getDatabaseShardingStrategy());
        assertNull(actual.getTableShardingStrategy());
        assertNull(actual.getKeyGenerateStrategy());
    }
    
    @Test
    public void assertSwapToObjectWithMaxProperties() {
        YamlTableRuleConfiguration yamlConfig = new YamlTableRuleConfiguration();
        yamlConfig.setLogicTable("tbl");
        yamlConfig.setActualDataNodes("ds_$->{0..1}.tbl_$->{0..1}");
        yamlConfig.setDatabaseStrategy(mock(YamlShardingStrategyConfiguration.class));
        yamlConfig.setTableStrategy(mock(YamlShardingStrategyConfiguration.class));
        yamlConfig.setKeyGenerateStrategy(mock(YamlKeyGenerateStrategyConfiguration.class));
        yamlConfig.setAuditStrategy(mock(YamlShardingAuditStrategyConfiguration.class));
        ShardingTableRuleConfiguration actual = swapper.swapToObject(yamlConfig);
        assertThat(actual.getLogicTable(), is("tbl"));
        assertThat(actual.getActualDataNodes(), is("ds_$->{0..1}.tbl_$->{0..1}"));
        assertNotNull(actual.getDatabaseShardingStrategy());
        assertNotNull(actual.getTableShardingStrategy());
        assertNotNull(actual.getKeyGenerateStrategy());
        assertNotNull(actual.getAuditStrategy());
    }
}
