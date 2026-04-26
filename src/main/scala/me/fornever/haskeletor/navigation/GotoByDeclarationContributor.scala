/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.navigation

import com.intellij.lang.Language
import com.intellij.navigation.{ChooseByNameContributor, GotoClassContributor, NavigationItem}
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.stubs.StubIndex
import com.intellij.util.{ArrayUtil, Processor}
import me.fornever.haskeletor.core.HaskellLanguage
import me.fornever.haskeletor.psi.stubs.index.HaskellAllNameIndex
import me.fornever.haskeletor.psi.{HaskellNamedElement, HaskellPsiUtil}
import me.fornever.haskeletor.util.HaskellProjectUtil

import scala.collection.mutable.ListBuffer
import scala.jdk.CollectionConverters._

class GotoByDeclarationContributor extends GotoClassContributor {

  override def getNames(project: Project, includeNonProjectItems: Boolean): Array[String] = {
    GotoHelper.getNames(project, includeNonProjectItems)
  }

  override def getItemsByName(name: String, pattern: String, project: Project, includeNonProjectItems: Boolean): Array[NavigationItem] = {
    val namedElements = GotoHelper.getNamedElements(name, pattern, project, includeNonProjectItems)
    namedElements.flatMap(ne => HaskellPsiUtil.findHighestDeclarationElement(ne)).toArray
  }

  override def getQualifiedName(item: NavigationItem): String = {
    Option(item.getPresentation).map(_.getPresentableText).getOrElse("-")
  }

  override def getQualifiedNameSeparator: String = {
    "."
  }

  override def getElementKind: String = {
    "declaration"
  }

  override def getElementLanguage: Language = {
    HaskellLanguage.Instance
  }
}

class GotoByNameContributor extends ChooseByNameContributor {

  override def getNames(project: Project, includeNonProjectItems: Boolean): Array[String] = {
    GotoHelper.getNames(project, includeNonProjectItems)
  }

  override def getItemsByName(name: String, pattern: String, project: Project, includeNonProjectItems: Boolean): Array[NavigationItem] = {
    val namedElements = GotoHelper.getNamedElements(name, pattern, project, includeNonProjectItems)
    namedElements.toArray
  }
}

private object GotoHelper {

  def getNames(project: Project, includeNonProjectItems: Boolean): Array[String] = {
    if (HaskellProjectUtil.isHaskellProject(project)) {
      ArrayUtil.toStringArray(StubIndex.getInstance.getAllKeys(HaskellAllNameIndex.Key, project))
    } else {
      Array()
    }
  }

  def getNamedElements(name: String, pattern: String, project: Project, includeNonProjectItems: Boolean): Seq[HaskellNamedElement] = {
    val searchScope = HaskellProjectUtil.getSearchScope(project, includeNonProjectItems)
    val result = ListBuffer[String]()
    val re = pattern.toLowerCase.flatMap(c => StringUtil.escapeToRegexp(c.toString) + ".*")
    val processor = new Processor[String]() {
      override def process(ne: String): Boolean = {
        ProgressManager.checkCanceled()
        if (ne.toLowerCase.matches(re)) {
          result.+=(ne)
        }
        true
      }
    }

    StubIndex.getInstance().processAllKeys(HaskellAllNameIndex.Key, processor, searchScope, null)

    result.flatMap(name => StubIndex.getElements(HaskellAllNameIndex.Key, name, project, searchScope, classOf[HaskellNamedElement]).asScala).toSeq
  }
}
