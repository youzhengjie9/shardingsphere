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

package org.apache.shardingsphere.shadow.yaml.swapper.table;

import org.apache.shardingsphere.shadow.api.config.table.ShadowTableConfiguration;
import org.apache.shardingsphere.shadow.yaml.config.table.YamlShadowTableConfiguration;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public final class YamlShadowTableConfigurationSwapperTest {
    
    @Test
    public void assertSwapToYamlConfiguration() {
        ShadowTableConfiguration shadowTableConfig = new ShadowTableConfiguration(Collections.singletonList("shadow-data-source"), Arrays.asList("t_order", "t_user"));
        YamlShadowTableConfiguration yamlShadowTableConfig = new YamlShadowTableConfigurationSwapper().swapToYamlConfiguration(shadowTableConfig);
        assertThat(yamlShadowTableConfig.getShadowAlgorithmNames(), is(shadowTableConfig.getShadowAlgorithmNames()));
        assertThat(yamlShadowTableConfig.getDataSourceNames(), is(shadowTableConfig.getDataSourceNames()));
    }
    
    @Test
    public void assertSwapToObject() {
        YamlShadowTableConfiguration yamlConfig = new YamlShadowTableConfiguration();
        yamlConfig.setDataSourceNames(Collections.singletonList("shadow-data-source"));
        yamlConfig.setShadowAlgorithmNames(Arrays.asList("user-id-match-algorithm", "note-algorithm"));
        ShadowTableConfiguration shadowTableConfig = new YamlShadowTableConfigurationSwapper().swapToObject(yamlConfig);
        assertThat(shadowTableConfig.getDataSourceNames(), is(yamlConfig.getDataSourceNames()));
        assertThat(shadowTableConfig.getShadowAlgorithmNames(), is(yamlConfig.getShadowAlgorithmNames()));
    }
}
