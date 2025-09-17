package cn.ussshenzhou.util;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author USS_Shenzhou
 */
public class ResizableCounter<K> {
    private final ConcurrentHashMap<K, ResizableIntRing> counter = new ConcurrentHashMap<>();
    private int windowLength;

    public ResizableCounter(int windowLength) {
        this.windowLength = windowLength;
    }

    public void increment(K key) {
        synchronized (counter) {
            counter.computeIfAbsent(key, k -> new ResizableIntRing(windowLength)).increment();
        }
    }

    public void advance() {
        counter.values().forEach(ResizableIntRing::advance);
    }

    public void advance(int size) {
        synchronized (counter) {
            windowLength = size;
            counter.values().forEach(r -> r.advance(size));
        }
    }

    public int count(K key) {
        return counter.get(key).sum();
    }

    public void remove(K key) {
        synchronized (counter) {
            counter.remove(key);
        }
    }

    private static class ResizableIntRing {
        private int[] buckets;
        private int idx;

        private ResizableIntRing(int size) {
            buckets = new int[size];
        }

        private synchronized void increment() {
            buckets[idx]++;
        }

        private synchronized void advance() {
            idx = (idx + 1) % buckets.length;
            buckets[idx] = 0;
        }

        private synchronized void advance(int size) {
            if (buckets.length != size) {
                var newBucket = new int[size];
                var lap = Math.min(size, buckets.length);
                for (int i = idx; i < idx + lap; i++) {
                    var j = i % lap;
                    newBucket[j] = buckets[j];
                }
                buckets = newBucket;
            }
            this.advance();
        }

        private synchronized int sum() {
            int sum = 0;
            for (int v : buckets) {
                sum += v;
            }
            return sum;
        }
    }

}
