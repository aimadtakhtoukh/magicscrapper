package app.beans

case class Card (
  multiverseId : String = "",
  name : String = "",
  setName : String = "",
  rarity : String = "",
  artist : String = "",
  cardFaces : Array[CardFace] = Array.empty,
  legalities: Array[Legality]= Array.empty
)

case class CardFace (
  numberInSet : String = "0",
  name: String = "",
  manaCost: String = "",
  convertedManaCost: String = "0",
  types : String = "",
  cardText : String = "",
  flavorText : String = "",
  power : String = "",
  toughness : String = "",
  loyalty : String = "",
  rulings : Array[Ruling] = Array.empty,
  languages : Array[Language] = Array.empty
)

case class Language(
  multiverseId : String = "",
  name: String = "",
  types : String = "",
  cardText : String = "",
  flavorText : String = "",
  language : String = ""
)

case class Ruling (
  date : String = "",
  rule : String = ""
)

case class Legality (
  format : String = "",
  legality : String = ""
)