# sbt-frc
Build your First Robotics Competition robot code with [SBT](http://www.scala-sbt.org/)!

## Features
+ Dependency Management! Use Maven/Ivy repositories to grab dependencies from across the internet!
+ IDEs!
  + IntelliJ (with the Scala plugin)
  + Eclipse (with https://github.com/typesafehub/sbteclipse)
  + Emacs (https://github.com/ensime/ensime-emacs)
  + Atom (https://github.com/ensime/ensime-atom)
  + Sublime Text (https://github.com/ensime/ensime-sublime)
  + Vim (https://github.com/ensime/ensime-vim)!
  + The above text editors don't just do syntax highlighting, but also give code completion with ENSIME (https://github.com/ensime/ensime-sublime)
+ Extensible! There are [lots](https://github.com/sbt) of SBT plugins for all sorts of tasks such as style checking, unit testing, and building websites

## Installation
Create a new SBT project as per http://www.scala-sbt.org/0.13/docs/Directories.html.

Add the following lines to your `project/plugins.sbt`:
```scala
resolvers += "Funky-Repo" at "http://team846.github.io/repo"

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.13.0")

addSbtPlugin("com.lynbrookrobotics" % "sbt-frc" % "0.2.0.1-SNAPSHOT")
```

Enable the plugin in your `build.sbt` with:
```scala
enablePlugins(FRCPlugin)

teamNumber := #
```

## Robot Main Class
The SBT plugin will attempt to detect the main robot class, but if it cannot you can set the robot class by putting `robotClass := "your class here"` in your `build.sbt`.

## Disabling the Scala Standard Library
If your robot code is Java-only, you can choose to disable automatically adding a dependency on the Scala Standard Library by putting `autoScalaLibrary := false` in your `build.sbt`.
