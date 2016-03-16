package io.junheng.github.akka.hbase.actor

import io.github.junheng.akka.hbase.{CF, CQ}

@CF(0x41)
case class TestPost
(
  @CQ(0x00) id: Option[String] = None,
  @CQ(0x01) target: Option[String] = None,
  @CQ(0x02) targetId: Option[String] = None,
  @CQ(0x03) locationLat: Option[Float] = None,
  @CQ(0x04) locationLon: Option[Float] = None,
  @CQ(0x05) timestamp: Option[Long] = None,
  @CQ(0x06) commentsCount: Option[Int] = None,
  @CQ(0x07) likesCount: Option[Int] = None,
  @CQ(0x08) postType: Option[Int] = None,
  @CQ(0x09) tags: Array[String] = Array(),
  @CQ(0x0A) content: Option[String] = None
)
