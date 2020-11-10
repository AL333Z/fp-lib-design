package lib

import cats.effect.{ExitCode, IO, IOApp, Resource}
import lib.config.DestinationName.QueueName
import lib.jms.JmsContext

object SampleConsumer extends IOApp {
  // an actual JmsContext with an implementation for a specific provider
  val jmsContextRes: Resource[IO, JmsContext] = null

  override def run(args: List[String]): IO[ExitCode] = {
    val jmsConsumerRes = for {
      jmsContext <- jmsContextRes
      consumer <- jmsContext.createJmsConsumer(QueueName("QUEUE1"))
    } yield consumer

    jmsConsumerRes.use(consumer =>
      for {
        msg <- consumer.receiveJmsMessage
        textMsg <- IO.fromTry(msg.attemptAsJmsTextMessage)
        _ <- IO.delay(println(s"Got 1 message with text: $textMsg. Ending now."))
      } yield ()
    ).as(ExitCode.Success)
  }
}
