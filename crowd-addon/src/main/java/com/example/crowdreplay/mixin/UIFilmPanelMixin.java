package com.example.crowdreplay.mixin;

import com.example.crowdreplay.CrowdPanel;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds the crowd button to the film editor's top bar, in the same slot as the camera editor and
 * replay editor buttons, so it sits right beside the replay editor button.
 */
@Mixin(value = UIFilmPanel.class, remap = false)
public abstract class UIFilmPanelMixin
{
    @Inject(method = "<init>", at = @At("TAIL"), remap = false)
    private void crowdreplay$addCrowdButton(UIDashboard dashboard, CallbackInfo ci)
    {
        UIFilmPanel panel = (UIFilmPanel) (Object) this;
        UIIcon crowd = new UIIcon(Icons.PLAYER, (b) -> CrowdPanel.open(panel));

        crowd.tooltip(IKey.raw("Crowd: make many actors from the selected one"));
        panel.actions().editor(crowd, () -> false);
    }
}
