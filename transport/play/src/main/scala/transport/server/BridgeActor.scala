package transport.server

import scala.concurrent._
import scala.util.Success
import akka.actor._
import play.api.mvc._

import transport._

private class BridgeActor(listener: ConnectionListener, out: ActorRef)(
      implicit ec: ExecutionContext) extends Actor {
  val promise = QueueablePromise[MessageListener]()
  
  override def preStart: Unit = {
    val connectionHandle = new ConnectionHandle {
      def handlerPromise: Promise[MessageListener] = promise
      def write(outboundPayload: String): Unit = out ! outboundPayload
      def close(): Unit = self ! PoisonPill
    }
    listener.notify(connectionHandle)
  }
  
  override def postStop: Unit = {
    promise.queue(_.closed())
  }
  
  def receive = {
    case m: String =>
      promise.queue(_.notify(m))
  }
}

private object BridgeActor {
  def props(listener: ConnectionListener)(out: ActorRef)(implicit ec: ExecutionContext) =
    Props(new BridgeActor(listener, out))

  def actionHandle(promise: Promise[ConnectionListener])(request: RequestHeader)(
      implicit ec: ExecutionContext): Future[Either[Result, ActorRef => Props]] = {
    Future.successful(
      promise.future.value match {
        case Some(Success(listener)) =>
          Right(BridgeActor.props(listener))
        case _ =>
          Left(Results.Forbidden)
      }
    )
  }
}
