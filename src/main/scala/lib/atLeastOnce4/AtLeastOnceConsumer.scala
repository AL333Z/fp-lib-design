package lib.atLeastOnce4

import cats.Id
import cats.effect.{ IO, IOApp, Resource }
import cats.implicits._
import lib.DemoUtils.{ jmsTransactedContextRes, logger, queueName }
import lib.atLeastOnce4.AtLeastOnceConsumer.TransactionResult
import lib.config.DestinationName.QueueName
import lib.jms.{ JmsContext, JmsMessage, JmsMessageConsumer, JmsTransactedContext }
import org.typelevel.keypool.KeyPool

class AtLeastOnceConsumer private[lib] (
  private[lib] val pool: KeyPool[IO, Unit, (JmsContext, JmsMessageConsumer)],
  private[lib] val concurrencyLevel: Int
) {

  def handle(runBusinessLogic: JmsMessage => IO[TransactionResult]): IO[Nothing] =
    IO.parSequenceN[Id, Unit](concurrencyLevel) {
        pool.take(()).use { res =>
          val (ctx, consumer) = res.value
          for {
            message <- consumer.receive
            txRes   <- runBusinessLogic(message)
            _ <- txRes match {
              case TransactionResult.Commit   => IO.blocking(ctx.raw.commit())
              case TransactionResult.Rollback => IO.blocking(ctx.raw.rollback())
            }
          } yield ()
        }
      }
      .foreverM
}

object AtLeastOnceConsumer {
  sealed trait TransactionResult

  object TransactionResult {
    case object Commit   extends TransactionResult
    case object Rollback extends TransactionResult
  }

  def make(
    rootContext: JmsTransactedContext,
    queueName: QueueName,
    concurrencyLevel: Int
  ): Resource[IO, AtLeastOnceConsumer] =
    KeyPool
      .Builder[IO, Unit, (JmsContext, JmsMessageConsumer)](_ =>
        for {
          ctx      <- rootContext.makeTransactedContext
          consumer <- ctx.makeJmsConsumer(queueName)
        } yield (ctx, consumer)
      )
      .withMaxTotal(concurrencyLevel)
      .build
      .map(pool => new AtLeastOnceConsumer(pool, concurrencyLevel))
}

object Demo extends IOApp.Simple {

  override def run: IO[Unit] =
    jmsTransactedContextRes
      .flatMap(ctx => AtLeastOnceConsumer.make(ctx, queueName, 5))
      .use(consumer =>
        consumer.handle { msg =>
          for {
            _ <- logger.info(msg.show)
            //          _ <- ... actual business logic...
          } yield TransactionResult.Commit
        }
      )
}
