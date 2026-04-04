/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.psi.impl

import com.intellij.navigation.ItemPresentation
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry
import com.intellij.psi.{PsiElement, PsiReference}
import com.intellij.util.ArrayUtil
import me.fornever.haskeletor.HaskellFileType
import me.fornever.haskeletor.icons.HaskellIcons
import me.fornever.haskeletor.psi._
import me.fornever.haskeletor.util.StringUtil

import javax.swing._
import scala.jdk.CollectionConverters._

object HaskellPsiImplUtil {

  def getName(qVarCon: HaskellQVarCon): String = {
    qVarCon.getText
  }

  def getIdentifierElement(qVarCon: HaskellQVarCon): HaskellNamedElement = {
    Option(qVarCon.getVarid).orElse(Option(qVarCon.getQCon).map(_.getConid)).orElse(Option(qVarCon.getConsym)).orElse(Option(qVarCon.getVarsym)).
      getOrElse(throw new IllegalStateException(s"Identifier for ${qVarCon.getText} should exist"))
  }

  def getName(varCon: HaskellVarCon): String = {
    varCon.getText
  }

  def getIdentifierElement(varCon: HaskellVarCon): HaskellNamedElement = {
    Option(varCon.getConid).
      orElse(Option(varCon.getConsym)).
      orElse(Option(varCon.getVarid)).
      orElse(Option(varCon.getVarsym)).
      getOrElse(throw new IllegalStateException(s"Identifier for ${varCon.getText} should exist"))
  }

  def getName(qName: HaskellQName): String = {
    Option(qName.getVarCon).map(getName).orElse(Option(qName.getQVarCon).map(getName)).getOrElse(qName.getText)
  }

  def getIdentifierElement(qName: HaskellQName): HaskellNamedElement = {
    Option(qName.getVarCon).map(getIdentifierElement).
      orElse(Option(qName.getQVarCon).map(getIdentifierElement)).
      getOrElse(throw new IllegalStateException(s"Identifier for ${qName.getText} should exist"))
  }

  def getQualifierName(qName: HaskellQName): Option[String] = {
    Option(qName.getQVarCon).flatMap(qvc =>
      Option(qvc.getQualifier).map(getName(_: HaskellQualifierElement)).
        orElse(Option(qvc.getQCon).flatMap(qc => Option(qc.getQConQualifier1).orElse(Option(qc.getQConQualifier2)).orElse(Option(qc.getQConQualifier3)).map(_.getText))))
  }

  def getName(modid: HaskellModid): String = {
    modid.getText
  }

  def setName(modid: HaskellModid, newName: String): PsiElement = {
    val newModid = HaskellElementFactory.createModid(modid.getProject, newName)
    newModid.foreach(modid.replace)
    modid
  }

  def getName(varid: HaskellVarid): String = {
    varid.getText
  }

  def setName(varid: HaskellVarid, newName: String): PsiElement = {
    val newVarid = HaskellElementFactory.createVarid(varid.getProject, newName)
    newVarid.foreach(varid.replace)
    varid
  }

  def getName(varsym: HaskellVarsym): String = {
    varsym.getText
  }

  def setName(varsym: HaskellVarsym, newName: String): PsiElement = {
    val newVarsym = HaskellElementFactory.createVarsym(varsym.getProject, newName)
    newVarsym.foreach(varsym.replace)
    varsym
  }

  def getName(conid: HaskellConid): String = {
    conid.getText
  }

  def setName(conid: HaskellConid, newName: String): PsiElement = {
    val newConid = HaskellElementFactory.createConid(conid.getProject, newName)
    newConid.foreach(conid.replace)
    conid
  }

  def getName(consym: HaskellConsym): String = {
    consym.getText
  }

  def setName(consym: HaskellConsym, newName: String): PsiElement = {
    val newConsym = HaskellElementFactory.createConsym(consym.getProject, newName)
    newConsym.foreach(consym.replace)
    consym
  }

  def getName(qualifier: HaskellQualifier): String = {
    qualifier.getText
  }

  def setName(qualifier: HaskellQualifier, newName: String): PsiElement = {
    val newQualifier = HaskellElementFactory.createQualifier(qualifier.getProject, removeFileExtension(newName))
    newQualifier.foreach(qualifier.replace)
    qualifier
  }

  def getName(qConQualifier: HaskellQualifierElement): String = {
    qConQualifier.getText
  }

  def setName(qualifier: HaskellQualifierElement, newName: String): PsiElement = {
    val newQualifier = HaskellElementFactory.createQConQualifier(qualifier.getProject, newName)
    newQualifier.foreach(qualifier.replace)
    qualifier
  }

