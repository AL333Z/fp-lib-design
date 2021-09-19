name := "fp-lib-design"

version := "0.1"

scalaVersion := "2.13.6"

val jmsV              = "2.0.1"
val ibmMQV            = "9.2.0.1"
val activeMQV         = "2.15.0"
val catsEffectV       = "3.2.9"
val fs2V              = "3.0.6"
val log4jSlf4jImplV   = "2.13.3"
val scalaTestV        = "3.2.0"
val kindProjectorV    = "0.11.0"
val betterMonadicForV = "0.3.1"
val log4catsV         = "2.1.1"
val keyPoolV          = "0.4.7"

scalafmtOnCompile := true

fork in run := true

addCompilerPlugin("com.olegpy" %% "better-monadic-for" % betterMonadicForV)

libraryDependencies ++= Seq(
  "com.ibm.mq"               % "com.ibm.mq.allclient"   % ibmMQV,
  "org.apache.activemq"      % "artemis-jms-client-all" % activeMQV,
  "javax.jms"                % "javax.jms-api"          % jmsV,
  "org.apache.logging.log4j" % "log4j-slf4j-impl"       % log4jSlf4jImplV % Runtime,
  "org.typelevel"            %% "cats-effect"           % catsEffectV,
  "org.typelevel"            %% "keypool"               % keyPoolV,
  "co.fs2"                   %% "fs2-core"              % fs2V,
  "org.typelevel"            %% "log4cats-slf4j"        % log4catsV
)
