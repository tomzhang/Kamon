/* =========================================================================================
 * Copyright © 2013-2014 the kamon project <http://kamon.io/>
 *
 * Licensed under the Apache License, Version 2.0 (the "License") you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 * =========================================================================================
 */

package kamon.jdbc.instrumentation

import java.sql.{DriverManager, SQLException}

import akka.actor.ActorSystem
import akka.testkit.{TestKitBase, TestProbe}
import com.typesafe.config.ConfigFactory
import kamon.Kamon
import kamon.jdbc.SlowQueryProcessor
import kamon.jdbc.metric.StatementsMetrics
import kamon.jdbc.metric.StatementsMetrics.StatementsMetricsSnapshot
import kamon.metric.Metrics
import kamon.metric.Subscriptions.TickMetricSnapshot
import kamon.trace.TraceRecorder
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import scala.concurrent.duration._

class StatementInstrumentationSpec extends TestKitBase with WordSpecLike with Matchers with BeforeAndAfterAll {

  implicit lazy val system: ActorSystem = ActorSystem("jdbc-spec", ConfigFactory.parseString(
    """
      |kamon {
      |   jdbc {
      |     slow-query-threshold = 100 milliseconds
      |
      |     # Fully qualified name of the implementation of kamon.jdbc.SlowQueryProcessor.
      |     slow-query-processor = kamon.jdbc.instrumentation.NOPSlowQueryProcessor
      |   }
      |}
    """.stripMargin))

  val connection = DriverManager.getConnection("jdbc:h2:mem:jdbc-spec","SA", "")

  override protected def beforeAll(): Unit = {
    connection should not be null

    val create = "CREATE TABLE Address (Nr INTEGER, Name VARCHAR(128));"
    val createStatement = connection.createStatement()
    createStatement.executeUpdate(create)

    val sleep = "CREATE ALIAS SLEEP FOR \"java.lang.Thread.sleep(long)\""
    val sleepStatement = connection.createStatement()
    sleepStatement.executeUpdate(sleep)
  }

  "the StatementInstrumentation" should {
    "record the execution time of the INSERT operation" in new StatementsMetricsListenerFixture {
      TraceRecorder.withNewTraceContext("jdbc-trace-insert") {

        val metricsListener = subscribeToMetrics()

        for(id <- 1 to 100) {
          val insert = s"INSERT INTO Address (Nr, Name) VALUES($id, 'foo')"
          val insertStatement = connection.prepareStatement(insert)
          insertStatement.execute()
        }

        val StatementMetrics = expectStatementsMetrics(metricsListener, 3 seconds)
        StatementMetrics.writes.numberOfMeasurements should be(100)
      }
    }

    "record the execution time of SELECT operation" in new StatementsMetricsListenerFixture {
      TraceRecorder.withNewTraceContext("jdbc-trace-select") {

        val metricsListener = subscribeToMetrics()

        for(id <- 1 to 100) {
          val select = s"SELECT * FROM Address where Nr = $id"
          val selectStatement = connection.createStatement()
          selectStatement.execute(select)
        }

        val StatementMetrics = expectStatementsMetrics(metricsListener, 3 seconds)
        StatementMetrics.reads.numberOfMeasurements should be(100)
      }
    }

    "record the execution time of UPDATE operation" in new StatementsMetricsListenerFixture {
      TraceRecorder.withNewTraceContext("jdbc-trace-update") {

        val metricsListener = subscribeToMetrics()

        for(id <- 1 to 100) {
          val update  = s"UPDATE Address SET Name = 'bar$id' where Nr = $id"
          val updateStatement = connection.prepareStatement(update)
          updateStatement.execute()
        }

        val StatementMetrics = expectStatementsMetrics(metricsListener, 3 seconds)
        StatementMetrics.writes.numberOfMeasurements should be(100)
      }
    }

    "record the execution time of DELETE operation" in new StatementsMetricsListenerFixture {
      TraceRecorder.withNewTraceContext("jdbc-trace-insert") {

        val metricsListener = subscribeToMetrics()

        for(id <- 1 to 100) {
          val delete  = s"DELETE FROM Address where Nr = $id"
          val deleteStatement = connection.createStatement()
          deleteStatement.execute(delete)
        }

        val StatementMetrics = expectStatementsMetrics(metricsListener, 3 seconds)
        StatementMetrics.writes.numberOfMeasurements should be(100)
      }
    }

    "record the execution time of SLOW QUERIES based on the kamon.jdbc.slow-query-threshold" in new StatementsMetricsListenerFixture {
      TraceRecorder.withNewTraceContext("jdbc-trace-slow") {

        val metricsListener = subscribeToMetrics()

        for(id <- 1 to 2) {
          val select = s"SELECT * FROM Address; CALL SLEEP(100)"
          val selectStatement = connection.createStatement()
          selectStatement.execute(select)
        }

        val StatementMetrics = expectStatementsMetrics(metricsListener, 3 seconds)
        StatementMetrics.slows.count should be(2)
      }
    }

    "count the total ERRORS" in new StatementsMetricsListenerFixture {
      TraceRecorder.withNewTraceContext("jdbc-trace-slow") {

        val metricsListener = subscribeToMetrics()

        for(_ <- 1 to 10) {
          intercept[SQLException] {
            val error = "SELECT * FROM NON_EXIST_TABLE"
            val errorStatement = connection.createStatement()
            errorStatement.execute(error)
          }
        }
        val StatementMetrics = expectStatementsMetrics(metricsListener, 3 seconds)
        StatementMetrics.errors.count should be(10)
      }
    }
  }

  trait StatementsMetricsListenerFixture {
    def subscribeToMetrics( ): TestProbe = {
      val metricsListener = TestProbe()
      Kamon(Metrics).subscribe(StatementsMetrics, "*", metricsListener.ref, permanently = true)
      // Wait for one empty snapshot before proceeding to the test.
      metricsListener.expectMsgType[TickMetricSnapshot]
      metricsListener
    }
  }

  def expectStatementsMetrics(listener: TestProbe, waitTime: FiniteDuration): StatementsMetricsSnapshot = {
    val tickSnapshot = within(waitTime) {
      listener.expectMsgType[TickMetricSnapshot]
    }
    val statementsMetricsOption = tickSnapshot.metrics.get(StatementsMetrics(StatementInstrumentation.Statements))
    statementsMetricsOption should not be empty
    statementsMetricsOption.get.asInstanceOf[StatementsMetricsSnapshot]
  }
}

class NOPSlowQueryProcessor extends SlowQueryProcessor {
  override def process(sql: String, executionTimeInMillis: Long, queryThresholdInMillis: Long): Unit = {/*do nothing!!!*/}
}


