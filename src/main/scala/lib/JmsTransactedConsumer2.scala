package lib

import cats.effect.{ IO, IOApp, Resource }
import cats.implicits._
import lib.DemoUtils._
import lib.JmsTransactedConsumer2.TransactionResult
import lib.config.DestinationName.QueueName
import lib.jms.{ JmsContext, JmsMessage, JmsMessageConsumer, JmsTransactedContext }

class JmsTransactedConsumer2 private[lib] (
  private[lib] val ctx: JmsContext,
  private[lib] val consumer: JmsMessageConsumer
) {

  def handle(runBusinessLogic: JmsMessage => IO[TransactionResult]): IO[Nothing] =
    consumer.receive
      .flatMap(runBusinessLogic)
      .flatMap {
        case TransactionResult.Commit   => IO.blocking(ctx.raw.commit())
        case TransactionResult.Rollback => IO.blocking(ctx.raw.rollback())
      }
      .foreverM
}

object JmsTransactedConsumer2 {
  sealed trait TransactionResult

  object TransactionResult {
    case object Commit   extends TransactionResult
    case object Rollback extends TransactionResult
  }

  def make(context: JmsTransactedContext, queueName: QueueName): Resource[IO, JmsTransactedConsumer2] =
    context.makeJmsConsumer(queueName).map(consumer => new JmsTransactedConsumer2(context, consumer))
}

object SampleJmsTransactedConsumer2 extends IOApp.Simple {

  override def run: IO[Unit] =
    jmsTransactedContextRes
      .flatMap(ctx => JmsTransactedConsumer2.make(ctx, queueName))
      .use(consumer =>
        consumer.handle { msg =>
          for {
            _ <- logger.info(msg.show)
//          _ <- ... actual business logic...
          } yield TransactionResult.Commit
        }
      )
}
