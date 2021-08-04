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

# Agenda

- A reference library to wrap
- Sketching library design
  - Introduce a bunch of building blocks
  - Refine edges, evaluate alternatives
  - Iterate

---

# Goals

// TODO

- bottom-up approach

---

# A reference library

__Java Message Service__ a.k.a. JMS

- provides generic messaging models
- able to handle the producer–consumer problem
- can be used to facilitate the sending and receiving of messages between enterprise software systems, whatever it means enterprise!

---
# JMS main elements

- __Provider__: an implementation of JMS (ActiveMQ, IBM MQ, RabbitMQ, etc...)
- __Producer__/__Publisher__: a client that creates/sends messages
- __Consumer__/__Subscriber__: a client that receives messages
- __Message__: a object that contains the data being transferred
- __Queue__: a buffer that contains messages sent and waiting to be read
- __Topic__: a mechanism for sending messages that are potentialy delivered to multiple subscribers

---

# Why JMS?

- ~~old~~ stable enough (born in 1998, latest revision in 2015)
- its apis are a __good testbed for sketching a purely functional wrapper__
  - loads of state, side-effects, exceptions, ...
- found pretty much nothing about (no FP-like bindings...)
- I don't like suffering to much while working

---

# Disclaimer

Our focus here is **_NOT_** on building the coolest library doing the coolest thing ever.

Chances are that you'll never use JMS at all!

We'll just put our attention on **_designing a set of APIs_** which wraps an existing lib written in the _good old imperative way_, using Pure Functional Programming and the Typelevel stack.

---

# Let's start

---

# A look at the beast: receiving

```java
public void receiveMessage(ConnectionFactory connectionFactory, String queueName){
   try (JMSContext context = connectionFactory.createContext();){
      Queue queue = conxtex.createQueue(queueName);
      JMSConsumer consumer = session.createConsumer(queue);
      Message msg = consumer.receive();
      // ...
   } catch (JMSRuntimeException ex) {
      // ...
   }
}
```

---

- `JMSContext` is in charge of _opening low level stuff_ (connections, sessions, ...), implements `AutoClosable` (see the try-with-resources block)
- `JMSConsumer` is in charge of receiving messages, via:
  - `Message receive()` will block indefinitely
  - `Message receive(long timeout)` will block up to a timeout
  - `Message receiveNoWait()` receives the next message if one is immediately available
  - other variants...
- `JMSRuntimeException` is an _unchecked exception_
  
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
- For this session we'll just need to focus on these, which is only a relevant subset

---

# What's wrong with these APIs?

- not really composable:
  - unchecked exceptions everywhere
  - side-effects everywhere
- _low-level_ in terms of how to build complete programs

---

# What can we do to improve them?

- wrapping side-effects and methods which throws
- understand what are the core-feature we want to expose
- evaluate what is the __design which better supports our intent__

---

# Our intent

- having all __effects__ explicitly marked in the types
- properly handle __resource__ acquisition/dispose (avoiding leaks!)
- __prevent__ the developer using our lib from doing __wrong things__ (e.g. unconfirmed messages, deadlocks, etc...) by design
- offering a __high-level__ set of APIs

---

# How Functional Programming can help?

---

# Let's start
## From the lowest level

---

# Don't forget the basics

![inline](pics/gap.png)

---

# Destination

```java
public interface Destination { }

public interface Queue extends Destination {
    String getQueueName() throws JMSException;
}
// ...
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
  class JmsQueue private[lib] (private[lib] val wrapped: javax.jms.Queue) 
    extends JmsDestination
  // ...
}
```

