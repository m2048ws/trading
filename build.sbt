val scala3Version          = "3.8.4"
val catsVersion            = "2.13.0"
val algebraVersion         = "2.13.0"
val disciplineMunitVersion = "2.0.0"

val staticDimensionCompilerClasspath =
  taskKey[Seq[File]]("Immutable classpath for real-source static-dimension compiler fixtures")
val instrumentEconomicsCompilerClasspath =
  taskKey[Seq[File]]("Immutable classpath for completed-JAR instrument-economics compiler fixtures")

ThisBuild / scalaVersion := scala3Version
ThisBuild / version      := "0.1.0-SNAPSHOT"

lazy val root =
  project
    .in(file("."))
    .aggregate(
      quantities,
      referenceData,
      application,
      instrumentEconomics,
      economics,
      adversarialBoundary
    )
    .settings(
      name                    := "trading",
      publish / skip          := true,
      Test / test / aggregate := false,
      // Keep the compiler-heavy module suites ordered. Each test task obtains immutable main JARs through SBT's
      // exportedProducts graph, so artifact construction remains a dependency of its consumer rather than a separate
      // prebuild stage that can reevaluate the same package path.
      Test / test :=
        Def
          .sequential(
            quantities / Test / test,
            referenceData / Test / test,
            application / Test / test,
            instrumentEconomics / Test / test,
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

      // SBT packages this project's completed main products before exposing them to same-build consumers.
      Compile / exportJars := true,

      Test / classLoaderLayeringStrategy := ClassLoaderLayeringStrategy.Flat,
      // The clean aggregate gate showed missing compiled classes when quantity suites shared this in-process loader.
      // Isolate and serialize this module's tests so the configured gate does not depend on classloader timing.
      Test / fork              := true,
      Test / parallelExecution := false,

      libraryDependencies ++= Seq(
        "org.typelevel"  %% "algebra"          % algebraVersion,
        "org.typelevel"  %% "cats-kernel"      % catsVersion,
        "org.typelevel"  %% "algebra-laws"     % algebraVersion         % Test,
        "org.typelevel"  %% "cats-laws"        % catsVersion            % Test,
        "org.typelevel"  %% "discipline-munit" % disciplineMunitVersion % Test,
        "org.scalameta"  %% "munit"            % "1.3.5"                % Test,
        "org.scalacheck" %% "scalacheck"       % "1.20.0"               % Test,
        "org.scalameta"  %% "munit-scalacheck" % "1.3.0"                % Test
      )
    )

lazy val referenceData =
  project
    .in(file("reference-data"))
    .dependsOn(quantities)
    .settings(
      name       := "trading-reference-data",
      moduleName := "trading-reference-data",

      Compile / exportJars := true,
      Compile / javacOptions ++= Seq("--release", "17"),

      Test / classLoaderLayeringStrategy := ClassLoaderLayeringStrategy.Flat,

      libraryDependencies ++= Seq(
        "org.scalameta"  %% "munit"            % "1.3.5"  % Test,
        "org.scalacheck" %% "scalacheck"       % "1.20.0" % Test,
        "org.scalameta"  %% "munit-scalacheck" % "1.3.0"  % Test
      )
    )

lazy val economics =
  project
    .in(file("economics"))
    .dependsOn(instrumentEconomics)
    .settings(
      name       := "trading-economics",
      moduleName := "trading-economics",

      Compile / exportJars := true,

      Test / classLoaderLayeringStrategy := ClassLoaderLayeringStrategy.Flat,
      // The root clean gate rebuilds reference-data between sequential module test tasks. Isolate this downstream test
      // runner so it observes one completed dependency classpath instead of an in-process loader cached across
      // rebuilds.
      Test / fork := true,

      libraryDependencies ++= Seq(
        "org.typelevel"  %% "cats-core"        % catsVersion,
        "org.scalameta"  %% "munit"            % "1.3.5"  % Test,
        "org.scalacheck" %% "scalacheck"       % "1.20.0" % Test,
        "org.scalameta"  %% "munit-scalacheck" % "1.3.0"  % Test
      )
    )

lazy val instrumentEconomics =
  project
    .in(file("instrument-economics"))
    .dependsOn(quantities, referenceData)
    .settings(
      name       := "trading-instrument-economics",
      moduleName := "trading-instrument-economics",

      Compile / exportJars := true,

      Test / classLoaderLayeringStrategy := ClassLoaderLayeringStrategy.Flat,
      Test / fork                        := true,

      libraryDependencies ++= Seq(
        "org.typelevel"  %% "cats-core"        % catsVersion,
        "org.scalameta"  %% "munit"            % "1.3.5"  % Test,
        "org.scalacheck" %% "scalacheck"       % "1.20.0" % Test,
        "org.scalameta"  %% "munit-scalacheck" % "1.3.0"  % Test
      )
    )

lazy val application =
  project
    .in(file("application"))
    .dependsOn(referenceData)
    .settings(
      name       := "trading-application",
      moduleName := "trading-application",

      Compile / exportJars := true,

      Test / classLoaderLayeringStrategy := ClassLoaderLayeringStrategy.Flat,
      // The root clean gate rebuilds reference data before this downstream contract suite. Fork the test runner so
      // it always loads that completed dependency generation instead of an in-process classloader cached earlier.
      Test / fork := true,

      libraryDependencies += "org.scalameta" %% "munit" % "1.3.5" % Test
    )

// Non-published performance evidence is compiled and run explicitly; it is intentionally outside root aggregation.
lazy val benchmarks =
  project
    .in(file("benchmarks"))
    .enablePlugins(JmhPlugin)
    .dependsOn(referenceData)
    .settings(
      name           := "trading-benchmarks",
      publish / skip := true
    )

lazy val adversarialBoundary =
  project
    .in(file("adversarial-boundary"))
    .dependsOn(quantities, referenceData, application, instrumentEconomics, economics)
    .settings(
      name           := "trading-quantities-adversarial-boundary",
      publish / skip := true,
      Test / scalacOptions += "-Werror",
      // These suites run several Scala 3 compiler instances in-process. Dotty compiler state is not safe to share
      // across concurrently executing suites, so serialize this test-only boundary without affecting other modules.
      Test / parallelExecution         := false,
      staticDimensionCompilerClasspath := {
        val moduleProducts = (quantities / Compile / exportedProducts).value.files ++
          (referenceData / Compile / exportedProducts).value.files ++
          (application / Compile / exportedProducts).value.files ++
          (instrumentEconomics / Compile / exportedProducts).value.files ++
          (economics / Compile / exportedProducts).value.files
        val quantitiesDependencies    = (quantities / Compile / externalDependencyClasspath).value.files
        val referenceDataDependencies = (referenceData / Compile / externalDependencyClasspath).value.files
        val applicationDependencies   = (application / Compile / externalDependencyClasspath).value.files
        val instrumentDependencies    = (instrumentEconomics / Compile / externalDependencyClasspath).value.files
        val economicsDependencies     = (economics / Compile / externalDependencyClasspath).value.files
        val compilerDependencies      = (Test / externalDependencyClasspath).value.files
        (moduleProducts ++ quantitiesDependencies ++ referenceDataDependencies ++ applicationDependencies ++
          instrumentDependencies ++ economicsDependencies ++ compilerDependencies).distinct
      },
      instrumentEconomicsCompilerClasspath := {
        val moduleProducts = (quantities / Compile / exportedProducts).value.files ++
          (referenceData / Compile / exportedProducts).value.files ++
          (instrumentEconomics / Compile / exportedProducts).value.files
        val quantitiesDependencies = (quantities / Compile / externalDependencyClasspath).value.files
        val referenceDependencies  = (referenceData / Compile / externalDependencyClasspath).value.files
        val instrumentDependencies = (instrumentEconomics / Compile / externalDependencyClasspath).value.files
        val compilerDependencies   = (Test / externalDependencyClasspath).value.files
        (moduleProducts ++ quantitiesDependencies ++ referenceDependencies ++ instrumentDependencies ++
          compilerDependencies).distinct
      },
      Test / resourceGenerators += Def.task {
        val output    = (Test / resourceManaged).value / "static-dimension-compiler.classpath"
        val classpath =
          staticDimensionCompilerClasspath.value.map(_.getAbsolutePath).mkString(java.io.File.pathSeparator)
        IO.write(output, classpath)
        Seq(output)
      }.taskValue,
      Test / resourceGenerators += Def.task {
        val output    = (Test / resourceManaged).value / "instrument-economics-compiler.classpath"
        val classpath =
          instrumentEconomicsCompilerClasspath.value.map(_.getAbsolutePath).mkString(java.io.File.pathSeparator)
        IO.write(output, classpath)
        Seq(output)
      }.taskValue,
      libraryDependencies ++= Seq(
        "org.scala-lang" %% "scala3-compiler" % scala3Version % Test,
        "org.scalameta"  %% "munit"           % "1.3.5"       % Test
      )
    )
