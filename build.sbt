import ScalaJSKeys._

val commonSettings = Seq(
  organization := "org.scalajs",
  version := "0.1-SNAPSHOT",
  scalaVersion := "2.11.2",
  normalizedName ~= { _.replace("scala-js", "scalajs") },
  scalacOptions ++= Seq(
    "-language:reflectiveCalls",
    "-deprecation",
    "-unchecked",
    "-feature",
    "-encoding", "utf8"
  )
)

// lazy val root = project.in(file("."))
//   .settings(commonSettings: _*)
//   .aggregate(actors)

// lazy val examples = project.settings(commonSettings: _*).aggregate(
//   webRTCExample,
//   chatWebSocket,
//   chatWebRTC,
//   autowire)

lazy val transportJvm = project.in(file("transport/play"))
  .settings(commonSettings: _*)
  .settings(unmanagedSourceDirectories in Compile += baseDirectory.value / "../shared")

lazy val transportJs = project.in(file("transport/js"))
  .settings((commonSettings ++ scalaJSSettings): _*)
  .settings(unmanagedSourceDirectories in Compile += baseDirectory.value / "../shared")

lazy val webRTCExample = project.in(file("examples/webrtc"))
  .settings((commonSettings ++ scalaJSSettings): _*)

lazy val autowire = project.in(file("examples/autowire/jvm"))
  .enablePlugins(PlayScala)
  .dependsOn(transportJvm)
  .settings(commonSettings: _*)
  .settings(unmanagedSourceDirectories in Compile += baseDirectory.value / "../shared")
  .settings(unmanagedResourceDirectories in Compile += baseDirectory.value / "../js/src")

lazy val autowireJs = project.in(file("examples/autowire/js"))
  .settings((commonSettings ++ scalaJSSettings): _*)
  .dependsOn(transportJs)
  .settings(
    unmanagedSourceDirectories in Compile +=
      (baseDirectory in autowire).value / "../shared",
    fastOptJS in Compile <<= (fastOptJS in Compile) triggeredBy (compile in (autowire, Compile))
  )
  .settings(
    Seq(fastOptJS, fullOptJS) map {
      packageJSKey =>
        crossTarget in (Compile, packageJSKey) :=
          (baseDirectory in autowire).value / "public/javascripts"
    }: _*
  )

lazy val chatWebSocket = project.in(file("examples/chat-websocket/jvm"))
  .enablePlugins(PlayScala)
  .dependsOn(transportJvm)
  .settings(commonSettings: _*)
  .settings(unmanagedSourceDirectories in Compile += baseDirectory.value / "../shared")
  .settings(unmanagedResourceDirectories in Compile += baseDirectory.value / "../js/src")

lazy val chatWebSocketJs = project.in(file("examples/chat-websocket/js"))
  .settings((commonSettings ++ scalaJSSettings): _*)
  // .dependsOn(actors)
  .dependsOn(transportJs)
  .settings(
    unmanagedSourceDirectories in Compile +=
      (baseDirectory in chatWebSocket).value / "../shared",
    fastOptJS in Compile <<= (fastOptJS in Compile) triggeredBy (compile in (chatWebSocket, Compile))
  )
  .settings(
    Seq(fastOptJS, fullOptJS) map {
      packageJSKey =>
        crossTarget in (Compile, packageJSKey) :=
          (baseDirectory in chatWebSocket).value / "public/javascripts"
    }: _*
  )

lazy val chatWebRTC = project.in(file("examples/chat-webrtc/jvm"))
  .enablePlugins(PlayScala)
  .dependsOn(transportJvm)
  .settings(commonSettings: _*)
  .settings(unmanagedSourceDirectories in Compile += baseDirectory.value / "../shared")
  .settings(unmanagedResourceDirectories in Compile += baseDirectory.value / "../js/src")

lazy val chatWebRTCJs = project.in(file("examples/chat-webrtc/js"))
  .settings((commonSettings ++ scalaJSSettings): _*)
  // .dependsOn(actors)
  .dependsOn(transportJs)
  .settings(
    unmanagedSourceDirectories in Compile +=
      (baseDirectory in chatWebRTC).value / "../shared",
    fastOptJS in Compile <<= (fastOptJS in Compile) triggeredBy (compile in (chatWebRTC, Compile))
  )
  .settings(
    Seq(fastOptJS, fullOptJS) map {
      packageJSKey =>
        crossTarget in (Compile, packageJSKey) :=
          (baseDirectory in chatWebRTC).value / "public/javascripts"
    }: _*
  )
