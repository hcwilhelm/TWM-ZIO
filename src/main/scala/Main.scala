import domain.Domain._
import persistence.Persistence
import weather.Weather
import zio._
import zio.console._

import scala.math.Ordering.Double

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

  def fetchForecast(city: City): ZIO[Persistence[City, Forecast] with Weather, Throwable, Forecast] = for {
    maybeForecast <- persistence.get[Persistence[City, Forecast], City, Forecast](city)
    forecast      <- maybeForecast.fold(weather.forecast(city))(ZIO.effectTotal(_))
    _             <- persistence.set[Persistence[City, Forecast], City, Forecast](city, forecast)
  } yield forecast

  val askCity: ZIO[Console, Throwable, City] = for {
    _         <- console.putStrLn(line = "What is the next city ?")
    cityName  <- console.getStrLn
    city      <- cityByName(cityName)
  } yield city

  implicit val ordering: Double.TotalOrdering.type = Ordering.Double.TotalOrdering

  val askFetchJudge: ZIO[Console with Persistence[City, Forecast] with Weather, Throwable, Unit] = for {
    city                <- askCity
    forecast            <- fetchForecast(city)
    _                   <- console.putStrLn(line = s"Forecast for $city is ${forecast.temperature}")
    (hCity, hForecast)  <- persistence.maxBy[Persistence[City, Forecast], City, Forecast, Double](_._2.temperature.value)
    _                   <- console.putStrLn(line = s"Hottest city found so far is ${hCity.name} ${hForecast.temperature.value} ${hForecast.temperature.unit}")
  } yield ()

  val logic: ZIO[Console with Persistence[City, Forecast] with Weather with configuration.Configuration, Throwable, Unit] = for {
    config  <- configuration.getConfig
    _       <- console.putStrLn(s"Host : ${config.host} | Port : ${config.port}")
    _       <- askFetchJudge.forever
  } yield ()


  object PEnv {
    val l: ZLayer[Any, Nothing, Persistence[City, Forecast] with configuration.Configuration] =  Persistence.inMemory[City, Forecast] ++ configuration.Configuration.live
    val w: ZLayer[Any, Nothing, Weather] = configuration.Configuration.live >>> Weather.test
  }



  val program: ZIO[Console, Throwable, Unit] = for {
    logic      <- logic.provideSomeLayer[Console](PEnv.l ++ PEnv.w)
  } yield logic
}