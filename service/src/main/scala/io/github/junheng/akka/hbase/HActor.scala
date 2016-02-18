package io.github.junheng.akka.hbase

import akka.actor.{Actor, ActorLogging}
import io.github.junheng.akka.utils.BytesTrait
import org.apache.hadoop.hbase.TableName

import scala.language.implicitConversions

abstract class HActor extends Actor with ActorLogging with BytesTrait{

  implicit def arrayToTableName(name: Array[Byte]): TableName = TableName.valueOf(name)

  protected def cost(proc: => Unit) = {
    val started = System.nanoTime()
    proc
    val used = System.nanoTime() - started
    used / 1000 / 1000
  }

  override def unhandled(message: Any): Unit = log.warning(s"unexpected message ${message.getClass.getCanonicalName} received")
}
