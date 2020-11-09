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

```
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

```
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

```
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

```
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

---



---