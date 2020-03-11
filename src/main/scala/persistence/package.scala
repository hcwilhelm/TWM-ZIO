import persistence.Persistence.Service
import zio._

package object persistence {
  type Persistence[K, V] = Has[Persistence.Service[K, V]]

  object Persistence {

    trait Service[K, V] {
      def get(key: K): UIO[Option[V]]

      def set(key: K, value: V): UIO[Unit]

      def maxBy[B: Ordering](f: ((K, V)) => B): Task[(K, V)]
    }

    final case class InMemory[K, V](state: Ref[Map[K, V]]) extends Persistence.Service[K, V] {
      override def get(key: K): UIO[Option[V]] = state.get.map(_.get(key))

      override def set(key: K, value: V): UIO[Unit] = state.update(_.updated(key, value))

      override def maxBy[B: Ordering](f: ((K, V)) => B): Task[(K, V)] = for {
        map <- state.get
        max <- Task.effect(map.maxBy(f))
      } yield max
    }

    def inMemory[K, V](implicit tag: Tagged[Service[K, V]]): ZLayer[Any, Nothing, Persistence[K, V]] = ZLayer.fromEffect {
      Ref.make(Map.empty[K, V]).map(state => InMemory(state))
    }
  }

  final def get[R <: Persistence[K, V], K, V](key: K)(implicit tag: Tagged[Service[K, V]]): ZIO[Persistence[K, V], Nothing, Option[V]] =
    ZIO.accessM[Persistence[K, V]](_.get.get(key))

  final def set[R <: Persistence[K, V]: Tagged, K, V](key: K, value: V)(implicit tag: Tagged[Service[K, V]]): ZIO[Persistence[K, V], Nothing, Unit] =
    ZIO.accessM[Persistence[K, V]](_.get.set(key, value))

  final def maxBy[R <: Persistence[K, V]: Tagged, K, V, B: Ordering](f: ((K, V)) => B)(implicit tag: Tagged[Service[K, V]]): ZIO[Persistence[K, V], Throwable, (K, V)] =
    ZIO.accessM[Persistence[K, V]](_.get.maxBy(f))

}
