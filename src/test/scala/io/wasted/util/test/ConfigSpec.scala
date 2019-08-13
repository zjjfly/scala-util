package io.wasted.util.test

import java.net.InetSocketAddress

import io.wasted.util.Config
import org.scalatest._

class ConfigSpec extends WordSpec {
  val ourInt = 5
  val ourBool = true
  val ourString = "chicken"
  val ourList = List("foo", "bar", "baz")
  val ourAddrs = List(
    new InetSocketAddress("127.0.0.1", 8080),
    new InetSocketAddress("::", 8081))

  "Preset Integer (" + ourInt + ")" should {
    val testVal = Config.getInt("test.int")
    "be the same as the config value (" + testVal + ")" in {
      assert(testVal.contains(ourInt))
    }
  }

  "Preset Boolean (" + ourBool + ")" should {
    val testVal = Config.getBool("test.bool")
    "be the same as the config value (" + testVal + ")" in {
      assert(testVal.contains(ourBool))
    }
  }

  "Preset String (" + ourString + ")" should {
    val testVal = Config.getString("test.string")
    "be the same as the config value (" + testVal + ")" in {
      assert(testVal.contains(ourString))
    }
  }

  "Preset String-List (" + ourList + ")" should {
    val testVal = Config.getStringList("test.list")
    "be the same as the config value (" + testVal + ")" in {
      assert(testVal.contains(ourList))
    }
  }

  "Preset InetSocketAddress-List (" + ourAddrs + ")" should {
    val testVal = Config.getInetAddrList("test.addrs")
    "be the same as the config value (" + testVal + ")" in {
      assert(testVal.contains(ourAddrs))
    }
  }

  "Non-existing config-lookups" should {
    "for Integers be the None if no default-value was given" in {
      assert(Config.getInt("foo").isEmpty)
    }
    "for Integers be Some(5) if a default-value of 5 was given" in {
      assert(Config.getInt("foo", 5) == 5)
    }
    "for Boolean be the None if no default-value was given" in {
      assert(Config.getBool("foo").isEmpty)
    }
    "for Boolean be Some(true) if a default-value of true was given" in {
      assert(Config.getBool("foo", fallback = true))
    }
    "for String be the None if no default-value was given" in {
      assert(Config.getString("foo").isEmpty)
    }
    "for String be Some(bar) if a default-value of bar was given" in {
      assert(Config.getString("foo", "bar") == "bar")
    }
    "for String-List be the None if no default-value was given" in {
      assert(Config.getStringList("foo").isEmpty)
    }
    "for String-List be Some(List(\"bar\", \"baz\")) if a default-value was given" in {
      assert(
        Config.getStringList("foo", List("bar", "baz")) == List("bar", "baz"))
    }
    "for InetSocketAddress-List be the None if no default-value was given" in {
      assert(Config.getInetAddrList("foo").isEmpty)
    }
    "for InetSocketAddress-List be Some(InetSocketAddress(\"1.2.3.4\", 80)) if a default-value was given" in {
      assert(
        Config.getInetAddrList("foo", List("1.2.3.4:80")) == List(
          new java.net.InetSocketAddress("1.2.3.4", 80)))
    }
  }

}
