package lib

import cats.effect.{ IO, IOApp }
import lib.DemoUtils._

object SampleConsumer extends IOApp.Simple {

  override def run: IO[Unit] = {
    val jmsConsumerRes = for {
      jmsContext <- jmsTransactedContextRes
      consumer   <- jmsContext.makeJmsConsumer(queueName)
    } yield consumer

    jmsConsumerRes
      .use(consumer =>
        for {
          msg     <- consumer.receive
          textMsg <- IO.fromTry(msg.tryAsJmsTextMessage)
          _       <- logger.info(s"Got 1 message with text: $textMsg. Ending now.")
        } yield ()
      )
  }
}
