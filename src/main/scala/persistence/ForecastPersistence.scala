package persistence

import domain.Domain._
import zio._

trait ForecastPersistence extends Serializable{
  val forecastPersistence: ForecastPersistence.Service[Any]
}

object ForecastPersistence {
  trait Service[R] {
    def insert(request: Request): ZIO[R, Nothing, Unit]
    def findByCity(city: City): ZIO[R, Nothing, Option[Forecast]]
    def findHottest: ZIO[R, Error, Request]
  }

  trait InMemory extends ForecastPersistence {
    val requestsMapRef: Ref[Map[City, Forecast]]

    implicit val ordering = Ordering.Double.TotalOrdering

    override val forecastPersistence: Service[Any] = new Service[Any] {
      override def insert(request: Request): ZIO[Any, Nothing, Unit] = for {
        _ <- requestsMapRef.update(_ + (request.city -> request.forecast))
      } yield ()

      override def findByCity(city: City): ZIO[Any, Nothing, Option[Forecast]] = for {
        requestMap <- requestsMapRef.get
        forecast <- ZIO.effectTotal(requestMap.get(city))
      } yield forecast

      override def findHottest: ZIO[Any, Error, Request] = for {
        requestMap <- requestsMapRef.get
        maxRequest <- ZIO.effect {
          requestMap.maxBy { case (_, forecast) => forecast.temperature.value } match {
            case (city, forecast) => Request(city, forecast)
          }
        }.refineOrDie{ case _: UnsupportedOperationException => RequestMapEmpty }
      } yield maxRequest
    }
  }

  object factory extends ForecastPersistence.Service[ForecastPersistence] {
    final val forecastPersistence: ZIO[ForecastPersistence, Nothing, ForecastPersistence.Service[Any]] =
      ZIO.access(_.forecastPersistence)

    final def insert(request: Request): ZIO[ForecastPersistence, Nothing, Unit] =
      ZIO.accessM(_.forecastPersistence.insert(request))

    final def findByCity(city: City): ZIO[ForecastPersistence, Nothing, Option[Forecast]] =
      ZIO.accessM(_.forecastPersistence.findByCity(city))

    final def findHottest: ZIO[ForecastPersistence, Error, Request] =
      ZIO.accessM(_.forecastPersistence.findHottest)
  }
}
