package aia.config

import com.typesafe.config.ConfigFactory

object ReadConfig extends App {

  def getIntValue(configFileNameKey: String, propertyKey: String): Int = {
    val configFileName = System.getProperty(configFileNameKey)

    println(s"Value $configFileName")

    val config = ConfigFactory.load(configFileName)

    config.getInt(propertyKey)
  }
}