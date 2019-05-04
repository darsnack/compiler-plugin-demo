name := "compiler-plugin-demo5"

version := "0.1"

scalaVersion := "2.12.6"

autoCompilerPlugins := true

scalacOptions += "-Xplugin:./lib/compiler_plugin_demo4_2.12-0.1.jar"
scalacOptions += "-Xprint:parser,compiler-plugin-phase"
scalacOptions += "-Ybrowse:"