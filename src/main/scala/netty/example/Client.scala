package netty.example

import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory
import java.util.concurrent.Executors
import org.jboss.netty.bootstrap.ClientBootstrap
import org.jboss.netty.channel._
import java.net.InetSocketAddress
import org.jboss.netty.buffer.ChannelBuffer
import java.util.Date
import akka.actor.{Props, ActorSystem, Actor}

class Client(host: String, port: Int, handler: ChannelHandler) extends Actor {
  def run {
    val factory =
      new NioClientSocketChannelFactory(
        Executors.newCachedThreadPool(),
        Executors.newCachedThreadPool())

    val bootstrap = new ClientBootstrap(factory)

    bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
      def getPipeline = Channels.pipeline(handler)
    })

    bootstrap.setOption("tcpNoDelay", true)
    bootstrap.setOption("keepAlive", true)
    bootstrap.connect(new InetSocketAddress(host, port))
  }

  def receive = {
    case "query" => run
    case _ => println("don't know what are you talking about")
  }
}


class TimeClientHandler extends SimpleChannelHandler {

  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
    val buf = e.getMessage().asInstanceOf[ChannelBuffer]
    val currentTimeMillis = buf.readInt() * 1000L
    println(new Date(currentTimeMillis))
    e.getChannel().close()
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) {
    e.getCause().printStackTrace()
    val ch = e.getChannel()
    ch.close()
  }
}

object MainClient extends App {

  val system = ActorSystem("MySystem")
  val myActor = {
    system.actorOf(Props(new Client("localhost", 8080, new TimeClientHandler)), name = "TimeClient")
  }

  (1 to 3).foreach {
    x => {
      myActor ! "query"
      Thread.sleep(5000)
    }
  }
}