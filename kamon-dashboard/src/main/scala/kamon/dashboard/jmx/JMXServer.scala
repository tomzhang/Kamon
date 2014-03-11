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
package kamon.dashboard.jmx

import org.jolokia.jvmagent.{ JolokiaServerConfig, JolokiaServer }

class JMXServer(config: Map[String, String]) {
  import scala.collection.JavaConverters._

  val server = new JolokiaServer(new JolokiaServerConfig(config.asJava), false)

  def start(): this.type = {
    server.start()
    this
  }

  def stop(): this.type = {
    server.stop()
    this
  }
}

object JMXServer {
  def apply(interface: String, port: String) = new JMXServer(Map[String, String]("host" -> interface, "port" -> port))
}