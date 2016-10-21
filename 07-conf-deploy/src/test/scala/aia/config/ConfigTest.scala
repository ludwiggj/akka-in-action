package aia.config

import akka.actor.ActorSystem
import org.scalatest.WordSpecLike
import com.typesafe.config.{ConfigException, ConfigFactory}
import org.scalatest.MustMatchers

class ConfigTest extends WordSpecLike with MustMatchers {

  "Configuration" must {
    "has configuration" in {
      val mySystem = ActorSystem("myTest")
      val config = mySystem.settings.config
      config.getInt("myTest.intParam") must be(20)
      config.getString("myTest.applicationDesc") must be("My Config Test")
    }

    "has defaults" in {
      val mySystem = ActorSystem("myDefaultsTestAsThisEntryDoesNotExist")
      val config = mySystem.settings.config
      config.getInt("myTestDefaults.intParam") must be(20)
      config.getString("myTestDefaults.applicationDesc") must be("My Current Test")
    }

    "can include file" in {
      val mySystem = ActorSystem("myIncludeTest")
      val config = mySystem.settings.config
      config.getInt("myTestIncluded.intParam") must be(20)
      config.getString("myTestIncluded.applicationDesc") must be("My Include Test")
    }

    "can be loaded by ourself" in {
      val configuration = ConfigFactory.load("load")
      val mySystem = ActorSystem("myLoadTest", configuration)
      val config = mySystem.settings.config
      config.getInt("myTestLoad.intParam") must be(20)
      config.getString("myTestLoad.applicationDesc") must be("My Load Test")
    }

    "can be lifted" in {
      val configuration = ConfigFactory.load("lift")
      val mySystem = ActorSystem("myFirstLiftTest", configuration.getConfig("myTestLift").withFallback(configuration))
      val config = mySystem.settings.config
      config.getString("myTest.applicationDesc") must be("My Lift Test")
      config.getString("rootParam") must be("root")

      // Following two only work as same config file is used as fallback
      config.getString("myTestLift.rootParam") must be("root")
      config.getInt("myTest.intParam") must be(20)

      val mySystem2 = ActorSystem("mySecondLiftTest", configuration.getConfig("myTestLift")
        .withOnlyPath("myTest").withFallback(configuration))
      val config2 = mySystem2.settings.config

      config2.getString("myTest.applicationDesc") must be("My Lift Test")
      an[ConfigException.Missing] must be thrownBy {
        config2.getString("rootParam")
      }

      // Following two only work as same config file is used as fallback
      config2.getString("myTestLift.rootParam") must be("root")
      config2.getInt("myTest.intParam") must be(20)
    }

    "can be controlled via a system property" in {
      val configFilename = "config.filename"
      val propertyName = "myTestDefaults.intParam"

      def setSystemProperty(key: String, value: String): Unit = {
        val props = System.getProperties;
        props.setProperty(key, value)
        System.setProperties(props)
      }

      setSystemProperty(configFilename, "")

      ReadConfig.getIntValue(configFilename, propertyName) must be(20)

      setSystemProperty(configFilename, "mine")

      ReadConfig.getIntValue(configFilename, propertyName) must be(200)
    }
  }
}