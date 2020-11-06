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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CacheTest {
    @Mock
    private TimestampGenerator timestampGenerator;

    @Test
    public void constructorWithNoItemsSetsInitialStateToNotFullyPopulated() {
        Cache<String> cache = new Cache<>(value -> value);

        assertThat(cache.isFullyPopulated()).isFalse();
    }

    @Test
    public void allWhenNotPopulatedProvidedThrowsIllegalStateException() {
        Cache<String> cache = new Cache<>(value -> value);

        assertThrows(IllegalStateException.class, cache::all);
    }

    @Test
    public void allWhenNotFullyPopulatedProvidedThrowsIllegalStateException() {
        Cache<String> cache = new Cache<>(value -> value);
        cache.add("val1");

        assertThrows(IllegalStateException.class, cache::all);
    }

    @Test
    public void allWhenFullyPopulatedProvidedReturnsAllItems() {
        Cache<String> cache = new Cache<>(value -> value, Arrays.asList("val1", "val2", "val3"));

        Collection<String> result = cache.all();

        assertThat(result).isNotNull().containsExactlyInAnyOrder("val1", "val2", "val3");
    }

    @Test
    public void allWithTimeoutWhenNotPopulatedAndNotUpdatedWithinTimeoutThrowsPollingTimeout() {
        Cache<String> cache = new Cache<>(value -> value);

        assertThrows(PollingTimeout.class, () -> cache.all(10));
    }

    @Test
    public void allWithTimeoutWhenNotFullyPopulatedAndNotUpdatedWithinTimeoutThrowsPollingTimeout() {
        Cache<String> cache = new Cache<>(value -> value);
        cache.add("val1");

        assertThrows(PollingTimeout.class, () -> cache.all(10));
    }

    @Test
    public void allWithTimeoutWhenNotPopulatedAndUpdatedWithinTimeoutReturnsAllItems() throws PollingTimeout {
        Cache<String> cache = new Cache<>(value -> value);

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                cache.setAll(Arrays.asList("val1", "val2", "val3"));
            }
        }, 20);

        Collection<String> result = cache.all(1000);

        assertThat(result).isNotNull().containsExactlyInAnyOrder("val1", "val2", "val3");
    }

    @Test
    public void allWithTimeoutWhenNotFullyPopulatedAndUpdatedWithinTimeoutReturnsAllItems() throws PollingTimeout {
        Cache<String> cache = new Cache<>(value -> value);
        cache.add("val1");

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                cache.setAll(Arrays.asList("val1", "val2", "val3"));
            }
        }, 20);

        Collection<String> result = cache.all(1000);

        assertThat(result).isNotNull().containsExactlyInAnyOrder("val1", "val2", "val3");
    }

    @Test
    public void allWithTimeoutWhenFullyPopulatedReturnsAllItems() throws PollingTimeout {
        Cache<String> cache = new Cache<>(value -> value, Arrays.asList("val1", "val2", "val3"));

        Collection<String> result = cache.all(1000);

        assertThat(result).isNotNull().containsExactlyInAnyOrder("val1", "val2", "val3");
    }

    @Test
    public void sizeWhenNotPopulatedThrowsIllegalStateException() {
        Cache<String> cache = new Cache<>(value -> value);

        assertThrows(IllegalStateException.class, cache::size);
    }

    @Test
    public void sizeWhenNotFullyPopulatedThrowsIllegalStateException() {
        Cache<String> cache = new Cache<>(value -> value);
        cache.add("val1");

        assertThrows(IllegalStateException.class, cache::size);
    }

    @Test
    public void sizeWhenFullyPopulatedReturnsSize() {
        Cache<String> cache = new Cache<>(value -> value, Arrays.asList("val1", "val2", "val3"));

        int result = cache.size();

        assertThat(result).isEqualTo(3);
    }

    @Test
    public void setAllWhenNotFullyPopulatedMarksAsFullyPopulated() {
        Cache<String> cache = new Cache<>(value -> value);

        cache.setAll(Arrays.asList("val1", "val2", "val3"));

        assertThat(cache.isFullyPopulated()).isTrue();
    }

    @Test
    public void containsWhenNotPopulatedReturnsFalse() {
        Cache<String> cache = new Cache<>(value -> value);

        boolean result = cache.contains("val1");

        assertThat(result).isFalse();
    }

    @Test
    public void containsWhenNotFullyPopulatedAndMissingRequestedIdReturnsFalse() {
        Cache<String> cache = new Cache<>(value -> value);
        cache.add("val1");

        boolean result = cache.contains("val2");

        assertThat(result).isFalse();
    }

    @Test
    public void containsWhenNotFullyPopulatedAndContainsRequestedIdReturnsTrue() {
        Cache<String> cache = new Cache<>(value -> value);
        cache.add("val1");

        boolean result = cache.contains("val1");

        assertThat(result).isTrue();
    }

    @Test
    public void containsWhenFullyPopulatedAndMissingRequestedIdReturnsTrue() {
        Cache<String> cache = new Cache<>(value -> value, Arrays.asList("val1", "val2", "val3"));

        boolean result = cache.contains("val4");

        assertThat(result).isFalse();
    }

    @Test
    public void containsWhenFullyPopulatedAndContainsRequestedIdReturnsTrue() {
        Cache<String> cache = new Cache<>(value -> value, Arrays.asList("val1", "val2", "val3"));

        boolean result = cache.contains("val2");

        assertThat(result).isTrue();
    }

    @Test
    public void getWhenNotPopulatedReturnsEmpty() {
        Cache<String> cache = new Cache<>(value -> value);

        Optional<String> result = cache.get("val1");

        assertThat(result).isNotNull().isEmpty();
    }

    @Test
    public void getWhenNotFullyPopulatedAndMissingRequestedIdReturnsEmpty() {
        Cache<String> cache = new Cache<>(value -> value);
        cache.add("val1");

        Optional<String> result = cache.get("val2");

        assertThat(result).isNotNull().isEmpty();
    }

    @Test
    public void getWhenNotFullyPopulatedAndContainsRequestedIdReturnsRequestedItem() {
        Cache<String> cache = new Cache<>(value -> value);
        cache.add("val1");

        Optional<String> result = cache.get("val1");

        assertThat(result).isNotNull().isPresent().contains("val1");
    }

    @Test
    public void getWhenFullyPopulatedAndMissingRequestedIdReturnsEmpty() {
        Cache<String> cache = new Cache<>(value -> value, Arrays.asList("val1", "val2", "val3"));

        Optional<String> result = cache.get("val4");

        assertThat(result).isNotNull().isEmpty();
    }

    @Test
    public void getWhenFullyPopulatedAndContainsRequestedIdReturnsRequestedItem() {
        Cache<String> cache = new Cache<>(value -> value, Arrays.asList("val1", "val2", "val3"));

        Optional<String> result = cache.get("val2");

        assertThat(result).isNotNull().isPresent().contains("val2");
    }

    @Test
    public void getWithTimeoutWhenNotPopulatedAndNotUpdatedWithinTimeoutReturnsEmpty() {
        Cache<String> cache = new Cache<>(value -> value);

        Optional<String> result = cache.get("val1", 1);

        assertThat(result).isNotNull().isEmpty();
    }

    @Test
    public void getWithTimeoutWhenNotFullyPopulatedAndNotUpdatedWithinTimeoutReturnsEmpty() {
        Cache<String> cache = new Cache<>(value -> value);
        cache.add("val1");

        Optional<String> result = cache.get("val2", 1);

        assertThat(result).isNotNull().isEmpty();
    }

    @Test
    public void getWithTimeoutWhenNotPopulatedAndUpdatedWithRequestedItemWithinTimeoutReturnsRequestedItem() {
        Cache<String> cache = new Cache<>(value -> value);

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                cache.add("val1");
            }
        }, 10);

        Optional<String> result = cache.get("val1", 1000);

        assertThat(result).isNotNull().isPresent().contains("val1");
    }

    @Test
    public void getWithTimeoutWhenNotFullyPopulatedAndUpdatedWithRequestedItemWithinTimeoutReturnsRequestedItem() {
        Cache<String> cache = new Cache<>(value -> value);
        cache.add("val1");

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                cache.add("val2");
            }
        }, 10);

        Optional<String> result = cache.get("val2", 1000);

        assertThat(result).isNotNull().isPresent().contains("val2");
    }

    @Test
    public void getWithTimeoutWhenNotFullyPopulatedAndContainsRequestedIdReturnsRequestedItem() {
        Cache<String> cache = new Cache<>(value -> value);
        cache.add("val1");

        Optional<String> result = cache.get("val1", 1000);

        assertThat(result).isNotNull().isPresent().contains("val1");
    }

    @Test
    public void getWithTimeoutWhenFullyPopulatedAndMissingRequestedIdReturnsEmpty() {
        Cache<String> cache = new Cache<>(value -> value, Arrays.asList("val1", "val2", "val3"));

        Optional<String> result = cache.get("val4", 1000);

        assertThat(result).isNotNull().isEmpty();
    }

    @Test
    public void getWithTimeoutWhenFullyPopulatedAndContainsRequestedIdReturnsRequestedItem() {
        Cache<String> cache = new Cache<>(value -> value, Arrays.asList("val1", "val2", "val3"));

        Optional<String> result = cache.get("val2", 1000);

        assertThat(result).isNotNull().isPresent().contains("val2");
    }

    @Test
    public void addWhenNotFullyPopulatedLeavesStateAsNotFullyPopulated() {
        Cache<String> cache = new Cache<>(value -> value);

        cache.add("val1");

        assertThat(cache.isFullyPopulated()).isFalse();
    }

    @Test
    public void addWhenFullyPopulatedLeavesStateAsFullyPopulated() {
        Cache<String> cache = new Cache<>(value -> value, Arrays.asList("val1", "val2", "val3"));

        cache.add("val4");

        assertThat(cache.isFullyPopulated()).isTrue();
    }

    @Test
    public void updateWhenNotFullyPopulatedAndItemIsMissingAddsItem() {
        Cache<String> cache = new Cache<>(value -> value);

        cache.update("val1");

        assertThat(cache.get("val1")).isNotNull().isPresent().contains("val1");
    }

    @Test
    public void updateWhenFullyPopulatedAndItemIsMissingThrowsIllegalStateException() {
        Cache<String> cache = new Cache<>(value -> value, Arrays.asList("val1", "val2", "val3"));

        assertThrows(IllegalStateException.class, () -> cache.update("val4"));
    }

    @Test
    public void updateWhenFullyPopulatedAndItemIsMissingAddsItem() {
        Cache<String> cache = new Cache<>(value -> value, Arrays.asList("val1", "val2", "val3"));

        try {
            cache.update("val4");
        }
        catch (IllegalStateException e) {
            assertThat(cache.get("val4")).isNotNull().isPresent().contains("val4");
        }
    }

    @Test
    public void updateWhenFullyPopulatedAndItemIsMissingSetsStateToNotFullyPopulated() {
        Cache<String> cache = new Cache<>(value -> value, Arrays.asList("val1", "val2", "val3"));

        try {
            cache.update("val4");
        }
        catch (IllegalStateException e) {
            assertThat(cache.isFullyPopulated()).isFalse();
        }
    }

    @Test
    public void updateWhenNotFullyPopulatedUpdatesItem() {
        Cache<String> cache = new Cache<>(String::length);
        cache.add("val1");

        cache.update("val2");

        assertThat(cache.get(4)).isNotNull().isPresent().contains("val2");
    }

    @Test
    public void deleteWhenNotPopulatedDoesNothing() {
        Cache<String> cache = new Cache<>(value -> value);

        cache.delete("val1");

        assertThat(cache.contains("val1")).isFalse();
    }

    @Test
    public void deleteWhenNotFullyPopulatedAndRequestedItemNotFoundDoesNotDeleteAnyExistingItems() {
        Cache<String> cache = new Cache<>(value -> value);
        cache.add("val1");

        cache.delete("val2");

        assertThat(cache.contains("val1")).isTrue();
    }

    @Test
    public void deleteWhenNotFullyPopulatedAndRequestedItemFoundDeletesItem() {
        Cache<String> cache = new Cache<>(value -> value);
        cache.add("val1");

        cache.delete("val1");

        assertThat(cache.contains("val1")).isFalse();
    }

    @Test
    public void deleteWhenFullyPopulatedAndRequestedItemNotFoundThrowsIllegalStateException() {
        Cache<String> cache = new Cache<>(value -> value, Arrays.asList("val1", "val2", "val3"));

        assertThrows(IllegalStateException.class, () -> cache.delete("val4"));
    }

    @Test
    public void deleteWhenFullyPopulatedAndRequestedItemNotFoundSetsStateToNotFullyPopulated() {
        Cache<String> cache = new Cache<>(value -> value, Arrays.asList("val1", "val2", "val3"));

        try {
            cache.delete("val4");
        }
        catch (IllegalStateException e) {
            assertThat(cache.isFullyPopulated()).isFalse();
        }
    }

    @Test
    public void deleteWhenFullyPopulatedAndRequestedItemFoundDeletesItem() {
        Cache<String> cache = new Cache<>(value -> value, Arrays.asList("val1", "val2", "val3"));

        cache.delete("val2");

        assertThat(cache.contains("val2")).isFalse();
    }

    @Test
    public void deleteWhenFullyPopulatedAndLastItemDeletedLeavesStateAsFullyPopulated() {
        Cache<String> cache = new Cache<>(value -> value, Collections.singletonList("val1"));

        cache.delete("val1");

        assertThat(cache.isFullyPopulated()).isTrue();
    }

    @Test
    public void evictUnaccessedWhenNotPopulatedLeavesStateAsNotFullyPopulated() {
        Cache<String> cache = new Cache<>(value -> value);

        cache.evictUnaccessed(0);

        assertThat(cache.isFullyPopulated()).isFalse();
    }

    @Test
    public void evictUnaccessedWhenNotFullyPopulatedLeavesStateAsNotFullyPopulated() {
        when(timestampGenerator.timestamp()).thenReturn(1000000000L, 1000000001L);
        Cache<String> cache = new Cache<>(timestampGenerator, value -> value);
        cache.add("val1");

        cache.evictUnaccessed(0);

        assertThat(cache.isFullyPopulated()).isFalse();
    }

    @Test
    public void evictUnaccessedWhenNotFullyPopulatedAndContainsOnlyUnaccessedItemsRemovesAllItems() {
        when(timestampGenerator.timestamp()).thenReturn(1000000000L, 1000000001L);
        Cache<String> cache = new Cache<>(timestampGenerator, value -> value);
        cache.add("val1");

        cache.evictUnaccessed(1000);

        assertThat(cache.contains("val1")).isFalse();
    }

    @Test
    public void evictUnaccessedWhenNotFullyPopulatedAndContainsOnlyAccessedItemsRemovesNothing() {
        when(timestampGenerator.timestamp()).thenReturn(1000000000L, 1000000001L, 1000000002L);
        Cache<String> cache = new Cache<>(timestampGenerator, value -> value);
        cache.add("val1");
        cache.get("val1");

        cache.evictUnaccessed(1000);

        assertThat(cache.contains("val1")).isTrue();
    }

    @Test
    public void evictUnaccessedWhenNotFullyPopulatedAndContainsAccessedAndUnaccessedItemsRemovesUnaccessedItems() {
        when(timestampGenerator.timestamp()).thenReturn(1000000000L, 1000000001L, 1000000002L, 1000000003L);
        Cache<String> cache = new Cache<>(timestampGenerator, value -> value);
        cache.add("val1");
        cache.add("val2");
        cache.get("val2");

        cache.evictUnaccessed(1000);

        assertThat(cache.contains("val1")).isFalse();
    }

    @Test
    public void evictUnaccessedWhenNotFullyPopulatedAndContainsAccessedAndUnaccessedItemsLeavesAccessedItems() {
        when(timestampGenerator.timestamp()).thenReturn(1000000000L, 1000000001L, 1000000002L, 1000000003L);
        Cache<String> cache = new Cache<>(timestampGenerator, value -> value);
        cache.add("val1");
        cache.add("val2");
        cache.get("val2");

        cache.evictUnaccessed(1000);

        assertThat(cache.contains("val2")).isTrue();
    }

    @Test
    public void evictUnaccessedWhenFullyPopulatedAndContainsOnlyUnaccessedItemsRemovesAllItems() {
        when(timestampGenerator.timestamp()).thenReturn(1000000000L, 1000000001L);
        Cache<String> cache = new Cache<>(timestampGenerator, value -> value, Collections.singletonList("val1"));

        cache.evictUnaccessed(1000);

        assertThat(cache.contains("val1")).isFalse();
    }

    @Test
    public void evictUnaccessedWhenFullyPopulatedAndContainsOnlyUnaccessedItemsSetsStateToNotFullyPopulated() {
        when(timestampGenerator.timestamp()).thenReturn(1000000000L, 1000000001L);
        Cache<String> cache = new Cache<>(timestampGenerator, value -> value, Collections.singletonList("val1"));

        cache.evictUnaccessed(1000);

        assertThat(cache.isFullyPopulated()).isFalse();
    }

    @Test
    public void evictUnaccessedWhenFullyPopulatedAndContainsOnlyAccessedItemsRemovesNothing() {
        when(timestampGenerator.timestamp()).thenReturn(1000000000L, 1000000001L, 1000000002L);
        Cache<String> cache = new Cache<>(timestampGenerator, value -> value, Collections.singletonList("val1"));
        cache.get("val1");

        cache.evictUnaccessed(1000);

        assertThat(cache.contains("val1")).isTrue();
    }

    @Test
    public void evictUnaccessedWhenFullyPopulatedAndContainsOnlyAccessedItemsLeavesStateAsFullyPopulated() {
        when(timestampGenerator.timestamp()).thenReturn(1000000000L, 1000000001L, 1000000002L);
        Cache<String> cache = new Cache<>(timestampGenerator, value -> value, Collections.singletonList("val1"));
        cache.get("val1");

        cache.evictUnaccessed(1000);

        assertThat(cache.isFullyPopulated()).isTrue();
    }

    @Test
    public void evictUnaccessedWhenFullyPopulatedAndContainsAccessedAndUnaccessedItemsRemovesUnaccessedItems() {
        when(timestampGenerator.timestamp()).thenReturn(1000000000L, 1000000000L, 1000000001L, 1000000002L);
        Cache<String> cache = new Cache<>(timestampGenerator, value -> value, Arrays.asList("val1", "val2"));
        cache.get("val1");

        cache.evictUnaccessed(1000);

        assertThat(cache.contains("val2")).isFalse();
    }

    @Test
    public void evictUnaccessedWhenFullyPopulatedAndContainsAccessedAndUnaccessedItemsLeavesAccessedItems() {
        when(timestampGenerator.timestamp()).thenReturn(1000000000L, 1000000000L, 1000000001L, 1000000002L);
        Cache<String> cache = new Cache<>(timestampGenerator, value -> value, Arrays.asList("val1", "val2"));
        cache.get("val1");

        cache.evictUnaccessed(1000);

        assertThat(cache.contains("val1")).isTrue();
    }

    @Test
    public void evictUnaccessedWhenFullyPopulatedAndContainsAccessedAndUnaccessedItemsSetsStateToNotFullyPopulated() {
        when(timestampGenerator.timestamp()).thenReturn(1000000000L, 1000000000L, 1000000001L, 1000000002L);
        Cache<String> cache = new Cache<>(timestampGenerator, value -> value, Arrays.asList("val1", "val2"));
        cache.get("val1");

        cache.evictUnaccessed(1000);

        assertThat(cache.isFullyPopulated()).isFalse();
    }

    @Test
    public void evictUnmodifiedWhenNotPopulatedLeavesStateAsNotFullyPopulated() {
        Cache<String> cache = new Cache<>(value -> value);

        cache.evictUnmodified(0);

        assertThat(cache.isFullyPopulated()).isFalse();
    }

    @Test
    public void evictUnmodifiedWhenNotFullyPopulatedLeavesStateAsNotFullyPopulated() {
        when(timestampGenerator.timestamp()).thenReturn(1000000000L, 1000000001L);
        Cache<String> cache = new Cache<>(timestampGenerator, value -> value);
        cache.add("val1");

        cache.evictUnmodified(0);

        assertThat(cache.isFullyPopulated()).isFalse();
    }

    @Test
    public void evictUnmodifiedWhenNotFullyPopulatedAndContainsOnlyUnmodifiedItemsRemovesAllItems() {
        when(timestampGenerator.timestamp()).thenReturn(1000000000L, 1000000001L);
        Cache<String> cache = new Cache<>(timestampGenerator, value -> value);
        cache.add("val1");

        cache.evictUnmodified(1000);

        assertThat(cache.contains("val1")).isFalse();
    }

    @Test
    public void evictUnmodifiedWhenNotFullyPopulatedAndContainsOnlyModifiedItemsRemovesNothing() {
        when(timestampGenerator.timestamp()).thenReturn(1000000000L, 1000000001L, 1000000002L);
        Cache<String> cache = new Cache<>(timestampGenerator, value -> value);
        cache.add("val1");
        cache.update("val1");

        cache.evictUnmodified(1000);

        assertThat(cache.contains("val1")).isTrue();
    }

    @Test
    public void evictUnmodifiedWhenNotFullyPopulatedAndContainsModifiedAndUnmodifiedItemsRemovesUnmodifiedItems() {
        when(timestampGenerator.timestamp()).thenReturn(1000000000L, 1000000001L, 1000000002L, 1000000003L);
        Cache<String> cache = new Cache<>(timestampGenerator, value -> value);
        cache.add("val1");
        cache.add("val2");
        cache.update("val2");

        cache.evictUnmodified(1000);

        assertThat(cache.contains("val1")).isFalse();
    }

    @Test
    public void evictUnmodifiedWhenNotFullyPopulatedAndContainsModifiedAndUnmodifiedItemsLeavesModifiedItems() {
        when(timestampGenerator.timestamp()).thenReturn(1000000000L, 1000000001L, 1000000002L, 1000000003L);
        Cache<String> cache = new Cache<>(timestampGenerator, value -> value);
        cache.add("val1");
        cache.add("val2");
        cache.update("val2");

        cache.evictUnmodified(1000);

        assertThat(cache.contains("val2")).isTrue();
    }

    @Test
    public void evictUnmodifiedWhenFullyPopulatedAndContainsOnlyUnmodifiedItemsRemovesAllItems() {
        when(timestampGenerator.timestamp()).thenReturn(1000000000L, 1000000001L);
        Cache<String> cache = new Cache<>(timestampGenerator, value -> value, Collections.singletonList("val1"));

        cache.evictUnmodified(1000);

        assertThat(cache.contains("val1")).isFalse();
    }

    @Test
    public void evictUnmodifiedWhenFullyPopulatedAndContainsOnlyUnmodifiedItemsSetsStateToNotFullyPopulated() {
        when(timestampGenerator.timestamp()).thenReturn(1000000000L, 1000000001L);
        Cache<String> cache = new Cache<>(timestampGenerator, value -> value, Collections.singletonList("val1"));

        cache.evictUnmodified(1000);

        assertThat(cache.isFullyPopulated()).isFalse();
    }

    @Test
    public void evictUnmodifiedWhenFullyPopulatedAndContainsOnlyModifiedItemsRemovesNothing() {
        when(timestampGenerator.timestamp()).thenReturn(1000000000L, 1000000001L, 1000000002L);
        Cache<String> cache = new Cache<>(timestampGenerator, value -> value, Collections.singletonList("val1"));
        cache.update("val1");

        cache.evictUnmodified(1000);

        assertThat(cache.contains("val1")).isTrue();
    }

    @Test
    public void evictUnmodifiedWhenFullyPopulatedAndContainsOnlyModifiedItemsLeavesStateAsFullyPopulated() {
        when(timestampGenerator.timestamp()).thenReturn(1000000000L, 1000000001L, 1000000002L);
        Cache<String> cache = new Cache<>(timestampGenerator, value -> value, Collections.singletonList("val1"));
        cache.update("val1");

        cache.evictUnmodified(1000);

        assertThat(cache.isFullyPopulated()).isTrue();
    }

    @Test
    public void evictUnmodifiedWhenFullyPopulatedAndContainsModifiedAndUnmodifiedItemsRemovesUnmodifiedItems() {
        when(timestampGenerator.timestamp()).thenReturn(1000000000L, 1000000000L, 1000000001L, 1000000002L);
        Cache<String> cache = new Cache<>(timestampGenerator, value -> value, Arrays.asList("val1", "val2"));
        cache.update("val1");

        cache.evictUnmodified(1000);

        assertThat(cache.contains("val2")).isFalse();
    }

    @Test
    public void evictUnmodifiedWhenFullyPopulatedAndContainsModifiedAndUnmodifiedItemsLeavesModifiedItems() {
        when(timestampGenerator.timestamp()).thenReturn(1000000000L, 1000000000L, 1000000001L, 1000000002L);
        Cache<String> cache = new Cache<>(timestampGenerator, value -> value, Arrays.asList("val1", "val2"));
        cache.update("val1");

        cache.evictUnmodified(1000);

        assertThat(cache.contains("val1")).isTrue();
    }

    @Test
    public void evictUnmodifiedWhenFullyPopulatedAndContainsModifiedAndUnmodifiedItemsSetsStateToNotFullyPopulated() {
        when(timestampGenerator.timestamp()).thenReturn(1000000000L, 1000000000L, 1000000001L, 1000000002L);
        Cache<String> cache = new Cache<>(timestampGenerator, value -> value, Arrays.asList("val1", "val2"));
        cache.update("val1");

        cache.evictUnaccessed(1000);

        assertThat(cache.isFullyPopulated()).isFalse();
    }

    @Test
    public void evictWhenNotPopulatedLeavesStateAsNotFullyPopulated() {
        Cache<String> cache = new Cache<>(value -> value);

        cache.evict(1);

        assertThat(cache.isFullyPopulated()).isFalse();
    }

    @Test
    public void evictWhenNotFullyPopulatedLeavesStateAsNotFullyPopulated() {
        when(timestampGenerator.timestamp()).thenReturn(1000000000L, 1000000001L);
        Cache<String> cache = new Cache<>(timestampGenerator, value -> value);
        cache.add("val1");

        cache.evict(1);

        assertThat(cache.isFullyPopulated()).isFalse();
    }

    @Test
    public void evictWhenNotFullyPopulatedAndContainsOnlyOldItemsRemovesAllItems() {
        when(timestampGenerator.timestamp()).thenReturn(1000000000L, 1000000002L);
        Cache<String> cache = new Cache<>(timestampGenerator, value -> value);
        cache.add("val1");

        cache.evict(1);

        assertThat(cache.contains("val1")).isFalse();
    }

    @Test
    public void evictWhenNotFullyPopulatedAndContainsOnlyNewerItemsRemovesNothing() {
        when(timestampGenerator.timestamp()).thenReturn(1000000000L, 1000000001L);
        Cache<String> cache = new Cache<>(timestampGenerator, value -> value);
        cache.add("val1");

        cache.evict(1000);

        assertThat(cache.contains("val1")).isTrue();
    }

    @Test
    public void evictWhenNotFullyPopulatedAndContainsNewerAndOldItemsRemovesOldItems() {
        when(timestampGenerator.timestamp()).thenReturn(1000000000L, 1000000001L, 1000000002L);
        Cache<String> cache = new Cache<>(timestampGenerator, value -> value);
        cache.add("val1");
        cache.add("val2");

        cache.evict(1);

        assertThat(cache.contains("val1")).isFalse();
    }

    @Test
    public void evictWhenNotFullyPopulatedAndContainsNewerAndOldItemsLeavesNewerItems() {
        when(timestampGenerator.timestamp()).thenReturn(1000000000L, 1000000001L, 1000000002L);
        Cache<String> cache = new Cache<>(timestampGenerator, value -> value);
        cache.add("val1");
        cache.add("val2");

        cache.evict(1);

        assertThat(cache.contains("val2")).isTrue();
    }

    @Test
    public void evictWhenFullyPopulatedAndContainsOnlyOldItemsRemovesAllItems() {
        when(timestampGenerator.timestamp()).thenReturn(1000000000L, 1000000002L);
        Cache<String> cache = new Cache<>(timestampGenerator, value -> value, Collections.singletonList("val1"));

        cache.evict(1);

        assertThat(cache.contains("val1")).isFalse();
    }

    @Test
    public void evictWhenFullyPopulatedAndContainsOnlyOldItemsSetsStateToNotFullyPopulated() {
        when(timestampGenerator.timestamp()).thenReturn(1000000000L, 1000000002L);
        Cache<String> cache = new Cache<>(timestampGenerator, value -> value, Collections.singletonList("val1"));

        cache.evict(1);

        assertThat(cache.isFullyPopulated()).isFalse();
    }

    @Test
    public void evictWhenFullyPopulatedAndContainsOnlyNewerItemsRemovesNothing() {
        when(timestampGenerator.timestamp()).thenReturn(1000000000L, 1000000001L);
        Cache<String> cache = new Cache<>(timestampGenerator, value -> value, Collections.singletonList("val1"));

        cache.evict(1);

        assertThat(cache.contains("val1")).isTrue();
    }

    @Test
    public void evictWhenFullyPopulatedAndContainsOnlyNewerItemsLeavesStateAsFullyPopulated() {
        when(timestampGenerator.timestamp()).thenReturn(1000000000L, 1000000001L);
        Cache<String> cache = new Cache<>(timestampGenerator, value -> value, Collections.singletonList("val1"));

        cache.evict(1);

        assertThat(cache.isFullyPopulated()).isTrue();
    }

    @Test
    public void evictWhenFullyPopulatedAndContainsNewerAndOldItemsRemovesOldItems() {
        when(timestampGenerator.timestamp()).thenReturn(1000000000L, 1000000001L, 1000000002L);
        Cache<String> cache = new Cache<>(timestampGenerator, value -> value, Collections.singletonList("val1"));
        cache.add("val2");

        cache.evict(1);

        assertThat(cache.contains("val1")).isFalse();
    }

    @Test
    public void evictWhenFullyPopulatedAndContainsNewerAndOldItemsLeavesNewerItems() {
        when(timestampGenerator.timestamp()).thenReturn(1000000000L, 1000000001L, 1000000002L);
        Cache<String> cache = new Cache<>(timestampGenerator, value -> value, Collections.singletonList("val1"));
        cache.add("val2");

        cache.evict(1);

        assertThat(cache.contains("val2")).isTrue();
    }

    @Test
    public void evictWhenFullyPopulatedAndContainsNewerAndOldItemsSetsStateToNotFullyPopulated() {
        when(timestampGenerator.timestamp()).thenReturn(1000000000L, 1000000001L, 1000000002L);
        Cache<String> cache = new Cache<>(timestampGenerator, value -> value, Collections.singletonList("val1"));
        cache.add("val2");

        cache.evict(1);

        assertThat(cache.isFullyPopulated()).isFalse();
    }

    @Test
    public void evictAllWhenNotPopulatedLeavesStateAsNotFullyPopulated() {
        Cache<String> cache = new Cache<>(value -> value);

        cache.evictAll();

        assertThat(cache.isFullyPopulated()).isFalse();
    }

    @Test
    public void evictAllWhenNotFullyPopulatedAndContainsUnaccessedItemsRemovesUnaccessedItems() {
        when(timestampGenerator.timestamp()).thenReturn(1000000000L, 1000000001L);
        Cache<String> cache = new Cache<>(timestampGenerator, value -> value);
        cache.add("val1");

        cache.evictAll();

        assertThat(cache.contains("val1")).isFalse();
    }

    @Test
    public void evictAllWhenNotFullyPopulatedAndContainsAccessedItemsRemovesAccessedItems() {
        when(timestampGenerator.timestamp()).thenReturn(1000000000L, 1000000001L, 1000000002L);
        Cache<String> cache = new Cache<>(timestampGenerator, value -> value);
        cache.add("val1");
        cache.get("val1");

        cache.evictAll();

        assertThat(cache.contains("val1")).isFalse();
    }

    @Test
    public void evictAllWhenNotFullyPopulatedAndContainsAccessedAndUnaccessedItemsRemovesUnaccessedItems() {
        when(timestampGenerator.timestamp()).thenReturn(1000000000L, 1000000001L, 1000000002L, 1000000003L);
        Cache<String> cache = new Cache<>(timestampGenerator, value -> value);
        cache.add("val1");
        cache.add("val2");
        cache.get("val2");

        cache.evictAll();

        assertThat(cache.contains("val1")).isFalse();
    }

    @Test
    public void evictAllWhenNotFullyPopulatedAndContainsAccessedAndUnaccessedItemsRemovesAccessedItems() {
        when(timestampGenerator.timestamp()).thenReturn(1000000000L, 1000000001L, 1000000002L, 1000000003L);
        Cache<String> cache = new Cache<>(timestampGenerator, value -> value);
        cache.add("val1");
        cache.add("val2");
        cache.get("val2");

        cache.evictAll();

        assertThat(cache.contains("val2")).isFalse();
    }

    @Test
    public void evictAllWhenNotFullyPopulatedLeavesStateAsNotFullyPopulated() {
        when(timestampGenerator.timestamp()).thenReturn(1000000000L, 1000000001L, 1000000002L);
        Cache<String> cache = new Cache<>(timestampGenerator, value -> value);
        cache.add("val1");
        cache.get("val1");

        cache.evictAll();

        assertThat(cache.isFullyPopulated()).isFalse();
    }

    @Test
    public void evictAllWhenFullyPopulatedAndContainsUnaccessedItemsRemovesUnaccessedItems() {
        when(timestampGenerator.timestamp()).thenReturn(1000000000L, 1000000001L);
        Cache<String> cache = new Cache<>(timestampGenerator, value -> value, Collections.singletonList("val1"));

        cache.evictAll();

        assertThat(cache.contains("val1")).isFalse();
    }

    @Test
    public void evictAllWhenFullyPopulatedAndContainsAccessedItemsRemovesAccessedItems() {
        when(timestampGenerator.timestamp()).thenReturn(1000000000L, 1000000001L, 1000000002L);
        Cache<String> cache = new Cache<>(timestampGenerator, value -> value, Collections.singletonList("val1"));
        cache.get("val1");

        cache.evictAll();

        assertThat(cache.contains("val1")).isFalse();
    }

    @Test
    public void evictAllWhenFullyPopulatedAndContainsAccessedAndUnaccessedItemsRemovesUnaccessedItems() {
        when(timestampGenerator.timestamp()).thenReturn(1000000000L, 1000000001L, 1000000002L, 1000000003L);
        Cache<String> cache = new Cache<>(timestampGenerator, value -> value, Arrays.asList("val1", "val2"));
        cache.get("val2");

        cache.evictAll();

        assertThat(cache.contains("val1")).isFalse();
    }

    @Test
    public void evictAllWhenFullyPopulatedAndContainsAccessedAndUnaccessedItemsRemovesAccessedItems() {
        when(timestampGenerator.timestamp()).thenReturn(1000000000L, 1000000001L, 1000000002L, 1000000003L);
        Cache<String> cache = new Cache<>(timestampGenerator, value -> value, Arrays.asList("val1", "val2"));
        cache.get("val2");

        cache.evictAll();

        assertThat(cache.contains("val2")).isFalse();
    }

    @Test
    public void evictAllWhenFullyPopulatedSetsStateToNotFullyPopulated() {
        when(timestampGenerator.timestamp()).thenReturn(1000000000L, 1000000001L, 1000000002L);
        Cache<String> cache = new Cache<>(timestampGenerator, value -> value, Arrays.asList("val1", "val2"));

        cache.evictAll();

        assertThat(cache.isFullyPopulated()).isFalse();
    }
}
