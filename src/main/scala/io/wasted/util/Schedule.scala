package io.wasted.util

import java.util.concurrent.{ ConcurrentHashMap, TimeUnit }

import com.twitter.util.JavaTimer
import io.netty.util.{ Timeout, TimerTask }

import scala.concurrent.duration.Duration

class WheelTimer(timerMillis: Int, wheelSize: Int)
  extends io.netty.util.HashedWheelTimer(
    timerMillis.toLong,
    TimeUnit.MILLISECONDS,
    wheelSize) {
  lazy val twitter = new JavaTimer()
}

object WheelTimer extends WheelTimer(100, 512)

/**
 * Wasted Scheduler based on Netty's HashedWheelTimer
 */
object Schedule extends Logger {
  private val repeatTimers = new ConcurrentHashMap[Long, Timeout]()

  /**
   * Creates a TimerTask from a Function
   *
   * @param func Function to be tasked
   */
  private def task(func: () => Any): TimerTask = new TimerTask() {
    def run(timeout: Timeout): Unit = func()
  }

  private def repeatFunc(id: Long, func: () => Any, delay: Duration)(
    implicit
    timer: WheelTimer): () => Any = () => {
    val to = timer.newTimeout(
      task(repeatFunc(id, func, delay)),
      delay.length,
      delay.unit)
    repeatTimers.put(id, to)
    func()
  }

  /**
   * Schedule an event.
   *
   * @param func Function to be scheduled
   * @param initialDelay Initial delay before first firing
   * @param delay Optional delay to be used if it is to be rescheduled (again)
   */
  def apply(
    func:         () => Any,
    initialDelay: Duration,
    delay:        Option[Duration] = None)(implicit timer: WheelTimer): Action =
    delay match {
      case Some(d) => apply(func, initialDelay, d)
      case None =>
        new Action(Some(
          timer.newTimeout(task(func), initialDelay.length, initialDelay.unit)))
    }

  /**
   * Schedule an event over and over again saving timeout reference.
   *
   * @param func Function to be scheduled
   * @param initialDelay Initial delay before first firing
   * @param delay Delay to be called after the first firing
   */
  def apply(func: () => Any, initialDelay: Duration, delay: Duration)(
    implicit
    timer: WheelTimer): Action = {
    val action = new Action(None)
    val to = timer.newTimeout(
      task(repeatFunc(action.id, func, delay)),
      initialDelay.length,
      initialDelay.unit)
    repeatTimers.put(action.id, to)
    action
  }

  /**
   * Schedule an event once.
   *
   * @param func Function to be scheduled
   * @param initialDelay Initial delay before first firing
   */
  def once(func: () => Any, initialDelay: Duration)(
    implicit
    timer: WheelTimer): Action = apply(func, initialDelay)

  /**
   * Schedule an event over and over again.
   *
   * @param func Function to be scheduled
   * @param initialDelay Initial delay before first firing
   * @param delay Delay to be called after the first firing
   */
  def again(func: () => Any, initialDelay: Duration, delay: Duration)(
    implicit
    timer: WheelTimer): Action = apply(func, initialDelay, delay)

  /**
   * This is a proxy-class, which works around the rescheduling issue.
   * If a task is scheduled once, it will have Some(Timeout).
   * If it is to be scheduled more than once, it will have None, but
   * methods will work transparently through a reference in Schedule.repeatTimers.
   *
   * @param timeout Optional Timeout parameter only provided by Schedule.once
   */
  class Action(timeout: Option[Timeout]) {
    lazy val id = scala.util.Random.nextLong()
    private def getTimeout = timeout orElse Option(repeatTimers.get(id))

    /**
     * Cancel the scheduled event.
     */
    def cancel(): Unit = getTimeout match {
      case Some(t) =>
        t.cancel
        repeatTimers.remove(id)
      case None =>
    }

    /**
     * Get the according TimerTask.
     */
    def task(): Option[TimerTask] = getTimeout.map(_.task)
  }
}
