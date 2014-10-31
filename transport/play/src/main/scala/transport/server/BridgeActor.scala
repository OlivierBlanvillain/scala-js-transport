package transport.server

import scala.concurrent._
import scala.util.Success
import akka.actor._
import play.api.mvc._

import transport._

private class BridgeActor(listener: ConnectionListener, out: ActorRef)(
      implicit ec: ExecutionContext) extends Actor {
  val promise = Promise[MessageListener]()
  var poorMansBuffer: Future[MessageListener] = promise.future

  val connectionHandle = new ConnectionHandle {
    override def handlerPromise: Promise[MessageListener] = promise
    override def write(outboundPayload: String): Unit = out ! outboundPayload
    override def close(): Unit = context.stop(self)
  }
  
  override def preStart: Unit = {
    listener.notify(connectionHandle)
  }
  
  override def postStop: Unit = {
    poorMansBuffer = poorMansBuffer.andThen {
      case Success(l) => l.closed()
    }
  }
  
  override def receive = {
    case inboundPayload: String =>
      poorMansBuffer = poorMansBuffer.andThen {
        case Success(l) => l.notify(inboundPayload)
      }
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
