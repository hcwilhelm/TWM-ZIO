import config.ConfigurationModule
import domain.Domain._
import persistence.ForecastPersistence
import weather.WeatherModule
import zio._
import zio.console._

object Main extends App {

  def run(args: List[String]): URIO[Console, Int] =
    program.tapError(e => console.putStrLn(e.toString)).fold(_ => 1, _ => 0)

  def cityByName(cityName: String): ZIO[Any, Error, City] = cityName match {
    case "Berlin" => ZIO.effectTotal(City(cityName))
    case "Hamburg" => ZIO.effectTotal(City(cityName))
    case "Paris" => ZIO.effectTotal(City(cityName))
    case "Cadiz" => ZIO.effectTotal(City(cityName))
    case _ => ZIO.fail(UnknownCity(cityName))
  }

  def fetchForecast(city: City): ZIO[ForecastPersistence with WeatherModule, Throwable, Forecast] = for {
    maybeForecast <- ForecastPersistence.factory.findByCity(city)
    forecast      <- maybeForecast.fold(WeatherModule.factory.forecast(city))(ZIO.effectTotal(_))
    _             <- ForecastPersistence.factory.insert(Request(city, forecast))
  } yield forecast

  val askCity: ZIO[Console, Throwable, City] = for {
    _ <- console.putStrLn("What is the next city ?")
    cityName <- console.getStrLn
    city <- cityByName(cityName)
  } yield city

  val askFetchJudge: ZIO[Console with ForecastPersistence with WeatherModule, Throwable, Unit] = for {
    city      <- askCity
    forecast  <- fetchForecast(city)
    _         <- console.putStrLn(s"Forecast for $city is ${forecast.temperature}")
    hottest   <- ForecastPersistence.factory.findHottest
    _         <- console.putStrLn(s"Hottest city found so far is ${hottest.city.name} ${hottest.forecast.temperature.value} ${hottest.forecast.temperature.unit}")
  } yield ()

  val logic: ZIO[Console with ForecastPersistence with WeatherModule with ConfigurationModule, Throwable, Unit] = for {
    config  <- ConfigurationModule.factory.getConfig
    _       <- console.putStrLn(s"Host : ${config.host} | Port : ${config.port}")
    _       <- askFetchJudge.forever
  } yield ()

  val program: ZIO[Console, Throwable, Unit] = for {
    requestMap <- Ref.make[Map[City, Forecast]](Map.empty)
    logic      <- logic.provideSome[Console]{c =>
      new Console with WeatherModule.Live with ConfigurationModule.Live with ForecastPersistence.InMemory {
        println(configurationModule)
        override val console: Console.Service[Any] = c.console
        override val requestsMapRef: Ref[Map[City, Forecast]] = requestMap
        override val configuration: ConfigurationModule.Service[Any] = configurationModule
      }
    }
  } yield logic
}