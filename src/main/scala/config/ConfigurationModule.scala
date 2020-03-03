package config

import domain.Domain.Configuration
import zio.{UIO, ZIO}

trait ConfigurationModule extends Serializable {
  val configurationModule: ConfigurationModule.Service[Any]
}

object ConfigurationModule extends Serializable {
  trait Service[R] {
    def getConfig: ZIO[R, Error, Configuration]
  }

  trait Live extends ConfigurationModule {
    override val configurationModule: Service[Any] = new Service[Any] {
      override def getConfig: ZIO[Any, Error, Configuration] = ZIO.effectTotal(Configuration("http://api.openweathermap.org/data/2.5/weather", 80, "31ee415c28fb16ec573ddc0aeaa3c6b6"))
    }
  }

  object Live extends Live

  object factory extends ConfigurationModule.Service[ConfigurationModule] {
    final val configurationModuleService: ZIO[ConfigurationModule, Error, ConfigurationModule.Service[Any]] = ZIO.access(_.configurationModule)

    final def getConfig: ZIO[ConfigurationModule, Error, Configuration] = ZIO.accessM(_.configurationModule.getConfig)
  }
}