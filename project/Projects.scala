import sbt._
import Keys._

object Projects extends Build {
  import AspectJ._
  import Settings._
  import Dependencies._

  lazy val root = Project("root", file("."))
    .aggregate(kamonCore, kamonSpray, kamonNewrelic, kamonPlayground, kamonDashboard, kamonTestkit, kamonPlay, kamonStatsD,
      kamonDatadog, kamonSystemMetrics, kamonLogReporter, kamonAkkaRemote, kamonSigar, kamonJdbc)
    .settings(basicSettings: _*)
    .settings(formatSettings: _*)
    .settings(noPublishing: _*)


  lazy val kamonCore = Project("kamon-core", file("kamon-core"))
    .dependsOn(kamonMacros % "compile-internal, test-internal")
    .settings(basicSettings: _*)
    .settings(formatSettings: _*)
    .settings(aspectJSettings: _*)
    .settings(
      javacOptions  in Compile ++= Seq("-XDignore.symbol.file"),
      mappings in (Compile, packageBin) ++= mappings.in(kamonMacros, Compile, packageBin).value,
      mappings in (Compile, packageSrc) ++= mappings.in(kamonMacros, Compile, packageSrc).value,
      libraryDependencies ++=
        compile(akkaActor, hdrHistogram) ++
        provided(aspectJ) ++
        optional(logback, scalazConcurrent) ++
        test(scalatest, akkaTestKit, akkaSlf4j, logback))


  lazy val kamonAkkaRemote = Project("kamon-akka-remote", file("kamon-akka-remote"))
    .dependsOn(kamonCore)
    .settings(basicSettings: _* )
    .settings(formatSettings: _*)
    .settings(aspectJSettings: _*)
    .settings(
      libraryDependencies ++=
        compile(akkaRemote, akkaCluster) ++
        provided(aspectJ) ++
        test(scalatest, akkaTestKit))


  lazy val kamonSpray = Project("kamon-spray", file("kamon-spray"))
    .dependsOn(kamonMacros % "compile-internal, test-internal")
    .settings(basicSettings: _*)
    .settings(formatSettings: _*)
    .settings(aspectJSettings: _*)
    .settings(
      mappings in (Compile, packageBin) ++= mappings.in(kamonMacros, Compile, packageBin).value,
      mappings in (Compile, packageSrc) ++= mappings.in(kamonMacros, Compile, packageSrc).value,
      libraryDependencies ++=
        compile(akkaActor, sprayCan, sprayClient, sprayRouting) ++
        provided(aspectJ) ++
        test(scalatest, akkaTestKit, sprayTestkit, slf4Api, slf4nop))
    .dependsOn(kamonCore)
    .dependsOn(kamonTestkit % "test")


  lazy val kamonNewrelic = Project("kamon-newrelic", file("kamon-newrelic"))
    .settings(basicSettings: _*)
    .settings(formatSettings: _*)
    .settings(aspectJSettings: _*)
    .settings(
      libraryDependencies ++=
        compile(sprayCan, sprayClient, sprayRouting, sprayJson, sprayJsonLenses, newrelic, akkaSlf4j) ++
        provided(aspectJ) ++
        test(scalatest, akkaTestKit, sprayTestkit, slf4Api, akkaSlf4j))
    .dependsOn(kamonCore)


  lazy val kamonPlayground = Project("kamon-playground", file("kamon-playground"))
    .settings(basicSettings: _*)
    .settings(formatSettings: _*)
    .settings(noPublishing: _*)
    .settings(aspectJSettings: _*)
    .settings(
      libraryDependencies ++=
        compile(akkaActor, akkaSlf4j, sprayCan, sprayClient, sprayRouting, logback))
    .dependsOn(kamonSpray, kamonNewrelic, kamonStatsD, kamonDatadog, kamonLogReporter, kamonSystemMetrics)


  lazy val kamonDashboard = Project("kamon-dashboard", file("kamon-dashboard"))
    .settings(basicSettings: _*)
    .settings(formatSettings: _*)
    .settings(
      libraryDependencies ++=
        compile(akkaActor, akkaSlf4j, sprayRouting, sprayCan, sprayJson))
    .dependsOn(kamonCore)


  lazy val kamonTestkit = Project("kamon-testkit", file("kamon-testkit"))
    .settings(basicSettings: _*)
    .settings(formatSettings: _*)
    .settings(
      libraryDependencies ++=
        compile(akkaActor, akkaTestKit) ++
        provided(aspectJ) ++
        test(slf4Api, slf4nop))
    .dependsOn(kamonCore)

  lazy val kamonPlay = Project("kamon-play", file("kamon-play"))
    .settings(basicSettings: _*)
    .settings(formatSettings: _*)
    .settings(aspectJSettings: _*)
    .settings(
      libraryDependencies ++=
        compile(play, playWS) ++
        provided(aspectJ) ++
        test(playTest, akkaTestKit, slf4Api))
    .dependsOn(kamonCore)

  lazy val kamonStatsD = Project("kamon-statsd", file("kamon-statsd"))
    .settings(basicSettings: _*)
    .settings(formatSettings: _*)
    .settings(
      libraryDependencies ++=
        compile(akkaActor) ++
        test(scalatest, akkaTestKit, slf4Api, slf4nop))
    .dependsOn(kamonCore)
    .dependsOn(kamonSystemMetrics % "provided")
 
  lazy val kamonDatadog = Project("kamon-datadog", file("kamon-datadog"))
    .settings(basicSettings: _*)
    .settings(formatSettings: _*)
    .settings(
      libraryDependencies ++=
        compile(akkaActor) ++
        test(scalatest, akkaTestKit, slf4Api, slf4nop))
    .dependsOn(kamonCore)
    .dependsOn(kamonSystemMetrics % "provided")

  lazy val kamonLogReporter = Project("kamon-log-reporter", file("kamon-log-reporter"))
    .settings(basicSettings: _*)
    .settings(formatSettings: _*)
    .settings(
      libraryDependencies ++=
        compile(akkaActor) ++
        test(scalatest, akkaTestKit, slf4Api, slf4nop))
    .dependsOn(kamonCore)
    .dependsOn(kamonSystemMetrics % "provided")

  lazy val kamonMacros = Project("kamon-macros", file("kamon-macros"))
    .settings(basicSettings: _*)
    .settings(formatSettings: _*)
    .settings(noPublishing: _*)
    .settings(libraryDependencies ++= compile(scalaCompiler))

  lazy val kamonSystemMetrics = Project("kamon-system-metrics", file("kamon-system-metrics"))
      .settings(basicSettings: _*)
      .settings(formatSettings: _*)
      .settings(fork in Test :=  true)
      .settings(
        libraryDependencies ++=
          compile(sigar) ++
          test(scalatest, akkaTestKit, slf4Api, slf4nop))
      .dependsOn(kamonCore)
      .dependsOn(kamonSigar)

  lazy val kamonSigar = Project("kamon-sigar", file("kamon-sigar"))
    .settings(basicSettings: _*)
    .settings(formatSettings: _*)
    .settings(Sigar.settings: _*)

  lazy val kamonJdbc = Project("kamon-jdbc", file("kamon-jdbc"))
    .settings(basicSettings: _*)
    .settings(formatSettings: _*)
    .settings(aspectJSettings: _*)
    .settings(
      libraryDependencies ++=
        test(h2,scalatest, akkaTestKit, slf4Api) ++
        provided(aspectJ))
    .dependsOn(kamonCore)

  val noPublishing = Seq(publish := (), publishLocal := (), publishArtifact := false)
}
