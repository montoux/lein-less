(ns leiningen.less.engine
  (:require [clojure.java.io :as jio])
  (:import (javax.script ScriptEngineManager ScriptEngine ScriptContext Bindings)
           (java.nio.file Path)
           (java.io File Reader)))


(def ^ScriptEngine default-engine (.getEngineByName (ScriptEngineManager.) "javascript"))


(defn eval!
  "Evaluate a resource in the provided (or default) javascript engine. Returns the result.
  Engine will be modified as a sideeffect."
  ([resource]
   (eval! default-engine resource))
  ([^ScriptEngine engine resource]
   (if (string? resource)
     (.eval engine ^String resource)
     (eval! engine resource (str resource))))
  ([^ScriptEngine engine ^Reader reader resource-name]
   (let [^Bindings bindings (.getBindings engine ScriptContext/ENGINE_SCOPE)]
     (try
       (when resource-name (.put bindings ScriptEngine/FILENAME (str resource-name)))
       (.eval engine reader)
       (finally
         (.remove bindings ScriptEngine/FILENAME)
         )))))
