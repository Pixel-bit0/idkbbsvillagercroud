package com.example.crowdreplay;

import mchorse.bbs_mod.camera.Camera;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UISliderTrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIConfirmOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.utils.UI;

import java.util.Random;

/**
 * Makes a crowd of actors out of the selected one. Every new actor is a copy of it (same form,
 * same settings) with its own position and rotation keyframes: random running inside an area
 * until the stop tick, then everyone turns to face one spot and stays there.
 *
 * Run and stop are ordinary keyframes, so they can be edited afterwards like any others.
 */
public class CrowdPanel extends UIConfirmOverlayPanel
{
    /* Remembered between openings */
    private static double lastCount = 30;
    private static double lastSizeX = 20;
    private static double lastSizeZ = 20;
    private static double lastStopAfter = 100;
    private static double lastSpeed = 4;
    private static boolean lastLook = true;

    private static final double EYE_HEIGHT = 1.62;
    private static final int TURN_TICKS = 10;

    private final UIFilmPanel panel;
    private final Replay template;

    private final UISliderTrackpad count;
    private final UITrackpad sizeX;
    private final UITrackpad sizeZ;
    private final UITrackpad startTick;
    private final UITrackpad stopTick;
    private final UITrackpad speed;
    private final UITrackpad lookX;
    private final UITrackpad lookY;
    private final UITrackpad lookZ;
    private final UIToggle look;

    public static void open(UIFilmPanel panel)
    {
        Film film = panel.getData();
        Replay replay = panel.replayEditor.getReplay();

        if (film == null || replay == null)
        {
            panel.getContext().notifyError(IKey.raw("Open a film and select an actor (replay) first"));

            return;
        }

        UIOverlay.addOverlay(panel.getContext(), new CrowdPanel(panel, replay), 330, 300);
    }

    public CrowdPanel(UIFilmPanel panel, Replay template)
    {
        super(IKey.raw("Crowd from the selected actor"), IKey.EMPTY, null);

        this.panel = panel;
        this.template = template;
        this.callback = (ok) ->
        {
            if (ok)
            {
                this.generate();
            }
        };

        this.message.setVisible(false);

        int cursor = panel.getCursor();
        Camera camera = panel.getCamera();

        this.count = new UISliderTrackpad();
        this.count.limit(1, 500, true);
        this.count.setValue(lastCount);

        this.sizeX = new UITrackpad().limit(1, 1000);
        this.sizeX.forcedLabel(IKey.raw("Area width (X)"));
        this.sizeX.setValue(lastSizeX);

        this.sizeZ = new UITrackpad().limit(1, 1000);
        this.sizeZ.forcedLabel(IKey.raw("Area depth (Z)"));
        this.sizeZ.setValue(lastSizeZ);

        this.startTick = new UITrackpad().limit(0, 1000000, true);
        this.startTick.forcedLabel(IKey.raw("Start tick"));
        this.startTick.setValue(cursor);

        this.stopTick = new UITrackpad().limit(1, 1000000, true);
        this.stopTick.forcedLabel(IKey.raw("Stop tick"));
        this.stopTick.setValue(cursor + lastStopAfter);

        this.speed = new UITrackpad().limit(0.5, 30);
        this.speed.forcedLabel(IKey.raw("Speed (blocks/sec)"));
        this.speed.setValue(lastSpeed);

        this.lookX = new UITrackpad().limit(-30000000, 30000000);
        this.lookX.forcedLabel(IKey.raw("Look X"));
        this.lookX.setValue(camera.position.x);

        this.lookY = new UITrackpad().limit(-30000000, 30000000);
        this.lookY.forcedLabel(IKey.raw("Look Y"));
        this.lookY.setValue(camera.position.y);

        this.lookZ = new UITrackpad().limit(-30000000, 30000000);
        this.lookZ.forcedLabel(IKey.raw("Look Z"));
        this.lookZ.setValue(camera.position.z);

        UIButton useCamera = new UIButton(IKey.raw("Look at the camera's position"), (b) ->
        {
            Camera current = this.panel.getCamera();

            this.lookX.setValue(current.position.x);
            this.lookY.setValue(current.position.y);
            this.lookZ.setValue(current.position.z);
        });

        this.look = new UIToggle(IKey.raw("Everyone turns to look there when they stop"), lastLook, (b) -> {});

        UIElement column = UI.column(4, 0,
            UI.label(IKey.raw("Number of actors")),
            this.count,
            UI.row(5, 0, this.sizeX, this.sizeZ),
            UI.row(5, 0, this.startTick, this.stopTick),
            this.speed,
            UI.row(5, 0, this.lookX, this.lookY, this.lookZ),
            useCamera,
            this.look
        );

        column.relative(this.content).xy(6, 6).w(1F, -12).h(1F, -40);

        this.confirm.w(1F, -10);
        this.content.add(column);
    }

