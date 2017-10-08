# cljsc2
`cljsc2` let you use the StarCraft II AI API from Clojure using regular Clojure namespaced maps for interaction.

It generates clojure.spec specifications for all the endpoint the SC2 API exposes and serializes requests over a protobuf websocket connection to running instances of the game.

Implement an agent that plays the game by implementing a simple step function that:
 - Receives the state of the game as first argument
 - Receives a connection to the running game instance as the second argument.
 - Returns an action or list of actions to step forward in the game.
 - Returns `nil` to end the agents' run.

It visualizes the feature-layers that the API exposes for Machine Learning purposes through a ClojureScript app that draws the feature layers to canvas.

## Setup

If you're new to StarCraft II or Clojure development and would like to use this library please don't be shy to ask for help by making an issue or sending me a mail at baruchberger@gmail.com.

- Install Clojure (for example via [leiningen](https://leiningen.org/)

- Install StarCraft II [the free starter edition will do!](http://us.battle.net/sc2/en/blog/3250656/starcraft-ii-starter-edition-8-3-2011)

- Change the [project.clj](project.clj) to use the correct paths for the API proto files in the [:profiles :dev :env] path.

To get an interactive development environment run:

    lein figwheel

## Note
This library is in early development stage, if you find bugs let me know. It's not yet packaged into a nice typical maven library.

## License

Copyright © 2017

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
