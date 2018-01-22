# cljsc2
`cljsc2` let you use the StarCraft II AI API from Clojure using regular Clojure namespaced maps for interaction.

It generates clojure.spec specifications for all the endpoint the SC2 API exposes and serializes requests over a protobuf websocket connection to running instances of the game.

Jupyter notebook example:
https://bbss.github.io/cljsc2/

## Setup and running an agent

If you're new to StarCraft II or Clojure development and would like to use this library please don't be shy to ask for help by making an issue or sending me a mail at baruchberger@gmail.com.

- Install Clojure (for example via [leiningen](https://leiningen.org/))

- [Install StarCraft II](https://eu.shop.battle.net/en-gb/family/starcraft-ii)

See jupyter notebook for usage example:
https://bbss.github.io/cljsc2/

Agents need to implement a simple step function that:
 - Receives the state of the game as first argument
 - Receives a connection to the running game instance as the second argument.
 - Returns an action or list of actions to step forward in the game.
 - Returns `nil` to end the agents' run.

The feature-layers that the API exposes for Machine Learning purposes can be displayed through a ClojureScript app that draws the feature layers to canvas, see code [here](https://github.com/bbss/cljsc2/blob/master/src/cljsc2/cljs/core.cljs).


## Note
This library is in early development stage, if you find bugs let me know. It's not yet packaged into a nice typical maven library with nice usability.

## License

Copyright © 2018

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
