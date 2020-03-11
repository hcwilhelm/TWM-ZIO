import configuration.AppConfig
import domain.Domain._
import persistence.{ForecastPersistence, KeyValuePersistence}
import weather.OpenWeatherClient
import zio.ZLayer.NoDeps
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

  def fetchForecast(city: City): ZIO[ForecastPersistence with OpenWeatherClient, Throwable, Forecast] = for {
    maybeForecast <- ForecastPersistence.get(city)
    forecast      <- maybeForecast.fold(OpenWeatherClient.forecast(city))(ZIO.effectTotal(_))
    _             <- ForecastPersistence.set(city, forecast)
  } yield forecast

  val askCity: ZIO[Console, Throwable, City] = for {
    _         <- console.putStrLn(line = "What is the next city ?")
    cityName  <- console.getStrLn
    city      <- cityByName(cityName)
  } yield city

  val askFetchJudge: ZIO[Console with ForecastPersistence with OpenWeatherClient, Throwable, Unit] = for {
    city                <- askCity
    forecast            <- fetchForecast(city)
    _                   <- console.putStrLn(line = s"Forecast for $city is ${forecast.temperature}")
    (hCity, hForecast)  <- ForecastPersistence.hottest
    _                   <- console.putStrLn(line = s"Hottest city found so far is ${hCity.name} ${hForecast.temperature.value} ${hForecast.temperature.unit}")
  } yield ()

  val logic: ZIO[Console with ForecastPersistence with OpenWeatherClient with AppConfig, Throwable, Unit] = for {
    config  <- AppConfig.getConfig
    _       <- console.putStrLn(s"Host : ${config.host} | Port : ${config.port}")
    _       <- askFetchJudge.forever
  } yield ()

  val appConfigLayer: NoDeps[Nothing, AppConfig] = AppConfig.live
  val forecastPersistenceLayer: NoDeps[Nothing, ForecastPersistence] = KeyValuePersistence.inMemory[City, Forecast] >>> ForecastPersistence.live
  val openWeatherClientLayer: NoDeps[Nothing, OpenWeatherClient] = Console.live ++ appConfigLayer >>> OpenWeatherClient.live

  val program: ZIO[Console, Throwable, Unit] = logic.provideSomeLayer[Console](appConfigLayer ++ forecastPersistenceLayer ++ openWeatherClientLayer)
}