- defining an _abstract class_ which __wraps__ and hides the java counterpart
- `private[lib]` will make sure users of the lib __can't access java counterparts__
- `sealed` will __close the domain__ to have only the defined possible concretions
- the constructor is private as well, only the lib can call it ✅

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
  def tryAsJmsTextMessage: Try[JmsTextMessage] = wrapped match {
    case textMessage: javax.jms.TextMessage => Success(new JmsTextMessage(textMessage))
    // others...
    case _ => Failure(UnsupportedMessage(wrapped))
  }

  val getJMSMessageId: Option[String] = getOpt(wrapped.getJMSMessageID)
  val getJMSTimestamp: Option[Long]   = getOpt(wrapped.getJMSTimestamp)
  val getJMSType: Option[String]      = getOpt(wrapped.getJMSType)
  def getStringProperty(name: String): Option[String] = getOpt(wrapped.getStringProperty(name))

  def setJMSType(`type`: String): Try[Unit] = Try(wrapped.setJMSType(`type`))
  def setStringProperty(name: String, value: String): Try[Unit] = Try(wrapped.setStringProperty(name, value))

  private def getOpt[A](body: => A): Option[A] = // ...
}
```

- defining a _sealed class_ which __wraps and hides__ the java counterpart
- exposing a safer variant of its operations ✅

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
- wrapping specific operations in order to catch exceptions ✅

---

# You may think this is boring and useless...

We just wrapped existing java classes

  - catching/wrapping side-effects
  - and exposing explicit effect types for failures/optionality
  
## Hold on...

---

# Receiving

```java
public void receiveMessage(ConnectionFactory connectionFactory, String queueName){
   try (JMSContext context = connectionFactory.createContext();){
      Queue queue = conxtex.createQueue(queueName);
      JMSConsumer consumer = session.createConsumer(queue);
      Message msg = consumer.receive();
      // ...
   } catch (JMSRuntimeException ex) {
      // ...
   }
}
```

- how to handle JMSRuntimeException?
- how to build a consumer that can be injected in our application components?
- how to handle the resource lifecycle?

---

# Let's see how FP con help us in doing the right thing!

---

# Introducing IO
#### A data type for **encoding effects** as pure values

---

# Introducing IO

- enable capturing and controlling actions - a.k.a _effects_ - that your program _wishes to perform_ within a _resource-safe_, _typed_ context with seamless support for _concurrency_ and _coordination_
- these effects may be _asynchronous_ (callback-driven) or _synchronous_ (directly returning values); they may _return_ within microseconds or run _infinitely_.

---

# IO values

- are *pure* and *immutable*
- represents just a description of a *side effectful computation*
- are not evaluated (_suspended_) until the **end of the world**
- respects _referential transparency_

---

# IO and combinators

[.column]

[.code-highlight: none]
[.code-highlight: all]

```scala
object IO {
  def delay[A](a: => A): IO[A]
  def pure[A](a: A): IO[A]
  def raiseError[A](e: Throwable): IO[A]
  def sleep(duration: FiniteDuration): IO[Unit]
  def async[A](k: /* ... */): IO[A]
  ...
}
```

[.column]

[.code-highlight: none]
[.code-highlight: all]

```scala
class IO[A] {
  def map[B](f: A => B): IO[B]
  def flatMap[B](f: A => IO[B]): IO[B]
  def *>[B](fb: IO[B]): IO[B]
  ...
}
```

---

# Composing sequential effects

[.column]
[.code-highlight: 1-4]
[.code-highlight: 7-8]
[.code-highlight: 7-9]
[.code-highlight: 7-11]
[.code-highlight: 7-12]
[.code-highlight: all]

```scala
val ioInt: IO[Int] = 
  IO.delay { println("hello") }
    .map(_ => 1)

val program: IO[Unit] =
 for {
    i1 <- ioInt
    _  <- IO.sleep(i1.second)
    _  <- IO.raiseError( // not throwing!
            new RuntimeException("boom!")) 
    i2 <- ioInt //comps is short-circuted
 } yield ()
```
[.column]
[.code-highlight: none]
[.code-highlight: all]
```
> Output:
> hello
> <...1 second...>
> RuntimeException: boom!
```

---

# How IO values are executed?

If IO values are just a description of _effectful computations_ which can be composed and so on... 

Who's gonna **_run_** the suspended computation then?

---

[.background-color: #FFFFFF]

# How to fill the abstraction gap?

![Inline](pics/io.png)

---

# JmsContext - first iteration

```scala
class JmsContext(private val context: javax.jms.JMSContext) {
  def createQueue(queue: QueueName): IO[JmsQueue] =
    IO.delay(new JmsQueue(context.createQueue(queue.value)))

