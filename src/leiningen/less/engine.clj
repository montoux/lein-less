(ns leiningen.less.engine
  (:require [clojure.java.io :as jio])
  (:import (javax.script ScriptEngineManager ScriptEngine ScriptContext Bindings ScriptException)
           (java.nio.file Path)
           (java.io File Reader)
           (leiningen.less LessError)
           (java.util Map)))


(def ^:private ^ScriptEngineManager engine-manager (ScriptEngineManager.))


(def ^:dynamic ^:private ^ScriptEngine *engine* nil)


(defn create-engine
  "Create a new script engine for the specified name. E.g. rhino, nashorn."
  ([] (create-engine "javascript"))
  ([^String engine-type]
   (.getEngineByName engine-manager engine-type)))


(defn with-engine* [engine-param body-fn]
  (let [engine (if (string? engine-param) (create-engine engine-param) engine-param)]
    (when-not (instance? ScriptEngine engine)
      (throw (IllegalArgumentException. (str "Invalid javascript engine (" engine-param ")"))))
    (binding [*engine* engine]
      (body-fn))))

(defmacro with-engine
  "Run the specified body expressions on the provided javascript engine."
  [engine & body]
  `(with-engine* ~engine (fn [] ~@body)))


(defmacro ^:private check-engine []
  `(when-not *engine*
     (throw (IllegalStateException. "eval! must be called from within a `(with-engine ..)` expression."))))


(defn throwable? [e]
  (when (instance? Throwable e)
    e))


(def ^:private get-class
  (memoize (fn [class-name]
             (try (Class/forName class-name)
                  (catch ClassNotFoundException _ nil)))))


(defmacro dynamic-instance?
  "Given a class name, attempts a dynamic lookup of the class, and if found does an instance? test against the object."
  [class-name obj]
  `(some-> (get-class ~class-name) (instance? ~obj)))


(defn error!
  "Conservative error handling for JS eval function:
    * handles error structures passed directly to this function from JS and throws as LessExceptions
    * handles unwrapping Java exceptions that have been wrapped by the JS VM
   Uses dynamic class resolution to avoid explicit dependencies JVM-internal classes.
   "
  [error message]
  (let [cause (when (throwable? error) (.getCause ^Throwable error))]
    (cond
      (not (throwable? error))
      (throw (LessError. (str message) nil))

      (instance? LessError error)
      (throw error)

      (instance? ScriptException error)
      (if cause (recur cause message)
                (throw (LessError. (str message) error)))

      (or (dynamic-instance? "jdk.nashorn.api.scripting.NashornException" error)
          (dynamic-instance? "sun.org.mozilla.javascript.internal.JavaScriptException" error))
      (throw (LessError. (str message) error))

      (or (dynamic-instance? "org.mozilla.javascript.WrappedException" error)
          (dynamic-instance? "sun.org.mozilla.javascript.internal.WrappedException" error))
      (recur (.getWrappedException error) message)

      :default (throw error)
      )))


(defn eval!
  "Evaluate the specified string or resource. Must be called from within a `(with-engine ..)` expression."
  ([^String js-expression]
   (check-engine)
   (try
     (.eval *engine* js-expression)
     (catch Exception ex
            (error! ex (.getMessage ex)))))
  ([resource ^String resource-name]
   (check-engine)
   (let [^Reader reader (jio/reader resource)
         ^Bindings bindings (.getBindings *engine* ScriptContext/ENGINE_SCOPE)]
     (try
       (when resource-name (.put bindings ScriptEngine/FILENAME (str resource-name)))
       (.eval *engine* reader)
       (catch Exception ex
              (error! ex (.getMessage ex)))
       (finally (.remove bindings ScriptEngine/FILENAME))))))
