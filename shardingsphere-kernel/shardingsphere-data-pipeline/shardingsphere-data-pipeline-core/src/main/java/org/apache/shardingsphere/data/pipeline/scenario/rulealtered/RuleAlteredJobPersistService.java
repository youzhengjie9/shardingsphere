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

package org.apache.shardingsphere.data.pipeline.scenario.rulealtered;

import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.data.pipeline.api.job.persist.PipelineJobPersistContext;
import org.apache.shardingsphere.data.pipeline.core.api.GovernanceRepositoryAPI;
import org.apache.shardingsphere.data.pipeline.core.api.PipelineAPIFactory;
import org.apache.shardingsphere.infra.executor.kernel.thread.ExecutorThreadFactoryBuilder;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Rule altered job persist service.
 */

@Slf4j
public final class RuleAlteredJobPersistService {
    
    private static final Map<String, Map<Integer, PipelineJobPersistContext>> JOB_PERSIST_MAP = new ConcurrentHashMap<>();
    
    private static final GovernanceRepositoryAPI REPOSITORY_API = PipelineAPIFactory.getGovernanceRepositoryAPI();
    
    private static final ScheduledExecutorService JOB_PERSIST_EXECUTOR = Executors.newSingleThreadScheduledExecutor(ExecutorThreadFactoryBuilder.build("scaling-job-schedule-%d"));
    
    private static final long DELAY_SECONDS = 1;
    
    static {
        JOB_PERSIST_EXECUTOR.scheduleWithFixedDelay(new PersistJobContextRunnable(), 5, DELAY_SECONDS, TimeUnit.SECONDS);
    }
    
    /**
     * Remove job schedule parameter by job id.
     *
     * @param jobId job id
     */
    public static void removeJobPersistParameter(final String jobId) {
        log.info("Remove job persist, job id: {}", jobId);
        JOB_PERSIST_MAP.remove(jobId);
    }
    
    /**
     * Add job schedule parameter.
     *
     * @param jobId job id
     * @param shardingItem sharding item
     */
    public static void addJobPersistParameter(final String jobId, final int shardingItem) {
        log.info("Add job schedule, jobId={}, shardingItem={}", jobId, shardingItem);
        JOB_PERSIST_MAP.computeIfAbsent(jobId, key -> new ConcurrentHashMap<>()).put(shardingItem, new PipelineJobPersistContext(jobId, shardingItem));
    }
    
    /**
     * Persist job process, may not be implemented immediately, depending on persist interval.
     *
     * @param jobId job id
     * @param shardingItem sharding item
     */
    public static void triggerPersist(final String jobId, final int shardingItem) {
        Map<Integer, PipelineJobPersistContext> intervalParamMap = JOB_PERSIST_MAP.getOrDefault(jobId, Collections.emptyMap());
        PipelineJobPersistContext parameter = intervalParamMap.get(shardingItem);
        if (null == parameter) {
            log.debug("Persist interval parameter is null, jobId={}, shardingItem={}", jobId, shardingItem);
            return;
        }
        parameter.getHasNewEvents().set(true);
    }
    
    private static void persist(final String jobId, final int shardingItem, final PipelineJobPersistContext persistContext) {
        Long beforePersistingProgressMillis = persistContext.getBeforePersistingProgressMillis().get();
        if ((null == beforePersistingProgressMillis || System.currentTimeMillis() - beforePersistingProgressMillis < TimeUnit.SECONDS.toMillis(DELAY_SECONDS))
                && !persistContext.getHasNewEvents().get()) {
            return;
        }
        Map<Integer, RuleAlteredJobScheduler> schedulerMap = RuleAlteredJobSchedulerCenter.getJobSchedulerMap(jobId);
        RuleAlteredJobScheduler scheduler = schedulerMap.get(shardingItem);
        if (null == scheduler) {
            log.warn("persist, job schedule not exists, jobId={}, shardingItem={}", jobId, shardingItem);
            return;
        }
        if (null == beforePersistingProgressMillis) {
            persistContext.getBeforePersistingProgressMillis().set(System.currentTimeMillis());
        }
        persistContext.getHasNewEvents().set(false);
        long startTimeMillis = System.currentTimeMillis();
        REPOSITORY_API.persistJobProgress(scheduler.getJobContext());
        persistContext.getBeforePersistingProgressMillis().set(null);
        if (6 == ThreadLocalRandom.current().nextInt(100)) {
            log.info("persist, jobId={}, shardingItem={}, cost time: {} ms", jobId, shardingItem, System.currentTimeMillis() - startTimeMillis);
        }
    }
    
    private static final class PersistJobContextRunnable implements Runnable {
        
        @Override
        public void run() {
            for (Entry<String, Map<Integer, PipelineJobPersistContext>> entry : JOB_PERSIST_MAP.entrySet()) {
                entry.getValue().forEach((shardingItem, persistContext) -> {
                    persist(entry.getKey(), shardingItem, persistContext);
                });
            }
        }
    }
}
