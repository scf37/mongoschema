package me.scf37.ms.schema

import org.bson.BsonArray
import org.bson.BsonBinary
import org.bson.BsonBoolean
import org.bson.BsonDateTime
import org.bson.BsonDbPointer
import org.bson.BsonDocument
import org.bson.BsonDouble
import org.bson.BsonInt32
import org.bson.BsonInt64
import org.bson.BsonJavaScript
import org.bson.BsonJavaScriptWithScope
import org.bson.BsonMaxKey
import org.bson.BsonMinKey
import org.bson.BsonNull
import org.bson.BsonObjectId
import org.bson.BsonRegularExpression
import org.bson.BsonString
import org.bson.BsonSymbol
import org.bson.BsonTimestamp
import org.bson.BsonUndefined
import org.bson.BsonValue

import scala.collection.immutable.ListMap

/**
  * Type hierarchy for BSON types
  */
sealed trait Type {}

sealed trait PrimitiveType extends Type {}

case class CompositeType(alternatives: Seq[Type]) extends Type

case object NullType extends Type
case object UndefinedType extends Type
case object OptionalType extends Type

case class ObjectType(fields: Map[String, Type]) extends Type

case class ArrayType(
  elementType: Type,
  maxLength: Int
) extends Type

case class BinaryType(maxLength: Int) extends PrimitiveType

case object BooleanType extends PrimitiveType

case object DateTimeType extends PrimitiveType

case object DbPointerType extends PrimitiveType

case object JavaScriptType extends PrimitiveType

case object JavaScriptWithScopeType extends PrimitiveType

case object DoubleType extends PrimitiveType

case object Int32Type extends PrimitiveType

case object Int64Type extends PrimitiveType

case object ObjectIdType extends PrimitiveType

case object RegularExpressionType extends PrimitiveType

case class StringType(
  maxLength: Int,
  enumValues: Set[String],
  count: Int
) extends PrimitiveType

case object SymbolType extends PrimitiveType

case object TimestampType extends PrimitiveType
//if we know nothing of this type. e.g. element type of empty array
case object UnknownType extends Type

object Type {
  import scala.collection.JavaConversions._

  def parse(v: BsonValue): Type = v match {
    case v: BsonArray =>
      ArrayType(v.getValues.map(v => parse(v)).foldLeft[Type](UnknownType)(merge), v.size())
    case v: BsonBinary =>
      BinaryType(v.getData.length)
    case v: BsonBoolean =>
      BooleanType
    case v: BsonDateTime =>
      DateTimeType
    case v: BsonDbPointer =>
      DbPointerType
    case v: BsonDocument =>
      val s = v.keySet().toSeq.map(k => k -> parse(v.get(k)))
      ObjectType(ListMap(s : _*))
    case v: BsonJavaScript =>
      JavaScriptType
    case v: BsonJavaScriptWithScope =>
      JavaScriptWithScopeType
    case v: BsonMaxKey => ???
    case v: BsonMinKey => ???
    case v: BsonNull =>
      NullType
    case v: BsonDouble =>
      DoubleType
    case v: BsonInt32 =>
      Int32Type
    case v: BsonInt64 =>
      Int64Type
    case v: BsonObjectId =>
      ObjectIdType
    case v: BsonRegularExpression =>
      RegularExpressionType
    case v: BsonString =>
      StringType(v.getValue.length, Set(v.getValue), 1)
    case v: BsonSymbol =>
      SymbolType
    case v: BsonTimestamp =>
      TimestampType
    case v: BsonUndefined =>
      UndefinedType
  }


