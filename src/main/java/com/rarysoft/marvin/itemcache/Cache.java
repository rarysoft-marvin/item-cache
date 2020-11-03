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

/**
 * <p>
 * Represents an in-memory cache of items backed by a remote repository. The items can be any type, but
 * must be uniquely identifiable. Multiple constructors are provided, but all require an expression to
 * use to uniquely identify an item.
 * </p>
 * <p>
 * The cache has not connection to or awareness of the remote repository. It is up to the code using this
 * cache to obtain items from the remote repository and maintain them within the cache. The cache operates
 * under the assumption that all deletions and modifications to items previously cached are kept
 * synchronized by use of the {@link Cache#update(Object)} and {@link Cache#delete(Serializable)} methods.
 * It is not, however, assumed that the cache is a complete replica of the remote repository unless explicitly
 * designated as such.
 * </p>
 * <p>
 * To designate a cache as a complete replica, the cache must either be constructed using one of the
 * constructors that receive a collection of items as an argument, or provided a complete collection using
 * the {@link Cache#setAll(Collection)} method. This collection is assumed to be a complete collection of
 * items from the remote repository, and therefore indicates that the cache is a complete replica.
 * </p>
 * The cache tracks its state is either not fully populated or fully populated. A fully populated cache is
 * one that is a complete replica. Some methods are unavailable on a not fully populated cache.
 * <p>
 * Meta-data is attached to the items internally to monitor when they were added to the cache, when they
 * were last accessed, and when they were last modified. This meta-data is not visible outside the cache,
 * but can be used to evict items from the cache.
 * </p>
 * @param <T> The type of item to store in the cache.
 */
public class Cache<T> {
    private final TimestampGenerator timestampGenerator;
    private final Function<T, Serializable> idExtractor;
    private final Map<Serializable, CachedItem<T>> all;

    private boolean fullyPopulated;

    /**
     * <p>
     * Constructs a cache of items of type T, using the provided expression to uniquely identify items.
     * The newly constructed cache will be in a not fully populated state.
     * </p>
     * <p>
     * Timestamps will be generated internally using the default {@link SystemTimestampGenerator}.
     * </p>
     * @param idExtractor The expression to use to uniquely identify a particular item.
     */
    public Cache(Function<T, Serializable> idExtractor) {
        this.timestampGenerator = new SystemTimestampGenerator();
        this.idExtractor = idExtractor;
        this.all = new HashMap<>();
        this.fullyPopulated = false;
    }

    /**
     * <p>
     * Constructs a cache of items of type T, using the provided expression to uniquely identify items,
     * and populates it with the provided items. Use this constructor to provide a complete collection
     * of items during construction. The newly constructed cache will be in a fully populated state.
     * </p>
     * <p>
     * If an empty collection is provided in the second parameter, the cache will still be in a fully
     * populated state, representing a cache that is synchronized with a remote repository that is
     * currently empty.
     * </p>
     * <p>
     * Timestamps will be generated internally using the default {@link SystemTimestampGenerator}.
     * </p>
     * @param idExtractor The expression to use to uniquely identify a particular item.
     * @param all A collection containing all items in the cache.
     */
    public Cache(Function<T, Serializable> idExtractor, Collection<T> all) {
        this.timestampGenerator = new SystemTimestampGenerator();
        this.idExtractor = idExtractor;
        this.all = new HashMap<>();
        all.forEach(item -> this.all.put(this.idExtractor.apply(item), new CachedItem<>(item, timestampGenerator.timestamp())));
        this.fullyPopulated = true;
    }

    /**
     * <p>
     * Constructs a cache of items of type T, using the provided expression to uniquely identify items.
     * The newly constructed cache will be in a not fully populated state.
     * </p>
     * <p>
     * Timestamps will be generated internally using the provided {@link TimestampGenerator}.
     * </p>
     * @param timestampGenerator The timestamp generator to use to generate timestamps internally.
     * @param idExtractor The expression to use to uniquely identify a particular item.
     */
    public Cache(TimestampGenerator timestampGenerator, Function<T, Serializable> idExtractor) {
        this.timestampGenerator = timestampGenerator;
        this.idExtractor = idExtractor;
        this.all = new HashMap<>();
        this.fullyPopulated = false;
    }

