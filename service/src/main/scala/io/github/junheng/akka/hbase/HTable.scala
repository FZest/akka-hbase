package io.github.junheng.akka.hbase

import java.util.concurrent.Executors

import akka.actor.ActorRef
import akka.dispatch.ControlMessage
import io.github.junheng.akka.hbase.HScanner.HNext
import io.github.junheng.akka.hbase.HTable._
import io.github.junheng.akka.hbase.OHM._
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
    case HPut(key, entity) => async {
      (table, receipt) =>
        val time = cost(table.put(toPut(key, entity)))
        receipt ! HPutted(time)
    }

    case HPuts(puts) => async {
      (table, receipt) =>
        val time = cost(table.put(puts.map(x => toPut(x.key, x.entity))))
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

object HTable {

  case class HPut(key: Array[Byte], entity: Any) {
    def entityAs[C] = entity.asInstanceOf[C]
  }


  case class HPuts(puts: List[HPut])

  case class HPutThenFlush(key: Array[Byte], entity: Any)

  case class HPutThenFlushes(puts: List[HPutThenFlush])

  case class HPutted(cost: Long)

  case class HScan(startRow: Option[Array[Byte]], stopRow: Option[Array[Byte]], caching: Int = 100, cacheBlocks: Boolean = true) extends ControlMessage

  case class HRange(startRow: Option[Array[Byte]], stopRow: Option[Array[Byte]], count: Int = 1000, caching: Int = 100, cacheBlocks: Boolean = true) extends ControlMessage

  case class HGet(rowKey: Array[Byte])

  case class HGets(gets: List[HGet])

  case class HDelete(rowKey: Array[Byte])

  case class HGetBinary(rowKey: String)

  case class HGetBinaries(gets: List[HGetBinaries])

  case class HResult(payload: Result) {
    def key = payload.getRow

    def entity[T: Manifest] = if (payload.isEmpty) None else Option(OHM.fromResult[T](payload))
  }

  case class HResults(cost: Long, entities: List[HResult])

}
