(ns leiningen.less.io)

(defn readFile [^String filename ^String charset]
  (slurp filename))
