# lein-less

A Leiningen plugin that compiles Less css files (see [lesscss.org](http://lesscss.org)) using the `less.js` compiler.

## Usage

Add `[lein-less "1.6.3"]` to the `:plugins` vector of your `project.clj` file.

`lein-less` will compile `.less` files found in your Add a `:less` map to your `project.clj` file, specifying `:source-paths` and `:target-path`:

```
  :less {:source-paths ["src/main/less"]
         :target-path "target/public/css"}
```

To compile `.less` files once:

```
lein less once
```

To continuously compile `.less` files whenever a file changes:

```
lein less auto
```

If you want less compilation to happen on regular lein targets (e.g. compile),
add `leiningen.less` into the `:hooks` vector of your `project.clj` file.

## License

Copyright © 2013 Montoux Ltd.

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
