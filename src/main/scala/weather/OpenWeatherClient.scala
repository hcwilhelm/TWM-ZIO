package weather

import configuration.AppConfig
import domain.Domain
import domain.Domain._
import io.circe
import io.circe.Json
import io.circe.optics.JsonPath._
import sttp.client._
import sttp.client.asynchttpclient.zio.AsyncHttpClientZioBackend
import sttp.client.circe._
import zio.console.Console
import zio.{RIO, Task, ZIO, ZLayer}

object OpenWeatherClient {

  trait Service {
    def forecast(city: City): Task[Forecast]
  }

  final case class Test(configService: AppConfig.Service) extends Service {
    override def forecast(city: City): Task[Forecast] = ZIO.effect {
      city match {
        case City("Hamburg") => Forecast(Temperature(20, Celsius))
        case City("Berlin") => Forecast(Temperature(22, Celsius))
        case City("Paris") => Forecast(Temperature(25, Celsius))
        case City("Cadiz") => Forecast(Temperature(29, Celsius))
      }
    }.refineOrDie { case _: MatchError => Domain.HttpError("404", "woops") }
  }

  val test: ZLayer[AppConfig, Nothing, OpenWeatherClient] = ZLayer fromService Test

  final case class Live(configService: AppConfig.Service, console: Console.Service) extends Service {
    private val temperatureLens = root.main.temp.double

    override def forecast(city: City): Task[Forecast] = for {
      response  <- requestForecast(city)
      temp      <- parseResponse(response)
    } yield Forecast(Temperature(temp, Celsius))

    private def requestForecast(city: City): ZIO[Any, Throwable, Response[Either[ResponseError[circe.Error], Json]]] =  for {
      config    <- configService.getConfig
      _         <- console.putStrLn(line = "Call to : " + uri"${config.host}?q=${city.name}&appid=${config.appID}&units=metric".port(config.port).toString())
      response  <- AsyncHttpClientZioBackend().flatMap { implicit backend =>
        basicRequest.get(uri"${config.host}?q=${city.name}&appid=${config.appID}&units=metric".port(config.port))
          .response(asJson[Json])
          .send()
      }
    } yield response

    private def parseResponse(response: Response[Either[ResponseError[circe.Error], Json]]) = for {
      body <- ZIO.fromEither(response.body)
      temp <- ZIO.fromOption(temperatureLens.getOption(body)).mapError( _ => NoTemperatureFound )
    } yield temp
  }

  val live: ZLayer[AppConfig with Console, Nothing, OpenWeatherClient] = ZLayer.fromServices[AppConfig.Service, Console.Service, OpenWeatherClient.Service] {
    (appConfig: AppConfig.Service, console: Console.Service) => Live(appConfig, console)
  }

  final def forecast(city: City): RIO[OpenWeatherClient, Forecast] = ZIO.accessM(_.get.forecast(city))
}

