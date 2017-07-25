package nl.surfsara.sda

import nl.surfsara.warcutils.WarcInputFormat
import org.apache.commons.io.IOUtils
import org.apache.hadoop.io.LongWritable
import org.apache.hadoop.io.compress.DefaultCodec
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.jwat.warc.WarcRecord

import scala.util.Try

object SparkWarcExample {

  def extractHtmlContent(rdd: RDD[(LongWritable, WarcRecord)]): RDD[(String, String)] = {
    rdd.filter {
      case (id, record) => Try(record.getHttpHeader.contentType.contains("text/html")) getOrElse false
    }.map {
      case (id, record) => (record.header.warcTargetUriStr, IOUtils.toString(record.getPayloadContent))
    }
  }

  /*
  def extractExternalLinks(rdd: RDD[(String, String)]): RDD[(String, String)] = {
    rdd.flatMap {
      case (uri, content) => Jsoup.parse(content).select("a")
        .map(l => (uri, l.attr("abs:href")))
        .filterNot {
          case (src, dest) => dest.equals("")
        }
    }
  }

  def extractHostPairs(rdd: RDD[(String, String)]): RDD[(String, String)] = {
    rdd.map {
      case (src, dest) => {
        Try((new URI(src)).getHost, (new URI(dest)).getHost) getOrElse(null, null)
      }
    }.filter {
      case (null, _) => false
      case (_, null) => false
      case (src, dest) if src.equals(dest) => false
      case _ => true
    }
  }
  */

  def main(args: Array[String]): Unit = {

    val inPath = args(0)
    val outPath = args(1)

    val conf = new SparkConf().setAppName("warcutils-example")
    val sc = new SparkContext(conf)

    val warcInput = sc.newAPIHadoopFile[LongWritable, WarcRecord, WarcInputFormat](inPath)
    val htmlRecords = extractHtmlContent(warcInput)

    htmlRecords.saveAsSequenceFile(outPath, Some(classOf[DefaultCodec]))

  }
}

