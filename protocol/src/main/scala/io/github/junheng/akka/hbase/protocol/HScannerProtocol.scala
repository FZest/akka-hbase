package io.github.junheng.akka.hbase.protocol

object HScannerProtocol {
  case class HNext(step: Int)
}
