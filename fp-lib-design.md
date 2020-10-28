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
- lot of libs/apis which are just _not designed with FP in mind_
- one may argue that FP is _not suited_ for all the use cases

---

# Agenda

- A sample reference library
- Sketching library design
- Introduce a bunch of building blocks
- Refine edges, evaluate alternatives
- Iterate

---

# A sample reference library

Java Message Service a.k.a. JMS

- provides generic messaging models
- able to handle the producer–consumer problem
- can be used to facilitate the sending and receiving of messages between software systems

---
# JMS main elements

- _Provider_: an implementation of JMS (ActiveMQ, IBM MQ, RabbitMQ, etc...)
- _Producer_/_Publisher_: a client that creates/sends messages
- _Consumer_/_Subscriber_: a client that receives messages
- _Message_: a object that contains the data being transferred
- _Queue_: a buffer that contains messages sent and waiting to be read
- _Topic_: a mechanism for sending messages that are delivered to multiple subscribers

---

# Why JMS?

- ~~old~~ stable enough (born in 1998, latest revision in 2015)
- its apis are a good testbed for sketching a purely functional wrapper
  - loads of state, side-effects, exceptions, ...
- found pretty much nothing about
- I don't like suffering to much while working

---

# Disclaimer

Our focus here is **_NOT_** on building the coolest

We'll just put our attention on _implementing an architecture component_ (the projector) using Pure Functional Programming, in Scala

---

# Why Scala

---

# Why Scala

## _I know Scala_

---

# Why Scala

- immutability, _ADTs_
- higher-kinded types + implicits -> *typeclasses*
- DSL-friendly
- __mature ecosystem__ of FP libs (cats, cats-effects, fs2, circe, http4s, etc..)

---
# Let's start
---
