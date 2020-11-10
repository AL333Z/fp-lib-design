package lib.jms

import cats.effect.IO
import cats.syntax.all._
import lib.config.DestinationName
import lib.config.DestinationName.{QueueName, TopicName}
import lib.jms.JmsDestination.{JmsQueue, JmsTopic}
import lib.jms.JmsMessage.JmsTextMessage

class JmsContext(private val context: javax.jms.JMSContext) {

  def createTextMessage(value: String): IO[JmsTextMessage] =
    IO.delay(new JmsTextMessage(context.createTextMessage(value)))

  private def createQueue(queue: QueueName): IO[JmsQueue] =
    IO.delay(new JmsQueue(context.createQueue(queue.value)))

  private def createTopic(topicName: TopicName): IO[JmsTopic] =
    IO.delay(new JmsTopic(context.createTopic(topicName.value)))

  def createDestination(destination: DestinationName): IO[JmsDestination] = destination match {
    case q: QueueName => createQueue(q).widen[JmsDestination]
    case t: TopicName => createTopic(t).widen[JmsDestination]
  }
}
