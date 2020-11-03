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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CachedItemTest {
    @Test
    public void constructorAlwaysStoresItem() {
        CachedItem<String> result = new CachedItem<>("val", 12345L);

        assertThat(result.getItem()).isNotNull().isEqualTo("val");
    }

    @Test
    public void constructorAlwaysStoresCreated() {
        CachedItem<String> result = new CachedItem<>("val", 12345L);

        assertThat(result.getCreated()).isEqualTo(12345L);
    }

    @Test
    public void constructorAlwaysSetsAccessedToNull() {
        CachedItem<String> result = new CachedItem<>("val", 12345L);

        assertThat(result.getAccessed()).isNull();
    }

    @Test
    public void constructorAlwaysSetsModifiedToNull() {
        CachedItem<String> result = new CachedItem<>("val", 12345L);

        assertThat(result.getModified()).isNull();
    }

    @Test
    public void accessedReturnsCachedItemWithSameItem() {
        CachedItem<String> item = new CachedItem<>("val", 12345L);

        CachedItem<String> result = item.accessed(23456L);

        assertThat(result.getItem()).isNotNull().isEqualTo("val");
    }

    @Test
    public void accessedReturnsCachedItemWithSameCreated() {
        CachedItem<String> item = new CachedItem<>("val", 12345L);

        CachedItem<String> result = item.accessed(23456L);

        assertThat(result.getCreated()).isEqualTo(12345L);
    }

    @Test
    public void accessedReturnsCachedItemWithCorrectAccessed() {
        CachedItem<String> item = new CachedItem<>("val", 12345L);

        CachedItem<String> result = item.accessed(23456L);

        assertThat(result.getAccessed()).isNotNull().isEqualTo(23456L);
    }

    @Test
    public void accessedDoesNotModifyOriginalAccessed() {
        CachedItem<String> item = new CachedItem<>("val", 12345L);

        item.accessed(23456L);

        assertThat(item.getAccessed()).isNull();
    }

    @Test
    public void accessedReturnsCachedItemWithNullModified() {
        CachedItem<String> item = new CachedItem<>("val", 12345L);

        CachedItem<String> result = item.accessed(23456L);

        assertThat(result.getModified()).isNull();
    }

    @Test
    public void accessedWhenModifiedReturnsCachedItemWithCorrectModified() {
        CachedItem<String> item = new CachedItem<>("val", 12345L).modified("val", 34567L);

        CachedItem<String> result = item.accessed(23456L);

        assertThat(result.getModified()).isNotNull().isEqualTo(34567L);
    }

    @Test
    public void modifiedReturnsCachedItemWithNewItem() {
        CachedItem<String> item = new CachedItem<>("val1", 12345L);

        CachedItem<String> result = item.modified("val2", 23456L);

        assertThat(result.getItem()).isNotNull().isEqualTo("val2");
    }

    @Test
    public void modifiedReturnsCachedItemWithSameCreated() {
        CachedItem<String> item = new CachedItem<>("val1", 12345L);

        CachedItem<String> result = item.modified("val2", 23456L);

        assertThat(result.getCreated()).isEqualTo(12345L);
    }

    @Test
    public void modifiedReturnsCachedItemWithNullAccessed() {
        CachedItem<String> item = new CachedItem<>("val1", 12345L);

        CachedItem<String> result = item.modified("val2", 23456L);

        assertThat(result.getAccessed()).isNull();
    }

    @Test
    public void modifiedWhenAccessedReturnsCachedItemWithNullAccessed() {
        CachedItem<String> item = new CachedItem<>("val1", 12345L).accessed(34567L);

        CachedItem<String> result = item.modified("val2", 23456L);

        assertThat(result.getAccessed()).isNotNull().isEqualTo(34567L);
    }

    @Test
    public void modifiedReturnsCachedItemWithCorrectModified() {
        CachedItem<String> item = new CachedItem<>("val1", 12345L);

        CachedItem<String> result = item.modified("val", 34567L);

        assertThat(result.getModified()).isNotNull().isEqualTo(34567L);
    }

    @Test
    public void modifiedDoesNotModifyOriginalModified() {
        CachedItem<String> item = new CachedItem<>("val1", 12345L);

        item.modified("val2", 23456L);

        assertThat(item.getModified()).isNull();
    }
}
