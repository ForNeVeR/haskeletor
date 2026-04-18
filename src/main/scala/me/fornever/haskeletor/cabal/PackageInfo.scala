/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.cabal

import com.intellij.openapi.project.Project
import com.intellij.psi.tree.IElementType
import com.intellij.psi.{PsiElement, PsiFileFactory}
import me.fornever.haskeletor.cabal.lang.psi
import me.fornever.haskeletor.cabal.lang.psi._
import me.fornever.haskeletor.cabal.lang.psi.impl.{ExtensionsImpl, MainIsImpl, SourceDirsImpl}
import me.fornever.haskeletor.core.notifications.HaskellNotificationGroup
import me.fornever.haskeletor.psi.HaskellPsiUtil
import me.fornever.haskeletor.util.{ApplicationUtil, HaskellFileUtil}

import java.io.{File, IOException}
import java.nio.charset.StandardCharsets
import scala.io.Source

object PackageInfo {

  def create(project: Project, cabalFile: File): Option[PackageInfo] = {
    val source = try {
      Option(Source.fromFile(cabalFile, StandardCharsets.UTF_8.toString)).map(_.mkString)
    } catch {
      case e: IOException =>
        HaskellNotificationGroup.logErrorBalloonEvent(project, s"Could not read Cabal file ${cabalFile.getName}, error: ${e.getMessage}")
        None
    }

    source.flatMap(src => Option(PsiFileFactory.getInstance(project).createFileFromText(cabalFile.getName, CabalLanguage.Instance, src)) match {
      case Some(cabalPsiFile: CabalFile) => Some(new PackageInfo(cabalPsiFile, cabalFile.getParentFile.getAbsolutePath))
      case _ =>
        HaskellNotificationGroup.logErrorBalloonEvent(project, s"Could not parse Cabal file ${cabalFile.getName}")
        None
    })
  }
}

class PackageInfo(cabalFile: CabalFile, modulePath: String) {

  lazy val packageName: String = ApplicationUtil.runReadAction {
    for {
      pkgName <- HaskellPsiUtil.getChildOfType(cabalFile, classOf[PkgName])
      ff <- HaskellPsiUtil.getChildOfType(pkgName, classOf[Freeform])
    } yield ff.getText
  }.getOrElse(throw new IllegalStateException(s"Can not find package name in Cabal file ${cabalFile.getName}"))

  lazy val packageVersion: String = ApplicationUtil.runReadAction {
    for {
      pkgVersion <- HaskellPsiUtil.getChildOfType(cabalFile, classOf[PkgVersion])
      ff <- HaskellPsiUtil.getChildOfType(pkgVersion, classOf[Freeform])
    } yield ff.getText
  }.getOrElse(throw new IllegalStateException(s"Can not find package version in Cabal file ${cabalFile.getName}"))

  lazy val library: Option[LibraryCabalStanza] = ApplicationUtil.runReadAction {
    cabalFile.getChildren.collectFirst {
      case c: Library => LibraryCabalStanza(c, packageName, modulePath)
    }
  }

  lazy val executables: Iterable[ExecutableCabalStanza] = {
    ApplicationUtil.runReadAction {
      HaskellPsiUtil.streamChildren(cabalFile, classOf[Executable]).map(c => ExecutableCabalStanza(c, packageName, modulePath))
    }
  }

  lazy val testSuites: Iterable[TestSuiteCabalStanza] = {
    ApplicationUtil.runReadAction {
      HaskellPsiUtil.streamChildren(cabalFile, classOf[TestSuite]).map(c => TestSuiteCabalStanza(c, packageName, modulePath))
    }
  }

  lazy val benchmarks: Iterable[BenchmarkCabalStanza] = {
    ApplicationUtil.runReadAction {
      HaskellPsiUtil.streamChildren(cabalFile, classOf[Benchmark]).map(c => BenchmarkCabalStanza(c, packageName, modulePath))
    }
  }

  lazy val cabalStanzas: Iterable[CabalStanza] = {
    ApplicationUtil.runReadAction {
      library.toSeq ++
        cabalFile.getChildren.collect {
          case c: Executable => ExecutableCabalStanza(c, packageName, modulePath)
          case c: TestSuite => TestSuiteCabalStanza(c, packageName, modulePath)
          case c: Benchmark => BenchmarkCabalStanza(c, packageName, modulePath)
        }
    }
  }

  lazy val sourceRoots: Iterable[String] = {
    library.map(_.sourceDirs).getOrElse(Iterable()) ++ executables.flatMap(_.sourceDirs)
  }

  lazy val testSourceRoots: Iterable[String] = {
    (testSuites ++ benchmarks).flatMap(_.sourceDirs)
  }

