# UJWA – XML processing experiments for the Uwe Johnson-Werkausgabe

<img src="https://upload.wikimedia.org/wikipedia/commons/thumb/6/66/GuestrowUweJohnsonDetail.jpg/800px-GuestrowUweJohnsonDetail.jpg"
     title="Detail der Skulptur Uwe Johnsons von Wieland Förster vor dem John-Brinckman-Gymnasium in Güstrow by MrsMyer / Wikimedia Commons / CC-BY-SA-3.0"
     alt="Detail der Skulptur Uwe Johnsons von Wieland Förster vor dem John-Brinckman-Gymnasium in Güstrow by MrsMyer / Wikimedia Commons / CC-BY-SA-3.0"
     width="250"
     align="right">

The experiments in this project have been created in preparation for a workshop
on workflow challenges encountered by the [Rostocker
Ausgabe](http://www.uwe-johnson-werkausgabe.de/) of [Uwe
Johnson's](https://de.wikipedia.org/wiki/Uwe_Johnson) collected works. They are
published mostly for reference and – in their current state – only serve as
proofs of concept.

## Getting Started

### Prerequisites

In order to run the experiments, you need

* [Java (v8 or later)](https://jdk.java.net/)
* [Clojure (v1.10 or later)](https://clojure.org/guides/getting_started)

### Run

Given an excerpt from the UJWA in `excerpt.xml`, you can extract individual
witnesses via

```
$ clojure --report stderr -m ujwa.main excerpt.xml
```

The resulting XML and plain-text files are written to `data/`.

## License

Copyright &copy; 2020 Gregor Middell.

This project is licensed under the GNU General Public License v3.0.
