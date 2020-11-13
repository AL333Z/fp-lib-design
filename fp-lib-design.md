autoscale: true

## Sketching a library APIs with Functional Programming

### _Lessons learned while filling the gap_

---

# Who am I

## _@al333z_
### Software Engineer
### Member of _@FPinBO_ ![inline 10%](pics/fpinbo.jpg)
### Runner

![right](pics/pic.jpg)

---

# Why this talk?

### _Functional Programming is great, but..._

- not all the languages are born this way
- lot of libs/apis which are just __not designed with FP in mind__
- one may argue that FP is __not suited__ for all the use cases

---

# I disagree

---

# Agenda

- A reference library to wrap
- Sketching library design
  - Introduce a bunch of building blocks
  - Refine edges, evaluate alternatives
  - Iterate

---

# A reference library

__Java Message Service__ a.k.a. JMS

- provides generic messaging models
- able to handle the producer–consumer problem
- can be used to facilitate the sending and receiving of messages between software systems

---
# JMS main elements

- __Provider__: an implementation of JMS (ActiveMQ, IBM MQ, RabbitMQ, etc...)
- __Producer__/__Publisher__: a client that creates/sends messages
- __Consumer__/__Subscriber__: a client that receives messages
- __Message__: a object that contains the data being transferred
- __Queue__: a buffer that contains messages sent and waiting to be read
- __Topic__: a mechanism for sending messages that are delivered to multiple subscribers

---

# Why JMS?

- ~~old~~ stable enough (born in 1998, latest revision in 2015)
- its apis are a __good testbed for sketching a purely functional wrapper__
  - loads of state, side-effects, exceptions, ...
- found pretty much nothing about
- I don't like suffering to much while working

---

# Disclaimer

Our focus here is **_NOT_** on building the coolest library doing the coolest thing ever

Chances are that you'll never use JMS at all!

We'll just put our attention on **_designing a set of APIs_** which wraps an existing non-functional lib, using Pure Functional Programming in Scala

---

# Why Scala

---

# Why Scala

## _I know Scala_

---

# Why Scala

- rich set of features which let us code using pure functional programming without any frills: 
  - immutability, ADTs, pattern matching
  - higher-kinded types
  - type classes
- __mature and complete ecosystem__ of FP libs (cats, cats-effects, fs2, etc..)

---

# Let's start

---

# A look at the beast: sending

```java
public void sendMessage(ConnectionFactory connectionFactory, 
                        Queue queue, 
                        String text) {
   try (JMSContext context = connectionFactory.createContext();){
      Message msg = context.createTextMessage(text);
      context.createProducer().send(queue, msg);
   } catch (JMSRuntimeException ex) {
      // ...
   }
}
```

- `JMSContext` is in charge of _opening low level stuff_ (connections, sessions, ...), implements `AutoClosable` (see the try-with-resources block)
- `JMSProducer` is a lightweight abstraction which allows sending messages:
  - `JMSProducer send(Destination destination, Message message)` sends the message to the given destination (returning the `JMSProducer` instance, because of method chaining)
- `JMSRuntimeException` is an _unchecked exception_

---

# A look at the beast: receiving

```java
public void receiveMessage(ConnectionFactory connectionFactory, Queue queue){
   try (JMSContext context = connectionFactory.createContext();){
      JMSConsumer consumer = session.createConsumer(queue);
      Message msg = consumer.receive();
      // ...
   } catch (JMSRuntimeException ex) {
      // ...
   }
}
```

- `JMSConsumer` is in charge of receiving messages, via:
  - `Message receive()` will block indefinitely
  - `Message receive(long timeout)` will block up to a timeout
  - `Message receiveNoWait()` receives the next message if one is immediately available
  - other variants...
  
---

# A look at the beast: destinations

```java
public interface Destination { }

public interface Queue extends Destination {
    String getQueueName() throws JMSException;
}

public interface Topic extends Destination {
    String getTopicName() throws JMSException;
}
```

A simple hierarchy...

---

# A look at the beast: messages

```java
public interface Message {
    String getStringProperty(String name) throws JMSException;
    void setStringProperty(String name, String value) throws JMSException;
    // ...
}

public interface TextMessage extends Message {
    void setText(String string) throws JMSException;
    String getText() throws JMSException;
    // ...
}
public interface BytesMessage extends Message { ... }
public interface MapMessage extends Message { ... }
public interface ObjectMessage extends Message { ... }
public interface StreamMessage extends Message { ... }
```

