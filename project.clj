(defproject cljsc2 "0.1.0"
  :description "A clojure API for the StarCraft II AI Framework."
  :url "https://github.com/bbss/cljsc2"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :min-lein-version "2.7.1"

  :dependencies [[org.clojure/clojure "1.9.0"]
                 ;[org.clojure/clojurescript "1.9.946"]
                 ;[fulcrologic/fulcro "2.5.8"]
                 [org.clojure/core.async  "0.3.443"]
                 [org.clojure/core.logic "0.8.11"]
                 ;;[expound "0.6.0"]
                 [org.clojars.ghaskins/protobuf "3.3.1-1"]
                 ;;[binaryage/chromex "0.5.15"]
                 ;;[cider/piggieback "0.3.4"]
                 [aleph "0.4.4"]
                 [im.chit/lucid.mind "1.3.13"]
                 [instaparse "1.4.7"]
                 ;[org.clojure/test.check "0.10.0-alpha3"]
                 ;[hawk "0.2.11"]
                 [im.chit/hara.string.case "2.5.10"]
                 ;[im.chit/hara.zip "2.5.10"]]
                 [me.raynes/conch "0.8.0"]
                 [com.grammarly/perseverance "0.1.2"]
                 ;[com.cognitect/transit-cljs "0.8.243"]
                 [manifold "0.1.7-alpha5"]
                 [byte-streams "0.2.4-alpha3"]
                 ;;[cljsjs/d3 "4.12.0-0"]
                 [com.taoensso/nippy "2.14.0-alpha1"]
                 [com.taoensso/sente "1.12.0"]
                 [environ "1.1.0"]
                 [datascript "0.16.2"]
                 [net.mikera/telegenic "0.0.1"]
                 ;;[stylefruits/gniazdo "1.0.1"]
                 ;[thinktopic/think.image "0.4.17-SNAPSHOT"]]
                 ;;[http-kit "2.3.0-alpha4"]
                 [datascript-transit "0.2.2"]
                 [byte-transforms "0.1.5-alpha1"]
                 ;;[ring "1.6.3"]]
                 ;;[ring/ring-defaults "0.3.1"]]
                 ;;[compojure "1.5.1"]
                 ;;[clojupyter "0.2.1-SNAPSHOT" :exclusions [org.clojure/tools.reader]]
                 [lambda-ml "0.1.1"]]

  :plugins [[lein-environ "1.1.0"]]

  :java-source-paths ["SC2APIProtocol"]

  :profiles {:dev {:env {:proto-grammar "resources/proto.ebnf"
                         :proto-dir "resources/s2clientprotocol/"}
                   :source-paths ["src/cljsc2"]}
             :uberjar {:aot :all
                       :env {:proto-grammar "resources/proto.ebnf"
                             :proto-dir "resources/s2clientprotocol/"}}})
                   ;; for CIDER
                   ;;:plugins [[cider/cider-nrepl "0.16.0"]]
                   ;:repl-options {:nrepl-middleware [cider.piggieback/wrap-cljs-repl]}}})
                   ;; need to add the compliled assets to the :clean-targets
                   ;:clean-targets ^{:protect false} ["/usr/local/opt/python/Frameworks/Python.framework/Versions/Current/lib/python2.7/site-packages/notebook/static/notebook/js/compiled"
                   ;                                  "resources/unpacked/compiled/"
                   ;                                  :target-path]}})

  ;:jvm-opts ["--add-modules" "java.xml.bind"]) ;;for java9

