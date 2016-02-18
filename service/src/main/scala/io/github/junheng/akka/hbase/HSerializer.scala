package io.github.junheng.akka.hbase

import org.apache.hadoop.hbase.client.{Result, Put}

trait HSerializer[T] {
  def serialize(key:Array[Byte], entity: T): Put

  def deserialize(result: Result): (Array[Byte], Option[T])
}
