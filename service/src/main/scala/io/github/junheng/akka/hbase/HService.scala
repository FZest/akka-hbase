package io.github.junheng.akka.hbase

import akka.actor._
import com.typesafe.config.Config
import io.github.junheng.akka.hbase.HService._
import org.apache.hadoop.hbase.client.{Connection, ConnectionFactory}
import org.apache.hadoop.hbase.io.compress.Compression
import org.apache.hadoop.hbase.util.RegionSplitter.{HexStringSplit, UniformSplit}
import org.apache.hadoop.hbase.{HBaseConfiguration, HColumnDescriptor, HTableDescriptor, TableName}

import scala.collection.JavaConversions._
import scala.reflect.runtime._

class HService(config: Config, propsOfHTable: (Connection, String) => Props) extends HActor {

  protected val mirror = universe.runtimeMirror(getClass.getClassLoader)

  protected val conn = connect()

  protected val uniformSplit = new UniformSplit

  protected val hexStringSplit = new HexStringSplit

  override def preStart(): Unit = {
    super.preStart()
    log.info("started")
    config.getConfigList("tables") foreach { tb =>
      if (tb.hasPath("split")) {
        createHTable(tb.getString("name"), Array(Integer.parseInt(tb.getString("cf").replace("0x", ""), 16).toByte), tb.getInt("regions"), tb.getString("split"))
      } else {
        createHTable(tb.getString("name"), Array(Integer.parseInt(tb.getString("cf").replace("0x", ""), 16).toByte), -1, "none")
      }

    }
  }

  override def receive: Receive = {

    case CreateTableSplit(name, columnFamily, regionCount, split) => sender() ! HTableRef(createHTable(name, columnFamily, regionCount, split))

    case CreateTableWithOutSplit(name, entityType) => self forward CreateTableSplit(name, entityType, -1, "none")

    case DisableTable(name) => conn.getAdmin.disableTable(name)

    case DropTable(name) =>
      context.child(name) match {
        case Some(ref) =>
          ref ! PoisonPill
          conn.getAdmin.deleteTable(name)
        case None =>
      }
      self forward ListTables

    case ListTables => sender() ! status
  }

  def createHTable(name: Array[Byte], columnFamily: Array[Byte], regionCount: Int, split: String): ActorRef = {
    val table =
      if (conn.getAdmin.tableExists(name)) {
        context.child(name) match {
          case Some(ref) => ref
          case None =>
            val gotTable = conn.getTable(TableName.valueOf(name))
            gotTable.setWriteBufferSize(1024 * 1024 * 2)
            context.actorOf(propsOfHTable(conn, name), name)
        }
      } else {
        split match {
          case "uniform" => conn.getAdmin.createTable(createDescriptor(name, columnFamily), uniformSplit.split(regionCount))
          case "hex" => conn.getAdmin.createTable(createDescriptor(name, columnFamily), hexStringSplit.split(regionCount))
          case _ => conn.getAdmin.createTable(createDescriptor(name, columnFamily))
        }
        context.actorOf(propsOfHTable(conn, name), name)
      }
    table
  }

  def status = {
    val hbaseTables: List[String] = conn.getAdmin.listTableNames().map(x => x.getNameAsString).toList
    val hbaseActors: List[String] = context.children.toList.map(_.path.toStringWithoutAddress)
    HTableList(hbaseTables.map(x => x -> hbaseActors.find(_.endsWith(x))).toMap)
  }

  def createDescriptor(name: Array[Byte], columnFamily: Array[Byte]): HTableDescriptor = {
    val tableName = TableName.valueOf(name)
    val columnDescriptor = new HColumnDescriptor(columnFamily)
    columnDescriptor.setCompressionType(Compression.Algorithm.SNAPPY)
    columnDescriptor.setCompactionCompressionType(Compression.Algorithm.SNAPPY)
    val descriptor = new HTableDescriptor(tableName)
    descriptor.addFamily(columnDescriptor)
    descriptor
  }

  def connect(): Connection = {
    val configuration = HBaseConfiguration.create()
    configuration.set("hbase.zookeeper.quorum", config.getString("zookeepers"))
    configuration.set("hbase.zookeeper.property.clientPort", config.getString("zookeepers-port"))
    configuration.set("hbase.regionserver.lease.period", "86400000")

    ConnectionFactory.createConnection(configuration)
  }
}

object HService {
  type PropsOfHTable = (Connection, String) => Props

  private val defaultProps: PropsOfHTable = (conn, name) => Props(new HTable(conn, name))

  def start(config: Config, propsOfHTable: PropsOfHTable = defaultProps)(implicit system: ActorSystem): ActorRef = {
    system.actorOf(Props(new HService(config, propsOfHTable)), "hbase")
  }

  case class CreateTableSplit[T: Manifest](name: Array[Byte], columnFamily: Array[Byte], regionCount: Int, split: String)

  case class CreateTableWithOutSplit[T: Manifest](name: Array[Byte], columnFamily: Array[Byte])

  case class DisableTable(name: Array[Byte])

  case class DropTable(name: Array[Byte])

  case object ListTables

  case class HTableRef(actorRef: ActorRef)

  case class HTableList(tables: Map[String, Option[String]])

}
