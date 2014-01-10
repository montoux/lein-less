# lein-less

A Leiningen plugin that compiles {less} css files (see http://lesscss.org).

## Usage

Put `[lein-less "0.1.0-SNAPSHOT"]` into the `:plugins` vector of your project.clj.

Add a `:less` map to your project.clj specifying `:source-paths` and `:target-path`.

To compile {less} files once:
    $ lein less once

To continuously compile {less} files whenever a source file changes:
    $ lein less auto

If you want less compilation to happen on regular lein targets (e.g. compile),
add `leiningen.less` into the `:hooks` vector of your project.clj.

## License

Copyright © 2013 Montoux Ltd.

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later
version.
