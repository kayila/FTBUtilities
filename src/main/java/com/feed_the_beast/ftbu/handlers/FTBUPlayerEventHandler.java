package com.feed_the_beast.ftbu.handlers;

import com.feed_the_beast.ftbl.api.IForgePlayer;
import com.feed_the_beast.ftbl.api.IForgeTeam;
import com.feed_the_beast.ftbl.api.RegistryObject;
import com.feed_the_beast.ftbl.api.events.player.ForgePlayerDeathEvent;
import com.feed_the_beast.ftbl.api.events.player.ForgePlayerInfoEvent;
import com.feed_the_beast.ftbl.api.events.player.ForgePlayerLoggedInEvent;
import com.feed_the_beast.ftbl.api.events.player.ForgePlayerLoggedOutEvent;
import com.feed_the_beast.ftbl.api.events.player.ForgePlayerSettingsEvent;
import com.feed_the_beast.ftbl.api.events.player.IPlayerDataProvider;
import com.feed_the_beast.ftbl.lib.MouseButton;
import com.feed_the_beast.ftbl.lib.math.ChunkDimPos;
import com.feed_the_beast.ftbl.lib.math.EntityDimPos;
import com.feed_the_beast.ftbl.lib.util.LMInvUtils;
import com.feed_the_beast.ftbu.FTBLibIntegration;
import com.feed_the_beast.ftbu.FTBUNotifications;
import com.feed_the_beast.ftbu.FTBUPermissions;
import com.feed_the_beast.ftbu.api_impl.ClaimedChunkStorage;
import com.feed_the_beast.ftbu.api_impl.LoadedChunkStorage;
import com.feed_the_beast.ftbu.config.FTBUConfigLogin;
import com.feed_the_beast.ftbu.config.FTBUConfigWorld;
import com.feed_the_beast.ftbu.world.FTBUPlayerData;
import com.feed_the_beast.ftbu.world.FTBUUniverseData;
import com.google.common.base.Objects;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.server.permission.PermissionAPI;
import net.minecraftforge.server.permission.context.ContextKeys;
import net.minecraftforge.server.permission.context.PlayerContext;

public class FTBUPlayerEventHandler
{
    @RegistryObject
    public static final IPlayerDataProvider DATA_PROVIDER = player -> new FTBUPlayerData();

    @SubscribeEvent
    public void onLoggedIn(ForgePlayerLoggedInEvent event)
    {
        EntityPlayerMP ep = event.getPlayer().getPlayer();

        if(event.isFirstLogin())
        {
            if(FTBUConfigLogin.ENABLE_STARTING_ITEMS.getBoolean())
            {
                FTBUConfigLogin.STARTING_ITEMS.getItems().forEach(is -> LMInvUtils.giveItem(ep, is));
            }
        }

        if(FTBUConfigLogin.ENABLE_MOTD.getBoolean())
        {
            FTBUConfigLogin.MOTD.getText().forEach(ep::addChatMessage);
        }

        LoadedChunkStorage.INSTANCE.checkAll();
    }

    @SubscribeEvent
    public void onLoggedOut(ForgePlayerLoggedOutEvent event)
    {
        LoadedChunkStorage.INSTANCE.checkAll();
    }

    @SubscribeEvent
    public void onDeath(ForgePlayerDeathEvent event)
    {
        FTBUPlayerData data = FTBUPlayerData.get(event.getPlayer());
        data.lastDeath = new EntityDimPos(event.getPlayer().getPlayer()).toBlockDimPos();
    }

    @SubscribeEvent
    public void getSettings(ForgePlayerSettingsEvent event)
    {
        FTBUPlayerData data = FTBUPlayerData.get(event.getPlayer());
        data.addConfig(event.getSettings());
    }

    @SubscribeEvent
    public void addInfo(ForgePlayerInfoEvent event)
    {
        /*
        if(owner.getRank().config.show_rank.getMode())
		{
		    Rank rank = getRank();
		    IChatComponent rankC = new ChatComponentText("[" + rank.ID + "]");
		    rankC.getChatStyle().setColor(rank.color.getMode());
		    info.add(rankC);
		}
		*/
    }

    @SubscribeEvent
    public void onChunkChanged(EntityEvent.EnteringChunk e)
    {
        if(e.getEntity().worldObj.isRemote || !(e.getEntity() instanceof EntityPlayerMP))
        {
            return;
        }

        EntityPlayerMP ep = (EntityPlayerMP) e.getEntity();
        IForgePlayer player = FTBLibIntegration.API.getUniverse().getPlayer(ep);

        if(player == null || !player.isOnline())
        {
            return;
        }

        FTBUPlayerData data = FTBUPlayerData.get(player);
        data.lastSafePos = new EntityDimPos(ep).toBlockDimPos();
        updateChunkMessage(ep, new ChunkDimPos(e.getNewChunkX(), e.getNewChunkZ(), ep.dimension));
    }

