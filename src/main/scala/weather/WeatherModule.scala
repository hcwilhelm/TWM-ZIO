package weather

import config.ConfigurationModule
import domain.Domain
import domain.Domain._
import io.circe
import io.circe.Json
import io.circe.optics.JsonPath._
import sttp.client._
import sttp.client.circe._
import zio.ZIO
import zio.console.Console

trait WeatherModule extends Serializable{
  val weatherModule: WeatherModule.Service[Any]
}

object WeatherModule extends Serializable{
  trait Service[R] {
    def forecast(city: City): ZIO[R, Throwable, Forecast]
  }

  trait Test extends WeatherModule {
    override val weatherModule: Service[Any] = new Service[Any] {
      override def forecast(city: City): ZIO[Any, Error, Forecast] = ZIO.effect {
        city match {
          case City("Hamburg")  => Forecast(Temperature(20, Celsius))
          case City("Berlin")   => Forecast(Temperature(22, Celsius))
          case City("Paris")    => Forecast(Temperature(25, Celsius))
          case City("Cadiz")    => Forecast(Temperature(29, Celsius))
        }
      }.refineOrDie { case _: MatchError => Domain.HttpError("404", "woops") }
    }
  }

  object Test extends Test

  trait Live extends WeatherModule {
    val configuration: ConfigurationModule.Service[Any]
    val console: Console.Service[Any]

    import sttp.client.asynchttpclient.zio.AsyncHttpClientZioBackend

    override val weatherModule: Service[Any] = new Service[Any] {
      override def forecast(city: City): ZIO[Any, Throwable, Forecast] = for {
        response <- requestForecast(city)
        temp <- parseResponse(response)
      } yield Forecast(Temperature(temp, Celsius))
    }

    private def requestForecast(city: City): ZIO[Any, Throwable, Response[Either[ResponseError[circe.Error], Json]]] = for {
      config    <- configuration.getConfig
      _ <- console.putStrLn(uri"${config.host}?q=${city.name}&appid=${config.appID}&units=metric".port(config.port).toString())
      response  <- AsyncHttpClientZioBackend().flatMap { implicit backend =>
        basicRequest
          .get(uri"${config.host}?q=${city.name}&appid=${config.appID}&units=metric".port(config.port))
          .response(asJson[Json])
          .send()
      }

    } yield response

    private val temperatureLens = root.main.temp.double

    private def parseResponse(response: Response[Either[ResponseError[circe.Error], Json]]): ZIO[Any, Exception, Double] = for {
      body <- ZIO.fromEither(response.body)
      temp <- ZIO.fromOption(temperatureLens.getOption(body)).mapError( _ => NoTemperatureFound )
    } yield temp
  }

  object factory extends WeatherModule.Service[WeatherModule] {
    final val weatherModuleService: ZIO[WeatherModule, Throwable, WeatherModule.Service[Any]] = ZIO.access(_.weatherModule)

    final def forecast(city: City): ZIO[WeatherModule, Throwable, Forecast] = ZIO.accessM(_.weatherModule.forecast(city))
  }
}
