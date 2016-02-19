package io.github.junheng.akka.hbase.protocol

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, DataInputStream, DataOutputStream}

import akka.serialization.SerializerWithStringManifest
import io.github.junheng.akka.hbase.protocol.HTableProtocol.{HPut, HPuts, HResult, HResults}
import io.github.junheng.akka.hbase.protocol.ProtocolSerializer._
import org.apache.hadoop.hbase.client.{Put, Result}
import org.apache.hadoop.hbase.{Cell, CellScanner, CellUtil, KeyValue}

import scala.collection.mutable.ArrayBuffer
import scala.language.implicitConversions

class ProtocolSerializer extends SerializerWithStringManifest {
  override def identifier: Int = 0xFFC1

  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef = {
    manifest match {
      case "io.github.junheng.akka.hbase.protocol.HTableProtocol.HResult" => bytesToResult(bytes)
      case "io.github.junheng.akka.hbase.protocol.HTableProtocol.HResults" => bytesToResults(bytes)
      case "io.github.junheng.akka.hbase.protocol.HTableProtocol.HPut" => bytesToPut(bytes)
      case "io.github.junheng.akka.hbase.protocol.HTableProtocol.HPuts" => bytesToPuts(bytes)
      case other => throw new RuntimeException(s"can not deserialize message: $other")
    }
  }

  override def manifest(o: AnyRef): String = o.getClass.getCanonicalName

  override def toBinary(origin: AnyRef): Array[Byte] = {
    origin match {
      case result: HResult => resultToBytes(result)
      case results: HResults => resultsToBytes(results)
      case put: HPut => putToBytes(put)
      case puts: HPuts => putsToBytes(puts)
      case other => throw new RuntimeException(s"can not serialize: ${other.getClass.getCanonicalName}")
    }
  }

  def bytesToPut(bytes: Array[Byte]): HPut = {
    val (bis, dis) = asIS(bytes)
    try HPut(dis.readPut()) finally {
      dis.close()
      bis.close()
    }
  }

  def bytesToPuts(bytes: Array[Byte]): HPuts = {
    val (bis, dis) = asIS(bytes)
    try HPuts(dis.readPuts().map(HPut).toList) finally {
      dis.close()
      bis.close()
    }
  }

  def bytesToResult(bytes: Array[Byte]): HResult = {
    val (bis, dis) = asIS(bytes)
    try HResult(dis.readResult()) finally {
      dis.close()
      bis.close()
    }
  }

  def bytesToResults(bytes: Array[Byte]): HResults = {
    val (bis, dis) = asIS(bytes)
    try {
      val cost = dis.readLong()
      HResults(cost, dis.readResults().map(HResult).toList)
    } finally {
      dis.close()
      bis.close()
    }
  }

  def putToBytes(put: HPut): Array[Byte] = {
    val (bos, dos) = asOS()
    try {
      dos.writePut(put.put)
      bos.toByteArray
    } finally {
      dos.close()
      bos.close()
    }
  }

  def putsToBytes(puts: HPuts): Array[Byte] = {
    val (bos, dos) = asOS()
    try {
      dos.writePuts(puts.puts.map(_.put))
      bos.toByteArray
    } finally {
      dos.close()
      bos.close()
    }
  }

  def resultToBytes(result: HResult): Array[Byte] = {
    val (bos, dos) = asOS()
    try {
      dos.writeResult(result.result)
      bos.toByteArray
    } finally {
      dos.close()
      bos.close()
    }
  }

  def resultsToBytes(results: HResults): Array[Byte] = {
    val (bos, dos) = asOS()
    try {
      dos.writeLong(results.cost)
      dos.writeResults(results.results.map(_.result))
      bos.toByteArray
    } finally {
      dos.close()
      bos.close()
    }
  }

  def asIS(bytes: Array[Byte]): (ByteArrayInputStream, DataInputStream) = {
    val bis = new ByteArrayInputStream(bytes)
    val dis = new DataInputStream(bis)
    (bis, dis)
  }

  def asOS() = {
    val bos = new ByteArrayOutputStream()
    val dos = new DataOutputStream(bos)
    (bos, dos)
  }


}

object ProtocolSerializer {

  implicit def cellScannerToList(scanner: CellScanner): List[Cell] = {
    val cells = ArrayBuffer[Cell]()
    while (scanner.advance()) cells += scanner.current()
    cells.toList
  }

  implicit class InSteam(steam: DataInputStream) {

    def readPuts(): Seq[Put] = {
      val length = steam.readInt()
      val values = new ArrayBuffer[Put]()
      0 until length foreach (i => values += readPut())
      values
    }

    def readPut(): Put = {
      val cells = readCells()
      val row = CellUtil.cloneRow(cells.head)
      val ts = cells.head.getTimestamp
      val put = new Put(row, ts)
      cells foreach put.add
      put
    }

    def readResults(): Seq[Result] = {
      val length = steam.readInt()
      val values = new ArrayBuffer[Result]()
      0 until length foreach (i => values += readResult())
      values
    }

    def readResult(): Result = Result.create(readCells().toArray)

    def readCells(): Seq[Cell] = {
      val length = steam.readInt()
      val values = new ArrayBuffer[Cell]()
      0 until length foreach (i => values += readCell())
      values
    }

    def readCell(): Cell = {
      val length = steam.readInt()
      val value = new Array[Byte](length)
      steam.read(value)
      new KeyValue(value)
    }
  }

  implicit class OutSteam(steam: DataOutputStream) {

    def writePut(put: Put) = writeCells(put.cellScanner())

    def writeResult(result: Result) = writeCells(result.rawCells())

    def writePuts(puts: Seq[Put]) = {
      steam.writeInt(puts.size)
      puts foreach writePut
    }

    def writeResults(results: Seq[Result]) = {
      steam.writeInt(results.size)
      results foreach writeResult
    }

    def writeCells(cells: Seq[Cell]) = {
      steam.writeInt(cells.length)
      cells foreach writeCell
    }

    def writeCell(cell: Cell) = {
      val valueArray = cell.getValueArray
      steam.writeInt(valueArray.length)
      steam.write(valueArray)
    }
  }

}
