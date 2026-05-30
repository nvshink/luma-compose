plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.compose.multiplatform)
  alias(libs.plugins.compose.compiler)
}

kotlin {
  jvmToolchain(25)
}

dependencies {
  implementation(project(":visuals"))
  implementation(compose.desktop.currentOs)
  implementation(compose.material3)
}

compose.desktop {
  application {
    mainClass = "dev.luma.visuals.desktop.MainKt"
  }
}
