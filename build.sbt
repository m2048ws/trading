val scala3Version          = "3.8.4"
val catsVersion            = "2.13.0"
val algebraVersion         = "2.13.0"
val disciplineMunitVersion = "2.0.0"
val catsEffectVersion      = "3.7.0"
val munitCatsEffectVersion = "2.2.0"
val jdkRelease             = "25"

val staticDimensionCompilerClasspath =
  taskKey[Seq[File]]("Immutable classpath for real-source static-dimension compiler fixtures")
val applicationBoundaryClasspath =
  taskKey[Seq[File]]("Completed application artifact classpath without runtime dependencies")
val runtimeBoundaryClasspath =
  taskKey[Seq[File]]("Completed runtime artifact classpath with its concrete effect dependencies")
val referenceDataCompilerClasspath =
  taskKey[Seq[File]]("Immutable classpath for completed-JAR reference-data compiler fixtures")
val instrumentEconomicsCompilerClasspath =
  taskKey[Seq[File]]("Immutable classpath for completed-JAR instrument-economics compiler fixtures")
val riskCompilerClasspath =
  taskKey[Seq[File]]("Immutable classpath for completed-JAR risk compiler fixtures")
val orderModelCompilerClasspath =
  taskKey[Seq[File]]("Immutable classpath for completed-JAR order-model compiler fixtures")
val executionScenarioCompilerClasspath =
  taskKey[Seq[File]]("Immutable classpath for completed-JAR execution-scenario compiler fixtures")
val feePolicyCompilerClasspath =
  taskKey[Seq[File]]("Immutable classpath for completed-JAR fee-policy compiler fixtures")

ThisBuild / scalaVersion := scala3Version
ThisBuild / version      := "0.1.0-SNAPSHOT"
ThisBuild / javacOptions ++= Seq("--release", jdkRelease)
ThisBuild / scalacOptions ++= Seq("-release", jdkRelease)

lazy val root =
  project
    .in(file("."))
    .aggregate(
      quantities,
      referenceData,
      application,
      runtime,
      instrumentEconomics,
      risk,
      orderModel,
      executionScenario,
      feePolicy,
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
            runtime / Test / test,
            instrumentEconomics / Test / test,
            risk / Test / test,
            orderModel / Test / test,
            executionScenario / Test / test,
            feePolicy / Test / test,
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

      Test / classLoaderLayeringStrategy := ClassLoaderLayeringStrategy.Flat,

      libraryDependencies ++= Seq(
        "org.scalameta"  %% "munit"            % "1.3.5"  % Test,
        "org.scalacheck" %% "scalacheck"       % "1.20.0" % Test,
        "org.scalameta"  %% "munit-scalacheck" % "1.3.0"  % Test
      )
    )

