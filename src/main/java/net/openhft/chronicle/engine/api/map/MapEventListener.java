/*
 * Copyright 2016 higherfrequencytrading.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.openhft.chronicle.engine.api.map;

/**
 * This breaks down the events on a Map.  This is useful for providing one interface to handle all event types.
 */
@FunctionalInterface
public interface MapEventListener<K, V> {
    void update(String assetName, K key, V oldValue, V newValue);

    default void insert(String assetName, K key, V value) {
        update(assetName, key, null, value);
    }

    default void remove(String assetName, K key, V value) {
        update(assetName, key, value, null);
    }
}