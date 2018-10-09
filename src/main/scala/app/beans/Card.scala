package app.beans

case class Card (
  multiverseId : String = "",
  name : String = "",
  setName : String = "",
  rarity : String = "",
  artist : String = "",
  cardFaces : Array[CardFace] = Array.empty
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
  rulings : Array[Ruling] = Array.empty
)

case class Ruling (
  date : String,
  rule : String
)