(defproject cljsc2 "0.1.0-SNAPSHOT"
  :description "FIXME: write this!"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :min-lein-version "2.7.1"

  :dependencies [[org.clojure/clojure "1.9.0-beta3"]
                 [org.clojure/clojurescript "1.9.946"]
                 [org.clojure/core.async  "0.3.443"
                  :exclusions [org.clojure/tools.reader]]
                 [org.clojure/core.logic "0.8.11"]
                 [org.clojars.ghaskins/protobuf "3.3.1-1"]
                 [aleph "0.4.4"]
                 [im.chit/lucid.mind "1.3.13"]
                 [instaparse "1.4.7"]
                 [org.clojure/test.check "0.10.0-alpha2"]
                 [im.chit/hara.string.case "2.5.10"]
                 [im.chit/hara.zip "2.5.10"]
                 [me.raynes/conch "0.8.0"]
                 [com.grammarly/perseverance "0.1.2"]
                 [com.cognitect/transit-cljs "0.8.239"]
                 [cljs-ajax "0.7.2"]
                 [yada "1.2.9" :exclusions [[aleph]]]
                 [manifold "0.1.7-alpha5"]
                 [byte-streams "0.2.4-alpha3"]
                 [cljsjs/d3 "4.3.0-5"]
                 [thinktopic/cortex "0.9.22"]
                 [com.taoensso/nippy "2.14.0-alpha1"]
                 [environ "1.1.0"]
                 [datascript "0.16.2"]
                 [net.mikera/telegenic "0.0.1"]
                 [stylefruits/gniazdo "1.0.1"]
                 [thinktopic/think.image "0.4.17-SNAPSHOT"]
                 [http-kit "2.3.0-alpha4"]
                 [datascript-transit "0.2.2"]
                 [byte-transforms "0.1.5-alpha1"]]

  :plugins [[lein-figwheel "0.5.12"]
            [lein-cljsbuild "1.1.7" :exclusions [[org.clojure/clojure]]]
            [lein-environ "1.1.0"]
            [lein-jupyter "0.1.8"]]

  :java-source-paths ["SC2APIProtocol"]

  #_:jvm-opts #_["--add-modules" "java.xml.bind"] ;;for java9

  :cljsbuild {:builds
              [{:id "dev"
                :source-paths ["src/cljsc2/cljs"]

                ;; The presence of a :figwheel configuration here
                ;; will cause figwheel to inject the figwheel client
                ;; into your build
                :figwheel {:on-jsload "cljsc2.cljs.core/on-js-reload"
                           ;; :open-urls will pop open your application
                           ;; in the default browser once Figwheel has
                           ;; started and compiled your application.
                           ;; Comment this out once it no longer serves you.
                           ;; :open-urls ["http://localhost:3449/index.html"]
                           }

                :compiler {:main cljsc2.cljs.core
                           :asset-path "js/compiled/out"
                           :output-to "resources/public/js/compiled/cljsc2.js"
                           :output-dir "resources/public/js/compiled/out"
                           :source-map-timestamp true
                           ;; To console.log CLJS data-structures make sure you enable devtools in Chrome
                           ;; https://github.com/binaryage/cljs-devtools
                           :preloads [devtools.preload]}}
               ;; This next build is a compressed minified build for
               ;; production. You can build this with:
               ;; lein cljsbuild once min
               {:id "min"
                :source-paths ["src/cljsc2/cljs"]
                :compiler {:output-to "resources/public/js/compiled/cljsc2.js"
                           :main cljsc2.cljs.core
                           :optimizations :advanced
                           :pretty-print false}}]}

  :figwheel {;; :http-server-root "public" ;; default and assumes "resources"
             ;; :server-port 3449 ;; default
             ;; :server-ip "127.0.0.1"

             :css-dirs ["resources/public/css"] ;; watch and update CSS

             ;; Start an nREPL server into the running figwheel process
             ;; :nrepl-port 7888

             ;; Server Ring Handler (optional)
             ;; if you want to embed a ring handler into the figwheel http-kit
             ;; server, this is for simple ring servers, if this

             ;; doesn't work for you just run your own server :) (see lein-ring)

             ;; :ring-handler hello_world.server/handler

             ;; To be able to open files in your editor from the heads up display
             ;; you will need to put a script on your path.
             ;; that script will have to take a file path and a line number
             ;; ie. in  ~/bin/myfile-opener
             ;; #! /bin/sh
             ;; emacsclient -n +$2 $1
             ;;
             ;; :open-file-command "myfile-opener"

             ;; if you are using emacsclient you can just use
             ;; :open-file-command "emacsclient"

             ;; if you want to disable the REPL
             ;; :repl false

             ;; to configure a different figwheel logfile path
             ;; :server-logfile "tmp/logs/figwheel-logfile.log"

             ;; to pipe all the output to the repl
             ;; :server-logfile false
             }


  ;; Setting up nREPL for Figwheel and ClojureScript dev
  ;; Please see:
  ;; https://github.com/bhauman/lein-figwheel/wiki/Using-the-Figwheel-REPL-within-NRepl
  :profiles {:dev {:env {:proto-grammar "resources/proto.ebnf"
                         :proto-dir "resources/s2clientprotocol/"}
                   :dependencies [[binaryage/devtools "0.9.4"]
                                  [figwheel-sidecar "0.5.12"]
                                  [com.cemerick/piggieback "0.2.2"]]
                   ;; need to add dev source path here to get user.clj loaded
                   :source-paths ["src/cljsc2/cljs"
                                  "dev"]
                   ;; for CIDER
                   ;; :plugins [[cider/cider-nrepl "0.12.0"]]
                   :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}
                   ;; need to add the compliled assets to the :clean-targets
                   :clean-targets ^{:protect false} ["resources/public/js/compiled"
                                                     :target-path]}})
