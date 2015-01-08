package examples

import transport._
import transport.netty._

import scala.concurrent.ExecutionContext.Implicits.global

object RawServer { /**/

val transport = new WebSocketServer(8080, "/ws")
try {
  transport.listen() foreach { _.success { connection =>
    connection.handlerPromise.success { message =>
      connection.write(message)
    }
  }}
} finally transport.shutdown()

} /**/

/* try { */
/*   transport.listen() onSuccess { case listenerPromise => */
/*     listenerPromise.success { connection => */
/*       connection.handlerPromise.success { message => */
/*         connection.write(message) */
/*       } */
/*     } */
/*   } */
/* } finally transport.shutdown() */
