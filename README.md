# cljsc2
`cljsc2` let you use the StarCraft II AI API from Clojure using regular Clojure namespaced maps for interaction.

It generates clojure.spec specifications for all the endpoint the SC2 API exposes and serializes requests over a protobuf websocket connection to running instances of the game.

Implement an agent that plays the game by implementing a simple step function that:
 - Receives the state of the game as first argument
 - Receives a connection to the running game instance as the second argument.
 - Returns an action or list of actions to step forward in the game.
 - Returns `nil` to end the agents' run.

It visualizes the feature-layers that the API exposes for Machine Learning purposes through a ClojureScript app that draws the feature layers to canvas.

## Setup and running an agent

If you're new to StarCraft II or Clojure development and would like to use this library please don't be shy to ask for help by making an issue or sending me a mail at baruchberger@gmail.com.

- Install Clojure (for example via [leiningen](https://leiningen.org/)

- Install StarCraft II [the free starter edition will do!](http://us.battle.net/sc2/en/blog/3250656/starcraft-ii-starter-edition-8-3-2011)

Build the front-end clojurescript app for visualizing the feature-layers by running
   `lein figwheel`
   enter `exit` after it finishes loading dependencies and compiling the app.

Then to get an interactive development environment run:

    `lein repl`

Once that finishes loading we can compile clojure.specs from the SC2 API proto files:

`(load-file "src/cljsc2/clj/proto.clj")`

And to run a server that hosts the clojurescript app feature layer streams on http://localhost:3000 :

`(load-file "src/cljsc2/clj/web.clj")`

Visit the webpage at http://localhost:3000 which will be blank until we start running an agent, so lets do that!

`(load-file "src/cljsc2/clj/core.clj")`

Switch to the core namespace:
`(in-ns 'cljsc2.clj.core)`

If you're on OSX you can run
`(start)`

On windows/linux you need to pass the path to the StarCraft II executable.
`(start "path/to/SC2.exe")`

This will take a 5-15 seconds create a connection to a running StarCraft II session.

You can then load a game for example (this assumes you [have the map in your `Maps` directory](https://github.com/deepmind/pysc2#get-the-maps)):
`(load-mineral-game)`

As you can see the marines aren't moving and the game isn't running forward. That's because we need the game to issue commands and step through the game.
We can do that using an agent function. For example we can select the army and move them to a random direction every once in a while. Have a look at [the source for such a function](https://github.com/bbss/cljsc2/blob/fe6a96abea92a43913c6cb3845f55755cdc64f9f/src/cljsc2/clj/core.clj#L383)

To start running the step function:

``` clojure
(def running-loop
    (run-loop
     connection
     {:step-fn random-move-step
      :throttle-max-per-second 60
      :description "steps -> agent"}
     {:additional-steppers
      [cljsc2.clj.web/stepper]}))
```

To pause the stepping through:
`(s/close! (:incoming-step-observations-stream running-loop))`

To reload after time finished:
`(reset! counter 0)`
`(load-mineral-game)`

Then to start/resume stepping run the run-loop command again:

``` clojure
(def running-loop
    (run-loop
     connection
     {:step-fn random-move-step
      :throttle-max-per-second 60
      :description "steps -> agent"}
     {:additional-steppers
      [cljsc2.clj.web/stepper]}))
```

## Note
This library is in early development stage, if you find bugs let me know. It's not yet packaged into a nice typical maven library with nice usability.

## License

Copyright © 2017

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
