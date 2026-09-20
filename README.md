# Morph guesser

A guessing game built on the pixel-morph engine. A random photo from `SourceImages`
dissolves into a photo of a person from `TargetImage`, and everyone races to name
the person. The drawer's only move is picking which of three targets it becomes.

## Where this sits relative to the existing code

Nothing already in the repo was modified. `Main.java`, `animation/`, `src/`, and
`dataAggregregation/` still run as the original single-morph demo. The game lives in
four new packages:

| Package | What's in it |
| --- | --- |
| `morph/` | `MorphBuilder` (pixel assignment), `MorphAnimator` (physics), `MorphData` (wire format), `ImageField` (image normalisation) |
| `game/` | `RoundEngine` (the authoritative loop), `ImageLibrary` (both photo pools), `GuessChecker`, `Config`, `Player`, `Transport` |
| `net/` | `GameServer`, `Session`, `GameClient`, `Msg` (the protocol) |
| `ui/` | `GameWindow` and its panels |

Three pieces are rewrites of existing classes rather than new ideas:

- `MorphBuilder` is `TargetMaskAssigner.assignTargetsByColor` with the same scoring
  weights, moved onto primitive arrays, with claimed targets swap-removed from the
  candidate list and the per-pixel scan split across cores. Roughly 2x from the
  removal plus however many cores you have. Ties can resolve to a different target
  than before; nothing else about the output changes.
- `MorphAnimator` is `Start` without the statics, so a client can throw away one
  round and start another. `Util.pixels` being a single global was the main thing
  standing between the old code and multiple rounds.
- `MorphCanvasPanel` is `Grapher` as a `JPanel`. The `Canvas` plus `BufferStrategy`
  approach is fast but does not coexist with a Swing layout, and this needed a
  layout. Same technique inside: write ints into a `BufferedImage`'s backing array,
  then blit.

## Build and run

From the repository root, because the server resolves image folders relative to the
working directory:

```
javac -d out $(find . -path ./out -prune -o -name '*.java' -print)
java -cp out MorphGuess
```

On Windows:

```
javac -d out MorphGuess.java game\*.java morph\*.java net\*.java ui\*.java
java -cp out MorphGuess
```

The launcher asks for a name and whether to host or join. Hosting starts a server
inside the same process and then connects to it over loopback, so the host is an
ordinary player. Everyone else picks "Join a game" and types the host's LAN address.
Port 5757 by default.

`java -cp out MorphGuess --server` runs a headless server with no window, if you'd
rather keep one machine as a dedicated host.

Two players minimum. The server sits in the lobby and says so until a second one
connects.

## Rename your target files

A target's filename **is the answer**. `ImageLibrary` turns `kobe_bryant2.jpg` into
"Kobe Bryant" by stripping the extension, swapping underscores and hyphens for
spaces, dropping digits, and title-casing.

So `TargetImage/6aaede0902432_download-modified.jpg` currently becomes "Download
Modified", which nobody is going to guess. Rename the files in `TargetImage` after
whoever is in them before you demo this.

Multiple photos of the same person share an answer if their filenames agree:
`ritika1.jpg` and `ritika2.jpg` both become "Ritika" and count as one person with
two photos.

## Adding people mid-game

Bottom left: type a name, attach any number of photos, send. The photos go to the
server, so everyone gets them, and they land in `UserImages/<Name>/` as PNGs. They
persist across restarts.

The guarantee works by booking a deadline round. A person added during round 4 gets
assigned a round somewhere in 5 through 11, and when that round arrives *all three*
of the drawer's choices are photos of that person, so whichever they pick, that
person is the answer. Before the deadline they hold one of the three slots, which
lets the drawer resolve it early.

Deadlines are staggered so several people added at once each get their own round.
Add more than seven people inside one seven-round window and the overflow shares the
last round, which is the only case where the guarantee can slip. `Config.GUARANTEE_ROUNDS`
widens the window if that matters.

One social note: these are photos of real people going to everyone on the network
and staying on the host's disk. Fine among friends at a table, worth thinking about
before a public demo.

## Tuning

Everything is in `game/Config.java`.

`MAX_PIXELS` is the one that matters. The assignment costs about n²/2 scored pairs,
so 60000 is a few seconds on a laptop with several cores and noticeably more on two.
Halving it makes loading four times faster and the picture visibly coarser. The
loading bar exists because this is unavoidably slow; if the wait annoys people in
practice, this is the dial.

`MORPH_TIME_SCALE` sets how long the reveal takes. Careful here: the closing rate
goes as the *square* of the timestep, so 0.5 makes the morph four times longer than
1.0, not twice. 0.5 lands near fifty seconds inside a seventy-five second round.

## Scoring

A correct guess is worth 50 plus up to 150 scaled by time remaining, plus 25 for
being first. The drawer gets 20 per person who got it, so picking something
impossible is bad for them. Two letters of the answer are revealed as the clock runs
down, never more than two thirds of it.

## Not compiled

I wrote this without a JDK available, so none of it has been through `javac` and
none of it has been run. Expect to spend a few minutes on compile errors before it
starts. The places I'd look first:

- The protocol uses Java serialization, so server and clients must be built from the
  same source. A stale client against a fresh server fails at `readObject`.
- `MorphBuilder.bestParallel` allocates a handful of `Callable`s per source pixel.
  It should be fine next to the scoring work, but if loading is slower than expected,
  raising `PARALLEL_THRESHOLD` or dropping to a serial scan is the first thing to try.
- The Swing layout is fixed-proportion. It has a 880x600 minimum and I have not seen
  it at any size.
