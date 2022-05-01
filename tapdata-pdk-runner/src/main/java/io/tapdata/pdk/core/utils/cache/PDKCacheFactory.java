package io.tapdata.pdk.core.utils.cache;

import io.tapdata.entity.annotations.Implementation;
import io.tapdata.entity.utils.ClassFactory;
import io.tapdata.entity.utils.cache.CacheFactory;
import io.tapdata.entity.utils.cache.KVMap;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Implementation(value = CacheFactory.class, buildNumber = 0)
public class PDKCacheFactory implements CacheFactory {
    private final Map<String, KVMap<?>> kvMapMap = new ConcurrentHashMap<>();
    @Override
    public <T> KVMap<T> getOrCreateKVMap(String mapKey) {
        //noinspection unchecked
        return (KVMap<T>) kvMapMap.computeIfAbsent(mapKey, key -> {
            @SuppressWarnings("unchecked") KVMap<T> map = ClassFactory.create(KVMap.class);
            if(map != null)
                map.init(key);
            return ClassFactory.create(KVMap.class);
        });
    }

    @Override
    public boolean reset(String mapKey) {
        KVMap<?> map = kvMapMap.remove(mapKey);
        if(map != null) {
            map.reset();
            return true;
        }
        return false;
    }
}
