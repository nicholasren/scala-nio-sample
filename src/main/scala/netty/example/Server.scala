package netty.example

import org.jboss.netty.channel._
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory
import java.util.concurrent.Executors
import org.jboss.netty.bootstrap.ServerBootstrap
import java.net.InetSocketAddress
import org.jboss.netty.buffer.{ChannelBuffers, ChannelBuffer}

class Server(port: Int, handler: ChannelHandler) {
  def run() {
    val factory = new NioServerSocketChannelFactory(
      Executors.newCachedThreadPool(), Executors.newCachedThreadPool())

    val bootstrap = new ServerBootstrap(factory)

    bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
      def getPipeline = {
        Channels.pipeline(handler)
      }
    })

    bootstrap.setOption("child.tcpNoDelay", true)
    bootstrap.setOption("child.keepAlive", true)
    bootstrap.bind(new InetSocketAddress(port))

  }
}

class EchoServerHandler extends SimpleChannelHandler {
  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
    e.getChannel.write(e.getMessage)
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) {
    e.getCause().printStackTrace()
    val ch = e.getChannel()
    ch.close()
  }
}

class TimeServerHandler extends SimpleChannelHandler {
  override def channelConnected(ctx: ChannelHandlerContext, e: ChannelStateEvent) {
    val channel = e.getChannel()
    val time = ChannelBuffers.buffer(4)
    time.writeInt((System.currentTimeMillis() / 1000L + 2208988800L).toInt)

    val future = channel.write(time)

    future.addListener(new ChannelFutureListener() {
      def operationComplete(future: ChannelFuture) {
        val channel = future.getChannel()
        channel.close()
      }
    })
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) {
    e.getCause().printStackTrace()
    val ch = e.getChannel()
    ch.close()
  }
}


object Main extends App {
  val timeServer = new Server(8080, new TimeServerHandler).run
}
