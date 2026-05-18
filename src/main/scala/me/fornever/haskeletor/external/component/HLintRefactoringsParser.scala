/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.external.component

import fastparse.Parsed.{Failure, Success}
import fastparse.SingleLineWhitespace._
import fastparse._

object HLintRefactoringsParser {

  case class SrcSpan(startLine: Int, startCol: Int, endLine: Int, endCol: Int)

  type Subts = Seq[(String, SrcSpan)]

  sealed trait Refactoring
  case class Delete(rType: RType, pos: SrcSpan) extends Refactoring
  case class Replace(rType: RType, pos: SrcSpan, subts: Subts, orig: String, deletes: Seq[Delete]) extends Refactoring
  case class ModifyComment(pos: SrcSpan, newComment: String) extends Refactoring
  case class InsertComment(pos: SrcSpan, insertComment: String) extends Refactoring
  case class RemoveAsKeyword(pos: SrcSpan) extends Refactoring

  sealed trait RType
  case object Expr extends RType
  case object Decl extends RType
  case object Pattern extends RType
  case object Stmt extends RType
  case object Type extends RType
  case object ModuleName extends RType
  case object Bind extends RType
  case object Match extends RType
  case object Import extends RType

  def parseRefactoring(hlintOutput: String): Either[String, Refactoring] = parse(hlintOutput, refactoringParser(_), verboseFailures = true) match {
    case Success(value, _) => Right(value)
    case Failure(label, i, _) => Left(s"Could not parse HLint output | HLintOutput: $hlintOutput | Label: $label | Index: $i")
  }

  @annotation.nowarn
  private def refactoringParser[T: P]: P[Refactoring] = P("[" ~ (deleteParser | replaceParser | modifyCommentParser | insertCommentParser | removeAsKeywordParser) ~ "]")

  private[component] def parseSubts(hlintOutput: String): Parsed[Subts] = parse(hlintOutput, subtsParser(_), verboseFailures = true)

  private[component] def parsePos(hlintOutput: String): Parsed[SrcSpan] = parse(hlintOutput, posParser(_), verboseFailures = true)

  @annotation.nowarn
  private def deleteParser[T: P]: P[Delete] = P("Delete" ~ keyRtypePosParser(Pass)).map({ case (x, y, _) => Delete(x, y) })

  private def replaceParser[T: P]: P[Replace] = P("Replace" ~ keyRtypePosParser(commaParser ~ "subts =" ~ subtsParser ~ commaParser ~ keyValueParser("orig", string)) ~ (commaParser ~ deleteParser).rep)
    .map({ case (x, y, (w, z), q) => Replace(x, y, w, z, q) })

  private def modifyCommentParser[T: P]: P[ModifyComment] = P("ModifyComment" ~ "{" ~ posParser ~ commaParser ~ keyValueParser("newComment", string) ~ "}").
    map({ case (x, y) => ModifyComment(x, y) })

  private def insertCommentParser[T: P]: P[InsertComment] = P("InsertComment" ~ "{" ~ posParser ~ commaParser ~ keyValueParser("newComment", string) ~ "}").
    map({ case (x, y) => InsertComment(x, y) })

  private def removeAsKeywordParser[T: P]: P[RemoveAsKeyword] = P("RemoveAsKeyword" ~ "{" ~ posParser ~ "}").
    map({ case (x) => RemoveAsKeyword(x) })

  private def keyRtypePosParser[T: P, A](rest: => P[A]) = "{" ~ keyRtypeParser ~ commaParser ~ posParser ~ rest ~ "}"

  private def subtsParser[T: P] = P("[" ~ (subtParser ~ commaParser.?).rep ~ "]")

  private def subtParser[T: P] = P("(" ~ string ~ commaParser ~ srcSpanParser ~ ")")

  private def rtypeParser[T: P]: P[RType] = {
    P(IgnoreCase("Expr")).map(_ => Expr) |
      P(IgnoreCase("Decl")).map(_ => Decl) |
      P(IgnoreCase("Type")).map(_ => Type) |
      P(IgnoreCase("Pattern")).map(_ => Pattern) |
      P(IgnoreCase("Stmt")).map(_ => Stmt) |
      P(IgnoreCase("ModuleName")).map(_ => ModuleName) |
      P(IgnoreCase("Bind")).map(_ => Bind) |
      P(IgnoreCase("Match")).map(_ => Match) |
      P(IgnoreCase("Import")).map(_ => Import)
  }

  private def stringChars(c: Char) = c != '\"' && c != '\\'

  private def strChars[T: P] = P(CharsWhile(stringChars))

  private def hexDigit[T: P] = P(CharIn("0-9a-fA-F"))

  private def unicodeEscape[T: P] = P("u" ~ hexDigit ~ hexDigit ~ hexDigit ~ hexDigit)

  private def escape[T: P] = P("\\" ~ (CharIn("\"/\\\\bfnrt") | unicodeEscape))

  private def string[T: P] = P("\"" ~/ (strChars | escape).rep.! ~ "\"")

  @annotation.nowarn
  private def digits[T: P] = P(CharsWhileIn("0-9"))

  @annotation.nowarn
  private def keyValueParser[T: P, A](keyName: String, valueParser: => P[A]) = s"$keyName" ~ "=" ~ valueParser

  private def keyDigitsParser[p: P](keyName: String) = keyValueParser[p, String](keyName, digits.!).map(_.toInt)

  private def keyRtypeParser[T: P] = keyValueParser("rtype", rtypeParser)

  private def commaParser[T: P] = ","

  private def posParser[T: P] = "pos" ~ "=" ~ srcSpanParser

  private def srcSpanParser[T: P] = "SrcSpan" ~
    ("{" ~
      keyDigitsParser("startLine") ~ commaParser ~
      keyDigitsParser("startCol") ~ commaParser ~
      keyDigitsParser("endLine") ~ commaParser ~
      keyDigitsParser("endCol") ~
      "}").map { case (sl, sc, el, ec) => SrcSpan(sl, sc, el, ec) }
}
