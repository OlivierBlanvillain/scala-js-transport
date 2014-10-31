package controllers

import scala.concurrent._
import scala.util.Success

import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc._
import play.api.Play.current
import akka.actor._

import upickle._
import shared.Api
import autowire.Core.Request

import play.sockjs.api.SockJSRouter
import transport.server._
import transport.util._

object Application extends Controller {
  
  def indexDev = Action { implicit request =>
    Ok(views.html.index(devMode = true,
      SockJSServer.javascriptRoute(sockJS)))
  }

  def indexOpt = Action { implicit request =>
    Ok(views.html.index(devMode = false,
      WebSocketServer.javascriptRoute(routes.Application.webSocket)))
  }
  
  val webSocketTransport = WebSocketServer()
  val webSocket = webSocketTransport.action()

  val sockJStransport = SockJSServer()
  val sockJS = sockJStransport.action()
  
  sockJStransport.listen().map { promise =>
    promise.success(new magicConnectionListener(AutowireServer.route[Api](Server)))
  }

  // sockJStransport.listen().map {
  //   _.success {
  //     new ConnectionListener {
  //       override def notify(connection: ConnectionHandle): Unit = {
  //         connection.handlerPromise.success {
  //           new MessageListener {
  //             override def notify(pickle: String): Unit = {
  //               val request: Request[String] = upickle.read[Request[String]](pickle)
  //               val result: Future[String] = AutowireServer.route[Api](Server)(request)
  //               result.foreach { connection write _ }
  //             }
  //             override def closed(): Unit = ()
  //           }
  //         }
  //       }
  //     }
  //   }
  // }
}

object Server extends Api { 
  def list(path: String): Seq[String] = {
    val chunks = path.split("/", -1)
    val prefix = "./" + chunks.dropRight(1).mkString("/")
    val files = Option(new java.io.File(prefix).list()).toSeq.flatten
    files.filter(_.startsWith(chunks.last))
  }
  def double(i: Int) = 2 * i
}

object AutowireServer extends autowire.Server[String, upickle.Reader, upickle.Writer]{
  def read[Result: upickle.Reader](p: String) = upickle.read[Result](p)
  def write[Result: upickle.Writer](r: Result) = upickle.write(r)
}
