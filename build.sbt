val scala3Version          = "3.8.4"
val catsVersion            = "2.13.0"
val algebraVersion         = "2.13.0"
val disciplineMunitVersion = "2.0.0"

val staticDimensionCompilerClasspath =
  taskKey[Seq[File]]("Immutable classpath for real-source static-dimension compiler fixtures")
val quantitiesExternalArtifact =
  taskKey[File]("Completed quantities main artifact for external build consumers")
val referenceDataExternalArtifact =
  taskKey[File]("Completed reference-data main artifact for external build consumers")
val economicsExternalArtifact =
  taskKey[File]("Completed economics main artifact for external build consumers")

ThisBuild / scalaVersion := scala3Version
ThisBuild / version      := "0.1.0-SNAPSHOT"

lazy val root =
  project
    .in(file("."))
    .aggregate(
      quantities,
      referenceData,
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
            referenceData / Test / test,
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

      // Same-project tests retain SBT's normal Compile/classes dependency. External consumers synchronize on one
      // completed immutable artifact without changing Compile/exportedProducts or Test/internalDependencyClasspath.
      quantitiesExternalArtifact := (Compile / packageBin).value,

      Test / classLoaderLayeringStrategy := ClassLoaderLayeringStrategy.Flat,
      // The clean aggregate gate showed missing compiled classes when quantity suites shared this in-process loader.
      // Serialize this module's tests so the configured gate does not depend on classloader timing.
      Test / parallelExecution := false,

      libraryDependencies ++= Seq(
        "org.typelevel"  %% "algebra"          % algebraVersion,
        "org.typelevel"  %% "cats-kernel"      % catsVersion,
        "org.typelevel"  %% "algebra-laws"     % algebraVersion         % Test,
        "org.typelevel"  %% "cats-laws"        % catsVersion            % Test,
        "org.typelevel"  %% "discipline-munit" % disciplineMunitVersion % Test,
        "org.scalameta"  %% "munit"            % "1.3.4"                % Test,
        "org.scalacheck" %% "scalacheck"       % "1.19.0"               % Test,
        "org.scalameta"  %% "munit-scalacheck" % "1.0.0"                % Test
      )
    )

lazy val referenceData =
  project
    .in(file("reference-data"))
    .dependsOn(quantities)
    .settings(
      name       := "trading-reference-data",
      moduleName := "trading-reference-data",

      Compile / packageBin :=
        (Compile / packageBin).dependsOn(Compile / compile).value,
      referenceDataExternalArtifact := (Compile / packageBin).value,
      Compile / javacOptions ++= Seq("--release", "17"),

      Test / classLoaderLayeringStrategy := ClassLoaderLayeringStrategy.Flat,

      libraryDependencies ++= Seq(
        "org.scalameta"  %% "munit"            % "1.3.4"  % Test,
        "org.scalacheck" %% "scalacheck"       % "1.19.0" % Test,
        "org.scalameta"  %% "munit-scalacheck" % "1.0.0"  % Test
      )
    )

lazy val economics =
  project
    .in(file("economics"))
    .dependsOn(quantities, referenceData)
    .settings(
      name       := "trading-economics",
      moduleName := "trading-economics",

      Compile / packageBin :=
        (Compile / packageBin).dependsOn(Compile / compile).value,
      economicsExternalArtifact := (Compile / packageBin).value,

      Test / classLoaderLayeringStrategy := ClassLoaderLayeringStrategy.Flat,
      // The root clean gate rebuilds reference-data between sequential module test tasks. Isolate this downstream test
      // runner so it observes one completed dependency classpath instead of an in-process loader cached across
      // rebuilds.
      Test / fork := true,

      libraryDependencies ++= Seq(
        "org.typelevel"  %% "cats-core"        % catsVersion,
        "org.scalameta"  %% "munit"            % "1.3.4"  % Test,
        "org.scalacheck" %% "scalacheck"       % "1.19.0" % Test,
        "org.scalameta"  %% "munit-scalacheck" % "1.0.0"  % Test
      )
    )

lazy val adversarialBoundary =
  project
    .in(file("adversarial-boundary"))
    .dependsOn(quantities, referenceData, economics)
    .settings(
      name           := "trading-quantities-adversarial-boundary",
      publish / skip := true,
      Test / scalacOptions += "-Werror",
      // These suites run several Scala 3 compiler instances in-process. Dotty compiler state is not safe to share
      // across concurrently executing suites, so serialize this test-only boundary without affecting other modules.
      Test / parallelExecution := false,
      // External-consumer edges depend on completed package tasks; none observes Compile/classes.
      Compile / internalDependencyClasspath :=
        Seq(
          Attributed.blank((quantities / quantitiesExternalArtifact).value),
          Attributed.blank((referenceData / referenceDataExternalArtifact).value),
          Attributed.blank((economics / economicsExternalArtifact).value)
        ),
      Test / internalDependencyClasspath := {
        val ownMain          = (Compile / products).value.map(Attributed.blank)
        val quantitiesJar    = Attributed.blank((quantities / quantitiesExternalArtifact).value)
        val referenceDataJar = Attributed.blank((referenceData / referenceDataExternalArtifact).value)
        val economicsJar     = Attributed.blank((economics / economicsExternalArtifact).value)
        (ownMain ++ Seq(quantitiesJar, referenceDataJar, economicsJar)).distinct
      },
      staticDimensionCompilerClasspath := {
        val moduleProducts = Seq(
          (quantities / quantitiesExternalArtifact).value,
          (referenceData / referenceDataExternalArtifact).value,
          (economics / economicsExternalArtifact).value
        )
        val quantitiesDependencies    = (quantities / Compile / externalDependencyClasspath).value.files
        val referenceDataDependencies = (referenceData / Compile / externalDependencyClasspath).value.files
        val economicsDependencies     = (economics / Compile / externalDependencyClasspath).value.files
        val compilerDependencies      = (Test / externalDependencyClasspath).value.files
        (moduleProducts ++ quantitiesDependencies ++ referenceDataDependencies ++ economicsDependencies ++
          compilerDependencies).distinct
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