  def merge(t1: Type, t2: Type): Type = {
    def normalizeAlternatives(a: Seq[Type]): Seq[Type] = {
      val r = a.groupBy(_.getClass).mapValues(_.reduce(merge)).values.toSeq

      if (r.exists(_ != UnknownType))
        r.filter(_ != UnknownType)
      else r
    }

    if (t1 == t2) return t1

    if (t1.getClass != t2.getClass) {

      (t1, t2) match {
        case (t1: CompositeType, t2) =>
          return CompositeType(normalizeAlternatives(t1.alternatives :+ t2))
        case (t1, t2: CompositeType) =>
          return CompositeType(normalizeAlternatives(t2.alternatives :+ t1))
        case (t1, t2) =>
          return CompositeType(Seq(t1, t2))
      }

    }

    t1 match {
      case t1: CompositeType =>
        val all = t1.alternatives ++ t2.asInstanceOf[CompositeType].alternatives
        CompositeType(normalizeAlternatives(all))
      case t1: BinaryType =>
        BinaryType(Math.max(t1.maxLength, t2.asInstanceOf[BinaryType].maxLength))
      case t1: StringType =>
        //do not let enumValues to grow over 10000 elements to save memory
        val enumValues = if (t1.enumValues.size > 10000) {
          t1.enumValues
        } else {
          t1.enumValues ++ t2.asInstanceOf[StringType].enumValues
        }.filter(_.length < 256) //enum value cannot be larger than 256 char - isnt that obvious?

        StringType(Math.max(t1.maxLength, t2.asInstanceOf[StringType].maxLength), enumValues,
          t1.count + t2.asInstanceOf[StringType].count)

      case t1: ObjectType =>
        val otherFields = t2.asInstanceOf[ObjectType].fields
        def keys(m: Map[String, _]) = m.iterator.map(_._1).toIterable.toList

        val r = (keys(t1.fields) ++ keys(otherFields)).toSeq.distinct.map { f =>
          (t1.fields.get(f), otherFields.get(f)) match {
            case (Some(t1), Some(t2)) => f -> Type.merge(t1, t2)
            case (Some(t1), None) => f -> Type.merge(t1, OptionalType)
            case (None, Some(t2)) => f -> Type.merge(t2, OptionalType)
            case (None, None) => ??? //should never happen
          }
        }
        ObjectType(ListMap(r.toSeq: _*))

      case t1: ArrayType =>
        ArrayType(merge(t1.elementType, t2.asInstanceOf[ArrayType].elementType),
          Math.max(t1.maxLength, t2.asInstanceOf[ArrayType].maxLength))

      case UnknownType => UnknownType
      case BooleanType => BooleanType
      case DateTimeType => DateTimeType
      case DbPointerType => DbPointerType
      case DoubleType => DoubleType
      case Int32Type => Int32Type
      case Int64Type => Int64Type
      case JavaScriptType => JavaScriptType
      case JavaScriptWithScopeType => JavaScriptWithScopeType
      case ObjectIdType => ObjectIdType
      case RegularExpressionType => RegularExpressionType
      case SymbolType => SymbolType
      case TimestampType => TimestampType
      case NullType => NullType
      case OptionalType => OptionalType
      case UndefinedType => UndefinedType
    }
  }

  def print(t: Type, enumMaxCount: Int = 0, enumMaxLength: Int = 64, level: Int = 1): String = t match {
    case t1: CompositeType =>
      var prefix = ""

      if (t1.alternatives.contains(NullType)) {
        prefix += "Nullable "
      }
      if (t1.alternatives.contains(OptionalType)) {
        prefix += "Optional "
      }
      if (t1.alternatives.contains(UndefinedType)) {
        prefix += "CanBeUndefined "
      }

      val alt = t1.alternatives.filter(e =>
        e != NullType && e != OptionalType && e != UndefinedType)

      if (alt.length == 1) {
        prefix + print(alt.head, enumMaxCount, enumMaxLength, level)
      } else {
        prefix + "Composite\n" +
          alt.map(t => "  " * level + print(t, enumMaxCount, enumMaxLength, level + 1))
            .mkString("\n" + "  " * level + "OR\n")
      }
    case t1: ObjectType => "Object\n" + t1.fields.map{ case (k, v) =>
      ("  " * level) + k + ": " + print(v, enumMaxCount, enumMaxLength, level + 1)
    }.mkString("\n")

    case t1: ArrayType => s"Array(${t1.maxLength}) of " + print(t1.elementType, enumMaxCount, enumMaxLength, level)

    case t1: BinaryType => s"Binary(${t1.maxLength})"
    case t1: StringType =>
      if (
        t1.enumValues.nonEmpty
          && t1.enumValues.size <= enumMaxCount
          && t1.enumValues.size < t1.count
          && t1.enumValues.forall(_.length <= enumMaxLength)) {
        s"String(${t1.maxLength}) of Enum (${t1.enumValues.map("'" + _ + "'").mkString(", ")})"
      } else
        s"String(${t1.maxLength})"
    case UnknownType => "Unknown"
    case BooleanType => "Boolean"
    case DateTimeType => "DateTime"
    case DbPointerType => "DbPointer"
    case DoubleType => "Double"
    case Int32Type => "Int32"
    case Int64Type => "Int64"
    case JavaScriptType => "JavaScript"
    case JavaScriptWithScopeType => "JavaScriptWithScope"
    case ObjectIdType => "ObjectId"
    case RegularExpressionType => "RegularExpression"
    case SymbolType => "Symbol"
    case TimestampType => "Timestamp"
    case NullType => "Null"
    case OptionalType => "Optional"
    case UndefinedType => "Undefined"
  }


}


