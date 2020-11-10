package lib.config

sealed trait DestinationName extends Product with Serializable

object DestinationName {

  case class QueueName(value: String) extends DestinationName

  case class TopicName(value: String) extends DestinationName

}
