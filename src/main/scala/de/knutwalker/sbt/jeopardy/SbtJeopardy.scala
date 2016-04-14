/*
 * Copyright 2016 Paul Horn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.knutwalker.sbt.jeopardy

import sbt.Keys.compile
import sbt._

object SbtJeopardy extends AutoPlugin {
  import JeopardyKeys._

  override def trigger = allRequirements
  override def requires = plugins.JvmPlugin

  override lazy val projectSettings = Seq(
    jeopardyStartEndlessLoop := Jeopardy.Theme.start(),
    jeopardyPlay := Jeopardy.Theme.startOnce(),
    jeopardyStop := Jeopardy.Theme.forceStop(),
    (compile in Compile) <<= playOnTask(compile in Compile),
    (compile in Test) <<= playOnTask(compile in Test)
  )

  private def playOnTask[T](t: TaskKey[T]): Def.Initialize[Task[T]] =
    t.dependsOn(jeopardyStartEndlessLoop).andFinally(Jeopardy.Theme.stop())
}