  def makeJmsConsumer(queueName: QueueName): IO[JmsMessageConsumer] =
    for {
      destination <- createQueue(queueName)
      consumer    <- IO.delay(context.createConsumer(destination.wrapped))
    } yield new JmsMessageConsumer(consumer)
}
```

- handle JMSRuntimeException ✅
- build a consumer that can be injected in our application components ✅
- handle the resource lifecycle ❌ 

---

# How to handle the lifecycle of a resource?

---

# Introducing Resource

#### Effectfully allocates and releases a resource

---

# Extremely helpful to write code that:
- doesn't leak
- handles properly terminal signals (e.g. `SIGTERM`) by default (no need to register a shutdown hook)
- do _the right thing_<sup>TM</sup> by design
- avoid the need to reboot a container every once in a while :)

---

[.background-color: #FFFFFF]

# How to fill the abstraction gap?

![Inline](pics/resource.png)

---

[.code-highlight: 1-5]
[.code-highlight: 7-13]
[.code-highlight: all]

# Introducing Resource

```scala
object Resource {
  def make[A](
    acquire: IO[A])(
    release: A => IO[Unit]): Resource[A]
}

class Resource[A] {
  def use[B](f: A => IO[B]): IO[B]

  def map[B](f: A => B): Resource[B]
  def flatMap[B](f: A => Resource[B]): Resource[B]
  ...
}
```

[.footer: NB: not actual code, just a simplification sticking with IO type]
^ A note on the simplification

---

# Using a Resource

[.column]

```scala
val sessionPool: Resource[MySessionPool] = 
  for {
    connection <- openConnection()
    sessions   <- openSessionPool(connection)
  } yield sessions

sessionPool.use { sessions =>
  // use sessions to do whatever things!
}
```

[.column]

[.code-highlight: none]
[.code-highlight: all]

```
Output:
> Acquiring connection
> Acquiring sessions
> Using sessions
> Releasing sessions
> Releasing connection
```

---

# Gotchas:
- _Nested resources_ are released in *reverse order* of acquisition 
- Easy to _lift_ an `AutoClosable` to `Resource`, via `Resource.fromAutoclosable`
- Every time you need to use something which implements `AutoClosable`, you should really be using `Resource`!
- You can _lift_ any `IO[A]` into a `Resource[A]` with a no-op release via `Resource.eval`

---

# Why not scala.util.Using?

- not composable (no `map`, `flatMap`, etc...)
- no support for properly handling effects

---

# JmsContext - second iteration

```scala
class JmsContext(private val context: javax.jms.JMSContext) {

  def createQueue(queue: QueueName): IO[JmsQueue] =
    IO.delay(new JmsQueue(context.createQueue(queue.value)))

