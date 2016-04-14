# sbt-jeopardy

Plays the Jeopardy Theme music while scala compiles.


## Installation

I recommend adding the plugin globally so that every project of yours profits but it's not forced upon your contributors.
Unless you want that to be the case, of course :-)

To add it globally, place this in `~/.sbt/0.13/plugins/jeopardy.sbt`

```
resolvers += Resolver.bintrayRepo("knutwalker", "sbt-plugins")
addSbtPlugin("de.knutwalker" % "sbt-jeopardy"  % "0.1.0" )
```

Afterwards, run `compile` and profit.

## Tasks

This plugin defines three tasks:

##### *`jeopardyStartEndlessLoop`*

Plays the Jeopardy Theme forever until stopped. This is executed during `compile`.

##### *`jeopardyPlay`*

Plays the Jeopardy Theme once and then stops.


##### *`jeopardyStop`*

Stops the Jeopardy Theme if it is currently playing.
If, for some reason (aka bugs), the theme does not stop after compile has completed, you can use this task to stop the Theme manually.