    public static void updateChunkMessage(EntityPlayerMP player, ChunkDimPos pos)
    {
        IForgePlayer newTeamOwner = ClaimedChunkStorage.INSTANCE.getChunkOwner(pos);

        FTBUPlayerData data = FTBUPlayerData.get(FTBLibIntegration.API.getUniverse().getPlayer(player));

        if(!Objects.equal(data.lastChunkOwner, newTeamOwner))
        {
            data.lastChunkOwner = newTeamOwner;

            if(newTeamOwner != null)
            {
                IForgeTeam team = newTeamOwner.getTeam();

                if(team != null)
                {
                    FTBLibIntegration.API.sendNotification(player, FTBUNotifications.chunkChanged(team));
                }
            }
            else
            {
                FTBLibIntegration.API.sendNotification(player, FTBUNotifications.chunkChanged(null));
            }
        }
    }

    @SubscribeEvent
    public void onPlayerAttacked(LivingAttackEvent e)
    {
        if(e.getEntity().worldObj.isRemote)
        {
            return;
        }

        if(e.getEntity().dimension != 0 || !(e.getEntity() instanceof EntityPlayerMP) || e.getEntity() instanceof FakePlayer)
        {
            return;
        }

        Entity entity = e.getSource().getSourceOfDamage();

        if(entity != null && (entity instanceof EntityPlayerMP || entity instanceof IMob))
        {
            if(entity instanceof FakePlayer)
            {
                return;
            }
            /*else if(entity instanceof EntityPlayerMP && PermissionAPI.hasPermission(((EntityPlayerMP) entity).getGameProfile(), FTBLibPermissions.INTERACT_SECURE, false, new Context(entity)))
            {
                return;
            }*/

            if((FTBUConfigWorld.SAFE_SPAWN.getBoolean() && FTBUUniverseData.isInSpawnD(e.getEntity().dimension, e.getEntity().posX, e.getEntity().posZ)))
            {
                e.setCanceled(true);
            }
            /*else
            {
				ClaimedChunk c = Claims.getMode(dim, cx, cz);
				if(c != null && c.claims.settings.isSafe()) e.setCanceled(true);
			}*/
        }
    }

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event)
    {
        if(event.getEntityPlayer() instanceof EntityPlayerMP)
        {
            EntityPlayerMP player = (EntityPlayerMP) event.getEntityPlayer();
            BlockPos pos = event.getPos().offset(event.getFace() == null ? EnumFacing.UP : event.getFace());
            IBlockState state = player.worldObj.getBlockState(pos);

            if(!ClaimedChunkStorage.INSTANCE.canPlayerInteract(player, pos, MouseButton.RIGHT))
            {
                if(!PermissionAPI.hasPermission(player.getGameProfile(), FTBUPermissions.CLAIMS_BLOCK_INTERACT_PREFIX + FTBUPermissions.formatBlock(state.getBlock()), new PlayerContext(player).set(ContextKeys.POS, pos).set(ContextKeys.BLOCK_STATE, state)))
                {
                    event.setCanceled(true);
                }
            }
        }
    }

    @SubscribeEvent
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event)
    {
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event)
    {
        if(event.getPlayer() instanceof EntityPlayerMP)
        {
            EntityPlayerMP player = (EntityPlayerMP) event.getPlayer();
            BlockPos pos = event.getPos();
            IBlockState state = player.worldObj.getBlockState(pos);

            if(!ClaimedChunkStorage.INSTANCE.canPlayerInteract(player, pos, MouseButton.LEFT))
            {
                if(!PermissionAPI.hasPermission(player.getGameProfile(), FTBUPermissions.CLAIMS_BLOCK_BREAK_PREFIX + FTBUPermissions.formatBlock(state.getBlock()), new PlayerContext(player).set(ContextKeys.POS, pos).set(ContextKeys.BLOCK_STATE, state)))
                {
                    event.setCanceled(true);
                }
            }
        }
    }

    @Optional.Method(modid = "chiselsandbits")
    @SubscribeEvent
    public void onChiselEvent(mod.chiselsandbits.api.EventBlockBitModification event)
    {
        if(event.getPlayer() instanceof EntityPlayerMP)
        {
            EntityPlayerMP player = (EntityPlayerMP) event.getPlayer();
            BlockPos pos = event.getPos();
            if(!ClaimedChunkStorage.INSTANCE.canPlayerInteract(player, pos, event.isPlacing() ? MouseButton.RIGHT : MouseButton.LEFT))
            {
                if(!PermissionAPI.hasPermission(player.getGameProfile(), FTBUPermissions.CLAIMS_BLOCK_CNB, new PlayerContext(player).set(ContextKeys.POS, pos)))
                {
                    event.setCanceled(true);
                }
            }
        }
    }
}