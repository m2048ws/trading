val scala3Version          = "3.8.4"
val typelevelVersion       = "2.13.0"
val disciplineMunitVersion = "2.0.0"

val staticDimensionCompilerClasspath =
  taskKey[Seq[File]]("Immutable classpath for real-source static-dimension compiler fixtures")
val quantitiesExternalArtifact =
  taskKey[File]("Completed quantities main artifact for external build consumers")
val economicsExternalArtifact =
  taskKey[File]("Completed economics main artifact for external build consumers")

ThisBuild / scalaVersion := scala3Version
ThisBuild / version      := "0.1.0-SNAPSHOT"

lazy val root =
  project
    .in(file("."))
    .aggregate(
      quantities,
      economics,
      adversarialBoundary
    )
    .settings(
      name                    := "trading",
      publish / skip          := true,
      Test / test / aggregate := false,
      // Scala 3 compiler fixtures load the completed quantities TASTy from the packaged JAR. Keep the root test command
      // ordered so they never run a second compiler concurrently with quantities' own clean test compilation.
      Test / test :=
        Def
          .sequential(
            quantities / Test / test,
            economics / Test / test,
            adversarialBoundary / Test / test
          )
          .value
    )

lazy val quantities =
  project
    .in(file("quantities"))
    .settings(
      name       := "trading-quantities",
      moduleName := "trading-quantities",

      // Tests consume the completed main JAR so the Scala compiler never observes mutable Compile/classes TASTy.
      // External dependencies and downstream project classpaths retain their ordinary wiring.
      quantitiesExternalArtifact         := (Compile / packageBin).value,
      Test / internalDependencyClasspath :=
        Seq(Attributed.blank((Compile / packageBin).value)),

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

lazy val economics =
  project
    .in(file("economics"))
    .dependsOn(quantities)
    .settings(
      name       := "trading-economics",
      moduleName := "trading-economics",

      economicsExternalArtifact := (Compile / packageBin).value,

      Test / classLoaderLayeringStrategy := ClassLoaderLayeringStrategy.Flat,

      libraryDependencies ++= Seq(
        "org.scalameta"  %% "munit"            % "1.3.4"  % Test,
        "org.scalacheck" %% "scalacheck"       % "1.19.0" % Test,
        "org.scalameta"  %% "munit-scalacheck" % "1.0.0"  % Test
      )
    )

lazy val adversarialBoundary =
  project
    .in(file("adversarial-boundary"))
    .dependsOn(quantities, economics)
    .settings(
      name           := "trading-quantities-adversarial-boundary",
      publish / skip := true,
      Test / scalacOptions += "-Werror",
      // Both external-consumer edges depend on the same completed package task; neither observes Compile/classes.
      Compile / internalDependencyClasspath :=
        Seq(
          Attributed.blank((quantities / quantitiesExternalArtifact).value),
          Attributed.blank((economics / economicsExternalArtifact).value)
        ),
      Test / internalDependencyClasspath := {
        val ownMain       = (Compile / products).value.map(Attributed.blank)
        val quantitiesJar = Attributed.blank((quantities / quantitiesExternalArtifact).value)
        val economicsJar  = Attributed.blank((economics / economicsExternalArtifact).value)
        (ownMain ++ Seq(quantitiesJar, economicsJar)).distinct
      },
      staticDimensionCompilerClasspath := {
        val moduleProducts = Seq(
          (quantities / quantitiesExternalArtifact).value,
          (economics / economicsExternalArtifact).value
        )
        val quantitiesDependencies = (quantities / Compile / externalDependencyClasspath).value.files
        val compilerDependencies   = (Test / externalDependencyClasspath).value.files
        (moduleProducts ++ quantitiesDependencies ++ compilerDependencies).distinct
      },
      Test / resourceGenerators += Def.task {
        val output    = (Test / resourceManaged).value / "static-dimension-compiler.classpath"
        val classpath =
          staticDimensionCompilerClasspath.value.map(_.getAbsolutePath).mkString(java.io.File.pathSeparator)
        IO.write(output, classpath)
        Seq(output)
      }.taskValue,
      libraryDependencies ++= Seq(
        "org.scala-lang" %% "scala3-compiler" % scala3Version % Test,
        "org.scalameta"  %% "munit"           % "1.3.4"       % Test
      )
    )
