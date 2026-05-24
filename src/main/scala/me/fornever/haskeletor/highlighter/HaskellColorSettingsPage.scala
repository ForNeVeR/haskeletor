/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.highlighter

import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.options.colors.{AttributesDescriptor, ColorDescriptor, ColorSettingsPage}
import me.fornever.haskeletor.highlighter.HaskellSyntaxHighlighter.*
import me.fornever.haskeletor.icons.HaskellIcons
import org.jetbrains.annotations.NotNull

import javax.swing.*
import scala.jdk.CollectionConverters.*

object HaskellColorSettingsPage:
  private final val Attrs = Array[AttributesDescriptor](
    new AttributesDescriptor("Illegal character", Illegal),
    new AttributesDescriptor("Comment", Comment),
    new AttributesDescriptor("Block comment", BlockComment),
    new AttributesDescriptor("String", String),
    new AttributesDescriptor("Number", Number),
    new AttributesDescriptor("Keyword", Keyword),
    new AttributesDescriptor("Parentheses", Parentheses),
    new AttributesDescriptor("Brace", Brace),
    new AttributesDescriptor("Bracket", Bracket),
    new AttributesDescriptor("Variable", Variable),
    new AttributesDescriptor("Function name", FunctionName),
    new AttributesDescriptor("Constructor", Constructor),
    new AttributesDescriptor("Operator", Operator),
    new AttributesDescriptor("Reserved symbol", ReservedSymbol),
    new AttributesDescriptor("Pragma", Pragma),
    new AttributesDescriptor("Pragma content", PragmaContent),
    new AttributesDescriptor("Quasiquote", Quasiquote),
    new AttributesDescriptor("Default", Default))

  private final val AttributesKeyMap = Map[String, TextAttributesKey](
    "keyword" -> Keyword,
    "functionName" -> FunctionName,
    "pragmaContent" -> PragmaContent
  )

class HaskellColorSettingsPage extends ColorSettingsPage:

  @NotNull
  def getDisplayName: String =
    "Haskell"

  def getIcon: Icon =
    HaskellIcons.HaskellLogo

  @NotNull
  def getAttributeDescriptors: Array[AttributesDescriptor] =
    HaskellColorSettingsPage.Attrs

  @NotNull
  def getColorDescriptors: Array[ColorDescriptor] =
    ColorDescriptor.EMPTY_ARRAY

  @NotNull
  def getHighlighter: SyntaxHighlighter =
    new HaskellSyntaxHighlighter

  @NotNull
  def getDemoText: String =
    """{-# LANGUAGE <pragmaContent>CPP</pragmaContent> #-}
      |module ModuleName
      |import <keyword>qualified</keyword> ImportModuleName
      |"string literal"
      |'c'
      |<functionName>intPair</functionName> :: (Int, Int)
      |intPair = (456,434)
      |-- line comment
      |{- nested
      |comment -}
      |data Bool = True | False
      |let l1 = [1, 2]
      |let l2 = 1 : []
      |let two = 1 + 1
      |let f = \_ + 1
      |[t|select * from foo|]""".stripMargin.replaceAll("\r\n", "\n")

  @NotNull
  def getAdditionalHighlightingTagToDescriptorMap: java.util.Map[String, TextAttributesKey] =
    HaskellColorSettingsPage.AttributesKeyMap.asJava
