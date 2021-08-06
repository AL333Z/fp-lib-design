package lib

import cats.effect.{ IO, IOApp }
import cats.implicits._
import lib.DemoUtils._

object DummyMessageProducer extends IOApp.Simple {

  override def run: IO[Unit] =
    jmsContextRes
      .use(ctx =>
        for {
          jmsQueue <- ctx.createQueue(queueName)
          producer <- IO.delay(ctx.context.createProducer())
          _ <- (0 until 10).toList.traverse_ { i =>
            IO.blocking(producer.send(jmsQueue.wrapped, s"Body$i")) >>
              logger.info(s"Sent $i")
          }
        } yield ()
      )
}
