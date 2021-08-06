package lib

import cats.effect.{ IO, IOApp, Resource }
import cats.implicits._
import fs2.Stream
import lib.DemoUtils._
import lib.JmsAcknowledgerConsumer1.AckResult
import lib.config.DestinationName.QueueName
import lib.jms.{ JmsContext, JmsMessage }

object JmsAcknowledgerConsumer1 {

  type Acker    = AckResult => IO[Unit]
  type Consumer = Stream[IO, JmsMessage]

  sealed trait AckResult

  object AckResult {
    case object Ack  extends AckResult
    case object NAck extends AckResult
  }

  def make(context: JmsContext, queueName: QueueName): Resource[IO, (Consumer, Acker)] =
    for {
      ctx <- context.makeContextForAcknowledging
      acker = (ackResult: AckResult) =>
        ackResult match {
          case AckResult.Ack  => IO.blocking(ctx.context.acknowledge()) // ack all the messages delivered by this context
          case AckResult.NAck => IO.unit                                // do nothing, messages may be redelivered
        }
      consumer <- ctx.makeJmsConsumer(queueName)
    } yield (Stream.eval(consumer.receive).repeat, acker)
}

object SampleJmsAcknowledgerConsumer1 extends IOApp.Simple {

  override def run: IO[Unit] =
    jmsContextRes.flatMap(ctx => JmsAcknowledgerConsumer1.make(ctx, queueName)).use {
      case (consumer, acker) =>
        consumer.evalMap { msg =>
          logger.info(msg.show) >> // whatever business logic you need to perform
            acker(AckResult.Ack)
        }.compile.drain
    }
}
