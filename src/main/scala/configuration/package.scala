import domain.Domain
import zio.{Has, RIO, Task, ZIO, ZLayer}

package object configuration {
  type Configuration = Has[Configuration.Service]

  object Configuration extends Serializable {

    trait Service extends Serializable {
      def getConfig: Task[Domain.Configuration]
    }

    val live: ZLayer.NoDeps[Nothing, Configuration] = ZLayer.succeed {
      new Service {
        override def getConfig: Task[Domain.Configuration] =
          Task.effect(Domain.Configuration("http://api.openweathermap.org/data/2.5/weather", 80, "31ee415c28fb16ec573ddc0aeaa3c6b6"))
      }
    }
  }

  final def getConfig: RIO[Configuration, Domain.Configuration] = ZIO.accessM(_.get.getConfig)
}