    /**
     * <p>
     * Constructs a cache of items of type T, using the provided expression to uniquely identify items,
     * and populates it with the provided items. Use this constructor to provide a complete collection
     * of items during construction. The newly constructed cache will be in a fully populated state.
     * </p>
     * <p>
     * If an empty collection is provided in the second parameter, the cache will still be in a fully
     * populated state, representing a cache that is synchronized with a remote repository that is
     * currently empty.
     * </p>
     * <p>
     * Timestamps will be generated internally using the provided {@link TimestampGenerator}.
     * </p>
     * @param timestampGenerator The timestamp generator to use to generate timestamps internally.
     * @param idExtractor The expression to use to uniquely identify a particular item.
     * @param all A collection containing all items in the cache.
     */
    public Cache(TimestampGenerator timestampGenerator, Function<T, Serializable> idExtractor, Collection<T> all) {
        this.timestampGenerator = timestampGenerator;
        this.idExtractor = idExtractor;
        this.all = new HashMap<>();
        all.forEach(item -> this.all.put(this.idExtractor.apply(item), new CachedItem<>(item, timestampGenerator.timestamp())));
        this.fullyPopulated = true;
    }

    /**
     * <p>
     * Returns a collection of cached items that represents all items in the remote repository.
     * </p>
     * <p>
     * This method is only to be used when the cache is in fully populated state, otherwise it
     * would return only the cached items, which is a collection that would have no logical relation
     * to the remote repository.
     * </p>
     * <p>
     * It is up to the calling code to check the state of the cache before calling this method.
     * The state can be checked by calling {@link Cache#isFullyPopulated()}. If this method is
     * called when the cache is not fully populated, an {@link IllegalStateException} will be
     * thrown.
     * </p>
     * @return A collection of cached items that represents all items in the remote repository.*
     * @throws IllegalStateException If the cache is not fully populated.
     */
    public Collection<T> all() {
        if (! this.fullyPopulated) {
            throw new IllegalStateException("Attempt to retrieve all from a partial cache");
        }
        return this.allItems();
    }

