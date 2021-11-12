package com.antelope.clustercache.autoconfigure.aspect.processor;

import java.util.List;
import java.util.Map;

/**
 * @author yaml
 * @since 2021/8/5
 */
public interface AbstractBatchCacheableProcessor {

    Map<String, Object> mGet(String cacheName, String prefix, List<String> keyCollection);

    void mSet(String cacheName, String prefix, Map<String, Object> data);
}
