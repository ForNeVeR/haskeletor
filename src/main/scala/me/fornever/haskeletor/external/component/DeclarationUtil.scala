/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.external.component

object DeclarationUtil {

  def getDeclarationInfo(declarationLine: String, containsQualifiedIds: Boolean): Option[DeclarationInfo] = {
    val declaration = StringUtil.removeCommentsAndWhiteSpaces(declarationLine)
    val allTokens = declaration.split("""\s+""")
    (if (allTokens.isEmpty || allTokens(0) == "--") {
      None
    } else if (Seq("class", "instance").contains(allTokens(0))) {
      declaration.split("""where|=\s|\s\.\.\.""").headOption.flatMap { d =>
        val tokens = d.trim.split("=>")
        val size = tokens.size
        if (size == 1) {
          Option(tokens(0).split("""\s+""")(1))
        } else if (size > 1) {
          Option(tokens.last.trim.split("""\s+""")(0))
        } else {
          None
        }
      }
    } else if (allTokens(0) == "type" && allTokens(1) == "role") {
      Option(allTokens(2))
    } else if (Seq("data", "type", "newtype").contains(allTokens(0).trim)) {
      Option(allTokens(1))
    } else {
      val tokens = declaration.split("::")
      if (tokens.size > 1) {
        val name = tokens(0).trim
        Option(name)
      } else {
        None
      }
    }).map(name => {
      val operator = StringUtil.isWithinParens(name)
      val id = if (operator) {
        StringUtil.removeOuterParens(name)
      } else {
        name
      }
      if (containsQualifiedIds) {
        DeclarationInfo(StringUtil.removePackageModuleQualifier(id), Some(id), StringUtil.removePackageModuleQualifier(declaration), operator)
      } else {
        DeclarationInfo(id, None, declaration, operator)
      }
    })
  }

  case class DeclarationInfo(id: String, qualifiedId: Option[String], declarationLine: String, operator: Boolean)

}

