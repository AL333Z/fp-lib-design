package lib.jms

import cats.Show
import cats.syntax.all._
import lib.jms.JmsMessage.{ JmsTextMessage, UnsupportedMessage }

import scala.util.control.NoStackTrace
import scala.util.{ Failure, Success, Try }

sealed class JmsMessage private[lib] (private[lib] val wrapped: javax.jms.Message) {

  def attemptAsJmsTextMessage: Try[JmsTextMessage] = wrapped match {
    case textMessage: javax.jms.TextMessage => Success(new JmsTextMessage(textMessage))
    case _                                  => Failure(UnsupportedMessage(wrapped))
  }

  val getJMSMessageId: Option[String] = Try(Option(wrapped.getJMSMessageID)).toOpt
  val getJMSTimestamp: Option[Long]   = Try(Option(wrapped.getJMSTimestamp)).toOpt
  val getJMSType: Option[String]      = Try(Option(wrapped.getJMSType)).toOpt

  def getStringProperty(name: String): Option[String] =
    Try(Option(wrapped.getStringProperty(name))).toOpt

  def setJMSType(`type`: String): Try[Unit] = Try(wrapped.setJMSType(`type`))

  def setStringProperty(name: String, value: String): Try[Unit] = Try(wrapped.setStringProperty(name, value))

  implicit class TryUtils[T](val underlying: Try[Option[T]]) {

    def toOpt: Option[T] =
      underlying match {
        case Success(t) => {
          t match {
            case Some(null) => None
            case None       => None
            case x          => x
          }
        }
        case _ => None
      }
  }

}

object JmsMessage {

  implicit val showMessage: Show[javax.jms.Message] = Show.show[javax.jms.Message] { message =>
    def getStringContent: Try[String] = message match {
      case message: javax.jms.TextMessage => Try(message.getText)
      case _                              => Failure(new RuntimeException())
    }

    def propertyNames: List[String] = {
      val e   = message.getPropertyNames
      val buf = collection.mutable.Buffer.empty[String]
      while (e.hasMoreElements) {
        val propertyName = e.nextElement.asInstanceOf[String]
        buf += propertyName
      }
      buf.toList
    }

    Try {
      s"""
         |${propertyNames.map(pn => s"$pn       ${message.getObjectProperty(pn)}").mkString("\n")}
         |JMSMessageID        ${message.getJMSMessageID}
         |JMSTimestamp        ${message.getJMSTimestamp}
         |JMSCorrelationID    ${message.getJMSCorrelationID}
         |JMSReplyTo          ${message.getJMSReplyTo}
         |JMSDestination      ${message.getJMSDestination}
         |JMSDeliveryMode     ${message.getJMSDeliveryMode}
         |JMSRedelivered      ${message.getJMSRedelivered}
         |JMSType             ${message.getJMSType}
         |JMSExpiration       ${message.getJMSExpiration}
         |JMSPriority         ${message.getJMSPriority}
         |===============================================================================
         |${getStringContent.getOrElse(s"Unsupported message type: $message")}
        """.stripMargin
    }.getOrElse("")
  }

  implicit val showJmsMessage: Show[JmsMessage] = Show.show[JmsMessage](_.wrapped.show)

  case class UnsupportedMessage(message: javax.jms.Message)
      extends Exception("Unsupported Message: " + message.show)
      with NoStackTrace

  class JmsTextMessage private[lib] (override private[lib] val wrapped: javax.jms.TextMessage)
      extends JmsMessage(wrapped) {

    def setText(text: String): Try[Unit] =
      Try(wrapped.setText(text))

    val getText: Try[String] = Try(wrapped.getText)
  }

}