  lazy val ghcOptions: Set[String] = ApplicationUtil.runReadAction {
    HaskellPsiUtil.streamChildren(cabalFile, classOf[psi.impl.GhcOptionsImpl]).flatMap(_.getValue).toSet
  }
}

sealed trait CabalStanza {

  protected val sectionRootElement: PsiElement
  protected val modulePath: String

  val targetName: String

  val nameElementType: Option[IElementType]

  def sourceDirs: Seq[String]

  protected def findSourceDirs: Seq[String] = ApplicationUtil.runReadAction {
    HaskellPsiUtil.getChildOfType(sectionRootElement, classOf[SourceDirsImpl]).map(_.getValue).getOrElse(Array()).map(p => HaskellFileUtil.makeFilePathAbsolute(p, modulePath)).toSeq
  }

  lazy val buildDepends: Seq[String] = ApplicationUtil.runReadAction {
    HaskellPsiUtil.getChildrenOfType(sectionRootElement, classOf[BuildDepends]).flatMap(_.getPackageNames).toSeq
  }

  lazy val findLanguageExtensions: Set[String] = ApplicationUtil.runReadAction {
    HaskellPsiUtil.getChildOfType(sectionRootElement, classOf[ExtensionsImpl]).map(_.getValue.toSet).getOrElse(Set())
  }

  lazy val isNoImplicitPreludeActive: Boolean = {
    findLanguageExtensions.contains("NoImplicitPrelude")
  }

  protected def findSourceDirsOrElseModuleDir: Seq[String] = {
    val sourceDirs = findSourceDirs
    if (sourceDirs.isEmpty) {
      Seq(modulePath)
    } else {
      sourceDirs
    }
  }

  // Workaround: Noticed that when hpack file is converted to cabal file, the globally defined paths are added to every target/stanza.
  protected def findMainIs: Option[String] = ApplicationUtil.runReadAction {
    HaskellPsiUtil.getChildOfType(sectionRootElement, classOf[MainIsImpl]).flatMap(_.getValue).
      flatMap(p => sourceDirs.find(sd => new File(sd, p).exists()).map(sd => HaskellFileUtil.makeFilePathAbsolute(p, sd)))
  }

  lazy val name: Option[String] = nameElementType.flatMap(net => HaskellPsiUtil.getChildNodes(sectionRootElement, net).headOption).map(_.getText)

}

case class LibraryCabalStanza(sectionRootElement: PsiElement, packageName: String, modulePath: String) extends CabalStanza {
  val nameElementType: Option[IElementType] = Some(CabalTypes.LIBRARY_NAME)

  val targetName: String = s"$packageName:lib"

  lazy val sourceDirs: Seq[String] = findSourceDirsOrElseModuleDir

  lazy val exposedModuleNames: Seq[String] = findExposedModuleNames

  private def findExposedModuleNames: Seq[String] = ApplicationUtil.runReadAction {
    HaskellPsiUtil.getChildOfType(sectionRootElement, classOf[ExposedModules]).map(_.getModuleNames.toSeq).getOrElse(Seq())
  }
}

case class ExecutableCabalStanza(sectionRootElement: PsiElement, packageName: String, modulePath: String) extends CabalStanza {
  lazy val nameElementType: Option[IElementType] = Some(CabalTypes.EXECUTABLE_NAME)

  lazy val targetName: String = name.map(n => s"$packageName:exe:$n").getOrElse(throw new IllegalStateException(s"Executable should have name in package $packageName"))

  lazy val mainIs: Option[String] = findMainIs

  lazy val sourceDirs: Seq[String] = findSourceDirsOrElseModuleDir
}

case class TestSuiteCabalStanza(sectionRootElement: PsiElement, packageName: String, modulePath: String) extends CabalStanza {
  lazy val nameElementType: Option[IElementType] = Some(CabalTypes.TEST_SUITE_NAME)

  lazy val targetName: String = name.map(n => s"$packageName:test:$n").getOrElse(throw new IllegalStateException(s"Test-suite should have name in package $packageName"))

  lazy val mainIs: Option[String] = findMainIs

  lazy val sourceDirs: Seq[String] = findSourceDirs
}

case class BenchmarkCabalStanza(sectionRootElement: PsiElement, packageName: String, modulePath: String) extends CabalStanza {
  lazy val nameElementType: Option[IElementType] = Some(CabalTypes.BENCHMARK_NAME)

  lazy val targetName: String = name.map(n => s"$packageName:bench:$n").getOrElse(throw new IllegalStateException(s"Benchmark should have name in package $packageName"))

  lazy val mainIs: Option[String] = findMainIs

  lazy val sourceDirs: Seq[String] = findSourceDirs
}
