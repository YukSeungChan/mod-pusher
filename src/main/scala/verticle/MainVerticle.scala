import org.vertx.java.core.{AsyncResult, AsyncResultHandler, Handler}
import org.vertx.scala.core.eventbus.Message
import org.vertx.scala.platform.Verticle
import org.vertx.scala.core.json._

class MainVerticle extends Verticle
{
    override def start()
    {
        val pubConfig = new JsonObject().putString("address", "redis.pub").putNumber("port", 6379).putString("host", "localhost")
        val subConfig = new JsonObject().putString("address", "redis.sub").putNumber("port", 6379).putString("host", "localhost")

        container.deployModule("io.vertx~mod-redis~1.1.2", pubConfig, 1,
            (result: AsyncResult[String]) =>
            {

                println("deployModule")
                println(result.failed())
                println(result.succeeded())
                redisSub()
                redisPub()
            })
        container.deployModule("io.vertx~mod-redis~1.1.2", subConfig, 1)
        container.deployVerticle("verticle.WebsocketServer")
        container.deployVerticle("WebServer")



        vertx.eventBus.registerHandler("redis.sub.test")
        {
            message : Message[JsonObject] =>
            {
                println("redis.sub Message")
                println(message.body)
            }
        }

        def redisSub()
        {
            val jsonArray = new JsonArray().addString("test")
            val data = new JsonObject().putString("command", "subscribe").putArray("args", jsonArray)
            vertx.eventBus.send("redis.sub", data)
            {
                msg: Message[JsonObject] =>
                {
                    println(msg)
                }
            }
        }

        def redisPub()
        {
            val jsonArray = new JsonArray().addString("test").addString("Hello World!")
            val data = new JsonObject().putString("command", "publish").putArray("args", jsonArray)

            vertx.eventBus.send("redis.pub", data)
            {
                msg: Message[JsonObject] =>
                {
                    println(msg)
                }
            }

        }

    }
}