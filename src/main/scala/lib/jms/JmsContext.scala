package lib.jms

import cats.effect.{ IO, Resource }
import lib.config.DestinationName.{ QueueName, TopicName }
import lib.jms.JmsDestination.{ JmsQueue, JmsTopic }
import lib.jms.JmsMessage.JmsTextMessage

import javax.jms.Session

class JmsContext(private[lib] val context: javax.jms.JMSContext) {

  def createTextMessage(value: String): IO[JmsTextMessage] =
    IO.delay(new JmsTextMessage(context.createTextMessage(value)))

  def createQueue(queue: QueueName): IO[JmsQueue] =
    IO.delay(new JmsQueue(context.createQueue(queue.value)))

  def createTopic(topicName: TopicName): IO[JmsTopic] =
    IO.delay(new JmsTopic(context.createTopic(topicName.value)))

  def makeJmsConsumer(queueName: QueueName): Resource[IO, JmsMessageConsumer] =
    for {
      destination <- Resource.eval(createQueue(queueName))
      consumer    <- Resource.fromAutoCloseable(IO.delay(context.createConsumer(destination.wrapped)))
    } yield new JmsMessageConsumer(consumer)

  def makeContextForAcknowledging: Resource[IO, JmsContext] =
    Resource
      .make(IO.blocking(context.createContext(Session.CLIENT_ACKNOWLEDGE)))(context => IO.blocking(context.close()))
      .map(context => new JmsContext(context))

}
