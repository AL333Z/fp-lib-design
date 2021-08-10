package lib

import cats.effect.{ IO, IOApp, Resource }
import cats.implicits._
import fs2.Stream
import lib.DemoUtils._
import lib.JmsTransactedConsumer1.CommitAction
import lib.config.DestinationName.QueueName
import lib.jms.{ JmsMessage, JmsTransactedContext }

object JmsTransactedConsumer1 {

  sealed trait CommitAction

  object CommitAction {
    case object Commit   extends CommitAction
    case object Rollback extends CommitAction
  }

  type Committer = CommitAction => IO[Unit]
  type Consumer  = Stream[IO, JmsMessage]

  def make(context: JmsTransactedContext, queueName: QueueName): Resource[IO, (Consumer, Committer)] = {
    val committer = (txRes: CommitAction) =>
      txRes match {
        case CommitAction.Commit =>
          IO.blocking(context.raw.commit()) // ack all the messages delivered by this context
        case CommitAction.Rollback =>
          IO.blocking(context.raw.rollback()) // do nothing, messages may be redelivered
      }
    context.makeJmsConsumer(queueName).map(consumer => (Stream.eval(consumer.receive).repeat, committer))
  }
}

object SampleJmsTransactedConsumer1 extends IOApp.Simple {

  override def run: IO[Unit] =
    jmsTransactedContextRes.flatMap(ctx => JmsTransactedConsumer1.make(ctx, queueName)).use {
      case (consumer, committer) =>
        consumer.evalMap { msg =>
          logger.info(msg.show) >> // whatever business logic you need to perform
            committer(CommitAction.Commit)
        }.compile.drain
    }
}
