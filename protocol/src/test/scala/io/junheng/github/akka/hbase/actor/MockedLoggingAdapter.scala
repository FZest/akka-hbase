package io.junheng.github.akka.hbase.actor

import akka.event.LoggingAdapter

object MockedLoggingAdapter extends LoggingAdapter {
  override def isErrorEnabled: Boolean = true

  override def isInfoEnabled: Boolean = true

  override def isDebugEnabled: Boolean = true

  override def isWarningEnabled: Boolean = true

  override protected def notifyInfo(message: String): Unit = println(message)

  override protected def notifyError(message: String): Unit = println(message)

  override protected def notifyError(cause: Throwable, message: String): Unit = println(message)

  override protected def notifyWarning(message: String): Unit = println(message)

  override protected def notifyDebug(message: String): Unit = println(message)
}
