(defproject password-helper "0.1.0-SNAPSHOT"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.9.946"]
                 [org.clojure/core.async "0.4.474"]
                 [khroma "0.3.0"]
                 [hipo "0.5.2"]
                 [etaoin "0.2.4"]
                 [doo "0.1.8"]
                 [reagent "0.8.0-alpha2"]
                 [prismatic/dommy "1.1.0"]]
  :source-paths ["src"]
  :profiles {:dev
             {:plugins [[com.cemerick/austin "0.1.6"]
                        [lein-cljsbuild "1.1.7"]
                        [lein-eftest "0.4.2"]
                        [lein-doo "0.1.8"]
                        [lein-figwheel "0.5.14"]
                        [lein-chromebuild "0.3.0"]]

              :cljsbuild {:builds {
                                   :main
                                   {:source-paths ["src"]
                                    :compiler {:output-to "target/unpacked/password_helper.js"
                                               :output-dir "target/unpacked"
                                               :optimizations :whitespace
                                               :source-map "target/unpacked/password_helper.js.map"
                                               :closure-output-charset "US-ASCII"
                                               :pretty-print true}}

                                   :reloadable
                                   {:source-paths ["src"]
                                    :figwheel true
                                    :compiler {:output-to "target/reloadable/password_helper.js"
                                               :output-dir "target/reloadable"
                                               :asset-path "../target/reloadable"
                                               :main password-helper.content-start
                                               :source-map true
                                               :closure-output-charset "US-ASCII"
                                               :pretty-print true}}

                                   :test
                                   {:source-paths ["src" "test"]
                                    :compiler {:output-to "target/test/password_helper_test.js"
                                               :output-dir "target/test"
                                               :main password-helper.runner
                                               :optimizations :whitespace
                                               :source-map "target/test/password_helper_test.js.map"
                                               :closure-output-charset "US-ASCII"
                                               :pretty-print true}}}}

              :figwheel {:css-dirs ["resources/css"]}

              :chromebuild {:resource-paths ["resources/js" "resources/css" "resources/images"]
                            :target-path "target/unpacked"}

              :eftest {:multithread? true}
              }})
