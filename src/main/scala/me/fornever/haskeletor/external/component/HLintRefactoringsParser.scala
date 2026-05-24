/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.external.component

import fastparse.*
import fastparse.Parsed.{Failure, Success}
import fastparse.SingleLineWhitespace.*

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
  private inline def refactoringParser(implicit ctx: P[_]): P[Refactoring] = P("[" ~ (deleteParser | replaceParser | modifyCommentParser | insertCommentParser | removeAsKeywordParser) ~ "]")

  private[component] def parseSubts(hlintOutput: String): Parsed[Subts] = parse(hlintOutput, subtsParser(_), verboseFailures = true)

  private[component] def parsePos(hlintOutput: String): Parsed[SrcSpan] = parse(hlintOutput, posParser(_), verboseFailures = true)

  @annotation.nowarn
  private def deleteParser(implicit ctx: P[_]): P[Delete] = P("Delete" ~ keyRtypePosParser(Pass)).map({ case (x, y, _) => Delete(x, y) })

  private def replaceParser(implicit ctx: P[_]): P[Replace] = P("Replace" ~ keyRtypePosParser(commaParser ~ "subts =" ~ subtsParser ~ commaParser ~ keyValueParser("orig", string)) ~ (commaParser ~ deleteParser).rep)
    .map({ case (x, y, (w, z), q) => Replace(x, y, w, z, q) })

  private def modifyCommentParser(implicit ctx: P[_]): P[ModifyComment] = P("ModifyComment" ~ "{" ~ posParser ~ commaParser ~ keyValueParser("newComment", string) ~ "}").
    map({ case (x, y) => ModifyComment(x, y) })

  private def insertCommentParser(implicit ctx: P[_]): P[InsertComment] = P("InsertComment" ~ "{" ~ posParser ~ commaParser ~ keyValueParser("newComment", string) ~ "}").
    map({ case (x, y) => InsertComment(x, y) })

  private def removeAsKeywordParser(implicit ctx: P[_]): P[RemoveAsKeyword] = P("RemoveAsKeyword" ~ "{" ~ posParser ~ "}").
    map({ case (x) => RemoveAsKeyword(x) })

  private def keyRtypePosParser[A](rest: => P[A])(implicit ctx: P[_]) = "{" ~ keyRtypeParser ~ commaParser ~ posParser ~ rest ~ "}"

  private inline def subtsParser(implicit ctx: P[_]) = P("[" ~ (subtParser ~ commaParser.?).rep ~ "]")

  private def subtParser(implicit ctx: P[_]) = P("(" ~ string ~ commaParser ~ srcSpanParser ~ ")")

  private def rtypeParser(implicit ctx: P[_]): P[RType] = {
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

  private def strChars(implicit ctx: P[_]) = P(CharsWhile(stringChars))

  private def hexDigit(implicit ctx: P[_]) = P(CharIn("0-9a-fA-F"))

  private def unicodeEscape(implicit ctx: P[_]) = P("u" ~ hexDigit ~ hexDigit ~ hexDigit ~ hexDigit)

  private def escape(implicit ctx: P[_]) = P("\\" ~ (CharIn("\"/\\\\bfnrt") | unicodeEscape))

  private def string(implicit ctx: P[_]) = P("\"" ~/ (strChars | escape).rep.! ~ "\"")

  @annotation.nowarn
  private def digits(implicit ctx: P[_]) = P(CharsWhileIn("0-9"))

  @annotation.nowarn
  private def keyValueParser[A](keyName: String, valueParser: => P[A])(implicit ctx: P[_]) = s"$keyName" ~ "=" ~ valueParser

  private def keyDigitsParser(keyName: String)(implicit ctx: P[_]) = keyValueParser[String](keyName, digits.!).map(_.toInt)

  private def keyRtypeParser(implicit ctx: P[_]) = keyValueParser("rtype", rtypeParser)

  private def commaParser(implicit ctx: P[_]) = ","

  private inline def posParser(implicit ctx: P[_]) = "pos" ~ "=" ~ srcSpanParser

  private def srcSpanParser(implicit ctx: P[_]) = "SrcSpan" ~
    ("{" ~
      keyDigitsParser("startLine") ~ commaParser ~
      keyDigitsParser("startCol") ~ commaParser ~
      keyDigitsParser("endLine") ~ commaParser ~
      keyDigitsParser("endCol") ~
      "}").map { case (sl, sc, el, ec) => SrcSpan(sl, sc, el, ec) }
}
