/*
 * SPDX-FileCopyrightText: 2014-2022 Rik van der Kleij
 * SPDX-FileCopyrightText: 2014-2022 intellij-haskell contributors <https://github.com/rikvdkleij/intellij-haskell>
 * SPDX-FileCopyrightText: 2026 haskeletor contributors <https://github.com/ForNeVeR/haskeletor>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package me.fornever.haskeletor.util

import com.intellij.openapi.util.{Computable, Condition}

import java.util.concurrent.Callable
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

object ScalaUtil {

  implicit class RichBoolean(val b: Boolean) extends AnyVal {
    final def option[A](a: => A): Option[A] = if (b) Option(a) else None

    final def optionNot[A](a: => A): Option[A] = if (b) None else Option(a)
  }

  def runnable(f: => Unit): Runnable = {
    () => f
  }

  def computable[A](f: => A): Computable[A] = {
    () => f
  }

  def callable[A](f: => A): Callable[A] = {
    () => f
  }

  def condition[A](f: A => Boolean): Condition[A] = {
    (t: A) => f(t)
  }

  def maxsBy[A, B](xs: Iterable[A])(f: A => B)(implicit cmp: Ordering[B]): Iterable[A] = {
    val maxElems = mutable.Map[A, B]()

    for (elem <- xs) {
      val fx = f(elem)
      val removeKeys = maxElems.filter({ case (_, v) => cmp.gt(fx, v) }).keys
      removeKeys.map(maxElems.remove)

      maxElems.put(elem, fx)
    }
    maxElems.keys
  }

  def linesToMap(lines: Seq[String]): Map[String, String] = {
    val linePerKey = lines.foldLeft(ListBuffer[String]()) { case (xs, s) =>
      if (s.startsWith("  ")) {
        xs.update(xs.length - 1, xs.last + s)
        xs
      } else xs.+=(s)
    }

    linePerKey.flatMap(x => {
      val keyValuePair = x.split(": ", 2)
      if (keyValuePair.size == 2) {
        Some(keyValuePair(0), keyValuePair(1).trim)
      } else {
        None
      }
    }).toMap
  }
}
