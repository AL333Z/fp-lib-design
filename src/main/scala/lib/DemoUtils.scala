package lib

import cats.data.NonEmptyList
import cats.effect.{ IO, Resource }
import lib.config.DestinationName.QueueName
import lib.jms.JmsTransactedContext
import lib.providers.ibmMQ
import lib.providers.ibmMQ._
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

object DemoUtils {
  val logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  val jmsTransactedContextRes: Resource[IO, JmsTransactedContext] =
    ibmMQ.makeTransactedJmsClient(
      Config(
        qm = QueueManager("QM1"),
        endpoints = NonEmptyList.one(Endpoint("localhost", 1414)),
        channel = Channel("DEV.APP.SVRCONN"),
        username = Some(Username("app")),
        password = None,
        clientId = ClientId("jms-specs")
      )
    )

  val queueName: QueueName = QueueName("DEV.QUEUE.1")
}
