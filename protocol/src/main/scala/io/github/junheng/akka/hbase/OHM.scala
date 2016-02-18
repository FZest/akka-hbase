package io.github.junheng.akka.hbase

import java.nio.ByteBuffer

import akka.event.LoggingAdapter
import org.apache.hadoop.hbase.client.{Put, Result}
import org.apache.hadoop.hbase.util.Bytes
import org.reflections.Reflections

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.language.postfixOps
import scala.reflect.ClassTag
import scala.reflect.runtime.universe
import scala.reflect.runtime.universe._
import DataType._


object OHM {
  private val mirror = universe.runtimeMirror(OHM.getClass.getClassLoader)

  private var schemas = Map[String, Schema]()

  def loadSchemas(packages: String*)(implicit log: LoggingAdapter) = {
    packages foreach { _package =>
      new Reflections(_package).getTypesAnnotatedWith(classOf[CF]).map(mirror.classSymbol).toList foreach { symbol =>
        registerSchema(symbol)
      }
    }
  }

  def registerSchema[T](clazz:Class[T])(implicit log: LoggingAdapter): Schema = {
    registerSchema(mirror.classSymbol(clazz))
  }

  def registerSchema(symbol: universe.ClassSymbol)(implicit log: LoggingAdapter): Schema = {
    schemas.get(symbol.fullName) match {
      case Some(schema) => schema
      case None =>
        try {
          var mappings = mutable.LinkedHashMap[String, Byte]()
          var fields = Map[String, Byte]()
          symbol.toType.members foreach {
            case constructor: MethodSymbol if constructor.isConstructor =>
              constructor.paramLists.head foreach { cq =>
                mappings += cq.name.toString -> cq.annotations.head.tree.children.last.productElement(1).toString.toByte
                fields += cq.name.toString -> getDataType(cq.typeSignature)
              }
            case _ => false
          }
          val mappingOfFields: mutable.LinkedHashMap[String, SchemaField] = mappings.map {
            case (name, code) => name -> SchemaField(Array(code), fields.getOrElse(name, UNKNOWN))
          }
          val family = Array(symbol.annotations.head.tree.children.last.productElement(1).toString.toByte)
          val schema = Schema(family, mappingOfFields)
          schemas += symbol.fullName -> schema
          log.info(s"schema [${symbol.fullName}] family [${hex(family)}] qualifiers ${mappingOfFields.toList.sortBy(x => hex(x._2.qualifier)).map(x => s"[${x._1} ${hex(x._2.qualifier)} ${hex(x._2.dataType)}]").mkString(" ")}")
          schema
        } catch {
          case ex: Exception => throw CanNotLoadSchemaWithGivenEntityType(symbol.fullName, ex)
        }
    }

  }

  def toPut(row: Array[Byte], target: Any): Put = {
    val put = new Put(row)
    val _type = mirror.classSymbol(target.getClass).toType
    val reflect = mirror.reflect(target)
    schemas.get(reflect.symbol.fullName) match {
      case Some(schema) =>
        schema.fields.foreach {
          case (name, field) =>
            val symbol = _type.member(TermName(name)).asTerm
            val reflectField = reflect.reflectField(symbol)
            val value = reflectField.get match {
              case v: Byte => Bytes.toBytes(v)
              case v: Int => Bytes.toBytes(v)
              case v: Long => Bytes.toBytes(v)
              case v: Float => Bytes.toBytes(v)
              case v: Double => Bytes.toBytes(v)
              case v: String => Bytes.toBytes(v)
              case Some(v: Byte) => Bytes.toBytes(v)
              case Some(v: Int) => Bytes.toBytes(v)
              case Some(v: Long) => Bytes.toBytes(v)
              case Some(v: Float) => Bytes.toBytes(v)
              case Some(v: Double) => Bytes.toBytes(v)
              case Some(v: String) => Bytes.toBytes(v)
              case None => Array[Byte]()
              case v: Array[Byte] => v
              case v: Array[Int] => v.map(Bytes.toBytes).foldLeft(Array[Byte]())(_ ++ _)
              case v: Array[Float] => v.map(Bytes.toBytes).foldLeft(Array[Byte]())(_ ++ _)
              case v: Array[Double] => v.map(Bytes.toBytes).foldLeft(Array[Byte]())(_ ++ _)
              case v: Array[String] =>
                v.map(Bytes.toBytes).map(x => Bytes.toBytes(x.length) ++ x).foldLeft(Array[Byte]())(_ ++ _)
              case _ => Array[Byte]()
            }
            if (value.nonEmpty) {
              put.addColumn(schema.family, field.qualifier, value)
            }
        }
      case None => throw NoConfiguredSchemaFound(reflect.symbol.fullName)
    }
    put
  }

