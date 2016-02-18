package io.github.junheng.akka.hbase.protocol

import akka.dispatch.ControlMessage
import io.github.junheng.akka.hbase.OHM
import org.apache.hadoop.hbase.client.Result

object HTableProtocol {
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
