package io.github.junheng.akka.hbase.protocol

import akka.dispatch.ControlMessage
import io.github.junheng.akka.hbase.OHM
import org.apache.hadoop.hbase.client.{Put, Result}

object HTableProtocol {
  case class HPut(put:Put) extends HBaseComplicateMessage

  case class HPuts(puts: List[HPut]) extends HBaseComplicateMessage

  case class HPutted(cost: Long)

  case class HScan(startRow: Option[Array[Byte]], stopRow: Option[Array[Byte]], caching: Int = 100, cacheBlocks: Boolean = true) extends ControlMessage

  case class HRange(startRow: Option[Array[Byte]], stopRow: Option[Array[Byte]], count: Int = 1000, caching: Int = 100, cacheBlocks: Boolean = true) extends ControlMessage

  case class HGet(rowKey: Array[Byte])

  case class HGets(gets: List[HGet])

  case class HDelete(rowKey: Array[Byte])

  case class HGetBinary(rowKey: String)

  case class HGetBinaries(gets: List[HGetBinaries])

  case class HResult(result: Result) extends HBaseComplicateMessage {
    def key = result.getRow

    def entity[T: Manifest] = if (result.isEmpty) None else Option(OHM.fromResult[T](result))
  }

  case class HResults(cost: Long, results: List[HResult]) extends HBaseComplicateMessage

}
