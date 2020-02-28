package com.yueki1993.bloom.filtree;

import javax.annotation.Nonnull;
import java.util.Set;

public interface SetMap<K, V> {
    /**
     * @param key
     * @return return a set associated with the key with false-positive.
     * If no value found with the key, empty set is returned.
     */
    @Nonnull
    Set<V> getSet(@Nonnull K key);
}
