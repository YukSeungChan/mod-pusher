package channel

import org.vertx.scala.core.http.ServerWebSocket
import org.vertx.scala.core.json._

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.Map
import net.ConnectionManager

class Event(name: String)
{
    var data: JsonObject = new JsonObject()
    data.putString("event", name)

    def trigger(_data: JsonObject)
    {
        data.putObject("data", _data)
    }
}

class Channel(name: String)
{
    val events = Map[String, Event]()
    val sockets = ArrayBuffer[ServerWebSocket]()

    def subscribe(socket: ServerWebSocket)
    {
        sockets += socket
        socket.writeTextFrame(getSubscribeResult)
    }
    def unsubscribe(socket: ServerWebSocket)
    {
        sockets -= socket
    }

    def publish(eventName: String=null)
    {
        if(eventName != null && events.contains(eventName))
        {
            for(socket <- sockets)
            {
                socket.writeTextFrame(events(eventName).data.toString)
            }
        }
    }

    def getSubscribeResult(): String =
    {
        ConnectionManager.getResponse(name, new JsonObject(), "channel", name)
    }
}

class PrivateChannel(name: String) extends  Channel(name)
{
    override def getSubscribeResult(): String =
    {
        ConnectionManager.getResponse(name, new JsonObject(), "channel", name)
    }
}

class PresenceChannel(name: String) extends  Channel(name)
{
    val ids = ArrayBuffer[String]()
    val hash = Map[String, Object]()
    override def getSubscribeResult(): String =
    {
        val precenseObject = new JsonObject().putObject("presence", (new JsonObject().putValue("ids", ids).putValue("hash", hash)))
        ConnectionManager.getResponse("pusher_internal:subscription_succeeded", precenseObject, "channel", name)
    }

    def userAdd(user_id:String, user_info: Object)
    {
        ids += user_id
        hash(user_id) = user_info
        val jsonObject = new JsonObject().putString("user_id", user_id).putValue("user_info", user_info)
        for(socket <- sockets)
        {
            socket.writeTextFrame(ConnectionManager.getResponse("pusher_internal:member_added", jsonObject, "channel", name))
        }
    }

    def userRemove(user_id:String)
    {
        ids -= user_id
        hash -= user_id
        val jsonObject = new JsonObject().putString("user_id", user_id)
        for(socket <- sockets)
        {
            socket.writeTextFrame(ConnectionManager.getResponse("pusher_internal:member_removed", jsonObject, "channel", name))
        }
    }
}