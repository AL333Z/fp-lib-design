package lib

import cats.effect.{ IO, IOApp, Resource }
import cats.implicits._
import lib.DemoUtils._
import lib.JmsAcknowledgerConsumer2.AckResult
import lib.config.DestinationName.QueueName
import lib.jms.{ JmsContext, JmsMessage, JmsMessageConsumer }

class JmsAcknowledgerConsumer2 private[lib] (
  private[lib] val ctx: JmsContext,
  private[lib] val consumer: JmsMessageConsumer
) {

  def handle(runBusinessLogic: JmsMessage => IO[AckResult]): IO[Nothing] =
    consumer.receive
      .flatMap(runBusinessLogic)
      .flatMap {
        case AckResult.Ack  => IO.blocking(ctx.context.acknowledge())
        case AckResult.NAck => IO.unit
      }
      .foreverM
}

object JmsAcknowledgerConsumer2 {
  sealed trait AckResult

  object AckResult {
    case object Ack  extends AckResult
    case object NAck extends AckResult
  }

  def make(context: JmsContext, queueName: QueueName): Resource[IO, JmsAcknowledgerConsumer2] =
    for {
      ctx      <- context.makeContextForAcknowledging
      consumer <- ctx.makeJmsConsumer(queueName)
    } yield new JmsAcknowledgerConsumer2(ctx, consumer)
}

object SampleJmsAcknowledgerConsumer2 extends IOApp.Simple {

  override def run: IO[Unit] =
    jmsContextRes
      .flatMap(ctx => JmsAcknowledgerConsumer2.make(ctx, queueName))
      .use(consumer =>
        consumer.handle { msg =>
          for {
            _ <- logger.info(msg.show)
//          _ <- ... actual business logic...
          } yield AckResult.Ack
        }
      )
}
