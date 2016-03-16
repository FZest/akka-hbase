package io.junheng.github.akka.hbase.actor

import io.github.junheng.akka.hbase.{CF, CQ}

@CF(0x41)
case class TestUser
(
  @CQ(0x00) id: Option[String] = None,
  @CQ(0x01) url: Option[String] = None,
  @CQ(0x02) name: Option[String] = None,
  @CQ(0x03) avatar: Option[String] = None,
  @CQ(0x04) status: Option[String] = None,
  @CQ(0x05) source: Option[String] = None,
  @CQ(0x06) fansCount: Option[Long] = None,
  @CQ(0x07) postCount: Option[Long] = None,
  @CQ(0x08) createDate: Option[Long] = None,
  @CQ(0x09) age: Option[Int] = None,
  @CQ(0x0A) gender: Option[Int] = None,
  @CQ(0x0B) birthday: Option[String] = None,
  @CQ(0x0C) lastLocationLat: Option[Float] = None,
  @CQ(0x0D) lastLocationLon: Option[Float] = None,
  @CQ(0x0E) evaluation: Option[String] = None,
  @CQ(0x0F) tags: Array[String] = Array()
)
