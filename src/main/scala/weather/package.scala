import configuration.Configuration
import domain.Domain.{Celsius, City, Forecast, HttpError, Temperature}
import zio.{Has, RIO, Task, ZIO, ZLayer}

package object weather {
  type Weather = Has[Weather.Service]

  object Weather {
    trait Service {
      def forecast(city: City): Task[Forecast]
    }

    final case class Test(configuration: Configuration.Service) extends Service {
      override def forecast(city: City): Task[Forecast] = ZIO.effect {
        city match {
          case City("Hamburg") => Forecast(Temperature(20, Celsius))
          case City("Berlin") => Forecast(Temperature(22, Celsius))
          case City("Paris") => Forecast(Temperature(25, Celsius))
          case City("Cadiz") => Forecast(Temperature(29, Celsius))
        }
      }.refineOrDie { case _: MatchError => HttpError("404", "woops") }
    }

    val test: ZLayer[Configuration, Nothing, Weather] = ZLayer fromService Weather.Test
  }

  final def forecast(city: City): RIO[Weather, Forecast] = ZIO.accessM(_.get.forecast(city))
}
