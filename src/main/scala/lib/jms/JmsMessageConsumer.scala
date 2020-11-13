package lib.jms

import cats.effect.IO

class JmsMessageConsumer private[lib] (private[lib] val wrapped: javax.jms.JMSConsumer) {

  val receive: IO[JmsMessage] =
    for {
      recOpt <- IO.delay(Option(wrapped.receiveNoWait()))
      rec <- recOpt match {
        case Some(message) => IO.pure(new JmsMessage(message))
        case None          => receive
      }
    } yield rec
}
