package lib.jms

import cats.effect.{ IO, Resource }
import lib.config.DestinationName.{ QueueName, TopicName }
import lib.jms.JmsDestination.{ JmsQueue, JmsTopic }
import lib.jms.JmsMessage.JmsTextMessage

sealed abstract class JmsContext(private[lib] val raw: javax.jms.JMSContext) {

  def createTextMessage(value: String): IO[JmsTextMessage] =
    IO.delay(new JmsTextMessage(raw.createTextMessage(value)))

  def createQueue(queue: QueueName): IO[JmsQueue] =
    IO.delay(new JmsQueue(raw.createQueue(queue.value)))

  def createTopic(topicName: TopicName): IO[JmsTopic] =
    IO.delay(new JmsTopic(raw.createTopic(topicName.value)))

  def makeJmsConsumer(queueName: QueueName): Resource[IO, JmsMessageConsumer] =
    for {
      destination <- Resource.eval(createQueue(queueName))
      consumer    <- Resource.fromAutoCloseable(IO.delay(raw.createConsumer(destination.wrapped)))
    } yield new JmsMessageConsumer(consumer)

  def makeTransactedContext: Resource[IO, JmsTransactedContext] =
    Resource
      .make(IO.blocking(raw.createContext(javax.jms.Session.SESSION_TRANSACTED)))(context =>
        IO.blocking(context.close())
      )
      .map(context => new JmsTransactedContext(context))

}

class JmsTransactedContext private[lib] (override private[lib] val raw: javax.jms.JMSContext) extends JmsContext(raw)