  def fromResult[T: TypeTag : ClassTag](result: Result): T = {
    val _type = typeOf[T]
    val typeName = _type.typeSymbol.fullName
    schemas.get(typeName) match {
      case Some(schema) =>
        Option(result.getFamilyMap(schema.family)) match {
          case Some(cf) =>
            val cm = mirror.reflectClass(_type.typeSymbol.asClass)
            val constructor = cm.reflectConstructor(_type.decl(universe.nme.CONSTRUCTOR).asMethod)
            val constructorParams = schema.fields.map {
              case (name, SchemaField(qualifier, dataType)) =>
                Option(cf.get(qualifier)) match {
                  case Some(value) =>
                    dataType match {
                      case BYTE => value.head
                      case INT => Bytes.toInt(value)
                      case LONG => Bytes.toLong(value)
                      case FLOAT => Bytes.toFloat(value)
                      case DOUBLE => Bytes.toDouble(value)
                      case BOOLEAN => Bytes.toBoolean(value)
                      case STRING => Bytes.toString(value)
                      case SOME_BYTE => Option(value) map (_.head)
                      case SOME_INT => Option(value) map Bytes.toInt
                      case SOME_LONG => Option(value) map Bytes.toLong
                      case SOME_FLOAT => Option(value) map Bytes.toFloat
                      case SOME_DOUBLE => Option(value) map Bytes.toDouble
                      case SOME_BOOLEAN => Option(value) map Bytes.toBoolean
                      case SOME_STRING => Option(value) map Bytes.toString
                      case ARRAY_BYTE => value
                      case ARRAY_INT if value.length % 4 != 0 => value.sliding(value.length / 4, 4) map Bytes.toInt toArray
                      case ARRAY_LONG if value.length % 8 != 0 => value.sliding(value.length / 8, 8) map Bytes.toLong toArray
                      case ARRAY_FLOAT if value.length % 4 != 0 => value.sliding(value.length / 4, 4) map Bytes.toFloat toArray
                      case ARRAY_DOUBLE if value.length % 8 != 0 => value.sliding(value.length / 8, 8) map Bytes.toDouble toArray
                      case ARRAY_BOOLEAN if value.length % 4 != 0 => value.sliding(value.length / 4, 4) map Bytes.toBoolean toArray
                      case ARRAY_STRING =>
                        val buffer = ByteBuffer.wrap(value)
                        val strings = ArrayBuffer[String]()
                        while (buffer.remaining() > 0) {
                          val size = buffer.getInt()
                          val stringBytes = new Array[Byte](size)
                          buffer.get(stringBytes)
                          strings += Bytes.toString(stringBytes)
                        }
                        strings.toArray

                      case _ => throw CanNotFormatValue(name, value)
                    }
                  case None => dataType match {
                    case ARRAY_BOOLEAN => Array[Boolean]()
                    case ARRAY_BYTE => Array[Byte]()
                    case ARRAY_DOUBLE => Array[Double]()
                    case ARRAY_FLOAT => Array[Float]()
                    case ARRAY_INT => Array[Int]()
                    case ARRAY_LONG => Array[Long]()
                    case ARRAY_STRING => Array[String]()
                    case _ => None
                  }
                }

            }.toList
            constructor(constructorParams: _*).asInstanceOf[T]
          case None =>
            val families = Option(result.getMap).map(x => x.keys.map(hex(_)).toList).getOrElse(Nil)
            throw ExpectedColumnFamilyCanNotFound(hex(schema.family), families)
        }
      case None => throw NoConfiguredSchemaFound(typeName)
    }
  }

