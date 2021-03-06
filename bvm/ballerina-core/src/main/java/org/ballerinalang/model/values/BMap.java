/*
 * Copyright (c) 2016, WSO2 Inc. (http://wso2.com) All Rights Reserved.
 * <p>
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.ballerinalang.model.values;

import org.ballerinalang.bre.bvm.BVM;
import org.ballerinalang.bre.bvm.VarLock;
import org.ballerinalang.model.types.BField;
import org.ballerinalang.model.types.BMapType;
import org.ballerinalang.model.types.BRecordType;
import org.ballerinalang.model.types.BStructureType;
import org.ballerinalang.model.types.BTupleType;
import org.ballerinalang.model.types.BType;
import org.ballerinalang.model.types.BTypes;
import org.ballerinalang.model.types.BUnionType;
import org.ballerinalang.model.types.TypeTags;
import org.ballerinalang.model.util.Flags;
import org.ballerinalang.model.util.JsonGenerator;
import org.ballerinalang.persistence.serializable.SerializableState;
import org.ballerinalang.persistence.serializable.reftypes.Serializable;
import org.ballerinalang.persistence.serializable.reftypes.SerializableRefType;
import org.ballerinalang.persistence.serializable.reftypes.impl.SerializableBMap;
import org.ballerinalang.util.exceptions.BLangExceptionHelper;
import org.ballerinalang.util.exceptions.BLangFreezeException;
import org.ballerinalang.util.exceptions.BallerinaErrorReasons;
import org.ballerinalang.util.exceptions.BallerinaException;
import org.ballerinalang.util.exceptions.RuntimeErrors;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.ballerinalang.model.util.FreezeUtils.handleInvalidUpdate;
import static org.ballerinalang.model.util.FreezeUtils.isOpenForFreeze;

/**
 * {@code MapType} represents a map.
 * @param <K> Key
 * @param <V> Value
 * @since 0.8.0
 */
@SuppressWarnings("rawtypes")
public class BMap<K, V extends BValue> implements BRefType, BCollection, Serializable {

    private LinkedHashMap<K, V> map;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Lock readLock = lock.readLock();
    private final Lock writeLock = lock.writeLock();
    private BType type = BTypes.typeMap;
    private HashMap<String, Object> nativeData = new HashMap<>();
    private volatile BVM.FreezeStatus freezeStatus = new BVM.FreezeStatus(BVM.FreezeStatus.State.UNFROZEN);
    private ConcurrentHashMap<String, VarLock> lockMap = new ConcurrentHashMap();

    public BMap() {
        map =  new LinkedHashMap<>();
    }

    public BMap(BType type) {
        this.map = new LinkedHashMap<>();
        this.type = type;
    }

