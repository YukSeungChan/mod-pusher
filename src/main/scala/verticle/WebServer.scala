/*
 * Copyright © 2013 Yuk SeungChan, All rights reserved.
 */

package verticle

/*
 * Copyright © 2013 Yuk SeungChan, All rights reserved.
 */

import org.vertx.scala.platform.Verticle
import org.vertx.scala.core.http.HttpServerRequest

import org.vertx.scala.core.http.RouteMatcher

class WebServer extends Verticle
{
    override def start()
    {
        val routeMatcher = new RouteMatcher
        routeMatcher.post("/apps/:appsId/channels/:channelName/events")
        {
            request: HttpServerRequest => new TriggerView(request)
        }
        routeMatcher.post("/pusher/auth/")
        {
            request: HttpServerRequest => new AuthView(request)
        }
        vertx.createHttpServer().requestHandler(routeMatcher)listen(8080)
    }
}

class TriggerView(request: HttpServerRequest)
{
    val channelName = request.params().get("channelName")
    println(request.headers())
    println(request.method())
    println(request.params())
    println(request.query())
    println(request.uri())
    request.response.end()
}


class AuthView(request: HttpServerRequest)
{

}