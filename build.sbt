name := "compiler-plugin-demo5"

version := "0.1"

scalaVersion := "2.12.8"

autoCompilerPlugins := true

scalacOptions += "-Xplugin:/media/data/Projects/compiler-plugin-demo5/lib/compiler_plugin_demo4_2.12-0.1.jar"