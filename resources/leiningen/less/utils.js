/* clojure-backed utilities for lessc */

importClass(Packages.clojure.lang.RT);
var slurp = RT['var']("clojure.core", "slurp");

function readFile(fname, cs) {
    return String(slurp['invoke'](fname));
}

function quit(code) {
    if (code != 0) {
        throw new Error('Exit ' + code);
    }
}
