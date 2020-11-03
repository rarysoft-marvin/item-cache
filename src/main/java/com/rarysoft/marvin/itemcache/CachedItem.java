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

public class CachedItem<T> {
    private final T item;

    private final long created;

    private final Long accessed;

    private final Long modified;

    private CachedItem(T item, long created, Long accessed, Long modified) {
        this.item = item;
        this.created = created;
        this.accessed = accessed;
        this.modified = modified;
    }

    public CachedItem(T item, long created) {
        this(item, created, null, null);
    }

    public T getItem() {
        return item;
    }

    public long getCreated() {
        return created;
    }

    public Long getAccessed() {
        return accessed;
    }

    public Long getModified() {
        return modified;
    }

    public CachedItem<T> accessed(long accessed) {
        return new CachedItem<>(this.item, this.created, accessed, this.modified);
    }

    public CachedItem<T> modified(T item, long modified) {
        return new CachedItem<>(item, this.created, this.accessed, modified);
    }
}
