package zmaster587.advancedRocketry.integration.theoneprobe;

import mcjty.theoneprobe.api.IEntityDisplayOverride;
import mcjty.theoneprobe.api.IProbeHitEntityData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.ProbeMode;
import mcjty.theoneprobe.api.TextStyleClass;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import zmaster587.advancedRocketry.api.dimension.solar.StellarBody;
import zmaster587.advancedRocketry.dimension.DimensionProperties;
import zmaster587.advancedRocketry.entity.EntityUIButton;
import zmaster587.advancedRocketry.entity.EntityUIPlanet;
import zmaster587.advancedRocketry.entity.EntityUIStar;

public class HolographicBodyDisplayOverride implements IEntityDisplayOverride {

    @Override
    public boolean overrideStandardInfo(
            ProbeMode mode,
            IProbeInfo probeInfo,
            EntityPlayer player,
            World world,
            Entity entity,
            IProbeHitEntityData data) {

        /*
         EntityUIButton extends EntityUIPlanet.
         This must be checked first or its button ID, currently 0,
         is resolved as planet/dimension 0: Earth.
         */
        if (entity instanceof EntityUIButton) {
            addHeader(
                    probeInfo,
                    tr("msg.top.advancedrocketry.hologram.upLevel")
            );
            return true;
        }

        /*
         * EntityUIStar also extends EntityUIPlanet.
         * Stars must therefore be checked before planets.
         */
        if (entity instanceof EntityUIStar) {
            StellarBody star =
                    ((EntityUIStar) entity).getStarProperties();

            if (star == null || isBlank(star.getName())) {
                return false;
            }

            addHeader(probeInfo, star.getName());
            addEntry(
                    probeInfo,
                    "msg.top.advancedrocketry.hologram.type",
                    "msg.top.advancedrocketry.hologram.type.star"
            );
            addNumberEntry(
                    probeInfo,
                    "msg.top.advancedrocketry.hologram.planets",
                    star.getNumPlanets()
            );

            return true;
        }

        if (entity instanceof EntityUIPlanet) {
            DimensionProperties properties =
                    ((EntityUIPlanet) entity).getProperties();

            if (properties == null || isBlank(properties.getName())) {
                return false;
            }

            addHeader(probeInfo, properties.getName());

            boolean moon = properties.isMoon();

            addEntry(
                    probeInfo,
                    "msg.top.advancedrocketry.hologram.type",
                    moon
                            ? "msg.top.advancedrocketry.hologram.type.moon"
                            : "msg.top.advancedrocketry.hologram.type.planet"
            );

            if (!moon) {
                addEntry(
                        probeInfo,
                        "msg.top.advancedrocketry.hologram.surface",
                        properties.hasSurface()
                                ? "msg.top.advancedrocketry.hologram.surface.yes"
                                : "msg.top.advancedrocketry.hologram.surface.gasGiant"
                );

                addNumberEntry(
                        probeInfo,
                        "msg.top.advancedrocketry.hologram.moons",
                        properties.getChildPlanets().size()
                );
            }

            return true;
        }

        return false;
    }

    private static void addHeader(
            IProbeInfo probeInfo,
            String name) {

        probeInfo.text(TextStyleClass.NAME + name);
        probeInfo.text(
                TextStyleClass.MODNAME
                        + tr("msg.top.advancedrocketry.modname")
        );
    }

    private static void addNumberEntry(
            IProbeInfo probeInfo,
            String labelKey,
            int value) {

        probeInfo.text(
                TextStyleClass.LABEL
                        + tr(labelKey)
                        + ": "
                        + TextFormatting.WHITE
                        + value
        );
    }
    private static void addEntry(
            IProbeInfo probeInfo,
            String labelKey,
            String valueKey) {

        probeInfo.text(
                TextStyleClass.LABEL
                        + tr(labelKey)
                        + ": "
                        + TextFormatting.WHITE
                        + tr(valueKey)
        );
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String tr(String key) {
        return IProbeInfo.STARTLOC
                + key
                + IProbeInfo.ENDLOC;
    }
}