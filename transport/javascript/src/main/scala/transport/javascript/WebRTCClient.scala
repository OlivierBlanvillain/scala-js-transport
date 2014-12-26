package transport.javascript

import scala.concurrent._
import scala.util._
import scala.scalajs.js

import transport._
import transport.jsapi._

import org.scalajs.spickling._
import org.scalajs.spickling.jsany._

/** WebRTC JavaScript client. (Chrome and Firefox only) 
 *  
 *  Usage example:
 *  {{{
 *  new WebRTCClient().connect(signalingChannel).foreach { connection =>
 *    connection.handlerPromise.success { string => println("Recived: " + string) }
 *    connection.write("Hello WebRTC!")
 *  }
 *  }}}
 */
class WebRTCClient(implicit ec: ExecutionContext) extends Transport {
  type Address = ConnectionHandle
  
  def listen(): Future[Promise[ConnectionListener]] = 
    Future.failed(new UnsupportedOperationException(
      "WebRTCClient cannot listen for incomming connections."))

  def connect(signalingChannel: ConnectionHandle): Future[ConnectionHandle] = {
    new WebRTCPeer(signalingChannel).future
  }

  def shutdown(): Future[Unit] = Future.successful(Unit)
}

private class WebRTCPeer(
      signalingChannel: ConnectionHandle,
      priority: Double=js.Math.random())(
      implicit ec: ExecutionContext) {

  import WebRTCPeer._
  registerPicklers()
  
  private val webRTCConnection = new webkitRTCPeerConnection(null, DataChannelsConstraint)
  private val connectionPromise = Promise[ConnectionHandle]()
  private var isCaller: Boolean = _

  signalingChannel.handlerPromise.success { inboundPayload  =>
    val parsedPayload : js.Any = js.JSON.parse(inboundPayload)
    val unpickledPayload: Any = PicklerRegistry.unpickle(parsedPayload)
    revievedViaSignaling(unpickledPayload)
  }
  
  signalingChannel.closedFuture.onComplete { _ =>
    connectionPromise.tryFailure(new IllegalStateException(
      "Signaling channel closed before the end of connection establishment."))
  }

  webRTCConnection.onicecandidate = { event: RTCIceCandidateEvent =>
    if(event.candidate != null) {
      sendViaSignaling(IceCandidate(js.JSON.stringify(event.candidate)))
    }
  }
  
  sendViaSignaling(Priority(priority))
  
  def future: Future[ConnectionHandle] = connectionPromise.future

  private def sendViaSignaling(m: Any): Unit = {
    signalingChannel.write(js.JSON.stringify(PicklerRegistry.pickle(m)))
  }

  private def revievedViaSignaling(m: Any): Unit = {
    // Each message is received exactly once, in the order of appearance in this match.
    m match {
      
      case Priority(hisPriority) =>
        isCaller = hisPriority > priority
        if(isCaller) {
          createConnectionHandle(webRTCConnection.createDataChannel("sendDataChannel"))
          webRTCConnection.createOffer { description: RTCSessionDescription =>
            webRTCConnection.setLocalDescription(description)
            sendViaSignaling(SessionDescription(js.JSON.stringify(description)))
          }
        } else {
          webRTCConnection.ondatachannel = { event: Event =>
            // WebRTC API typo?
            createConnectionHandle(event.asInstanceOf[RTCDataChannelEvent].channel)
          }
        }
      
      case IceCandidate(candidate) =>
        webRTCConnection.addIceCandidate(new RTCIceCandidate(
          js.JSON.parse(candidate).asInstanceOf[RTCIceCandidate]))

      case SessionDescription(description) =>
        val remoteDescription = new RTCSessionDescription(
            js.JSON.parse(description).asInstanceOf[RTCSessionDescriptionInit])
        if(isCaller) {
          webRTCConnection.setRemoteDescription(remoteDescription)
        } else {
          webRTCConnection.setRemoteDescription(remoteDescription)
          webRTCConnection.createAnswer { localDescription: RTCSessionDescription =>
            webRTCConnection.setLocalDescription(localDescription)
            sendViaSignaling(SessionDescription(js.JSON.stringify(localDescription)))
          }
        }
        
    }
  }
  
  private def createConnectionHandle(dc: RTCDataChannel): Unit = {
    new ConnectionHandle {
      private val promise = QueueablePromise[MessageListener]()
      private val closePromise = Promise[Unit]()
      
      dc.onopen = { event: Event =>
        connectionPromise.success(this)
      }
      dc.onmessage = { event: RTCMessageEvent =>
        promise.queue(_(event.data.toString))
      }
      dc.onclose = { event: Event =>
        closePromise.trySuccess(())
      }
      dc.onerror = { event: Event =>
        val message = try { event.toString } catch { case e: ClassCastException => "" }
        closePromise.tryFailure(WebRTCException(message))
      }
      
      def handlerPromise: Promise[MessageListener] = promise
      def closedFuture: Future[Unit] = closePromise.future
      def write(outboundPayload: String): Unit = dc.send(outboundPayload)
      def close(): Unit = dc.close()
    }
  }
}
private object WebRTCPeer {
  case class Priority(value: Double)
  case class IceCandidate(string: String)
  case class SessionDescription(string: String)

  private lazy val _registerPicklers: Unit = {
    import org.scalajs.spickling._
    import PicklerRegistry.register
    
    register[Priority]
    register[IceCandidate]
    register[SessionDescription]
  }

  def registerPicklers(): Unit = _registerPicklers

  object OptionalMediaConstraint extends RTCOptionalMediaConstraint {
    override val DtlsSrtpKeyAgreement: js.Boolean = false
    override val RtpDataChannels: js.Boolean = false
  }

  object DataChannelsConstraint extends RTCMediaConstraints {
    override val mandatory: RTCMediaOfferConstraints = null
    override val optional: js.Array[RTCOptionalMediaConstraint] = js.Array(OptionalMediaConstraint)
  }
}
