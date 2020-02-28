package com.yueki1993.bloom.filtree;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnel;

import javax.annotation.Nonnull;
import java.util.*;

public class BloomFiltree<K, V> implements SetMap<K, V> {

    private final Funnel<? super K> funnel;
    private final long expectedInsetions;
    private final double fpp;

    private Map<V, BloomFilter<K>> bloomFilters = new HashMap<>();

    // binary tree respresented by array.
    // https://en.wikipedia.org/wiki/Binary_tree#Arrays
    private List<Map.Entry<V, BloomFilter<K>>> tree = null;

    public BloomFiltree(Funnel<? super K> funnel, long expectedInsetions, double fpp) {
        this.funnel = funnel;
        this.expectedInsetions = expectedInsetions;
        this.fpp = fpp;
    }

    public boolean put(@Nonnull K key, @Nonnull V value) {
        return bloomFilters
                .computeIfAbsent(value, k -> createBf())
                .put(key);
    }

    private static int roundUpToNextPowerOfTwo(int x) {
        // https://bits.stephan-brumme.com/roundUpToNextPowerOfTwo.html
        x--;
        x |= x >> 1;  // handle  2 bit numbers
        x |= x >> 2;  // handle  4 bit numbers
        x |= x >> 4;  // handle  8 bit numbers
        x |= x >> 8;  // handle 16 bit numbers
        x |= x >> 16; // handle 32 bit numbers
        x++;

        return x;
    }

    @Nonnull
    @Override
    public Set<V> getSet(@Nonnull K key) {
        if (tree == null)
            throw new IllegalStateException("initialize must be called before calling getSet");

        Set<V> ret = new HashSet<>();

        // BFS
        Queue<Integer> queue = new LinkedList<>();
        queue.add(0);

        while (!queue.isEmpty()) {
            int i = queue.remove();
            Map.Entry<V, BloomFilter<K>> e = tree.get(i);
            if (e == null) break;

            V val = e.getKey();
            BloomFilter<K> bf = e.getValue();

            if (bf.mightContain(key)) {
                if (val != null) {
                    ret.add(val);
                } else {
                    queue.add(2 * i + 1);
                    queue.add(2 * i + 2);
                }
            }
        }
        return ret;
    }


    private BloomFilter<K> createBf() {
        return BloomFilter.create(funnel, expectedInsetions, fpp);
    }

    public void initialize() {
        if (tree != null)
            throw new IllegalStateException("initialize is already called");

        int n = bloomFilters.size();

        // allocate arrays of length (2n'-1 + (2n')) = 4n'-1,
        // where n' is a number which is rounded up to the next power of two of n (e.g., n=1000 -> n'=1024),
        // and 2n' is the size of sentinel leaf node for simplifying implementation.
        int np = roundUpToNextPowerOfTwo(n);
        tree = new ArrayList<>(4 * np - 1);

        // put leaves to the tree
        Iterator<Map.Entry<V, BloomFilter<K>>> it = bloomFilters.entrySet().iterator();
        for (int i = np - 1; i < np - 1 + n; i++) {
            tree.add(i, it.next());
        }

        // take union of two nodes and make it their parent
        for (int i = np - 2; i >= 0; i--) {
            Map.Entry<V, BloomFilter<K>> left = tree.get(i * 2 + 1);
            Map.Entry<V, BloomFilter<K>> right = tree.get(i * 2 + 2);
            if (left == null && right == null) {
                // if children are null, their parent is also null
                continue;
            }

            if (right == null) {
                // if parent has only one child, copy the child to the parent
                Map.Entry<V, BloomFilter<K>> par = new AbstractMap.SimpleEntry<>(null, left.getValue());
                tree.add(i, par);
                continue;
            }

            // if both childrean are non-null, take union of them and make it their parent
            BloomFilter<K> leftBf = left.getValue();
            BloomFilter<K> rightBf = right.getValue();
            BloomFilter<K> parBf = leftBf.copy();
            parBf.putAll(rightBf);
            tree.add(i, new AbstractMap.SimpleEntry<>(null, parBf));
        }
    }
}
