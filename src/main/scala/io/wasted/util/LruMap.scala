package io.wasted.util

import java.util.concurrent.Callable

import com.google.common.cache._

case class KeyHolder[K](key: K)
case class ValueHolder[V](value: V)

/**
 * LruMap Companion to make creation easier
 */
object LruMap {
  def apply[K, V](maxSize: Int): LruMap[K, V] =
    new LruMap[K, V](maxSize, None, None)

  def apply[K, V](maxSize: Int, load: (K) => V): LruMap[K, V] =
    new LruMap[K, V](maxSize, Some(load), None)

  def apply[K, V](maxSize: Int, expire: (K, V) => Any): LruMap[K, V] =
    new LruMap[K, V](maxSize, None, Some(expire))

  def apply[K, V](
    maxSize: Int,
    load:    (K) => V,
    expire:  (K, V) => Any): LruMap[K, V] =
    new LruMap[K, V](maxSize, Some(load), Some(expire))
}

/**
 * LruMap Wrapper for Guava's Cache
 *
 * @param maxSize Maximum size of this cache
 * @param load Function to load objects
 * @param expire Function to be called on expired objects
 * @param builderConf Function to extend the CacheBuilder
 */
class LruMap[K, V](
    val maxSize: Int,
    load:        Option[(K) => V],
    expire:      Option[(K, V) => Any],
    builderConf: Option[CacheBuilder[AnyRef, AnyRef] => CacheBuilder[AnyRef, AnyRef]] = None) {
  lru =>
  private[this] val loader: Option[CacheLoader[KeyHolder[K], ValueHolder[V]]] =
    lru.load.map { loadFunc =>
      new CacheLoader[KeyHolder[K], ValueHolder[V]] {
        def load(key: KeyHolder[K]): ValueHolder[V] =
          ValueHolder(loadFunc(key.key))
      }
    }

  private[this] val removal: Option[RemovalListener[KeyHolder[K], ValueHolder[V]]] = lru.expire.map {
    expireFunc =>
      new RemovalListener[KeyHolder[K], ValueHolder[V]] {
        def onRemoval(
          removal: RemovalNotification[KeyHolder[K], ValueHolder[V]]): Unit =
          expireFunc(removal.getKey.key, removal.getValue.value)
      }
  }

  /**
   * Underlying Guava Cache
   */
  val cache: Cache[KeyHolder[K], ValueHolder[V]] = {
    val builder = CacheBuilder.newBuilder().maximumSize(maxSize)
    builderConf.map(_(builder))
    (loader, removal) match {
      case (Some(loaderO), Some(removalO)) =>
        builder.removalListener(removalO).build(loaderO)
      case (Some(loaderO), None)  => builder.build(loaderO)
      case (None, Some(removalO)) => builder.removalListener(removalO).build()
      case _                      => builder.build()
    }
  }

  /**
   * Current size of this LRU Map
   */
  def size = cache.size.toInt

  /**
   * Puts a value
   * @param key Key to put the Value for
   * @param value Value to put for the Key
   */
  def put(key: K, value: V): Unit =
    cache.put(KeyHolder(key), ValueHolder(value))

  /**
   * Gets a value associated with the given key.
   * @param key Key to get the Value for
   */
  def get(key: K): Option[V] =
    Option(cache.getIfPresent(KeyHolder(key))).map(_.value)

  /**
   * Get the value associated with the given key. If no value is already associated, then associate the given value
   * with the key and use it as the return value.
   *
   * @param key Key to put the Value for
   * @param value Value to put for the Key
   * @return
   */
  def getOrElseUpdate(key: K, value: => V): V = {
    cache
      .get(KeyHolder(key), new Callable[ValueHolder[V]] {
        def call(): ValueHolder[V] = ValueHolder(value)
      })
      .value
  }

  /**
   * Remove a value by key
   * @param key Key to be removed
   */
  def remove(key: K): Unit = cache.invalidate(KeyHolder(key))
}
