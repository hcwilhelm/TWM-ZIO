package domain

object Domain {
  final case class Configuration(host: String, port: Int, appID: String)

  sealed trait TemperatureUnit
  case object Celsius extends TemperatureUnit
  case object Fahrenheit extends TemperatureUnit

  final case class City(name: String)
  final case class Temperature(value: Double, unit: TemperatureUnit)
  final case class Forecast(temperature: Temperature)
  final case class Request(city: City, forecast: Forecast)

  sealed trait Error extends Throwable
  final case class UnknownCity(city: String) extends Error
  final case object RequestMapEmpty extends Error
  final case class HttpError(code: String, message: String) extends Error
  final case class JsonError(message: String) extends Error
  final case object NoTemperatureFound extends Error
  final case class ConfigurationError(message: String) extends Error
}
