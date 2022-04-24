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

package org.apache.shardingsphere.dbdiscovery.algorithm;

import com.google.common.base.Preconditions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.dbdiscovery.spi.DatabaseDiscoveryProviderAlgorithm;
import org.apache.shardingsphere.dbdiscovery.spi.status.HighlyAvailableStatus;
import org.apache.shardingsphere.dbdiscovery.spi.status.type.GlobalHighlyAvailableStatus;
import org.apache.shardingsphere.dbdiscovery.spi.status.type.RoleSeparatedHighlyAvailableStatus;
import org.apache.shardingsphere.infra.eventbus.ShardingSphereEventBus;
import org.apache.shardingsphere.infra.metadata.schema.QualifiedDatabase;
import org.apache.shardingsphere.infra.rule.event.impl.DataSourceDisabledEvent;
import org.apache.shardingsphere.infra.rule.event.impl.PrimaryDataSourceChangedEvent;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

/**
 * Database discovery engine.
 */
@RequiredArgsConstructor
@Slf4j
public final class DatabaseDiscoveryEngine {
    
    private final DatabaseDiscoveryProviderAlgorithm databaseDiscoveryProviderAlgorithm;
    
    /**
     * Check highly available status of database cluster.
     *
     * @param databaseName database name
     * @param dataSourceMap data source map
     * @throws SQLException SQL exception
     */
    public void checkHighlyAvailableStatus(final String databaseName, final Map<String, DataSource> dataSourceMap) throws SQLException {
        Collection<HighlyAvailableStatus> statuses = loadHighlyAvailableStatuses(dataSourceMap);
        Preconditions.checkState(!statuses.isEmpty(), "No database instance in database cluster `%s`.", databaseName);
        HighlyAvailableStatus sample = statuses.iterator().next();
        if (sample instanceof GlobalHighlyAvailableStatus) {
            checkGlobalHighlyAvailableStatus(databaseName, dataSourceMap, statuses);
        } else if (sample instanceof RoleSeparatedHighlyAvailableStatus) {
            checkRoleSeparatedHighlyAvailableStatus(databaseName, dataSourceMap, statuses);
        }
    }
    
    private Collection<HighlyAvailableStatus> loadHighlyAvailableStatuses(final Map<String, DataSource> dataSourceMap) throws SQLException {
        Collection<HighlyAvailableStatus> result = new HashSet<>();
        for (Entry<String, DataSource> entry : dataSourceMap.entrySet()) {
            // TODO query with multiple threads
            result.add(databaseDiscoveryProviderAlgorithm.loadHighlyAvailableStatus(entry.getValue()));
        }
        return result;
    }
    
    private void checkGlobalHighlyAvailableStatus(final String databaseName, final Map<String, DataSource> dataSourceMap, final Collection<HighlyAvailableStatus> statuses) throws SQLException {
        Preconditions.checkState(1 == statuses.size(), "Different status in highly available cluster in database `%s`.", databaseName);
        statuses.iterator().next().validate(databaseName, dataSourceMap, databaseDiscoveryProviderAlgorithm.getProps());
    }
    
    private void checkRoleSeparatedHighlyAvailableStatus(final String databaseName,
                                                         final Map<String, DataSource> dataSourceMap, final Collection<HighlyAvailableStatus> statuses) throws SQLException {
        for (HighlyAvailableStatus each : statuses) {
            each.validate(databaseName, dataSourceMap, databaseDiscoveryProviderAlgorithm.getProps());
        }
    }
    
    /**
     * Change primary data source.
     *
     * @param databaseName database name
     * @param groupName group name
     * @param originalPrimaryDataSourceName original primary data source name
     * @param dataSourceMap data source map
     * @param disabledDataSourceNames disabled data source names
     * @return changed primary data source name
     */
    public String changePrimaryDataSource(final String databaseName, final String groupName, final String originalPrimaryDataSourceName,
                                          final Map<String, DataSource> dataSourceMap, final Collection<String> disabledDataSourceNames) {
        Optional<String> newPrimaryDataSourceName = findPrimaryDataSourceName(dataSourceMap, disabledDataSourceNames);
        if (newPrimaryDataSourceName.isPresent() && !newPrimaryDataSourceName.get().equals(originalPrimaryDataSourceName)) {
            ShardingSphereEventBus.getInstance().post(new PrimaryDataSourceChangedEvent(new QualifiedDatabase(databaseName, groupName, newPrimaryDataSourceName.get())));
        }
        String result = newPrimaryDataSourceName.orElse(originalPrimaryDataSourceName);
        postReplicaDataSourceDisabledEvent(databaseName, groupName, result, dataSourceMap);
        return result;
    }
    
    private Optional<String> findPrimaryDataSourceName(final Map<String, DataSource> dataSourceMap, final Collection<String> disabledDataSourceNames) {
        for (Entry<String, DataSource> entry : getActiveDataSourceMap(dataSourceMap, disabledDataSourceNames).entrySet()) {
            boolean isPrimaryInstance = databaseDiscoveryProviderAlgorithm.isPrimaryInstance(entry.getValue());
            if (isPrimaryInstance) {
                return Optional.of(entry.getKey());
            }
        }
        return Optional.empty();
    }
    
    private Map<String, DataSource> getActiveDataSourceMap(final Map<String, DataSource> dataSourceMap, final Collection<String> disabledDataSourceNames) {
        Map<String, DataSource> result = new HashMap<>(dataSourceMap);
        if (!disabledDataSourceNames.isEmpty()) {
            result.entrySet().removeIf(each -> disabledDataSourceNames.contains(each.getKey()));
        }
        return result;
    }
    
    private void postReplicaDataSourceDisabledEvent(final String databaseName, final String groupName, final String primaryDataSourceName, final Map<String, DataSource> dataSourceMap) {
        for (Entry<String, DataSource> entry : dataSourceMap.entrySet()) {
            if (!entry.getKey().equals(primaryDataSourceName)) {
                ShardingSphereEventBus.getInstance().post(
                        new DataSourceDisabledEvent(databaseName, groupName, entry.getKey(), databaseDiscoveryProviderAlgorithm.getStorageNodeDataSource(entry.getValue())));
            }
        }
    }
}