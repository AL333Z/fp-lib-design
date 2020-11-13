package lib.providers

import cats.data.NonEmptyList
import cats.effect.{IO, Resource}
import com.ibm.mq.jms.MQConnectionFactory
import com.ibm.msg.client.wmq.common.CommonConstants
import lib.jms.JmsContext

object ibmMQ {

  case class Config(
                     qm: QueueManager,
                     endpoints: NonEmptyList[Endpoint],
                     channel: Channel,
                     username: Option[Username] = None,
                     password: Option[Password] = None,
                     clientId: ClientId
                   )

  case class Username(value: String) extends AnyVal

  case class Password(value: String) extends AnyVal

  case class Endpoint(host: String, port: Int)

  case class QueueManager(value: String) extends AnyVal

  case class Channel(value: String) extends AnyVal

  case class ClientId(value: String) extends AnyVal

  def makeJmsClient(config: Config): Resource[IO, JmsContext] = {
    for {
      context <- Resource.fromAutoCloseable(IO.delay {
        val connectionFactory: MQConnectionFactory = new MQConnectionFactory()
        connectionFactory.setTransportType(CommonConstants.WMQ_CM_CLIENT)
        connectionFactory.setQueueManager(config.qm.value)
        connectionFactory.setConnectionNameList(hosts(config.endpoints))
        connectionFactory.setChannel(config.channel.value)
        connectionFactory.setClientID(config.clientId.value)
        config.username.map { username =>
          connectionFactory.createContext(
            username.value,
            config.password.map(_.value).getOrElse("")
          )
        }.getOrElse(connectionFactory.createContext())
      })
    } yield new JmsContext(context)
  }

  private def hosts(endpoints: NonEmptyList[Endpoint]): String =
    endpoints.map(e => s"${e.host}(${e.port})").toList.mkString(",")

}
