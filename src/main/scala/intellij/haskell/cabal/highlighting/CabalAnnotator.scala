// SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
// SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
//
// SPDX-License-Identifier: Apache-2.0

package intellij.haskell.cabal.highlighting

import com.intellij.lang.annotation.{AnnotationHolder, Annotator}
import com.intellij.openapi.editor.colors.{EditorColorsManager, TextAttributesKey}
import com.intellij.psi.PsiElement
import intellij.haskell.cabal.lang.psi.CabalTypes._
import intellij.haskell.cabal.lang.psi._
import intellij.haskell.highlighter.HighlightingAnnotator

class CabalAnnotator extends Annotator {

  def annotate(el: PsiElement, h: AnnotationHolder): Unit = {
    el.getNode.getElementType match {
      case _: CabalFieldKeyTokenType => setHighlighting(el, h, CabalSyntaxHighlighter.KEY)
      case _: CabalStanzaKeyTokenType => setHighlighting(el, h, CabalSyntaxHighlighter.CONFIG)
      case _: CabalStanzaArgTokenType => setHighlighting(el, h, CabalSyntaxHighlighter.CONFIG)
      case LBRACE | RBRACE => setHighlighting(el, h, CabalSyntaxHighlighter.COLON)
      case _ => // noop
    }
  }

  private def setHighlighting(element: PsiElement, holder: AnnotationHolder, key: TextAttributesKey): Unit = {
    HighlightingAnnotator.infoAnnotation(holder, element, EditorColorsManager.getInstance.getGlobalScheme.getAttributes(key))
  }
}
