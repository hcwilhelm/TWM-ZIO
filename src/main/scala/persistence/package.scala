import zio.Has

package object persistence {
  type KeyValuePersistence[K, V] = Has[KeyValuePersistence.Service[K, V]]
  type ForecastPersistence = Has[ForecastPersistence.Service]
}
