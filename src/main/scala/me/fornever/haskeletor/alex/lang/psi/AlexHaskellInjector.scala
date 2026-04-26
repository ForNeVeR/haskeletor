/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.alex.lang.psi

import com.intellij.openapi.util.TextRange
import com.intellij.psi.{InjectedLanguagePlaces, LanguageInjector, PsiLanguageInjectionHost}
import me.fornever.haskeletor.core.HaskellLanguage

/**
  * @author ice1000
  */
class AlexHaskellInjector extends LanguageInjector {
  override def getLanguagesToInject(host: PsiLanguageInjectionHost, places: InjectedLanguagePlaces): Unit = {
    host match {
      case _: AlexTopModuleSection =>
        places.addPlace(HaskellLanguage.Instance, new TextRange(2, host.getTextLength - 1), null, null)
      case _: AlexUserCodeSection =>
        places.addPlace(HaskellLanguage.Instance, new TextRange(2, host.getTextLength - 1), null, null)
      case _ =>
    }
  }
}
