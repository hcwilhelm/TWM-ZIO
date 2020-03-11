import zio.Has

package object weather {
  type OpenWeatherClient = Has[OpenWeatherClient.Service]

}
