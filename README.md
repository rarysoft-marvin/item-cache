# Rarysoft Marvin - Item Cache

A simple item cache.

## Use Case

Although the cache is flexible, the primary use case for which it was developed is in an event-driven micro-service
architecture where modifications to items are published as events. In this architecture, a service that relies on
a particular domain model entity can retrieve it from the remote repository, store it in the local cache, and
subscribe to modification events in order to ensure that the local cache always contains the current version of any
items that have been cached.

## Usage

The purpose of the item cache is to store a local copy of remote items for quick access, with the ability to keep the
local items synchronized with the remote items.

The item cache has two states:

* Not fully poulated - in this state, the cache has either not been populated from the remote repository at all, or
  it has been populated with some of the remote items, but not all of them. The client application can rely on the
  cache for some items, but will need to retrieve from the remote repository for other items.
* Fully populated - in this state, the cache has been populated with all items from the remote repository. The client
  application can rely fully on the local cache, with no need to retrieve anything from the remote repository.

It is up to the client application to provide the ability to retrieve items from the remote repository, and to populate
the local cache with those items, as well as to synchronize any updates that may occur to the remote items with the
local cache.

The items stored in the cache must be uniquely identifiable.

### Creating a Cache Instance

```java
import com.rarysoft.marvin.itemcache.Cache;

public class WidgetRepository {
    // This is the cache. It is instantiated with the means to uniquely identify the items stored within it.
    private final Cache<Widget> cache = new Cache<>(Widget::getId);

    // ...
}
```

### Storing Items In the Cache

When adding individual items to the cache, the state of the cache will not change.

If the cache is not fully populated before adding the widget, the cache has no way of knowing if the newly added
widget is the last outstanding widget, and therefore will leave the state as not fully populated.

If the cache is fully populated before adding the widget, the cache can only assume that this is a brand new widget
that has been added to the remote repository, and therefore will leave the state as fully populated.

```java
import com.rarysoft.marvin.itemcache.Cache;

public class WidgetRepository {
    // ...

    public void addWidget(Widget widget) {
        cache.add(widget);
    }
}
```

### Getting Items From the Cache

When getting individual items from the cache, it is up to the client code to determine the reliability of the cache by
checking its state first.

If the cache is not fully populated, then the cache will return the item if an item with the requested ID is found in
the cache. However, failure to return an item only means that the requested item is not in the cache. It may or may
not exist in the remote repository, and it is up to the client code to retrieve it remotely and then update the cache.

If the cache is fully populated, then a simple get will return the appropriate item. If no item is returned, then no
item exists with the requested ID.

Items retrieved from the cache will be marked internally with an accessed timestamp, allowing the item to potentially
survive access-based cache evictions, which are described later.

```java
import com.rarysoft.marvin.itemcache.Cache;

public class WidgetRepository {
    // ...

    public Optional<Widget> getWidget(Serializable id) {
        if (cache.isFullyPopulated() || cache.contains(id)) {
            return cache.get(id);
        }
        // Retrieve from the remote repository and then use cache.add to store it in the local cache.
        return getRemoteWidgetAndUpdateCache(id);
    }
}
```

### Getting a Count of Items

When getting a count of items, the intent is to provide a total item count, not just a count of cached items. Therefore,
it is only possible to get the item count when the cache is fully populated. It is up to the client code to check the
state of the cache before attempting to get the item count. Failure to do so will result in an `IllegalStateException`.

```java
import com.rarysoft.marvin.itemcache.Cache;

public class WidgetRepository {
    // ...

    public int getWidgetCount() {
        if (cache.isFullyPopulated()) {
            return cache.size();
        }
        // Retrieve the count from the remote repository. Alternatively, you may want to use this opportunity to get
        // the full list of widgets from the remote repository and store them in the cache, making it fully populated.
        return getRemoteWidgetCount();
    }
}
```

### Updating a Cached Item