    /**
     * Retrieve the value for the given key from map.
     * A null will be returned if the key does not exists.
     *
     * @param key key used to get the value
     * @return value
     */
    public V get(K key) {
        readLock.lock();
        try {
            return map.get(key);
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Retrieve the value for the given key from map.
     * A {@code BallerinaException} will be thrown if the key does not exists.
     *
     * @param key key used to get the value
     * @return value
     */
    public V getIfExist(K key) {
        readLock.lock();
        try {
            if (!map.containsKey(key)) {
                throw new BallerinaException("cannot find key '" + key + "'");
            }
            return map.get(key);
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Retrieve the value for the given key from map.
     *
     * @param key key used to get the value
     * @param except flag indicating whether to throw an exception if the key does not exists
     * @return value
     */
    public V get(K key, boolean except) {
        readLock.lock();
        try {
            if (!map.containsKey(key) && except) {
                throw new BallerinaException(BallerinaErrorReasons.KEY_NOT_FOUND_ERROR,
                                             "cannot find key '" + key + "'");
            }
            return map.get(key);
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Insert a key value pair into the map.
     * @param key key related to the value
     * @param value value related to the key
     */
    public void put(K key, V value) {
        writeLock.lock();
        try {
            if (freezeStatus.getState() != BVM.FreezeStatus.State.UNFROZEN) {
                handleInvalidUpdate(freezeStatus.getState());
            }

            map.put(key, value);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Clear map entries.
     */
    public void clear() {
        writeLock.lock();
        try {
            if (freezeStatus.getState() != BVM.FreezeStatus.State.UNFROZEN) {
                handleInvalidUpdate(freezeStatus.getState());
            }

            map.clear();
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Check existence of a key of a map.
     *
     * @param key key of the map item
     * @return returns boolean true if key exists
     */
    public boolean hasKey(K key) {
        readLock.lock();
        try {
            return map.containsKey(key);
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Retrieve the internal map.
     * @return map
     */
    public LinkedHashMap<K, V> getMap() {
        return map;
    }

    /**
     * Get the size of the map.
     * @return returns the size of the map
     */
    public int size() {
        readLock.lock();
        try {
            return map.size();
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Remove an item from the map.
     *
     * @param key key of the item to be removed
     * @return boolean to indicate whether given key is removed.
     */
    public boolean remove(K key) {
        writeLock.lock();
        try {
            if (freezeStatus.getState() != BVM.FreezeStatus.State.UNFROZEN) {
                handleInvalidUpdate(freezeStatus.getState());
            }

            boolean hasKey = map.containsKey(key);
            if (hasKey) {
                map.remove(key);
            }
            return hasKey;
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Retrieve the keys related to this map as an array.
     *
     * @return keys as an array
     */
    public K[] keys() {
        readLock.lock();
        try {
            Set<K> keys = map.keySet();
            return (K[]) keys.toArray(new String[keys.size()]);
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Retrieve the value in the map as an array.
     *
     * @return values as an array
     */
    public V[] values() {
        readLock.lock();
        try {
            Collection<V> values = map.values();
            return (V[]) values.toArray(new BRefType[values.size()]);
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Return true if this map is empty.
     *
     * @return Flag indicating whether the map is empty or not
     */
    public boolean isEmpty() {
        readLock.lock();
        try {
            return map.size() == 0;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public Object value() {
        return null;
    }

    @Override
    public String stringValue() {
        readLock.lock();
        StringJoiner sj = new StringJoiner(", ", "{", "}");
        try {
            switch (type.getTag()) {
                case TypeTags.OBJECT_TYPE_TAG:
                    for (Map.Entry<String, BField> field : ((BStructureType) this.type).getFields().entrySet()) {
                        if (!Flags.isFlagOn(field.getValue().flags, Flags.PUBLIC)) {
                            continue;
                        }
                        String fieldName = field.getKey();
                        V fieldVal = get((K) fieldName);
                        sj.add(fieldName + ":" + getStringValue(fieldVal));
                    }
                    break;
                case TypeTags.JSON_TAG:
                    return getJSONString();
                case TypeTags.MAP_TAG:
                    // Map<json> is json.
                    if (((BMapType) type).getConstrainedType().getTag() == TypeTags.JSON_TAG) {
                        return getJSONString();
                    }
                    // Fallthrough
                default:
                    String keySeparator = type.getTag() == TypeTags.MAP_TAG ? "\"" : "";
                    for (Iterator<Map.Entry<K, V>> i = map.entrySet().iterator(); i.hasNext();) {
                        String key;
                        Map.Entry<K, V> e = i.next();
                        key = keySeparator + e.getKey() + keySeparator;
                        V value = e.getValue();
                        sj.add(key + ":" + getStringValue(value));
                    }
                    break;
            }
            return sj.toString();
        } finally {
            readLock.unlock();
        }
    }

    /**
     * String value with all the fields irrespective of the access modifiers.
     * Non-public fields of an object will also be stringified.
     *
     * @return string value
     */
    public String absoluteStringValue() {
        readLock.lock();
        StringJoiner sj = new StringJoiner(", ", "{", "}");
        try {
            switch (type.getTag()) {
                case TypeTags.OBJECT_TYPE_TAG:
                    ((BStructureType) this.type).getFields().forEach((fieldName, field) -> {
                        V fieldVal = get((K) fieldName);
                        sj.add((fieldName + ":" + getStringValue(fieldVal)));
                    });
                    break;
                case TypeTags.JSON_TAG:
                    return getJSONString();
                default:
                    String keySeparator = type.getTag() == TypeTags.MAP_TAG ? "\"" : "";
                    map.forEach((mapKey, value) -> {
                        String key = keySeparator + mapKey + keySeparator;
                        sj.add(key + ":" + getStringValue(value));
                    });
                    break;
            }
            return sj.toString();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public BType getType() {
        return this.type;
    }

    @Override
    public void stamp(BType type, List<BVM.TypeValuePair> unresolvedValues) {
        BVM.TypeValuePair typeValuePair = new BVM.TypeValuePair(this, type);
        if (unresolvedValues.contains(typeValuePair)) {
            throw new BallerinaException(BallerinaErrorReasons.CYCLIC_VALUE_REFERENCE_ERROR,
                                         BLangExceptionHelper.getErrorMessage(RuntimeErrors.CYCLIC_VALUE_REFERENCE, 
                                                                              this.type));
        }
        unresolvedValues.add(typeValuePair);
        if (type.getTag() == TypeTags.JSON_TAG) {
            type = BVM.resolveMatchingTypeForUnion(this, type);
            this.stamp(type, unresolvedValues);
        } else if (type.getTag() == TypeTags.MAP_TAG) {
            for (Object value : this.values()) {
                if (value != null) {
                    ((BValue) value).stamp(((BMapType) type).getConstrainedType(), unresolvedValues);
                }
            }
        } else if (type.getTag() == TypeTags.RECORD_TYPE_TAG) {
            Map<String, BType> targetTypeField = new HashMap<>();
            BType restFieldType = ((BRecordType) type).restFieldType;

            for (BField field : ((BStructureType) type).getFields().values()) {
                targetTypeField.put(field.getFieldName(), field.fieldType);
            }

            for (Map.Entry valueEntry : this.getMap().entrySet()) {
                String fieldName = valueEntry.getKey().toString();
                if ((valueEntry.getValue()) != null) {
                    BValue value = (BValue) valueEntry.getValue();
                    BType bType = targetTypeField.getOrDefault(fieldName, restFieldType);
                    value.stamp(bType, unresolvedValues);
                }
            }
        } else if (type.getTag() == TypeTags.UNION_TAG) {
            for (BType memberType : ((BUnionType) type).getMemberTypes()) {
                if (BVM.checkIsLikeType(this, memberType)) {
                    this.stamp(memberType, unresolvedValues);
                    type = memberType;
                    break;
                }
            }
        } else if (type.getTag() == TypeTags.ANYDATA_TAG) {
            type = BVM.resolveMatchingTypeForUnion(this, type);
            this.stamp(type, unresolvedValues);
        }

        this.type = type;
        unresolvedValues.remove(typeValuePair);
    }

    @Override
    public BValue copy(Map<BValue, BValue> refs) {
        readLock.lock();
        try {
            if (isFrozen()) {
                return this;
            }

            if (refs.containsKey(this)) {
                return refs.get(this);
            }

            BMap<K, BValue> newMap = new BMap<>(type);
            refs.put(this, newMap);
            for (Map.Entry<K, V> entry: map.entrySet()) {
                BValue value = entry.getValue();
                newMap.put(entry.getKey(), value == null ? null : value.copy(refs));
            }
            return newMap;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public BIterator newIterator() {
        return new BMapIterator<>(this);
    }

    @Override
    public SerializableRefType serialize(SerializableState state) {
        return new SerializableBMap<>(this, state);
    }

    /**
     * {@code {@link BMapIterator}} provides iterator implementation for map values.
     *
     * @since 0.96.0
     */
    static class BMapIterator<K, V extends BValue> implements BIterator {

        BMap<K, V> collection;
        Iterator<Map.Entry<K, V>> iterator;
        long cursor = 0;

        BMapIterator(BMap<K, V> value) {
            collection = value;
            iterator = new LinkedHashMap<>(value.map).entrySet().iterator();
        }

        @Override
        public BValue getNext() {
            if (cursor++ == collection.size()) {
                return null;
            }

            List<BType> types = new LinkedList<>();
            types.add(BTypes.typeString);
            types.add(BTypes.typeAny);
            BTupleType tupleType = new BTupleType(types);

            Map.Entry<K, V> next = iterator.next();
            BValueArray tuple = new BValueArray(tupleType);
            BString key = new BString((String) next.getKey());
            tuple.add(0, key);
            BRefType value = (BRefType<?>) next.getValue();
            tuple.add(1, value);

            return tuple;
        }

        @Override
        public boolean hasNext() {
            return cursor < collection.size() && iterator.hasNext();
        }
    }

    /**
     * Add natively accessible data.
     *
     * @param key key to store data with
     * @param data data to be stored
     */
    public void addNativeData(String key, Object data) {
        this.nativeData.put(key, data);
    }

    /**
     * Get natively accessible data.
     *
     * @param key key by which data was stored
     * @return data which was stored with given key or null if no value corresponding to key
     */
    public Object getNativeData(String key) {
        return this.nativeData.get(key);
    }

    @Override
    public String toString() {
        return stringValue();
    }

    /**
     * Get natively accessible data.
     *
     * @return map of the native data
     */
    public HashMap<String, Object> getNativeData() {
        return nativeData;
    }

    /**
     * Returns a variable lock for the given field.
     *
     * @param fieldName field of the map that need to be locked
     * @return VarLock for the given field
     */
    public VarLock getFieldLock(String fieldName) {
        lockMap.putIfAbsent(fieldName, new VarLock());
        return lockMap.get(fieldName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void attemptFreeze(BVM.FreezeStatus freezeStatus) {
        if (this.type.getTag() == TypeTags.OBJECT_TYPE_TAG) {
            throw new BLangFreezeException("'freeze()' not allowed on '" + getType() + "'");
        }

        if (isOpenForFreeze(this.freezeStatus, freezeStatus)) {
            this.freezeStatus = freezeStatus;
            map.values().forEach(val -> {
                if (val != null) {
                    val.attemptFreeze(freezeStatus);
                }
            });
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized boolean isFrozen() {
        return this.freezeStatus.isFrozen();
    }

    private String getStringValue(V value) {
        if (value == null) {
            return "()";
        } else if (value instanceof BString) {
            return "\"" + value.stringValue() + "\"";
        } else {
            return value.stringValue();
        }
    }

    private String getJSONString() {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        JsonGenerator gen = new JsonGenerator(byteOut);
        try {
            gen.serialize(this);
            gen.flush();
        } catch (IOException e) {
            throw new BallerinaException("Error in converting JSON to a string: " + e.getMessage(), e);
        }
        return new String(byteOut.toByteArray());
    }
}
