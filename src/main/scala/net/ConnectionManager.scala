package net

import org.vertx.scala.core.http.ServerWebSocket
import org.vertx.scala.core.json._
import org.vertx.java.core.buffer.Buffer

import scala.collection.mutable.Map
import channel.{Channel, PrivateChannel, PresenceChannel}

class Data(buffer: Buffer)
{
    private val jsonObject: JsonObject = new JsonObject(buffer.toString())
    println(jsonObject.toString)
    private val dataJsonObject: JsonObject = jsonObject.getObject("data")
    val channelData : JsonObject = dataJsonObject.getObject("channel_data")
    val channel: String = dataJsonObject.getString("channel")
    val auth: String = dataJsonObject.getString("auth")
    val event: String = jsonObject.getString("event")
}


trait ConnectionManager
{
    val connections = Map[String, ServerWebSocket]()
    val channels = Map[String, Channel]()
    val privateChannels = Map[String, PrivateChannel]()
    val precenseChannels = Map[String, PresenceChannel]()

    def connectionMade(socket: ServerWebSocket)
    {
        socket.endHandler(() =>
        {
            connections.remove(socket.textHandlerID)
        })
        socket.dataHandler((buffer: Buffer) =>
        {
            handler(socket, buffer)
        })
        connections(socket.textHandlerID()) = socket
        val jsonObject = new JsonObject()
        jsonObject.putString("socket_id", socket.textHandlerID())
        socket.writeTextFrame(ConnectionManager.getResponse("pusher:connection_established", jsonObject))
    }

    def handler(socket: ServerWebSocket, buffer: Buffer)
    {
        val data = new Data(buffer)
        data.event match
        {
            case "pusher:ping" =>
            {
                socket.writeTextFrame(ConnectionManager.getResponse("pusher:pong", new JsonObject()))
            }
            case "pusher:pong" =>
            {
                socket.writeTextFrame(ConnectionManager.getResponse("pusher:ping", new JsonObject()))
            }

            case "pusher:subscribe" =>
            {
                val channel = getOrCreateChannel(data.channel)
                channel.subscribe(socket)
            }
            case "pusher:unsubscribe" =>
            {
                if(channels.contains(data.channel))
                {
                    channels(data.channel).unsubscribe(socket)
                }
                else
                {
                    // error
                }
            }
            case _ =>
            {
                if(channels(data.channel).events.contains(data.event))
                {
                    channels(data.channel).publish(data.event)
                }
            }
        }
    }

    def getOrCreateChannel(name: String): Channel =
    {

        val privateRegexp = """^private-""".r
        val precenseRegexp = """^precense-""".r
        if(privateRegexp.findFirstIn(name) != None)
        {
            privateChannels(name) = new PrivateChannel(name)
            privateChannels(name)
        }
        else if(precenseRegexp.findFirstIn(name) != None)
        {
            precenseChannels(name) = new PresenceChannel(name)
            precenseChannels(name)
        }
        else
        {
            if(!channels.contains(name)) channels(name) = new Channel(name)
            channels(name)
        }
    }
}


object ConnectionManager
{
    def getResponse(eventName: String, dataJsonObject: JsonObject, optionalName: String=null, optionalData: String=null): String =
    {
        val response = new JsonObject
        response.putString("event", eventName)
        response.putObject("data", dataJsonObject)
        if(optionalName != null) response.putString(optionalName, optionalData)
        response.toString
    }
}