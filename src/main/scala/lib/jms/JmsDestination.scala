package lib.jms

sealed abstract class JmsDestination {
  private[lib] val wrapped: javax.jms.Destination
}

object JmsDestination {
  class JmsQueue private[lib] (private[lib] val wrapped: javax.jms.Queue) extends JmsDestination
  class JmsTopic private[lib] (private[lib] val wrapped: javax.jms.Topic) extends JmsDestination
}
