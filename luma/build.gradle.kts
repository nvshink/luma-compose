@file:Suppress("DEPRECATION")

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.android.kotlin.multiplatform.library)
  alias(libs.plugins.compose.multiplatform)
  alias(libs.plugins.compose.compiler)
  id("com.vanniktech.maven.publish")
}

kotlin {
  androidLibrary {
    namespace = "dev.luma.visuals"
    compileSdk = 36
    minSdk = 24

    compilerOptions {
      jvmTarget.set(JvmTarget.JVM_25)
    }
  }

  jvm("desktop") {
    compilerOptions {
      jvmTarget.set(JvmTarget.JVM_25)
    }
  }

  iosX64()
  iosArm64()
  iosSimulatorArm64()

  js(IR) {
    outputModuleName.set("luma-compose-visuals")
    browser()
    binaries.library()
  }

  sourceSets {
    commonMain {
      dependencies {
        implementation(compose.runtime)
        implementation(compose.ui)
        implementation(compose.foundation)
        implementation(compose.material3)
      }
    }

    commonTest {
      dependencies {
        implementation(kotlin("test"))
      }
    }

    androidMain {
      kotlin.srcDir("src/androidMain/kotlin")
    }
  }
}

group = providers.gradleProperty("GROUP").get()
version = providers.gradleProperty("VERSION_NAME").get()

val emptyJavadocJar by tasks.registering(Jar::class) {
  archiveClassifier.set("javadoc")
}

afterEvaluate {
  mavenPublishing {
    publishToMavenCentral()
    signAllPublications()
  }

  publishing {
    publications.withType<MavenPublication>().configureEach {
      val baseArtifactId = providers.gradleProperty("POM_ARTIFACT_ID").get()
      artifactId = when (name) {
        "kotlinMultiplatform" -> baseArtifactId
        "androidLibraryRelease" -> "$baseArtifactId-android"
        "desktop" -> "$baseArtifactId-desktop"
        else -> artifactId
      }

      if (name == "kotlinMultiplatform") {
        artifact(emptyJavadocJar)
      }

      pom {
        name.set(providers.gradleProperty("POM_NAME"))
        description.set(providers.gradleProperty("POM_DESCRIPTION"))
        url.set(providers.gradleProperty("POM_URL"))

        licenses {
          license {
            name.set(providers.gradleProperty("POM_LICENSE_NAME"))
            url.set(providers.gradleProperty("POM_LICENSE_URL"))
            distribution.set(providers.gradleProperty("POM_LICENSE_DIST"))
          }
        }

        developers {
          developer {
            id.set(providers.gradleProperty("POM_DEVELOPER_ID"))
            name.set(providers.gradleProperty("POM_DEVELOPER_NAME"))
            url.set(providers.gradleProperty("POM_DEVELOPER_URL"))
          }
        }

        scm {
          url.set(providers.gradleProperty("POM_SCM_URL"))
          connection.set(providers.gradleProperty("POM_SCM_CONNECTION"))
          developerConnection.set(providers.gradleProperty("POM_SCM_DEV_CONNECTION"))
        }
      }
    }

    repositories {
      maven {
        name = "projectLocal"
        url = uri(layout.buildDirectory.dir("repo"))
      }
    }
  }
}

dependencies {
}
