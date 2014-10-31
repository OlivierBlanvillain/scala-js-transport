package actors

import akka.actor._
import models._

class PeerMatcher extends Actor with ActorLogging {
  def receive: Receive = {
    case NewConnection =>
      context.watch(sender)
      context.become(pending(sender))
  }
    
  def pending(user: ActorRef): Receive = {
    case NewConnection =>
      context.unwatch(sender)
      sender ! Connected2(user)
      user ! Connected1(sender)
      context.unbecome()
    case Terminated(_) =>
      context.unbecome()
  }
}
object PeerMatcher {
  val props = Props(new PeerMatcher())
}

class UserActor(out: ActorRef, board: ActorRef) extends Actor with ActorLogging {
  override def preStart() = {
    board ! NewConnection
  }

  def receive: Receive = {
    case Connected1(peer) =>
      out ! PeerFound1
      context.watch(peer)
      context.become(connected(peer))
    case Connected2(peer) =>
      out ! PeerFound2
      context.watch(peer)
      context.become(connected(peer))
  }
  
  def connected(peer: ActorRef): Receive = {
    case Forward(m) =>
      out ! m
    case m @ Terminated(_) =>
      context.stop(self)
    case m =>
      peer ! Forward(m)
  }
}
object UserActor {
  def props(board: ActorRef, out: ActorRef) = Props(new UserActor(out, board))
}

case class Connected2(peer: ActorRef)
case class Connected1(peer: ActorRef)
case class Forward(message: Any)
object NewConnection