Another hierarchy with a set of common ops and type-specific ops

---

# A look at the beast

## Disclaimer

- JMS is really more than that
- JMS 2.0 brought in more goodies
- For this session we'll just need to focus on these, which is just a subset

---

# What's wrong with these APIs?

- they're imperative, but you can actually live with that
- unchecked exceptions everywhere
- side-effects everywhere
- low-level in terms of how to build complete programs

---

# What can we do to improve them?

- wrapping side-effects and methods which throws
- understand what are the core-feature we want to expose
- evaluate what is the design which better supports our intent

---

# Our intent

- having all __effects__ explicitly marked in the types
- properly handle __resource__ acquisition/dispose
- avoid the client (the developer using our lib) to mess with the APIs
- offering a __high-level__ set of APIs

---

# How Functional Programming can help?

---

# Let's start
## From the lowest level
### Where the nasty things happen

---

# Destination

```java
public interface Destination { }

public interface Queue extends Destination {
    String getQueueName() throws JMSException;
}

public interface Topic extends Destination {
    String getTopicName() throws JMSException;
}
```

- Concrete instances never gets created by the user of the lib
- They are always returned
- How to keep this invariant?

---

# Destination

```scala
sealed abstract class JmsDestination {
  private[lib] val wrapped: javax.jms.Destination
}

object JmsDestination {
  class JmsQueue private[lib] (private[lib] val wrapped: javax.jms.Queue) extends JmsDestination
  class JmsTopic private[lib] (private[lib] val wrapped: javax.jms.Topic) extends JmsDestination
}
```

- defining an _abstract class_ which __wraps__ and hides the java counterpart
- `private[lib]` will make sure users of the lib __can't access java counterparts__
- `sealed` will __close the domain__ to have only two possible concretions
- the constructor is private as well, only the lib can call it ✅
- the only reason we're doing this is that we'll need to pass these around

---

# Message

```java
public interface Message {
    String getStringProperty(String name) throws JMSException;
    void setStringProperty(String name, String value) throws JMSException;
    // ...
}

public interface TextMessage extends Message {
    void setText(String string) throws JMSException;
    String getText() throws JMSException;
    // ...
}
```

- A hierarchy of possible messages
- A set of _common operations_
- Other _type-specific_ operations

---

# Message

```scala
sealed class JmsMessage private[lib](private[lib] val wrapped: javax.jms.Message) {

  def attemptAsJmsTextMessage: Try[JmsTextMessage] = wrapped match {
    case textMessage: javax.jms.TextMessage => Success(new JmsTextMessage(textMessage))
    case _                                  => Failure(UnsupportedMessage(wrapped))
  }

  val getJMSMessageId: Option[String]                 = Try(Option(wrapped.getJMSMessageID)).toOpt
  val getJMSTimestamp: Option[Long]                   = Try(Option(wrapped.getJMSTimestamp)).toOpt
  val getJMSType: Option[String]                      = Try(Option(wrapped.getJMSType)).toOpt
  def getStringProperty(name: String): Option[String] = Try(Option(wrapped.getStringProperty(name))).toOpt

  def setJMSType(`type`: String): Try[Unit]                     = Try(wrapped.setJMSType(`type`))
  def setStringProperty(name: String, value: String): Try[Unit] = Try(wrapped.setStringProperty(name, value))
}
```

- defining a _sealed class_ which __wraps and hides__ the java counterpart
- wrapping common operations in order to catch side-effects ✅

---

# Message

```scala
object JmsMessage {
  implicit val showJmsMessage: Show[JmsMessage] = Show.show[JmsMessage](/*...a sensible string representation...*/)

  case class UnsupportedMessage(message: javax.jms.Message)
    extends Exception("Unsupported Message: " + message.show) with NoStackTrace

  class JmsTextMessage private[lib](override private[lib] val wrapped: javax.jms.TextMessage) 
    extends JmsMessage(wrapped) {
    
    def setText(text: String): Try[Unit] = Try(wrapped.setText(text))
    val getText: Try[String]             = Try(wrapped.getText)
  }

  // ... other concretions ...
}
```

- implementing _all possible concretions_
- wrapping specific operations in order to catch side-effects ✅

---

# JMSContext