  def makeJmsConsumer(queueName: QueueName): Resource[IO, JmsMessageConsumer] =
    for {
      destination <- Resource.eval(createQueue(queueName))
      consumer    <- Resource.fromAutoCloseable(IO.delay(context.createConsumer(destination.wrapped)))
    } yield new JmsMessageConsumer(consumer)
}
```

- handle JMSRuntimeException ✅
- build a consumer that can be injected in our application components ✅
- handle the resource lifecycle ✅

---

# JMSConsumer

```scala 
class JmsMessageConsumer private[lib] (
  private[lib] val wrapped: javax.jms.JMSConsumer
) {
  val receive: IO[JmsMessage] =
    for {
      recOpt <- IO.delay(Option(wrapped.receiveNoWait()))
      rec <- recOpt match {
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

# JMSConsumer - alternative implementation

```scala 
class JmsMessageConsumer private[lib] (
  private[lib] val wrapped: javax.jms.JMSConsumer,
  private[lib] val pollingInterval: FiniteDuration
) {

  val receive: IO[JmsMessage] =
    for {
      recOpt <- IO.blocking(Option(wrapped.receive(pollingInterval.toMillis)))
      rec <- recOpt match {
        case Some(message) => IO.pure(new JmsMessage(message))
        case None          => receive
      }
    } yield rec
}
```

- pretty much the same as the former two
- leveraging `receive(timeout)` and wrapping the blocking operation in `IO.blocking`

---

# JMSConsumer - final

```scala 
class JmsMessageConsumer private[lib] (
  private[lib] val wrapped: JMSConsumer,
  private[lib] val pollingInterval: FiniteDuration
) {

  val receive: IO[JmsMessage] =
    for {
      recOpt <- IO.delay(Option(wrapped.receiveNoWait()))
      rec <- recOpt match {
        case Some(message) => IO.pure(new JmsMessage(message))
        case None          => IO.cede >> IO.sleep(pollingInterval) >> receive
      }
    } yield rec
}
```

- pretty much the same as the former two
- assumes `receiveNoWait()` is not actually blocking
- introduce a fairness boundary via `IO.cede`, forcing the runtime to progress with other tasks if no message has been found ready to consume
- introduce an interval in order to avoid an high cpu usage when the queue has no messages for a long time

---

# Let's write down a nearly working example

```scala
object SampleConsumer extends IOApp.Simple {
  override def run: IO[Unit] = {
    val jmsConsumerRes = for {
      jmsContext <- ??? // A Resource[JmsContext] instance for a given provider
      consumer   <- jmsContext.makeJmsConsumer(QueueName("QUEUE1"))
    } yield consumer

    jmsConsumerRes
      .use(consumer =>
        for {
          msg     <- consumer.receive
          textMsg <- IO.fromTry(msg.tryAsJmsTextMessage)
          _       <- IO.delay(println(s"Got 1 message with text: $textMsg. Ending now."))
        } yield ()
      )
  }
}
```

- `IOApp` describes a _main_ which executes an `IO` (a.k.a. *End of the world*)
- It runs the side-effects described in the `IO`!
- It's the single _entry point_ to a **pure** program.

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
        // ...
      })
    } yield new JmsContext(context)
  }
}
```

That's it!

---

## Pros:
- resources get acquired and released in order, the user can't leak them
- the business logic is made by pure functions


## Cons:
- still low level
- how to specify message acknoledgements?
- what if the user needs to implement a never-ending message consumer?
- concurrency?

---

# Switching to top-down

- Let's evaluate how we can model an api for a never-ending message consumer!

---

# Consumer with explicit ack - first iteration

[.column]

```scala
object JmsAcknowledgerConsumer {

  sealed trait AckResult
  object AckResult {
    // ack all the messages delivered by this context
    case object Ack  extends AckResult
    // do nothing, messages may be redelivered
    case object NAck extends AckResult
  }

  type Acker    = AckResult => IO[Unit]
  type Consumer = Stream[IO, JmsMessage]

  def make(
    context: JmsContext, 
    queueName: QueueName
  ): Resource[IO, (Consumer, Acker)] =
    for {
      ctx <- context.makeContextForAcknowledging
      acker = (ackResult: AckResult) =>
        ackResult match {
          case AckResult.Ack  => IO.blocking(ctx.context.acknowledge())
          case AckResult.NAck => IO.unit
        }
      consumer <- ctx.makeJmsConsumer(queueName)
    } yield (Stream.eval(consumer.receive).repeat, acker)
}

```

[.column]

```scala
object SampleJmsAcknowledgerConsumer extends IOApp.Simple {

  val logger = ???
  val jmsContextRes: Resource[IO, JmsContext] = ???

  override def run: IO[Unit] =
    jmsContextRes.flatMap(ctx => 
      JmsAcknowledgerConsumer.make(ctx, QueueName("QUEUE1"))).use {
        case (consumer, acker) =>
          consumer.evalMap { msg =>
            // whatever business logic you need to perform
            logger.info(msg.show) >>
              acker(AckResult.Ack)
          }.compile.drain
    }
}
```

- Inspired by fs2-rabbit

---

# Consumer with explicit ack - first iteration

- all effects are expressed in the types (`IO`, etc...)
- resource lifecycle handled via `Resource`
- messages in the queue are exposed via a `Stream`

But...

- what happens if the user messes with our lib?
  - the user forget to `ack`/`nack`
  - the user `ack`/`nack` multiple times the same message
  - the user `ack` and then `nack` the same message (or viceversa)
- concurrency?

---
