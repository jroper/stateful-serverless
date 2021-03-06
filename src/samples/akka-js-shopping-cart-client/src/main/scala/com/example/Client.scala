package com.example

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.grpc.GrpcClientSettings
import com.example.shoppingcart._

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

object Client {
  def main(args: Array[String]): Unit = {
    val client = new Client("127.0.0.1", 9000)

    val userId = "viktor"
    val productId = "1337"
    val productName = "h4x0r"

    try {
      println(client.getCart(userId))
      for (_ <- 1 to 8) {
        client.addItem(userId, productId, productName, 1)
      }
      println(client.getCart(userId))
      client.removeItem(userId, productId)
      println(client.getCart(userId))
    } finally {
      try {
        client.shutdown()
      } finally {
        System.exit(0)
      }
    }
  }
}

/**
  * Designed for use in the REPL, run sbt console and then new com.example.Client("localhost", 9000)
  * @param hostname
  * @param port
  */
class Client(hostname: String, port: Int) {

  private implicit val system = ActorSystem()
  private implicit val mat = ActorMaterializer()
  import system.dispatcher

  val service = ShoppingCartClient(GrpcClientSettings.connectToServiceAt(hostname, port).withTls(false))

  def shutdown(): Unit = {
    await(service.close())
    await(system.terminate())
  }

  def await[T](future: Future[T]): T = Await.result(future, 10.seconds)

  def getCart(userId: String) = await(service.getCart(GetShoppingCart(userId)))
  def addItem(userId: String, productId: String, name: String, quantity: Int) =
    await(service.addItem(AddLineItem(userId, productId, name, quantity)))
  def removeItem(userId: String, productId: String) = await(service.removeItem(RemoveLineItem(userId, productId)))
}
