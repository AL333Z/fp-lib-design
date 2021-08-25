package lib

import cats.Id
import cats.effect.std.Queue
import cats.effect.{ IO, IOApp, Resource }
import cats.implicits._
import lib.DemoUtils._
import lib.JmsTransactedConsumer3.TransactionResult
import lib.config.DestinationName.QueueName
import lib.jms.{ JmsContext, JmsMessage, JmsMessageConsumer, JmsTransactedContext }

class JmsTransactedConsumer3 private[lib] (
  private[lib] val pool: Queue[IO, (JmsContext, JmsMessageConsumer)],
  private[lib] val concurrencyLevel: Int
) {

  def handle(runBusinessLogic: JmsMessage => IO[TransactionResult]): IO[Nothing] =
    IO.parSequenceN[Id, Unit](concurrencyLevel) {
        for {
          (ctx, consumer) <- pool.take
          message         <- consumer.receive
          txRes           <- runBusinessLogic(message)
          _ <- txRes match {
            case TransactionResult.Commit   => IO.blocking(ctx.raw.commit())
            case TransactionResult.Rollback => IO.blocking(ctx.raw.rollback())
          }
          _ <- pool.offer((ctx, consumer))
        } yield ()
      }
      .foreverM
}

object JmsTransactedConsumer3 {
  sealed trait TransactionResult

  object TransactionResult {
    case object Commit   extends TransactionResult
    case object Rollback extends TransactionResult
  }

  def make(
    rootContext: JmsTransactedContext,
    queueName: QueueName,
    concurrencyLevel: Int
  ): Resource[IO, JmsTransactedConsumer3] =
    for {
      pool <- Resource.eval(Queue.bounded[IO, (JmsContext, JmsMessageConsumer)](concurrencyLevel))
      _ <- List
        .fill(concurrencyLevel)(())
        .traverse_(_ =>
          for {
            ctx      <- rootContext.makeTransactedContext
            consumer <- ctx.makeJmsConsumer(queueName)
            _        <- Resource.eval(pool.offer((ctx, consumer)))
          } yield ()
        )
    } yield new JmsTransactedConsumer3(pool, concurrencyLevel)
}

object SampleJmsTransactedConsumer3 extends IOApp.Simple {

  override def run: IO[Unit] =
    jmsTransactedContextRes
      .flatMap(ctx => JmsTransactedConsumer3.make(ctx, queueName, 5))
      .use(consumer =>
        consumer.handle { msg =>
          for {
            _ <- logger.info(msg.show)
//          _ <- ... actual business logic...
          } yield TransactionResult.Commit
        }
      )
}
