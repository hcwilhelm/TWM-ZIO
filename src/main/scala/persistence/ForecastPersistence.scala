package persistence

import domain.Domain.{City, Forecast}
import zio._

import scala.math.Ordering.Double

object ForecastPersistence {

  trait Service {
    def get(city: City): UIO[Option[Forecast]]

    def set(city: City, forecast: Forecast): UIO[Unit]

    def hottest: Task[(City, Forecast)]
  }

  final case class Live(persistenceService: KeyValuePersistence.Service[City, Forecast]) extends Service {
    implicit val ordering: Double.TotalOrdering.type = Ordering.Double.TotalOrdering

    override def get(city: City): UIO[Option[Forecast]] = persistenceService.get(city)

    override def set(city: City, forecast: Forecast): UIO[Unit] = persistenceService.set(city, forecast)

    override def hottest: Task[(City, Forecast)] = persistenceService.maxBy { case (_, forecast) => forecast.temperature.value }
  }

  val live: ZLayer[KeyValuePersistence[City, Forecast], Nothing, ForecastPersistence] = ZLayer fromService Live


  final def get(city: City): URIO[ForecastPersistence, Option[Forecast]] = ZIO.accessM(_.get.get(city))

  final def set(city: City, forecast: Forecast): URIO[ForecastPersistence, Unit] = ZIO.accessM(_.get.set(city, forecast))

  final def hottest: RIO[ForecastPersistence, (City, Forecast)] = ZIO.accessM(_.get.hottest)
}
