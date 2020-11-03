/*
 * MIT License
 *
 * Copyright (c) 2020 Rarysoft Enterprises
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.rarysoft.marvin.itemcache;

import java.io.Serializable;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Cache<T> {
    private final Function<T, Serializable> idExtractor;

    private final Map<Serializable, CachedItem<T>> all;

    private boolean fullyPopulated;

    public Cache(Function<T, Serializable> idExtractor) {
        this.idExtractor = idExtractor;
        this.all = new HashMap<>();
        this.fullyPopulated = false;
    }

    public Cache(Function<T, Serializable> idExtractor, List<T> all) {
        this.idExtractor = idExtractor;
        this.all = new HashMap<>();
        all.forEach(item -> this.all.put(this.idExtractor.apply(item), new CachedItem<>(item, System.currentTimeMillis())));
        this.fullyPopulated = true;
    }

    public List<T> all() {
        if (! this.fullyPopulated) {
            throw new IllegalStateException("Attempt to retrieve all from a partial cache");
        }
        return this.all.values().stream().map(CachedItem::getItem).collect(Collectors.toList());
    }

    public synchronized List<T> all(long timeoutInMillis) throws PollingTimeout {
        try {
            this.wait(timeoutInMillis);
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (! this.fullyPopulated) {
            throw new PollingTimeout();
        }
        return this.all.values().stream().map(CachedItem::getItem).collect(Collectors.toList());
    }

    public synchronized void setAll(List<T> all) {
        this.all.clear();
        all.forEach(item -> this.all.put(this.idExtractor.apply(item), new CachedItem<>(item, System.currentTimeMillis())));
        this.fullyPopulated = true;
        this.notifyAll();
    }

    public boolean isFullyPopulated() {
        return this.fullyPopulated;
    }

    public int size() {
        if (! this.fullyPopulated) {
            throw new IllegalStateException("Attempt to get size of a partial cache");
        }
        return this.all.size();
    }

    public boolean contains(Serializable id) {
        return this.all.containsKey(id);
    }

    public Optional<T> get(Serializable id) {
        CachedItem<T> item = this.all.get(id);
        if (item == null) {
            return Optional.empty();
        }
        this.all.put(id, item.accessed(System.currentTimeMillis()));
        return Optional.of(item.getItem());
    }

    public void add(T item) {
        this.all.put(this.idExtractor.apply(item), new CachedItem<>(item, System.currentTimeMillis()));
    }

    public void update(T item) {
        CachedItem<T> oldItem = this.all.get(this.idExtractor.apply(item));
        if (oldItem == null) {
            this.add(item);
            if (this.fullyPopulated) {
                this.fullyPopulated = false;
                throw new IllegalStateException("Attempt to update a missing item in a fully populated cache");
            }
        }
        else {
            this.all.replace(this.idExtractor.apply(item), oldItem.modified(item, System.currentTimeMillis()));
        }
    }

    public void delete(T item) {
        Serializable id = this.idExtractor.apply(item);
        if (this.fullyPopulated && ! this.all.containsKey(id)) {
            this.fullyPopulated = false;
            throw new IllegalStateException("Attempt to remove a missing item from a fully populated cache");
        }
        this.all.remove(id);
    }

    public void evictUnaccessed(long ageInMillis) {
        this.evict(item -> Optional.ofNullable(item.getAccessed()).orElse(0L) < System.currentTimeMillis() - ageInMillis);
    }

    public void evictUnmodified(long ageInMillis) {
        this.evict(item -> Optional.ofNullable(item.getModified()).orElse(0L) < System.currentTimeMillis() - ageInMillis);
    }

    public void evict(long ageInMillis) {
        this.evict(item -> item.getCreated() < System.currentTimeMillis() - ageInMillis);
    }

    public void evictAll() {
        this.all.clear();
        this.fullyPopulated = false;
    }

    private void evict(Predicate<CachedItem<T>> selector) {
        int preEvictionSize = this.all.size();
        this.all.values()
                .stream()
                .filter(selector)
                .map(CachedItem::getItem)
                .map(this.idExtractor)
                .collect(Collectors.toList())
                .forEach(this.all::remove);
        this.fullyPopulated = this.fullyPopulated && this.all.size() == preEvictionSize;
    }
}
