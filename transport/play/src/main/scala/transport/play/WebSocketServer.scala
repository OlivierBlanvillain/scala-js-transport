package transport.play

import scala.concurrent._

import play.api.Application
import play.api.mvc._
import play.twirl.api.Html

import transport._

class WebSocketServer(implicit ec: ExecutionContext, app: Application)
    extends WebSocketTransport {
  private val promise = Promise[ConnectionListener]()
  
  /** Method to be called by the play controller for each new connection to socketRoute.
   *  
   *  For example,
   *  {{{
   *  // In conf/routes:
   *  GET     /socket                     controllers.Application.socket
   *  
   *  // In controllers.Application:
   *  val transport = WebSocketServer()
   *  def socket = transport.action()
   *  }}}
   */
  def action(): WebSocket[String, String] = WebSocket.tryAcceptWithActor[String, String] {
    BridgeActor.actionHandle(promise)
  }

  def listen(): Future[Promise[ConnectionListener]] =
    Future.successful(promise)
  
  def connect(remote: WebSocketUrl): Future[ConnectionHandle] =
    Future.failed(new UnsupportedOperationException(
      "Servers cannot initiate WebSockets connections."))
  
  def shutdown(): Unit = ()
}

object WebSocketServer {
  def apply()(implicit ec: ExecutionContext, app: Application) = new WebSocketServer()

  /** Generates a JavaScript route to a WebSocketServer. Use WebSocketClient.addressFromPlayRoute()
   *  to load the route as a WebSocketUrl on the client side. */
  def javascriptRoute(socketRoute: Call)(implicit request: RequestHeader) = Html {
    s"""var webSocketUrl = '${socketRoute.webSocketURL()}';"""
  }
}