  def getReference(element: PsiElement): PsiReference = {
    ArrayUtil.getFirstElement(ReferenceProvidersRegistry.getReferencesFromProviders(element))
  }

  private abstract class HaskellItemPresentation(haskellElement: PsiElement) extends ItemPresentation {

    override def getLocationString: String = {
      val psiFile = haskellElement.getContainingFile.getOriginalFile
      HaskellPsiUtil.findModuleDeclaration(psiFile).flatMap(md => getModuleName(md)).getOrElse("Unknown module")
    }

    override def getIcon(unused: Boolean): Icon = {
      findIcon(haskellElement)
    }

    protected def findIcon(element: PsiElement): Icon = {
      import me.fornever.haskeletor.icons.HaskellIcons._
      element match {
        case _: HaskellTypeDeclaration => Type
        case _: HaskellDataDeclaration => Data
        case _: HaskellNewtypeDeclaration => NewType
        case _: HaskellClassDeclaration => Class
        case _: HaskellInstanceDeclaration => Instance
        case _: HaskellDefaultDeclaration => Default
        case _: HaskellTypeSignature => HaskellSmallBlueLogo
        case _: HaskellForeignDeclaration => Foreign
        case _: HaskellTypeFamilyDeclaration => TypeFamily
        case _: HaskellTypeInstanceDeclaration => TypeInstance
        case _: HaskellModuleDeclaration => Module
        case _ => HaskellSmallBlueLogo
      }
    }
  }

  def getPresentation(namedElement: HaskellNamedElement): ItemPresentation = {

    new HaskellItemPresentation(namedElement) {

      override def getPresentableText: String = {
        namedElement.getName
      }

      override def getIcon(unused: Boolean): Icon = {
        HaskellIcons.HaskellSmallBlueLogo
      }
    }
  }

  def getPresentation(declarationElement: HaskellDeclarationElement): ItemPresentation = {

    new HaskellItemPresentation(declarationElement) {
      def getPresentableText: String = {
        getDeclarationText(declarationElement)
      }
    }
  }

  def getItemPresentableText(element: PsiElement): String = {
    HaskellPsiUtil.findNamedElement(element) match {
      case Some(namedElement) =>
        HaskellPsiUtil.findHighestDeclarationElement(element) match {
          case Some(de) => getDeclarationText(de)
          case _ => HaskellPsiUtil.findExpression(namedElement).map(_.getText).getOrElse(namedElement.getName)
        }
      case _ => element.getText
    }
  }

  private def getDeclarationText(declarationElement: HaskellDeclarationElement): String = {
    declarationElement match {
      case md: HaskellModuleDeclaration => s"module  ${getName(md.getModid)}"
      case de => StringUtil.sanitizeDeclaration(de.getText)
    }
  }

  def getName(declarationElement: HaskellDeclarationElement): String = {
    declarationElement.getPresentation.getPresentableText
  }

  def getIdentifierElements(typeSignature: HaskellTypeSignature): Seq[HaskellNamedElement] = {
    typeSignature.getQNamesList.asScala.flatMap(_.getQNameList.asScala).map(getIdentifierElement).toSeq
  }

  def getIdentifierElements(dataDeclaration: HaskellDataDeclaration): Seq[HaskellNamedElement] = {
    val constrs = dataDeclaration.getConstrList.asScala
    getIdentifierElements(dataDeclaration.getSimpletype) ++
      dataDeclaration.getTypeSignatureList.asScala.flatMap(getIdentifierElements) ++
      constrs.flatMap(constr => Option(constr.getConstr1)).flatMap(c => Option(c.getQName).map(getIdentifierElement) ++
        c.getFielddeclList.asScala.flatMap(_.getQNames.getQNameList.asScala.headOption.map(getIdentifierElement))) ++
      constrs.flatMap(constr => Option(constr.getConstr3)).flatMap(_.getTtypeList.asScala.headOption.flatMap(_.getQNameList.asScala.headOption.map(getIdentifierElement))) ++
      constrs.flatMap(constr => Option(constr.getConstr2)).flatMap(c => c.getQNameList.asScala.map(getIdentifierElement).find(e => e.isInstanceOf[HaskellConsym] || e.isInstanceOf[HaskellConid]).toSeq ++
        c.getTtypeList.asScala.headOption.flatMap(_.getQNameList.asScala.headOption.map(getIdentifierElement)))
  }

  def getIdentifierElements(typeDeclaration: HaskellTypeDeclaration): Seq[HaskellNamedElement] = {
    getIdentifierElements(typeDeclaration.getSimpletype)
  }