    /**
     * <p>
     * Returns a collection of cached items that represents all items in the remote repository.
     * </p>
     * <p>
     * If the cache is not fully populated, then this method blocks and waits, up to a length of
     * time defined by the first argument, for the cache to get populated by some other thread's
     * call to the {@link Cache##setAll(Collection)} method.
     * </p>
     * <p>
     * If the timeout period lapses without the cache being brought into a fully populated state,
     * this method throws a {@link PollingTimeout}.
     * </p>
     * <p>
     * If the cache is already fully populated when this method is called, the method will return
     * the items immediately. In this case, the timeout argument has no effect.
     * </p>
     * <p>
     * @param timeoutInMillis The number of milliseconds to wait for the cache to become fully
     *                        populated, if it is not already fully populated.
     * @return A collection of cached items that represents all items in the remote repository.
     * @throws PollingTimeout If the timeout period lapses without the cache being put into a
     *                        fully populated state.
     */
    public synchronized Collection<T> all(long timeoutInMillis) throws PollingTimeout {
        if (this.fullyPopulated) {
            return this.allItems();
        }
        try {
            this.wait(timeoutInMillis);
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (! this.fullyPopulated) {
            throw new PollingTimeout();
        }
        return this.allItems();
    }

    /**
     * <p>
     * Stores a complete collection of items in the cache. Any previously stored items are removed
     * from the cache, and the cache is left in fully populated state containing all of the newly
     * added items, with new timestamps.
     * </p>
     * <p>
     * Other threads that are blocked waiting for the cache to become fully populated through a call
     * to the {@link Cache#all(long)} method will become unblocked and return a collection that
     * matches the one that was provided to this method.
     * </p>
     * @param all A collection of items to store in the cache that represents all items in the remote
     *            repository.
     */
    public synchronized void setAll(Collection<T> all) {
        this.all.clear();
        all.forEach(item -> this.all.put(this.idExtractor.apply(item), new CachedItem<>(item, timestampGenerator.timestamp())));
        this.fullyPopulated = true;
        this.notifyAll();
    }

    /**
     * <p>
     * Indicates whether or not the cache is fully populated.
     * </p>
     * <p>
     * If the cache is fully populated, it is considered to be a reliable replica of the remote
     * repository.
     * </p>
     * <p>
     * If it is not fully populated, then it may or may not contain all of the items stored in the
     * remote repository. Any items that are in the cache should be considered current.
     * </p>
     * @return An indication of whether or not the cache is fully populated.
     */
    public boolean isFullyPopulated() {
        return this.fullyPopulated;
    }

    /**
     * <p>
     * Indicates the number of items in the remote repository.
     * </p>
     * <p>
     * Note that this is not intended to indicate the size of the cache, as that is not particularly
     * meaningful information. It is intended to indicate the total number of items in the remote
     * repository. Since the cache has no actual knowledge of the remote repository, it can only
     * know the item count if it is fully populated.
     * </p>
     * <p>
     * It is up to the calling code to check the state of the cache, using the
     * {@link Cache#isFullyPopulated()} method, prior to calling this method. If this method is
     * called on a not fully populated cache, an {@link IllegalStateException} will be thrown.
     * </p>
     * @return The number of items in the remote repository.
     * @throws IllegalStateException If the cache is not fully populated.
     */
    public int size() {
        if (! this.fullyPopulated) {
            throw new IllegalStateException("Attempt to get size of a partial cache");
        }
        return this.all.size();
    }

    /**
     * <p>
     * Indicates whether or not the cache contains an item identified by the provided identifier.
     * </p>
     * <p>
     * When the cache is not fully populated, a negative response does not necessarily indicate
     * that the requested item does not exist, just that it is not cached. A call to the remote
     * repository will be necessary to determine the actual state of the item.
     * </p>
     * <p>
     * When the cache is fully populated, a negative response indicates that the item does not
     * exist in the remote repository.
     * </p>
     * @param id The unique identifier to use to locate the requested item.
     * @return An indication of whether or not the cache contains the requested item.
     */
    public boolean contains(Serializable id) {
        return this.all.containsKey(id);
    }

    /**
     * <p>
     * Gets an item uniquely identified by the provided identifier from the cache, if such an item
     * exists in the cache.
     * </p>
     * <p>
     * In the case of a fully populated cache, this method returning an {@link Optional#empty()}
     * indicates that no such item exists in the remote repository.
     * </p>
     * <p>
     * In the case of a not fully populated cache, this method returning an {@link Optional#empty()}
     * simply means that no such item has been cached, and there may or may not exist a matching
     * item in the remote repository.
     * </p>
     * <p>
     * If the item is found in the cache, it will be marked internally with an accessed timestamp.
     * </p>
     * @param id The unique identifier to use to locate the requested item.
     * @return An {@link Optional} that either contains the item, if it exists in the cache, or is
     *         empty if the item does not exist in the cache.
     */
    public Optional<T> get(Serializable id) {
        CachedItem<T> item = this.all.get(id);
        if (item == null) {
            return Optional.empty();
        }
        this.all.put(id, item.accessed(timestampGenerator.timestamp()));
        return Optional.of(item.getItem());
    }

    /**
     * <p>
     * Adds an item to the cache.
     * </p>
     * <p>
     * This method may be called to synchronize a new addition to the remote repository on a fully
     * populated cache, or it may be called to add to a not fully populated cache an existing item
     * that has been retrieved from the remote repository.
     * </p>
     * <p>
     * This method will not change the state of the cache.
     * </p>
     * @param item The item to add to the cache.
     */
    public void add(T item) {
        this.all.put(this.idExtractor.apply(item), new CachedItem<>(item, timestampGenerator.timestamp()));
    }

    /**
     * <p>
     * Updates a previously cached item.
     * </p>
     * <p>
     * The previously cached item with a unique identifier matching the provided item will be replaced
     * with the provided item, and the modified timestamp will be set.
     * </p>
     * <p>
     * In the case of a not fully populated cache, a failure to find the item to update in the cache
     * is of no concern, and the item will simply be added to the cache, with its created timestamp
     * matching its modified timestamp.
     * </p>
     * <p>
     * In the case of a fully populated cache, a failure to find the item to update indicates that the
     * cache is not properly synchronized with the remote repository. This likely indicates a problem
     * with the synchronization of the cache in the calling code, and should probably be addressed.
     * The item will be added to the cache as a new item, the cache state will revert to not fully
     * populated, and an {@link IllegalStateException} will be thrown.
     * </p>
     * @param item The item to update.
     * @throws IllegalStateException If the cache is fully populated but the item to update is not
     *                               found in the cache.
     */
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
            this.all.replace(this.idExtractor.apply(item), oldItem.modified(item, timestampGenerator.timestamp()));
        }
    }

    /**
     * <p>
     * Removes a previously cached item from the cache.
     * </p>
     * <p>
     * The previously cached item with a unique identifier matching the provided item will be removed.
     * </p>
     * <p>
     * In the case of a not fully populated cache, a failure to find the item to delete in the cache
     * is of no concern, and the item will simply do nothing.
     * </p>
     * <p>
     * In the case of a fully populated cache, a failure to find the item to delete indicates that the
     * cache is not properly synchronized with the remote repository. This likely indicates a problem
     * with the synchronization of the cache in the calling code, and should probably be addressed.
     * The cache state will revert to not fully populated, and an {@link IllegalStateException} will be
     * thrown.
     * </p>
     * @param id The unique identifier of the item to delete.
     * @throws IllegalStateException If the cache is fully populated but the item to update is not
     *                               found in the cache.
     */
    public void delete(Serializable id) {
        if (this.fullyPopulated && ! this.all.containsKey(id)) {
            this.fullyPopulated = false;
            throw new IllegalStateException("Attempt to remove a missing item from a fully populated cache");
        }
        this.all.remove(id);
    }

    /**
     * <p>
     * Evicts from the cache any items that have either not been accessed at all since being stored
     * in the cache, or were last accessed longer in the past than the specified time period in
     * milliseconds.
     * </p>
     * <p>
     * The cache will be left in a not fully populated state.
     * </p>
     * @param ageInMillis Age in milliseconds of oldest unaccessed timestamp to keep.
     */
    public void evictUnaccessed(long ageInMillis) {
        this.evict(item -> Optional.ofNullable(item.getAccessed()).orElse(0L) < timestampGenerator.timestamp() - ageInMillis);
    }

    /**
     * <p>
     * Evicts from the cache any items that have either not been modified at all since being stored
     * in the cache, or were last modified longer in the past than the specified time period in
     * milliseconds.
     * </p>
     * <p>
     * The cache will be left in a not fully populated state.
     * </p>
     * @param ageInMillis Age in milliseconds of oldest modified timestamp to keep.
     */
    public void evictUnmodified(long ageInMillis) {
        this.evict(item -> Optional.ofNullable(item.getModified()).orElse(0L) < timestampGenerator.timestamp() - ageInMillis);
    }

    /**
     * <p>
     * Evicts from the cache any items that are older than the specified age in milliseconds.
     * </p>
     * <p>
     * The cache will be left in a not fully populated state.
     * </p>
     * @param ageInMillis Age in milliseconds of oldest item to keep.
     */
    public void evict(long ageInMillis) {
        this.evict(item -> item.getCreated() < timestampGenerator.timestamp() - ageInMillis);
    }

    /**
     * <p>
     * Evicts all items from the cache.
     * </p>
     * <p>
     * The cache will be left in a not fully populated state.
     */
    public void evictAll() {
        this.all.clear();
        this.fullyPopulated = false;
    }

    private Collection<T> allItems() {
        return this.all.values().stream().map(CachedItem::getItem).collect(Collectors.toList());
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
