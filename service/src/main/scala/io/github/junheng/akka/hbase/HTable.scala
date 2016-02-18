package io.github.junheng.akka.hbase

import java.util.concurrent.Executors

import akka.actor.ActorRef
import io.github.junheng.akka.hbase.protocol.HScannerProtocol.HNext
import io.github.junheng.akka.hbase.protocol.HTableProtocol._
import org.apache.commons.codec.binary.BinaryCodec
import org.apache.hadoop.hbase.TableName
import org.apache.hadoop.hbase.client._

import scala.collection.JavaConversions._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

class HTable(conn: Connection, tableName: String) extends HActor {

  implicit val execution = ExecutionContext.fromExecutor(Executors.newWorkStealingPool(16))

  override def preStart(): Unit = {
    super.preStart()
    log.info(s"started for $tableName")
  }

  override def receive: Receive = {
    case HPut(put) => async {
      (table, receipt) =>
        val time = cost(table.put(put))
        receipt ! HPutted(time)
    }

    case HPuts(puts) => async {
      (table, receipt) =>
        val time = cost(table.put(puts.map(_.put)))
        receipt ! HPutted(time)
    }

    case HGet(key) => async((table, receipt) => receipt ! HResult(table.get(new Get(key))))


    case HGets(gets) => async {
      (table, receipt) =>
        var results: List[HResult] = Nil
        val getCost = cost {
          results = table.get(gets.map(_.rowKey).map(k => new Get(k)))
            .map(x => HResult(x))
            .toList
        }
        receipt ! HResults(getCost, results)
    }

    case HGetBinary(key) => self forward HGet(BinaryCodec.fromAscii(key.toCharArray))

    case HScan(startRow, stopRow, caching, cacheBlocks) => async {
      (table, receipt) =>
        val scan = new Scan()
        startRow collect { case s => scan.setStartRow(s) }
        stopRow collect { case s => scan.setStopRow(s) }
        scan.setCaching(caching)
        scan.setCacheBlocks(cacheBlocks)
        receipt ! context.actorOf(HScanner.props(table.getScanner(scan)))
    }

    case HRange(startRow, stopRow, count, caching, cacheBlocks) => async {
      (table, receipt) =>
        val scan = new Scan()
        startRow collect { case s => scan.setStartRow(s) }
        stopRow collect { case s => scan.setStopRow(s) }
        scan.setCaching(caching)
        scan.setCacheBlocks(cacheBlocks)
        context.actorOf(HScanner.props(table.getScanner(scan), closeAfterNext = true)).tell(HNext(count), receipt)
    }

    case HDelete(key) => async((table, receipt) => table.delete(new Delete(key)))
  }

  def async(process: (Table, ActorRef) => Unit) = {
    val receipt = sender()
    Future {
      var _table: Table = null
      try {
        _table = conn.getTable(TableName.valueOf(tableName))
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