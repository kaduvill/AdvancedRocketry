package zmaster587.advancedRocketry.integration.waila;

import mcp.mobius.waila.api.IWailaPlugin;
import mcp.mobius.waila.api.IWailaRegistrar;
import mcp.mobius.waila.api.WailaPlugin;
import zmaster587.advancedRocketry.entity.EntityRocket;

@WailaPlugin
public class WailaIntegration implements IWailaPlugin {

	@Override
	public void register(IWailaRegistrar waila) {
        RocketEntityProvider rep = new RocketEntityProvider();
		waila.registerNBTProvider(rep, EntityRocket.class);
        waila.registerHeadProvider(rep, EntityRocket.class);
        waila.registerBodyProvider(rep, EntityRocket.class);
	}
    
}
