package lib.jms

import cats.effect.IO

class JmsMessageConsumer private[lib](private[lib] val wrapped: javax.jms.JMSConsumer) {

  val receiveJmsMessage: IO[JmsMessage] =
    for {
      recOpt <- IO.delay(Option(wrapped.receiveNoWait()))
      rec <- recOpt match {
        case Some(message) => IO.pure(new JmsMessage(message))
        case None => receiveJmsMessage
      }
    } yield rec
}
