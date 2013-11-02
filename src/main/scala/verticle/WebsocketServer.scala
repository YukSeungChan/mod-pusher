/*
 * Copyright © 2013 Yuk SeungChan, All rights reserved.
 */

import org.vertx.scala.platform.Verticle
import org.vertx.scala.core.http.ServerWebSocket

import net.ConnectionManager

class WebsocketServer extends Verticle with ConnectionManager
{
    override def start()
    {
        vertx.createHttpServer().websocketHandler(
        {

            ws:ServerWebSocket => ws.path match
            {
                case "/app/" =>
                {
                    connectionMade(ws)
                };
                case _ => ws.reject();
            }
        }) listen(8081)
    }
}