    private void generate()
    {
        Film film = this.panel.getData();

        if (film == null)
        {
            return;
        }

        int amount = (int) Math.round(this.count.getValue());
        int t0 = (int) Math.round(this.startTick.getValue());
        int t1 = Math.max(t0 + 1, (int) Math.round(this.stopTick.getValue()));
        double halfX = this.sizeX.getValue() / 2D;
        double halfZ = this.sizeZ.getValue() / 2D;
        double perTick = this.speed.getValue() / 20D;
        boolean lookAtTarget = this.look.getValue();
        double lx = this.lookX.getValue();
        double ly = this.lookY.getValue();
        double lz = this.lookZ.getValue();

        lastCount = amount;
        lastSizeX = this.sizeX.getValue();
        lastSizeZ = this.sizeZ.getValue();
        lastStopAfter = t1 - t0;
        lastSpeed = this.speed.getValue();
        lastLook = lookAtTarget;

        /* The area is centered on where the selected actor stands at the start tick */
        double cx = this.template.keyframes.x.interpolate(t0, 0D);
        double cy = this.template.keyframes.y.interpolate(t0, 0D);
        double cz = this.template.keyframes.z.interpolate(t0, 0D);

        Random random = new Random();
        Replay last = null;

        for (int i = 0; i < amount; i++)
        {
            Replay replay = film.replays.addReplay();

            replay.copy(this.template);

            replay.keyframes.x.removeAll();
            replay.keyframes.y.removeAll();
            replay.keyframes.z.removeAll();
            replay.keyframes.yaw.removeAll();
            replay.keyframes.pitch.removeAll();
            replay.keyframes.headYaw.removeAll();
            replay.keyframes.bodyYaw.removeAll();

            double x = cx + (random.nextDouble() * 2 - 1) * halfX;
            double z = cz + (random.nextDouble() * 2 - 1) * halfZ;
            double heading = random.nextDouble() * 360 - 180;
            int t = t0;

            this.position(replay, t, x, cy, z);
            this.rotation(replay, t, heading, 0);

            int attempts = 0;

            while (t < t1 && attempts < 200)
            {
                attempts += 1;

                double tx = cx + (random.nextDouble() * 2 - 1) * halfX;
                double tz = cz + (random.nextDouble() * 2 - 1) * halfZ;
                double dx = tx - x;
                double dz = tz - z;
                double distance = Math.hypot(dx, dz);

                if (distance < 0.5)
                {
                    continue;
                }

                heading = unwrap(heading, Math.toDegrees(Math.atan2(-dx, dz)));

                int duration = (int) Math.max(1, Math.ceil(distance / perTick));
                int end = t + duration;

                if (end > t1)
                {
                    double fraction = (double) (t1 - t) / duration;

                    tx = x + dx * fraction;
                    tz = z + dz * fraction;
                    end = t1;
                }

                /* Face the way it is going for the whole walk */
                this.rotation(replay, t, heading, 0);
                this.rotation(replay, end, heading, 0);
                this.position(replay, end, tx, cy, tz);

                x = tx;
                z = tz;
                t = end;

                /* Now and then stand around for a moment */
                if (t < t1 && random.nextInt(4) == 0)
                {
                    t = Math.min(t + 10 + random.nextInt(30), t1);

                    this.position(replay, t, x, cy, z);
                    this.rotation(replay, t, heading, 0);
                }
            }

            /* Stopped: turn to face the target and stay there */
            if (lookAtTarget)
            {
                double dx = lx - x;
                double dz = lz - z;
                double yaw = unwrap(heading, Math.toDegrees(Math.atan2(-dx, dz)));
                double pitch = -Math.toDegrees(Math.atan2(ly - (cy + EYE_HEIGHT), Math.hypot(dx, dz)));

                this.rotation(replay, t1 + TURN_TICKS, yaw, pitch);
            }

            last = replay;
        }

        if (last != null)
        {
            this.panel.replayEditor.replaysList.replays.refreshReplayList();
            this.panel.replayEditor.replaysList.replays.update();
            this.panel.replayEditor.setReplay(last);
            this.panel.getController().createEntities();
            this.panel.replayEditor.updateChannelsList();
        }
    }

    private void position(Replay replay, int tick, double x, double y, double z)
    {
        replay.keyframes.x.insert(tick, x);
        replay.keyframes.y.insert(tick, y);
        replay.keyframes.z.insert(tick, z);
    }

    private void rotation(Replay replay, int tick, double yaw, double pitch)
    {
        replay.keyframes.yaw.insert(tick, yaw);
        replay.keyframes.headYaw.insert(tick, yaw);
        replay.keyframes.bodyYaw.insert(tick, yaw);
        replay.keyframes.pitch.insert(tick, pitch);
    }

    /** The same angle as {@code to}, shifted by full turns so it lies within half a turn of {@code from}. */
    private static double unwrap(double from, double to)
    {
        while (to - from > 180)
        {
            to -= 360;
        }

        while (to - from < -180)
        {
            to += 360;
        }

        return to;
    }
}