  def getIdentifierElements(newtypeDeclaration: HaskellNewtypeDeclaration): Seq[HaskellNamedElement] = {
    val fielddecl = Option(newtypeDeclaration.getNewconstr.getNewconstrFielddecl)
    getIdentifierElements(newtypeDeclaration.getSimpletype) ++
      fielddecl.map(_.getQNameList.asScala.map(getIdentifierElement)).getOrElse(Seq()) ++
      newtypeDeclaration.getNewconstr.getQNameList.asScala.headOption.map(getIdentifierElement).toSeq
  }

  def getIdentifierElements(classDeclaration: HaskellClassDeclaration): Seq[HaskellNamedElement] = {
    val cdecls = Option(classDeclaration.getCdecls).map(_.getCdeclList.asScala).getOrElse(Seq())
    classDeclaration.getQNameList.asScala.headOption.map(getIdentifierElement).toSeq ++
      cdecls.flatMap(cd => Option(cd.getTypeSignature).map(getIdentifierElements).getOrElse(Seq())) ++
      cdecls.flatMap(cd => Option(cd.getCdeclDataDeclaration).toSeq.flatMap(_.getQNameList.asScala.map(getIdentifierElement)))
  }

  def getIdentifierElements(instanceDeclaration: HaskellInstanceDeclaration): Seq[HaskellNamedElement] = {
    Option(instanceDeclaration.getQName).map(getIdentifierElement).toSeq ++
      Option(instanceDeclaration.getInst).map(_.getInstvarList.asScala.
        flatMap(v => Option(v.getQName).map(getIdentifierElement).orElse(Option(v.getTtype).flatMap(_.getQNameList.asScala.headOption.map(getIdentifierElement))))).getOrElse(Seq()) ++
      Option(instanceDeclaration.getInst).map(_.getGtyconList.asScala.flatMap(c => Option(c.getQName).map(getIdentifierElement))).getOrElse(Seq())
  }

  def getIdentifierElements(typeFamilyDeclaration: HaskellTypeFamilyDeclaration): Seq[HaskellNamedElement] = {
    val familyType = typeFamilyDeclaration.getTypeFamilyType
    familyType.getQNameList.asScala.map(getIdentifierElement).toSeq ++
      familyType.getQNamesList.asScala.flatMap(_.getQNameList.asScala.map(getIdentifierElement))
  }

  def getIdentifierElements(derivingDeclaration: HaskellDerivingDeclaration): Seq[HaskellNamedElement] = {
    Seq(getIdentifierElement(derivingDeclaration.getQName))
  }

  def getIdentifierElements(typeInstanceDeclaration: HaskellTypeInstanceDeclaration): Seq[HaskellNamedElement] = {
    Seq()
  }

  def getIdentifierElements(simpleType: HaskellSimpletype): Seq[HaskellNamedElement] = {
    simpleType.getQNameList.asScala.map(getIdentifierElement).toSeq ++ {
      Option(simpleType.getTtype) match {
        case Some(t) => t.getQNameList.asScala.map(getIdentifierElement)
        case None => simpleType.getQNameList.asScala.map(getIdentifierElement)
      }
    }.filter(e => e.isInstanceOf[HaskellConid] || e.isInstanceOf[HaskellConsym])
  }

  def getIdentifierElements(defaultDeclaration: HaskellDefaultDeclaration): Seq[HaskellNamedElement] = {
    Seq()
  }

  def getIdentifierElements(foreignDeclaration: HaskellForeignDeclaration): Seq[HaskellNamedElement] = {
    Seq()
  }

  def getIdentifierElements(moduleDeclaration: HaskellModuleDeclaration): Seq[HaskellNamedElement] = {
    Seq(moduleDeclaration.getModid)
  }

  def getModuleName(importDeclaration: HaskellImportDeclaration): Option[String] = {
    Option(importDeclaration.getModid).map(getName)
  }

  def getModuleName(declarationElement: HaskellDeclarationElement): Option[String] = {
    Option(declarationElement.getPresentation).map(_.getLocationString)
  }

  def getModuleName(moduleDeclaration: HaskellModuleDeclaration): Option[String] = {
    Some(getName(moduleDeclaration.getModid))
  }

  def getDataTypeConstructor(dataConstructorDeclaration: HaskellDataConstructorDeclarationElement): HaskellNamedElement = {
    dataConstructorDeclaration.getIdentifierElements.head
  }

  def removeFileExtension(name: String): String = {
    val fileExtension = "." + HaskellFileType.INSTANCE.getDefaultExtension
    if (name.endsWith(fileExtension)) {
      name.replaceFirst(fileExtension, "")
    } else {
      name
    }
  }
}
