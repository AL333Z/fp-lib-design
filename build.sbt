name := "fp-lib-design"

version := "0.1"

scalaVersion := "2.13.3"

val catsV             = "2.2.0"
val jmsV              = "2.0.1"
val ibmMQV            = "9.2.0.1"
val activeMQV         = "2.15.0"
val catsEffectV       = "3.0.0-M1"
val fs2V              = "3.0.0-M1"
val log4jSlf4jImplV   = "2.13.3"
val scalaTestV        = "3.2.0"
val kindProjectorV    = "0.11.0"
val betterMonadicForV = "0.3.1"
val log4sV            = "1.8.2"

scalafmtOnCompile := true

addCompilerPlugin("org.typelevel" %% "kind-projector"     % kindProjectorV cross CrossVersion.full)
addCompilerPlugin("com.olegpy"    %% "better-monadic-for" % betterMonadicForV)

libraryDependencies ++= Seq(
  "com.ibm.mq"               % "com.ibm.mq.allclient"   % ibmMQV,
  "org.apache.activemq"      % "artemis-jms-client-all" % activeMQV,
  "javax.jms"                % "javax.jms-api"          % jmsV,
  "org.apache.logging.log4j" % "log4j-slf4j-impl"       % log4jSlf4jImplV % Runtime,
  "org.scalatest"            %% "scalatest-freespec"    % scalaTestV % Test,
  "org.typelevel"            %% "cats-core"             % catsV,
  "org.typelevel"            %% "cats-effect"           % catsEffectV,
  "co.fs2"                   %% "fs2-core"              % fs2V,
  "org.log4s"                %% "log4s"                 % log4sV
)
