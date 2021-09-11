package lib.atLeastOnce1

import cats.effect.{ IO, IOApp, Resource }
import cats.implicits._
import fs2.Stream
import lib.DemoUtils.{ jmsTransactedContextRes, logger, queueName }
import lib.atLeastOnce1.AtLeastOnceConsumer.CommitAction
import lib.config.DestinationName.QueueName
import lib.jms.{ JmsMessage, JmsMessageConsumer, JmsTransactedContext }

object AtLeastOnceConsumer {

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
    val buildStreamingConsumer = (consumer: JmsMessageConsumer) => Stream.eval[IO, JmsMessage](consumer.receive).repeat

    context
      .makeJmsConsumer(queueName)
      .map(buildStreamingConsumer)
      .map(consumer => (consumer, committer))
  }
}

object Demo extends IOApp.Simple {

  override def run: IO[Unit] =
    jmsTransactedContextRes.flatMap(ctx => AtLeastOnceConsumer.make(ctx, queueName)).use {
      case (consumer, committer) =>
        consumer.evalMap { msg =>
          logger.info(msg.show) >> // whatever business logic you need to perform
            committer(CommitAction.Commit)
        }.compile.drain
    }
}
