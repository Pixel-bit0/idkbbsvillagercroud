# BBS Crowd Actors (add-on for BBS FS, Minecraft 1.20.1)

Adds a button to the BBS film editor's top bar, right next to the Replay editor button.

## Use
1. Open a film and select ONE actor (replay) in the replays list. It can have any form.
2. Click the new crowd button (person icon) next to the Replay editor button.
3. Set:
   - Number of actors (slider, 1-500)
   - Area width/depth, centered on where the selected actor stands at the start tick
   - Start tick (defaults to the film cursor) and Stop tick
   - Speed in blocks per second
   - Look X/Y/Z, or "Look at the camera's position"
4. Click OK. Every new actor is a copy of the selected one with its own keyframes:
   random running inside the area, then everyone turns toward the look spot at the stop tick and stays still.

Everything is normal keyframes, so edit them in the replay editor afterwards.
The selected actor itself is not changed.

## Build (GitHub, no install)
Upload this folder's contents to a GitHub repo (keep the .github folder). The "build" action
clones BBS FS, builds against it, and produces the crowdreplay jar under Actions > the run > Artifacts.
Put that jar in your mods folder next to BBS.

The workflow builds against the 1.20.1 branch of Wemppy4/bbs-fs. Change `ref:` in
.github/workflows/build.yml to match your BBS version if it differs.
