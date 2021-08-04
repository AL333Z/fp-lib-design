package lib

import cats.effect.{ IO, IOApp, Resource }
import lib.config.DestinationName.QueueName
import lib.jms.JmsContext

object SampleConsumer extends IOApp.Simple {
  // an actual JmsContext with an implementation for a specific provider
  val jmsContextRes: Resource[IO, JmsContext] = null

  override def run: IO[Unit] = {
    val jmsConsumerRes = for {
      jmsContext <- jmsContextRes
      consumer   <- jmsContext.makeJmsConsumer(QueueName("QUEUE1"))
    } yield consumer

    jmsConsumerRes
      .use(consumer =>
        for {
          msg     <- consumer.receive
          textMsg <- IO.fromTry(msg.tryAsJmsTextMessage)
          _       <- IO.delay(println(s"Got 1 message with text: $textMsg. Ending now."))
        } yield ()
      )
  }
}
