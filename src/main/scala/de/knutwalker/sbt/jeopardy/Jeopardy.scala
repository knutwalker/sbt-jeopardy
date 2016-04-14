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

import com.sun.media.sound.{ JDK13Services, WaveFileReader }

import scala.Option.{ empty ⇒ none }
import scala.collection.JavaConverters._
import scala.util.Try
import java.io.InputStream
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.{ Executors, ScheduledFuture, ThreadFactory, TimeUnit }
import javax.sound.sampled.spi.AudioFileReader
import javax.sound.sampled.{ AudioInputStream, AudioSystem, Clip, FloatControl }

object Jeopardy {
  final type ExHandler = PartialFunction[Throwable, Try[Nothing]]
  final         val Theme     = fromResource("/Jeopardy-theme-song.wav")
  private final val Scheduler = Executors.newSingleThreadScheduledExecutor(SchedulerThreadFactory)

  abstract class Sound {
    def start(): Unit
    def startOnce(): Unit
    def stop(): Unit
    def forceStop(): Unit
  }

  object Sound {
    final val NoSound: Sound = new Sound {
      def start(): Unit = ()
      def startOnce(): Unit = ()
      def stop(): Unit = ()
      def forceStop(): Unit = ()
    }
    class ClipSound(clip: Clip) extends Sound {
      private[this] var executionLevel = 0
      private[this] val starter        = new AtomicReference(none[ScheduledFuture[_]])
      private[this] val stopper        = new AtomicReference(none[ScheduledFuture[_]])
      private[this] val fader          = new AtomicReference(none[ScheduledFuture[_]])
      private[this] val gain           = Try(clip.getControl(FloatControl.Type.MASTER_GAIN)).toOption
        .collect {case x: FloatControl ⇒ new Volume.ClipVolume(x)}
        .getOrElse(Volume.NoVolume)

      private def prepareClip(): Unit = {
        clip.setFramePosition(0)
        gain.max()
        fader.get.foreach(_.cancel(true))
        fader.set(None)
        stopper.get.foreach(_.cancel(true))
        stopper.set(None)
      }

      def start(): Unit = clip.synchronized {
        if (executionLevel == 0 && starter.get().isEmpty) {
          starter.set(Some(Scheduler.schedule(new Runnable {
            def run(): Unit = {
              prepareClip()
              clip.loop(Clip.LOOP_CONTINUOUSLY)
              starter.set(None)
            }
          }, 250, TimeUnit.MILLISECONDS)))
        }
        executionLevel += 1
      }
      def startOnce(): Unit = clip.synchronized {
        if (!clip.isRunning) {
          prepareClip()
          clip.start()
        }
      }
      def stop(): Unit = clip.synchronized {
        executionLevel -= 1
        if (executionLevel <= 0) {
          executionLevel = 0
          if (starter.get().isDefined) {
            starter.get().foreach(_.cancel(true))
            starter.set(None)
            clip.stop()
          } else {
            fader.set(Some(Scheduler.scheduleWithFixedDelay(new Runnable {
              def run(): Unit = gain.stepDown()
            }, 0, 50, TimeUnit.MILLISECONDS)))
            stopper.set(Some(Scheduler.schedule(new Runnable {
              def run(): Unit = {
                fader.get.foreach(_.cancel(false))
                clip.stop()
                fader.set(None)
                stopper.set(None)
              }
            }, 900, TimeUnit.MILLISECONDS)))
          }
        }
      }
      def forceStop(): Unit = clip.synchronized {
        executionLevel = 0
        clip.stop()
      }
    }
  }

  private abstract class Volume {
    def max(): Unit
    def stepDown(): Unit
  }
  private object Volume {
    final val NoVolume: Volume = new Volume {
      def max(): Unit = ()
      def stepDown(): Unit = ()
    }
    class ClipVolume(gain: FloatControl, steps: Int = 16) extends Volume {
      private[this] val current  = gain.getValue
      private[this] val stepSize = (current - gain.getMinimum) / steps max gain.getPrecision
      def max(): Unit = gain.setValue(current)
      def stepDown(): Unit = {
        val newVal = (gain.getValue - stepSize) max gain.getMinimum
        gain.setValue(newVal)
      }
    }
  }

  private def fromResource(location: String): Sound =
    clipFromResource(location).fold(Sound.NoSound)(new Sound.ClipSound(_))

  private def clipFromResource(location: String) = for {
    in ← input(location)
    as ← audioStream(in)
    clip ← mkClip(as)
  } yield clip

  private def input(resource: String) =
    Option(getClass.getResourceAsStream(resource))

  private def audioStream(input: InputStream) = Try {
    val waveReader = JDK13Services
      .getProviders(classOf[AudioFileReader])
      .asScala
      .collectFirst {case wav: WaveFileReader ⇒ wav}
    waveReader.get.getAudioInputStream(input)
  }.toOption

  private def mkClip(as: AudioInputStream) = Try {
    val clip = AudioSystem.getClip
    clip.open(as)
    clip.setLoopPoints(0, -1) // loop everything
    clip
  }.toOption


  private object SchedulerThreadFactory extends ThreadFactory {
    private[this] val group = Option(System.getSecurityManager).fold(Thread.currentThread.getThreadGroup)(_.getThreadGroup)
    def newThread(r: Runnable): Thread = {
      val t = new Thread(group, r, "jeopardy", 0)
      t.setDaemon(true)
      t.setPriority(Thread.NORM_PRIORITY)
      t
    }
  }

}
