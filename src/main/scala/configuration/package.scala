import zio.Has

package object configuration {
  type AppConfig = Has[AppConfig.Service]
}
