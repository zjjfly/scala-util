package io.wasted.util.test

import io.wasted.util.{ Hashing, HashingAlgo, HexingAlgo }
import org.scalatest._

class HashingSpec extends WordSpec {
  implicit val hashingAlgo = HashingAlgo("HmacSHA256")
  implicit val hexingAlgo = HexingAlgo("SHA")

  val ourString = "this must work!!"
  val ourHexDigest = "c2bf26e94cab462fa275a3dc41f04cf3e67d470a"
  val ourSignature =
    "6efac23cabff39ec218e18a7a2494591095e74913ada965fbf8ad9d9b9f38d91"
  val ourHexSignature = "this works?!"

  val theirHexDigest = Hashing.hexDigest(ourString.getBytes("UTF-8"))
  val theirSignature = Hashing.sign(ourString, theirHexDigest)
  val theirHexSignature = Hashing.hexEncode(ourHexSignature.getBytes("UTF-8"))

  "Precalculated hex-digest (" + ourHexDigest + ")" should {
    "be the same as the calculated (" + theirHexDigest + ")" in {
      assert(ourHexDigest == theirHexDigest)
    }
  }

  "Precalculated hex-encoded (" + ourHexSignature + ")" should {
    "be the same as the calculated (" + theirHexSignature + ")" in {
      assert(
        ourHexSignature == new String(
          Hashing.hexDecode(theirHexSignature),
          "UTF-8"))
    }
  }

  "Precalculated sign (" + ourSignature + ")" should {
    "be the same as the calculated (" + theirSignature + ")" in {
      assert(ourSignature == theirSignature)
    }
  }
}
