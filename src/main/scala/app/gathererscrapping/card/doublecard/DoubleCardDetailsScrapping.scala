package app.gathererscrapping.card.doublecard

import app.beans.{CardFace, Language, Ruling}
import app.gathererscrapping.GathererScrappingFunctions.extractDetails
import org.jsoup.nodes.Document

import scala.collection.JavaConverters._

trait DoubleCardDetailsScrapping {

  def cardSideFromExtractedDetails(details : Map[String, String], rulings : Array[Ruling], languages : Array[Language]) : CardFace = {
    val (power, toughness) = details.get("pt")
      .map(_.split("/"))
      .filter(_.length == 2)
      .map(split => (split(0), split(1)))
      .getOrElse(("", ""))

    CardFace(
      numberInSet = details.getOrElse("number", "0"),
      name = details.getOrElse("name", ""),
      manaCost = details.getOrElse("manaCost", ""),
      convertedManaCost = details.getOrElse("convertedManaCost", "0"),
      types = details.getOrElse("types", ""),
      cardText = details.getOrElse("cardText", ""),
      flavorText = details.getOrElse("flavorText", ""),
      power = power,
      toughness = toughness,
      loyalty = details.getOrElse("loyalty", ""),
      rulings = rulings,
      languages = languages
    )
  }

  def detailsFromDocument(document: Document): (Map[String, String], Map[String, String]) = {
    document
      .select("td.rightCol")
      .asScala
      .toList
      .take(2)
      .map(element => element
        .select("[id^=ctl00_ctl00_ctl00_MainContent_SubContent_SubContent_]")
        .asScala
        .filter(_.children.asScala.exists(_.hasClass("label")))
        .filter(_.children.asScala.exists(_.hasClass("value")))
        .map(p => (
          p.select(".label").text.dropRight(1),
          p.select(".value"))
        )
        .filterNot(_._1.isEmpty)
        .flatMap(extractDetails)
        .toMap) match {
      case List(a, b) => (a, b)
    }
  }

}