  def getDataType(_type: Type) = TYPE_MAPPING.getOrElse(_type, UNKNOWN)

  val TYPE_MAPPING = Map(
    //basic
    typeOf[Byte] -> BYTE,
    typeOf[Int] -> INT,
    typeOf[Long] -> LONG,
    typeOf[Float] -> FLOAT,
    typeOf[Double] -> DOUBLE,
    typeOf[Boolean] -> BOOLEAN,
    typeOf[String] -> STRING,
    //options
    typeOf[Option[Byte]] -> SOME_BYTE,
    typeOf[Option[Int]] -> SOME_INT,
    typeOf[Option[Long]] -> SOME_LONG,
    typeOf[Option[Float]] -> SOME_FLOAT,
    typeOf[Option[Double]] -> SOME_DOUBLE,
    typeOf[Option[Boolean]] -> SOME_BOOLEAN,
    typeOf[Option[String]] -> SOME_STRING,
    typeOf[Option[Nothing]] -> SOME_NONE,
    //arrays
    typeOf[Array[Byte]] -> ARRAY_BYTE,
    typeOf[Array[Int]] -> ARRAY_INT,
    typeOf[Array[Long]] -> ARRAY_LONG,
    typeOf[Array[Float]] -> ARRAY_FLOAT,
    typeOf[Array[Double]] -> ARRAY_DOUBLE,
    typeOf[Array[Boolean]] -> ARRAY_BOOLEAN,
    typeOf[Array[String]] -> ARRAY_STRING
  )

  final val TYPE_MAPPING_REVERSE = TYPE_MAPPING.map(x => x._2 -> x._1)


}

case class Field(name: String, value: Any)

case class Schema(family: Array[Byte], fields: mutable.LinkedHashMap[String, SchemaField])

case class SchemaField(qualifier: Array[Byte], dataType: Byte)

case class CanNotLoadSchemaWithGivenEntityType(clazz: String, exception: Exception) extends RuntimeException {
  override def getMessage: String = s"can not load $clazz to schema, cause ${exception.getMessage}"
}

case class NoConfiguredSchemaFound(name: String) extends RuntimeException {
  override def getMessage: String = s"schema not configured $name"
}

case class ExpectedColumnFamilyCanNotFound(expected: String, actual: List[String]) extends RuntimeException {
  override def getMessage: String = s"$expected column family can not found in ${actual.mkString(",")}"
}

case class CanNotFormatValue(field: String, value: Array[Byte]) extends RuntimeException {
  override def getMessage: String = s"$field can not parse ${if (value == null) "null" else hex(value)}"
}


object hex {

  def apply(buf: Array[Byte]): String = buf.map("%02X" format _).mkString

  def apply(byte: Byte): String = hex(Array(byte))
}

object DataType {
  final val BYTE = 0x00.toByte
  final val INT = 0x01.toByte
  final val LONG = 0x02.toByte
  final val FLOAT = 0x03.toByte
  final val DOUBLE = 0x04.toByte
  final val BOOLEAN = 0x05.toByte
  final val STRING = 0x06.toByte

  final val SOME_BYTE = 0xC0.toByte
  final val SOME_INT = 0xC1.toByte
  final val SOME_LONG = 0xC2.toByte
  final val SOME_FLOAT = 0xC3.toByte
  final val SOME_DOUBLE = 0xC4.toByte
  final val SOME_BOOLEAN = 0xC5.toByte
  final val SOME_STRING = 0xC6.toByte
  final val SOME_NONE = 0xCF.toByte

  final val ARRAY_BYTE = 0xA0.toByte
  final val ARRAY_INT = 0xA1.toByte
  final val ARRAY_LONG = 0xA2.toByte
  final val ARRAY_FLOAT = 0xA3.toByte
  final val ARRAY_DOUBLE = 0xA4.toByte
  final val ARRAY_BOOLEAN = 0xA5.toByte
  final val ARRAY_STRING = 0xA6.toByte

  final val UNKNOWN = 0xFF.toByte
}

