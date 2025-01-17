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

package org.apache.shardingsphere.mode.lock.manager;

import lombok.RequiredArgsConstructor;
import org.apache.shardingsphere.infra.lock.LockDefinition;
import org.apache.shardingsphere.mode.lock.manager.state.LockStateContext;

/**
 * Lock manager of ShardingSphere.
 */
@RequiredArgsConstructor
public final class ShardingSphereLockManager implements LockManager {
    
    private final LockStateContext lockStateContext = new LockStateContext();
    
    private final LockPersistService lockPersistService;
    
    @Override
    public boolean tryLock(final LockDefinition lockDefinition, final long timeoutMillis) {
        if (lockPersistService.tryLock(lockDefinition.getLockKey(), timeoutMillis)) {
            lockStateContext.register(lockDefinition);
            return true;
        }
        return false;
    }
    
    @Override
    public void unLock(final LockDefinition lockDefinition) {
        lockPersistService.unLock(lockDefinition.getLockKey());
        lockStateContext.unregister(lockDefinition);
    }
    
    @Override
    public boolean isLocked(final LockDefinition lockDefinition) {
        return lockStateContext.isLocked(lockDefinition);
    }
}
