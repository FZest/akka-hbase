package io.junheng.github.akka.hbase.actor

import io.github.junheng.akka.hbase.OHM
import io.github.junheng.akka.utils.measure
import org.apache.hadoop.hbase.KeyValue
import org.apache.hadoop.hbase.client.Result
import org.apache.hadoop.hbase.util.Bytes
import org.scalatest.concurrent.Timeouts
import org.scalatest.{Matchers, SequentialNestedSuiteExecution, WordSpecLike}

import scala.collection.JavaConversions._
import scala.concurrent.duration._
import scala.language.postfixOps

class OHMTest extends WordSpecLike with Matchers with SequentialNestedSuiteExecution with Timeouts {
  "OHM" should {
    OHM.loadSchemas("io.junheng.github.akka.hbase.actor")(MockedLoggingAdapter)
    val row = Array(0xFF.toByte, 0xFF.toByte, 0xF1.toByte)
    val cf = Array(0x41.toByte)

    val expectedProfileId = "33282123:weibo"
    val expectedUrl = "http://weibo.com/23211271"
    val expectedName = "junheng gong"
    val expectedAvatar = "http://weibo.com/23211271/_avatar/head.jpg"
    val expectedStatus = "normal"
    val expectedSource = "weibo"
    val expectedFansCount = 1049L
    val expectedPostCount = 1204L
    val expectedCreateDate = 1451976009171L
    val expectedAge = 29
    val expectedGender = 1
    val expectedBirthday = "1998/02/11"
    val expectedLastLocationLat = -31.32F
    val expectedLastLocationLon = 21.45F
    val expectedEvaluation = "high school"

    "can mapping a object to a hbase put" in {
      //Given
      val entity = TestUser(
        id = Some(expectedProfileId),
        url = Some(expectedUrl),
        name = Some(expectedName),
        avatar = Some(expectedAvatar),
        status = Some(expectedStatus),
        source = Some(expectedSource),
        fansCount = Some(expectedFansCount),
        postCount = Option(expectedPostCount),
        createDate = Option(expectedCreateDate),
        age = Option(expectedAge),
        gender = Option(expectedGender),
        birthday = Option(expectedBirthday),
        lastLocationLat = Option(expectedLastLocationLat),
        lastLocationLon = Option(expectedLastLocationLon),
        evaluation = Option(expectedEvaluation)
      )
      //When
      val put = OHM.toPut(row, entity)
      //Then
      put.getFamilyCellMap.values().head.size() shouldEqual 15
    }

    "can process a entity with some field none" in {
      //Given
      val idKV = new KeyValue(row, Array(0x41.toByte), Array(0x00.toByte), Bytes.toBytes(expectedProfileId))
      val result = Result.create(List(idKV))
      val entity = OHM.fromResult[TestUser](result)
      //Then
      entity.id.get shouldEqual expectedProfileId
      entity.url shouldEqual None
      entity.name shouldEqual None
      entity.avatar shouldEqual None
      entity.status shouldEqual None
      entity.source shouldEqual None
      entity.fansCount shouldEqual None
      entity.postCount shouldEqual None
      entity.createDate shouldEqual None
      entity.age shouldEqual None
      entity.gender shouldEqual None
      entity.birthday shouldEqual None
      entity.lastLocationLat shouldEqual None
      entity.lastLocationLon shouldEqual None
      entity.evaluation shouldEqual None
    }

    "can process a entity with all field filled" in {
      //Given
      val result = Result.create(
        List(
          kv(row, cf, 0x00, Bytes.toBytes(expectedProfileId)),
          kv(row, cf, 0x01, Bytes.toBytes(expectedUrl)),
          kv(row, cf, 0x02, Bytes.toBytes(expectedName)),
          kv(row, cf, 0x03, Bytes.toBytes(expectedAvatar)),
          kv(row, cf, 0x04, Bytes.toBytes(expectedStatus)),
          kv(row, cf, 0x05, Bytes.toBytes(expectedSource)),
          kv(row, cf, 0x06, Bytes.toBytes(expectedFansCount)),
          kv(row, cf, 0x07, Bytes.toBytes(expectedPostCount)),
          kv(row, cf, 0x08, Bytes.toBytes(expectedCreateDate)),
          kv(row, cf, 0x09, Bytes.toBytes(expectedAge)),
          kv(row, cf, 0x0A, Bytes.toBytes(expectedGender)),
          kv(row, cf, 0x0B, Bytes.toBytes(expectedBirthday)),
          kv(row, cf, 0x0C, Bytes.toBytes(expectedLastLocationLat)),
          kv(row, cf, 0x0D, Bytes.toBytes(expectedLastLocationLon)),
          kv(row, cf, 0x0E, Bytes.toBytes(expectedEvaluation))
        )
      )
      //When
      val entity = OHM.fromResult[TestUser](result)
      //Then
      entity.id.get shouldEqual expectedProfileId
      entity.url.get shouldEqual expectedUrl
      entity.name.get shouldEqual expectedName
      entity.avatar.get shouldEqual expectedAvatar
      entity.status.get shouldEqual expectedStatus
      entity.source.get shouldEqual expectedSource
      entity.fansCount.get shouldEqual expectedFansCount
      entity.postCount.get shouldEqual expectedPostCount
      entity.createDate.get shouldEqual expectedCreateDate
      entity.age.get shouldEqual expectedAge
      entity.gender.get shouldEqual expectedGender
      entity.birthday.get shouldEqual expectedBirthday
      entity.lastLocationLat.get shouldEqual expectedLastLocationLat
      entity.lastLocationLon.get shouldEqual expectedLastLocationLon
      entity.evaluation.get shouldEqual expectedEvaluation
    }

    "can process 100m data in one seconds" in {
      val result = Result.create(
        List(
          kv(row, cf, 0x00, Bytes.toBytes(expectedProfileId)),
          kv(row, cf, 0x01, Bytes.toBytes(expectedUrl)),
          kv(row, cf, 0x02, Bytes.toBytes(expectedName)),
          kv(row, cf, 0x03, Bytes.toBytes(expectedAvatar)),
          kv(row, cf, 0x04, Bytes.toBytes(expectedStatus)),
          kv(row, cf, 0x05, Bytes.toBytes(expectedSource)),
          kv(row, cf, 0x06, Bytes.toBytes(expectedFansCount)),
          kv(row, cf, 0x07, Bytes.toBytes(expectedPostCount)),
          kv(row, cf, 0x08, Bytes.toBytes(expectedCreateDate)),
          kv(row, cf, 0x09, Bytes.toBytes(expectedAge)),
          kv(row, cf, 0x0A, Bytes.toBytes(expectedGender)),
          kv(row, cf, 0x0B, Bytes.toBytes(expectedBirthday)),
          kv(row, cf, 0x0C, Bytes.toBytes(expectedLastLocationLat)),
          kv(row, cf, 0x0D, Bytes.toBytes(expectedLastLocationLon)),
          kv(row, cf, 0x0E, Bytes.toBytes(expectedEvaluation))
        )
      )
      failAfter(1000 millis) {
        0 until 20000 foreach { i =>
          OHM.fromResult[TestUser](result)
        }
      }
    }
  }

  def kv(row: Array[Byte], cf: Array[Byte], cq: Byte, value: Array[Byte]) = new KeyValue(row, cf, Array(cq), value)
}













