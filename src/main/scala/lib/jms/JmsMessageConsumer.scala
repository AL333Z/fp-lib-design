package lib.jms

import cats.effect.IO

import scala.concurrent.duration.{ DurationInt, FiniteDuration }

class JmsMessageConsumer1 private[lib] (
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

class JmsMessageConsumer2 private[lib] (
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

class JmsMessageConsumer private[lib] (
  private[lib] val wrapped: javax.jms.JMSConsumer,
  private[lib] val pollingInterval: FiniteDuration = 0.millis
) {

  val receive: IO[JmsMessage] =
    for {
      recOpt <- IO.blocking(Option(wrapped.receiveNoWait()))
      rec <- recOpt match {
        case Some(message) => IO.pure(new JmsMessage(message))
        case None          => IO.cede >> IO.sleep(pollingInterval) >> receive
      }
    } yield rec
}
