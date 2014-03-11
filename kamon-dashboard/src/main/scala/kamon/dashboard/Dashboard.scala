/* ===================================================
 * Copyright © 2013-2014 the kamon project <http://kamon.io/>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ========================================================== */
package kamon.dashboard

import akka.actor._
import akka.io.IO
import spray.can.Http
import kamon.Kamon
import kamon.dashboard.jmx.JMXServer

object Dashboard extends ExtensionId[DashboardExtension] with ExtensionIdProvider {
  override def lookup(): ExtensionId[_ <: Extension] = Dashboard
  override def createExtension(system: ExtendedActorSystem): DashboardExtension = new DashboardExtension(system)
}

class DashboardExtension(system: ExtendedActorSystem) extends Kamon.Extension {
  publishInfoMessage(system, "Kamon Dashboard Extension Loaded!!")
  val config = system.settings.config.getConfig("kamon.dashboard")

  val enabled = config.getBoolean("enabled")
  val interface = config getString ("interface")
  val dashboardPort = config.getInt("port")
  val jmxPort = config.getString("jmxServerPort")

  if (enabled) {
    val service = system.actorOf(Props[DashboardServiceActor], "kamon-dashboard-service")
    IO(Http)(system) ! Http.Bind(service, interface, dashboardPort)
    JMXServer(interface, jmxPort).start()
  }
}
