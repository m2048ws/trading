val scala3Version          = "3.8.4"
val typelevelVersion       = "2.13.0"
val disciplineMunitVersion = "2.0.0"

ThisBuild / scalaVersion := scala3Version
ThisBuild / version      := "0.1.0-SNAPSHOT"

lazy val root =
  project
    .in(file("."))
    .aggregate(
      quantities,
      adversarialBoundary
    )
    .settings(
      name           := "trading",
      publish / skip := true
    )

lazy val quantities =
  project
    .in(file("quantities"))
    .settings(
      name       := "trading-quantities",
      moduleName := "trading-quantities",

      Test / classLoaderLayeringStrategy := ClassLoaderLayeringStrategy.Flat,

      libraryDependencies ++= Seq(
        "org.typelevel"  %% "algebra"          % typelevelVersion,
        "org.typelevel"  %% "cats-kernel"      % typelevelVersion,
        "org.typelevel"  %% "algebra-laws"     % typelevelVersion       % Test,
        "org.typelevel"  %% "cats-laws"        % typelevelVersion       % Test,
        "org.typelevel"  %% "discipline-munit" % disciplineMunitVersion % Test,
        "org.scalameta"  %% "munit"            % "1.3.4"                % Test,
        "org.scalacheck" %% "scalacheck"       % "1.19.0"               % Test,
        "org.scalameta"  %% "munit-scalacheck" % "1.0.0"                % Test
      )
    )

lazy val adversarialBoundary =
  project
    .in(file("adversarial-boundary"))
    .dependsOn(quantities)
    .settings(
      name                                   := "trading-quantities-adversarial-boundary",
      publish / skip                         := true,
      libraryDependencies += "org.scalameta" %% "munit" % "1.3.4" % Test
    )
