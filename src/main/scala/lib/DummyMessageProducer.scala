package lib

import cats.effect.{ IO, IOApp }
import cats.implicits._
import lib.DemoUtils._

object DummyMessageProducer extends IOApp.Simple {

  override def run: IO[Unit] =
    jmsTransactedContextRes
      .use(ctx =>
        for {
          jmsQueue <- ctx.createQueue(queueName)
          producer <- IO.delay(ctx.raw.createProducer())
          _ <- List
            .fill(1000)(())
            .traverse_(_ =>
              (0 until 5).toList.traverse_ { i =>
                IO.blocking(producer.send(jmsQueue.wrapped, s"Body$i")) >>
                  logger.info(s"Sent $i")
              } >> IO.blocking(ctx.raw.commit())
            )
        } yield ()
      )
}
