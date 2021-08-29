package lib.atLeastOnce2

import cats.effect.{ IO, IOApp, Resource }
import cats.implicits._
import lib.DemoUtils.{ jmsTransactedContextRes, logger, queueName }
import lib.atLeastOnce2.AtLeastOnceConsumer.TransactionResult
import lib.config.DestinationName.QueueName
import lib.jms.{ JmsContext, JmsMessage, JmsMessageConsumer, JmsTransactedContext }

class AtLeastOnceConsumer private[lib] (
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

object AtLeastOnceConsumer {
  sealed trait TransactionResult

  object TransactionResult {
    case object Commit   extends TransactionResult
    case object Rollback extends TransactionResult
  }

  def make(context: JmsTransactedContext, queueName: QueueName): Resource[IO, AtLeastOnceConsumer] =
    context.makeJmsConsumer(queueName).map(consumer => new AtLeastOnceConsumer(context, consumer))
}

object Demo extends IOApp.Simple {

  override def run: IO[Unit] =
    jmsTransactedContextRes
      .flatMap(ctx => AtLeastOnceConsumer.make(ctx, queueName))
      .use(consumer =>
        consumer.handle { msg =>
          for {
            _ <- logger.info(msg.show)
            //          _ <- ... actual business logic...
          } yield TransactionResult.Commit
        }
      )
}