When updating a cached item with modifications that were made to the remote item, updating an item that is found in the
cache functions the same whether the cache is not fully populated or is fully populated. However, when the item to be
updated is not found in the cache, the behaviour depends on the cache state.

If the cache is not fully populated or is fully populated and the item exists in the cache, the previously cached
item will be replaced with the new item.

If the cache is not fully populated and the item does not exist in the cache, the cache will simply add the updated
item as a new item. The cache will remain in not fully populated state.

If the cache is fully populated and the item does not exist in the cache, the cache will add the item, change the
state to not fully populated, and throw an `IllegalStateException` to alert the client code that there is a
synchronization problem, where the application thinks the cache is fully populated, but in fact is missing at least
this one item and potentially others. Given that the state has been changed, it may be possible to ignore the exception
and continue handling the cache as a not fully populated cache. However, this scenario may indicate a coding error
that should be addressed.

Items updated in the cache will be marked internally with a modified timestamp, allowing the item to potentially
survive modification-based cache evictions, which are described later.

```java
import com.rarysoft.marvin.itemcache.Cache;

public class WidgetRepository {
    // ...

    public void updateWidget(Widget widget) {
        if (cache.isFullyPopulated() || cache.contains(id)) {
            cache.update(widget);
        }
    }
}
```

### Deleting a Cached Item

When deleting an item from the cache, an attempt to delete an item that is not found in the cache when the cache is
fully populated indicates a problem in the application somewhere.

If the cache is not fully populated or is fully populated and the requested item is found in the cache, the item is
removed from the cache.

If the cache is not fully populated and the item is not found in the cache, nothing happens.

If the cache is fully populated and the item is not found in the cache, the cache is marked as not fully populated an
an `IllegalStateException` is thrown. It is possible to continue operating with the cache now in a not fully
populated state, but it is likely that there is an issue that needs to be investigated. The exception allows the
application to provide an alert that this inconsistent state has occurred.

```java
import com.rarysoft.marvin.itemcache.Cache;

public class WidgetRepository {
    // ...

    public void deleteWidget(Serializable id) {
        if (cache.isFullyPopulated() || cache.contains(id)) {
            return cache.delete(id);
        }
    }
}
```

### Evicting Cached Items

It is possible that you want to manage the size of the cache as time goes on. Therefore, it is possible to evict
cached items that have not been accessed recently. If the cache is fully populated, evicting will always set the cache
state to not fully populated.

When a cached item is accessed, it is marked internally with an accessed timestamp. Similarly, when a cached item is
modified, it is marked internally with a modified timestamp. You may want to evict only those items that have not been
accessed within a particular time period, under the assumption that otherwise unaccessed items that have been recently
modified may be good candidates for access in the near future. On the other hand, you may want to evict all items that
have not been accessed within a particular time period, even if they have been modified.

There are two accessed-based eviction methods provided. One evicts the unaccessed items, and the other evicts the
unmodified items. The typical usage would be either to evict only the unaccessed items, or to evict the unaccessed
items and then the unmodified items.

```java
import com.rarysoft.marvin.itemcache.Cache;

public class WidgetRepository {
    // ...

    public void evictUnaccessedWidgets(long ageInMillis) {
        cache.evictUnaccessed(ageInMillis);
    }

    public void evictUnaccessedWidgets(long ageInMillis) {
        cache.evictUnmodified(ageInMillis);
    }
}
```

A third method is provided to evict items based on age, regardless of access or modification.

```java
import com.rarysoft.marvin.itemcache.Cache;

public class WidgetRepository {
    // ...

    public void evictByAge(long ageInMillis) {
        cache.evict(ageInMillis);
    }
}
```

A fourth method is provided to evict all items from the cache.

```java
import com.rarysoft.marvin.itemcache.Cache;

public class WidgetRepository {
    // ...
    public void evictCache() {
        cache.evictAll();
    }
}
```

Regardless which eviction method is used, if the cache was fully populated prior to eviction and at least one item was
evicted, the cache will be left in a not fully populated state. If no items met the criteria for eviction, then the
state will be unaltered.
