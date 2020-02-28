package com.yueki1993.bloom.filtree;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnel;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class BloomFilterSetMap<K, V> implements SetMap<K, V> {

    private final Funnel<? super K> funnel;
    private final long expectedInsetions;
    private final double fpp;

    private Map<V, BloomFilter<K>> bloomFilters = new HashMap<>();

    public BloomFilterSetMap(Funnel<? super K> funnel, long expectedInsetions, double fpp) {
        this.funnel = funnel;
        this.expectedInsetions = expectedInsetions;
        this.fpp = fpp;
    }

    public boolean put(@Nonnull K key, @Nonnull V value) {
        return bloomFilters
                .computeIfAbsent(value, k -> createBf())
                .put(key);
    }

    @Nonnull
    @Override
    public Set<V> getSet(@Nonnull K key) {
        Set<V> ret = new HashSet<>();
        for (Map.Entry<V, BloomFilter<K>> entry : bloomFilters.entrySet()) {
            if (entry.getValue().mightContain(key)) ret.add(entry.getKey());
        }
        return ret;
    }


    private BloomFilter<K> createBf() {
        return BloomFilter.create(funnel, expectedInsetions, fpp);
    }
}