```scala
class JmsContext(private val context: javax.jms.JMSContext) {
  val createTextMessage: IO[JmsTextMessage] =     
    IO.delay(new JmsTextMessage(context.createTextMessage(value)))
    
  def createQueue(queue: QueueName): IO[JmsQueue] =     
    IO.delay(new JmsQueue(context.createQueue(queue.value)))
  
  def createTopic(topicName: TopicName): IO[JmsTopic] =     
    IO.delay(new JmsTopic(context.createTopic(topicName.value)))
}
```

- `JMSContext` is the root of our interactions with JMS
- wrapping a bunch of low-level operations (again), leveraging the classes and constructors we already introduced
- the constructor is public, since the lib should be open to support multiple JMS implementations (e.g. IBM MQ, ActiveMQ, etc...)
- we'll continue adding more operations as we'll need them

---

# You may think this is boring and useless...

We just wrapped existing java classes
  - hiding/wrapping side-effects
  - exposing explicit effect types for failures/optionality
  
---  

# I fell your pain... 
## You're wondering how to finally consume/produce messages, aren't you?

---

# JMSConsumer

```scala 
class JmsMessageConsumer private[lib](private[lib] val wrapped: javax.jms.JMSConsumer) {
  val receive: IO[JmsMessage] =
    for {
      recOpt <- IO.delay(Option(wrapped.receiveNoWait()))
      rec    <- recOpt match {
        case Some(message) => IO.pure(new JmsMessage(message))
        case None          => receive
      }
    } yield rec
}
```

- only exposing `receive`, which is an `IO` value which:
  - _repeats_ a check-and-receive operation (`receiveNoWait()`) till a message is ready
  - _completes_ the IO with the message read
  - _cancels_ the computation, if a cancellation gets triggered (e.g. a `SIGTERM` signal)

---

# JMSConsumer

```scala
class JmsContext(private val context: javax.jms.JMSContext) {
  // ...
  def createJmsConsumer(queueName: QueueName): Resource[IO, JmsMessageConsumer] =
    for {
      destination <- Resource.liftF(createQueue(queueName))
      consumer    <- Resource.fromAutoCloseable(IO.delay(context.createConsumer(destination.wrapped)))
    } yield new JmsMessageConsumer(consumer)
}
```

- a `JmsMessageConsumer` will be only be created by our `JmsContext`, as a `Resource`
- We're now ready to consume 😎

---

# Let's write down a working example

```scala
object SampleConsumer extends IOApp {
  // an actual JmsContext with an implementation for a specific provider
  val jmsContextRes: Resource[IO, JmsContext] = ???

  override def run(args: List[String]): IO[ExitCode] = {
    val jmsConsumerRes = for {
      jmsContext <- jmsContextRes
      consumer   <- jmsContext.createJmsConsumer(QueueName("QUEUE1"))
    } yield consumer

    jmsConsumerRes.use(consumer =>
      for {
        msg     <- consumer.receiveJmsMessage
        textMsg <- IO.fromTry(msg.attemptAsJmsTextMessage)
        _       <- IO.delay(println(s"Got 1 message with text: $textMsg. Ending now."))
      } yield ()
    ).as(ExitCode.Success)
  }
}
```

---

# Adding support for a provider (e.g. IBM MQ)

```scala
object ibmMQ {
  // ...
  def makeJmsContext(config: Config): Resource[IO, JmsContext] = {
    for {
      context <- Resource.fromAutoCloseable(IO.delay {
        val connectionFactory: MQConnectionFactory = new MQConnectionFactory()
        connectionFactory.setTransportType(CommonConstants.WMQ_CM_CLIENT)
        connectionFactory.setQueueManager(config.qm.value)
        connectionFactory.setConnectionNameList(hosts(config.endpoints))
        connectionFactory.setChannel(config.channel.value)
        connectionFactory.setClientID(config.clientId.value)
        config.username.map { username => 
          connectionFactory.createContext(username.value, config.password.map(_.value).getOrElse(""))
        }.getOrElse(connectionFactory.createContext())
      })
    } yield new JmsContext(context)
  }
}
```

This is all we need to know about IBM MQ.

---

## Pros:
- resources get acquired and released in order, the user can't leak them
- the business logic is made by pure functions


## Cons:
- too low level
- what if the user needs to implement a never-ending message consumer?
- concurrency?

---

