package io.github.junheng.akka.hbase

import akka.actor.{PoisonPill, Props}
import io.github.junheng.akka.hbase.HScanner.HNext
import io.github.junheng.akka.hbase.protocol.HTableProtocol._
import org.apache.hadoop.hbase.client.ResultScanner

import scala.language.postfixOps

class HScanner(scanner: ResultScanner, closeAfterNext: Boolean) extends HActor {

  override def preStart(): Unit = log.info("started")

  override def receive: Receive = {
    case HNext(step) =>
      var results: List[HResult] = Nil
      val getCost = cost {
        results = scanner.next(step)
          .map(x => HResult(x))
          .toList
      }
      sender() ! HResults(getCost, results)
      if (closeAfterNext) self ! PoisonPill
  }

  override def postStop(): Unit = {
    scanner.close()
    log.info("stopped")
  }
}

object HScanner {
  def props(scanner: ResultScanner, closeAfterNext: Boolean = false) = Props(new HScanner(scanner, closeAfterNext))

  case class HNext(step: Int)

}