lazy val feePolicy =
  project
    .in(file("fee-policy"))
    .dependsOn(quantities, instrumentEconomics, orderModel, executionScenario, risk % "test->compile")
    .settings(
      name       := "trading-fee-policy",
      moduleName := "trading-fee-policy",

      Compile / exportJars := true,

      Test / classLoaderLayeringStrategy := ClassLoaderLayeringStrategy.Flat,
      // The root clean gate rebuilds upstream artifacts between sequential module test tasks. Isolate this downstream
      // runner so it observes one completed dependency classpath instead of an in-process loader cached across builds.
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

lazy val risk =
  project
    .in(file("risk"))
    .dependsOn(quantities, instrumentEconomics % "compile->compile;test->test")
    .settings(
      name       := "trading-risk",
      moduleName := "trading-risk",

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

lazy val orderModel =
  project
    .in(file("order-model"))
    .dependsOn(quantities, instrumentEconomics % "compile->compile;test->test")
    .settings(
      name       := "trading-order-model",
      moduleName := "trading-order-model",

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

lazy val executionScenario =
  project
    .in(file("execution-scenario"))
    .dependsOn(instrumentEconomics % "compile->compile;test->test", orderModel)
    .settings(
      name       := "trading-execution-scenario",
      moduleName := "trading-execution-scenario",

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

lazy val runtime =
  project
    .in(file("runtime"))
    .dependsOn(application % "compile->compile;test->test", referenceData)
    .settings(
      name       := "trading-runtime",
      moduleName := "trading-runtime",

      Compile / exportJars := true,
      // Compile the JVM-privacy bridge before its Scala factory instead of relying on Scala metadata that emits a
      // public implementation class. Target the repository's minimum JDK explicitly for this Java-owned boundary.
      Compile / compileOrder := CompileOrder.JavaThenScala,
      Compile / javacOptions ++= Seq("--release", jdkRelease),

      Test / classLoaderLayeringStrategy := ClassLoaderLayeringStrategy.Flat,
      Test / fork                        := true,

      libraryDependencies ++= Seq(
        "org.typelevel" %% "cats-effect"         % catsEffectVersion,
        "org.typelevel" %% "cats-effect-testkit" % catsEffectVersion      % Test,
        "org.typelevel" %% "munit-cats-effect"   % munitCatsEffectVersion % Test
      )
    )

// Non-published performance evidence is compiled and run explicitly; it is intentionally outside root aggregation.
lazy val benchmarks =
  project
    .in(file("benchmarks"))
    .enablePlugins(JmhPlugin)
    .dependsOn(referenceData, application, runtime, risk)
    .settings(
      name           := "trading-benchmarks",
      publish / skip := true
    )

lazy val adversarialBoundary =
  project
    .in(file("adversarial-boundary"))
    .dependsOn(
      quantities,
      referenceData,
      application,
      runtime,
      instrumentEconomics,
      risk,
      orderModel,
      executionScenario,
      feePolicy
    )
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
          (runtime / Compile / exportedProducts).value.files ++
          (instrumentEconomics / Compile / exportedProducts).value.files ++
          (risk / Compile / exportedProducts).value.files ++
          (orderModel / Compile / exportedProducts).value.files ++
          (executionScenario / Compile / exportedProducts).value.files ++
          (feePolicy / Compile / exportedProducts).value.files
        val quantitiesDependencies    = (quantities / Compile / externalDependencyClasspath).value.files
        val referenceDataDependencies = (referenceData / Compile / externalDependencyClasspath).value.files
        val applicationDependencies   = (application / Compile / externalDependencyClasspath).value.files
        val runtimeDependencies       = (runtime / Compile / externalDependencyClasspath).value.files
        val instrumentDependencies    = (instrumentEconomics / Compile / externalDependencyClasspath).value.files
        val riskDependencies          = (risk / Compile / externalDependencyClasspath).value.files
        val orderDependencies         = (orderModel / Compile / externalDependencyClasspath).value.files
        val scenarioDependencies      = (executionScenario / Compile / externalDependencyClasspath).value.files
        val feePolicyDependencies     = (feePolicy / Compile / externalDependencyClasspath).value.files
        val compilerDependencies      = (Test / externalDependencyClasspath).value.files
        (moduleProducts ++ quantitiesDependencies ++ referenceDataDependencies ++ applicationDependencies ++
          runtimeDependencies ++ instrumentDependencies ++ orderDependencies ++ scenarioDependencies ++
          riskDependencies ++ feePolicyDependencies ++ compilerDependencies).distinct
      },
      applicationBoundaryClasspath := {
        val products = (quantities / Compile / exportedProducts).value.files ++
          (referenceData / Compile / exportedProducts).value.files ++
          (application / Compile / exportedProducts).value.files
        val dependencies = (quantities / Compile / externalDependencyClasspath).value.files ++
          (referenceData / Compile / externalDependencyClasspath).value.files ++
          (application / Compile / externalDependencyClasspath).value.files ++
          (Test / externalDependencyClasspath).value.files.filter { file =>
            val name = file.getName
            name.startsWith("scala3-compiler_3-") ||
            name.startsWith("scala3-interfaces-") ||
            name.startsWith("tasty-core_3-") ||
            name.startsWith("scala-asm-") ||
            name.startsWith("compiler-interface-") ||
            name.startsWith("util-interface-")
          }
        (products ++ dependencies).distinct
      },
      runtimeBoundaryClasspath := {
        val products =
          applicationBoundaryClasspath.value ++
            (runtime / Compile / exportedProducts).value.files
        val dependencies = (runtime / Compile / externalDependencyClasspath).value.files
        (products ++ dependencies).distinct
      },
      referenceDataCompilerClasspath := {
        val moduleProducts = (quantities / Compile / exportedProducts).value.files ++
          (referenceData / Compile / exportedProducts).value.files
        val quantitiesDependencies = (quantities / Compile / externalDependencyClasspath).value.files
        val referenceDependencies  = (referenceData / Compile / externalDependencyClasspath).value.files
        val compilerDependencies   = (Test / externalDependencyClasspath).value.files.filter { file =>
          val name = file.getName
          name.startsWith("scala3-compiler_3-") ||
          name.startsWith("scala3-interfaces-") ||
          name.startsWith("tasty-core_3-") ||
          name.startsWith("scala-asm-") ||
          name.startsWith("compiler-interface-") ||
          name.startsWith("util-interface-")
        }
        (moduleProducts ++ quantitiesDependencies ++ referenceDependencies ++ compilerDependencies).distinct
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
      riskCompilerClasspath := {
        val moduleProducts = (quantities / Compile / exportedProducts).value.files ++
          (referenceData / Compile / exportedProducts).value.files ++
          (instrumentEconomics / Compile / exportedProducts).value.files ++
          (risk / Compile / exportedProducts).value.files
        val quantitiesDependencies = (quantities / Compile / externalDependencyClasspath).value.files
        val referenceDependencies  = (referenceData / Compile / externalDependencyClasspath).value.files
        val instrumentDependencies = (instrumentEconomics / Compile / externalDependencyClasspath).value.files
        val riskDependencies       = (risk / Compile / externalDependencyClasspath).value.files
        val compilerDependencies   = (Test / externalDependencyClasspath).value.files.filter { file =>
          val name = file.getName
          name.startsWith("scala3-compiler_3-") ||
          name.startsWith("scala3-interfaces-") ||
          name.startsWith("tasty-core_3-") ||
          name.startsWith("scala-asm-") ||
          name.startsWith("compiler-interface-") ||
          name.startsWith("util-interface-")
        }
        (moduleProducts ++ quantitiesDependencies ++ referenceDependencies ++ instrumentDependencies ++
          riskDependencies ++ compilerDependencies).distinct
      },
      orderModelCompilerClasspath := {
        val moduleProducts = (quantities / Compile / exportedProducts).value.files ++
          (referenceData / Compile / exportedProducts).value.files ++
          (instrumentEconomics / Compile / exportedProducts).value.files ++
          (orderModel / Compile / exportedProducts).value.files
        val quantitiesDependencies = (quantities / Compile / externalDependencyClasspath).value.files
        val referenceDependencies  = (referenceData / Compile / externalDependencyClasspath).value.files
        val instrumentDependencies = (instrumentEconomics / Compile / externalDependencyClasspath).value.files
        val orderDependencies      = (orderModel / Compile / externalDependencyClasspath).value.files
        val compilerDependencies   = (Test / externalDependencyClasspath).value.files
        (moduleProducts ++ quantitiesDependencies ++ referenceDependencies ++ instrumentDependencies ++
          orderDependencies ++ compilerDependencies).distinct
      },
      executionScenarioCompilerClasspath := {
        val moduleProducts = (quantities / Compile / exportedProducts).value.files ++
          (referenceData / Compile / exportedProducts).value.files ++
          (instrumentEconomics / Compile / exportedProducts).value.files ++
          (orderModel / Compile / exportedProducts).value.files ++
          (executionScenario / Compile / exportedProducts).value.files
        val quantitiesDependencies = (quantities / Compile / externalDependencyClasspath).value.files
        val referenceDependencies  = (referenceData / Compile / externalDependencyClasspath).value.files
        val instrumentDependencies = (instrumentEconomics / Compile / externalDependencyClasspath).value.files
        val orderDependencies      = (orderModel / Compile / externalDependencyClasspath).value.files
        val scenarioDependencies   = (executionScenario / Compile / externalDependencyClasspath).value.files
        val compilerDependencies   = (Test / externalDependencyClasspath).value.files
        (moduleProducts ++ quantitiesDependencies ++ referenceDependencies ++ instrumentDependencies ++
          orderDependencies ++ scenarioDependencies ++ compilerDependencies).distinct
      },
      feePolicyCompilerClasspath := {
        val moduleProducts = (quantities / Compile / exportedProducts).value.files ++
          (referenceData / Compile / exportedProducts).value.files ++
          (instrumentEconomics / Compile / exportedProducts).value.files ++
          (orderModel / Compile / exportedProducts).value.files ++
          (executionScenario / Compile / exportedProducts).value.files ++
          (feePolicy / Compile / exportedProducts).value.files
        val quantitiesDependencies = (quantities / Compile / externalDependencyClasspath).value.files
        val referenceDependencies  = (referenceData / Compile / externalDependencyClasspath).value.files
        val instrumentDependencies = (instrumentEconomics / Compile / externalDependencyClasspath).value.files
        val orderDependencies      = (orderModel / Compile / externalDependencyClasspath).value.files
        val scenarioDependencies   = (executionScenario / Compile / externalDependencyClasspath).value.files
        val feePolicyDependencies  = (feePolicy / Compile / externalDependencyClasspath).value.files
        val compilerDependencies   = (Test / externalDependencyClasspath).value.files.filter { file =>
          val name = file.getName
          name.startsWith("scala3-compiler_3-") ||
          name.startsWith("scala3-interfaces-") ||
          name.startsWith("tasty-core_3-") ||
          name.startsWith("scala-asm-") ||
          name.startsWith("compiler-interface-") ||
          name.startsWith("util-interface-")
        }
        (moduleProducts ++ quantitiesDependencies ++ referenceDependencies ++ instrumentDependencies ++
          orderDependencies ++ scenarioDependencies ++ feePolicyDependencies ++ compilerDependencies).distinct
      },
      Test / resourceGenerators += Def.task {
        val directory = (Test / resourceManaged).value
        val outputs   = Seq(
          directory / "static-dimension-compiler.classpath" -> staticDimensionCompilerClasspath.value,
          directory / "application-boundary.classpath"      -> applicationBoundaryClasspath.value,
          directory / "runtime-boundary.classpath"          -> runtimeBoundaryClasspath.value
        )
        outputs.foreach { case (output, classpath) =>
          IO.write(output, classpath.map(_.getAbsolutePath).mkString(java.io.File.pathSeparator))
        }
        outputs.map(_._1)
      }.taskValue,
      Test / resourceGenerators += Def.task {
        val output    = (Test / resourceManaged).value / "instrument-economics-compiler.classpath"
        val classpath =
          instrumentEconomicsCompilerClasspath.value.map(_.getAbsolutePath).mkString(java.io.File.pathSeparator)
        IO.write(output, classpath)
        Seq(output)
      }.taskValue,
      Test / resourceGenerators += Def.task {
        val output    = (Test / resourceManaged).value / "reference-data-compiler.classpath"
        val classpath = referenceDataCompilerClasspath.value.map(_.getAbsolutePath).mkString(java.io.File.pathSeparator)
        IO.write(output, classpath)
        Seq(output)
      }.taskValue,
      Test / resourceGenerators += Def.task {
        val output    = (Test / resourceManaged).value / "order-model-compiler.classpath"
        val classpath = orderModelCompilerClasspath.value.map(_.getAbsolutePath).mkString(java.io.File.pathSeparator)
        IO.write(output, classpath)
        Seq(output)
      }.taskValue,
      Test / resourceGenerators += Def.task {
        val output    = (Test / resourceManaged).value / "risk-compiler.classpath"
        val classpath = riskCompilerClasspath.value.map(_.getAbsolutePath).mkString(java.io.File.pathSeparator)
        IO.write(output, classpath)
        Seq(output)
      }.taskValue,
      Test / resourceGenerators += Def.task {
        val output    = (Test / resourceManaged).value / "execution-scenario-compiler.classpath"
        val classpath =
          executionScenarioCompilerClasspath.value.map(_.getAbsolutePath).mkString(java.io.File.pathSeparator)
        IO.write(output, classpath)
        Seq(output)
      }.taskValue,
      Test / resourceGenerators += Def.task {
        val output    = (Test / resourceManaged).value / "fee-policy-compiler.classpath"
        val classpath = feePolicyCompilerClasspath.value.map(_.getAbsolutePath).mkString(java.io.File.pathSeparator)
        IO.write(output, classpath)
        Seq(output)
      }.taskValue,
      libraryDependencies ++= Seq(
        "org.scala-lang" %% "scala3-compiler" % scala3Version % Test,
        "org.scalameta"  %% "munit"           % "1.3.5"       % Test
      )
    )
