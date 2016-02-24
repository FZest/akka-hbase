package io.github.junheng.akka.hbase

import java.util.concurrent._

import akka.actor.ActorRef
import io.github.junheng.akka.hbase.HTable.LogStats
import io.github.junheng.akka.hbase.protocol.HScannerProtocol.HNext
import io.github.junheng.akka.hbase.protocol.HTableProtocol._
import org.apache.commons.codec.binary.BinaryCodec
import org.apache.hadoop.hbase.TableName
import org.apache.hadoop.hbase.client._

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

class HTable(conn: Connection, tableName: String) extends HActor {

  val executorService = new ThreadPoolExecutor(8, 32, 30, TimeUnit.SECONDS, new LinkedBlockingDeque[Runnable](10000), new ThreadPoolExecutor.CallerRunsPolicy())

  implicit val execution = ExecutionContext.fromExecutor(executorService)

  val reporter = context.system.scheduler.schedule(15 seconds, 15 seconds, self, LogStats)

  val stats = mutable.Map[String, Int]()

  override def preStart(): Unit = {
    super.preStart()
    log.info(s"started for $tableName")
  }

  override def receive: Receive = {
    case LogStats => log.info(s"[$tableName] ${stats.toSeq.sortWith(_._1 < _._1).map(x => s"${x._1}:${x._2}").mkString(", ")}")

    case HPut(put) => async { (table, receipt) =>
      table.put(put)
      stats.get("put") match {
        case Some(current) => stats.put("put", current + 1)
        case None => stats.put("put", 1)
      }
    }

    case HPuts(puts) => async { (table, receipt) =>
      table.put(puts.map(_.put))
      stats.get("put") match {
        case Some(current) => stats.put("put", current + puts.size)
        case None => stats.put("put", puts.size)
      }
    }

    case HGet(key) => async { (table, receipt) =>
      receipt ! HResult(table.get(new Get(key)))
      stats.get("get") match {
        case Some(current) => stats.put("get", current + 1)
        case None => stats.put("get", 1)
      }
    }


    case HGets(gets) => async { (table, receipt) =>
      var results: List[HResult] = Nil
      val getCost = cost {
        results = table.get(gets.map(_.rowKey).map(k => new Get(k)))
          .map(x => HResult(x))
          .toList
      }
      receipt ! HResults(getCost, results)
      stats.get("get") match {
        case Some(current) => stats.put("get", current + gets.size)
        case None => stats.put("get", gets.size)
      }
    }

    case HGetBinary(key) => self forward HGet(BinaryCodec.fromAscii(key.toCharArray))

    case HScan(startRow, stopRow, caching, cacheBlocks) => async { (table, receipt) =>
      val scan = new Scan()
      startRow collect { case s => scan.setStartRow(s) }
      stopRow collect { case s => scan.setStopRow(s) }
      scan.setCaching(caching)
      scan.setCacheBlocks(cacheBlocks)
      receipt ! context.actorOf(HScanner.props(table.getScanner(scan)))
      stats.get("scan") match {
        case Some(current) => stats.put("scan", current + 1)
        case None => stats.put("scan", 1)
      }
    }

    case HRange(startRow, stopRow, count, caching, cacheBlocks) => async { (table, receipt) =>
      val scan = new Scan()
      startRow collect { case s => scan.setStartRow(s) }
      stopRow collect { case s => scan.setStopRow(s) }
      scan.setCaching(caching)
      scan.setCacheBlocks(cacheBlocks)
      context.actorOf(HScanner.props(table.getScanner(scan), closeAfterNext = true)).tell(HNext(count), receipt)
      stats.get("range") match {
        case Some(current) => stats.put("range", current + 1)
        case None => stats.put("range", 1)
      }
    }

    case HDelete(key) => async { (table, receipt) =>
      table.delete(new Delete(key))
      stats.get("delete") match {
        case Some(current) => stats.put("delete", current + 1)
        case None => stats.put("delete", 1)
      }
    }
  }

  def async(process: (Table, ActorRef) => Unit) = {
    val receipt = sender()
    Future {
      var _table: Table = null
      try {
        _table = conn.getTable(TableName.valueOf(tableName))
        _table.setWriteBufferSize(1024 * 1024 * 16)
        process(_table, receipt)
      }
      catch {
        case ex: Throwable => log.error(ex, s"error when process request in $tableName")
      }
      finally {
        if (_table != null) _table.close()
      }
    }
  }

  override def postStop(): Unit = {
    super.postStop()
    log.info("stopped")
  }

}

object HTable {

  case object LogStats

}