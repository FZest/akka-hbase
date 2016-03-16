package io.junheng.github.akka.hbase.actor

import io.github.junheng.akka.hbase.{CF, CQ}

@CF(0x41)
case class TestPerson
(
  @CQ(0x00) name: String,
  @CQ(0x01) gender: Byte,
  @CQ(0x02) age: Int,
  @CQ(0x03) bron: Long,
  @CQ(0x04) tags: Array[String],
  @CQ(0x05) score: Double,
  @CQ(0x06) address: Option[String],
  @CQ(0x07) cellphone: Option[String]
)
