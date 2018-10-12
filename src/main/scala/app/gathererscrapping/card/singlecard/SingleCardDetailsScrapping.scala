package app.gathererscrapping.card.singlecard

import app.beans.Ruling
import app.gathererscrapping.GathererScrappingFunctions.extractDetails
import com.google.gson.Gson
import org.jsoup.nodes.Document

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

trait SingleCardDetailsScrapping {

  def singleCardDetailsFromDocument(document : Document, multiverseId: String) : Try[Map[String, String]] = {
    var result = Map[String, String]()
    val singleCardInfo = extractSingleCardInformationFromDocument(document)
    if (singleCardInfo.isEmpty) {
      return Failure(new Exception(s"Error getting card $multiverseId"))
    }
    result ++= singleCardInfo
    result += extractSingleRulingsFromDocument(document)
    Success(result)
  }

  def extractSingleCardInformationFromDocument(document: Document) : Map[String, String] = {
    Try(
      document
        .select("td#ctl00_ctl00_ctl00_MainContent_SubContent_SubContent_rightCol " +
          "div[id^=ctl00_ctl00_ctl00_MainContent_SubContent_SubContent_]")
        .asScala
        .filter(_.children.asScala.exists(_.hasClass("label")))
        .filter(_.children.asScala.exists(_.hasClass("value")))
        .map(p => (
          p.select(".label").text.dropRight(1),
          p.select(".value"))
        )
        .filterNot(_._1.isEmpty)
        .flatMap(extractDetails)
        .toMap
    ) match {
      case Success(details) => details
      case Failure(_) => Map.empty
    }
  }

  def extractSingleRulingsFromDocument(document : Document) : (String, String) = {
    val gson = new Gson()
    Try(
      document
        .select("table.rulingsTable tr")
        .asScala
        .map(tr => Ruling(
          tr.select("td[id$=rulingDate]").text(),
          tr.select("td[id$=rulingText]").text()
        ))
        .toArray
    ) match {
      case Success(rulings) => "rulings" -> gson.toJson(rulings)
      case Failure(_) => "rulings" -> ""
    }
  }
}
