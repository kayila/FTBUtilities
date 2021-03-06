package com.feed_the_beast.ftbu.cmd;

import com.feed_the_beast.ftbl.api.IForgePlayer;
import com.feed_the_beast.ftbl.lib.cmd.CommandLM;
import com.feed_the_beast.ftbl.lib.math.ChunkDimPos;
import com.feed_the_beast.ftbl.lib.math.EntityDimPos;
import com.feed_the_beast.ftbu.FTBLibIntegration;
import com.feed_the_beast.ftbu.FTBUNotifications;
import com.feed_the_beast.ftbu.FTBUPermissions;
import com.feed_the_beast.ftbu.api.chunks.IClaimedChunk;
import com.feed_the_beast.ftbu.api_impl.ClaimedChunkStorage;
import com.feed_the_beast.ftbu.api_impl.LoadedChunkStorage;
import com.feed_the_beast.ftbu.config.FTBUConfigWorld;
import com.feed_the_beast.ftbu.handlers.FTBUPlayerEventHandler;
import com.feed_the_beast.ftbu.world.FTBUUniverseData;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.server.command.CommandTreeBase;
import net.minecraftforge.server.permission.PermissionAPI;
import net.minecraftforge.server.permission.context.BlockPosContext;

/**
 * Created by LatvianModder on 27.06.2016.
 */
public class CmdChunks extends CommandTreeBase
{
    public static void updateChunk(EntityPlayerMP ep, ChunkDimPos pos)
    {
        FTBUPlayerEventHandler.updateChunkMessage(ep, pos);
    }

    public class CmdClaim extends CommandLM
    {
        @Override
        public String getCommandName()
        {
            return "claim";
        }

        @Override
        public int getRequiredPermissionLevel()
        {
            return 0;
        }

        @Override
        public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
        {
            EntityPlayerMP player = getCommandSenderAsPlayer(sender);
            IForgePlayer p = FTBLibIntegration.API.getForgePlayer(player);
            ChunkDimPos pos = new EntityDimPos(player).toBlockDimPos().toChunkPos();

            if(!FTBUConfigWorld.CHUNK_CLAIMING.getBoolean())
            {
                FTBLibIntegration.API.sendNotification(player, FTBUNotifications.CLAIMING_NOT_ENABLED);
                return;
            }

            if(!PermissionAPI.hasPermission(player.getGameProfile(), FTBUPermissions.CLAIMS_CLAIM_CHUNKS, null))
            {
                FTBLibIntegration.API.sendNotification(player, FTBUNotifications.CLAIMING_NOT_ALLOWED);
                return;
            }

            if(FTBUUniverseData.claimChunk(p, pos))
            {
                FTBLibIntegration.API.sendNotification(player, FTBUNotifications.CHUNK_CLAIMED);
                updateChunk(player, pos);
            }
            else
            {
                FTBLibIntegration.API.sendNotification(player, FTBUNotifications.CANT_MODIFY_CHUNK);
            }

        }
    }

    public class CmdUnclaim extends CommandLM
    {
        @Override
        public String getCommandName()
        {
            return "unclaim";
        }

        @Override
        public int getRequiredPermissionLevel()
        {
            return 0;
        }

        @Override
        public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
        {
            EntityPlayerMP player = getCommandSenderAsPlayer(sender);
            IForgePlayer p = FTBLibIntegration.API.getForgePlayer(player);
            ChunkDimPos pos = new EntityDimPos(player).toBlockDimPos().toChunkPos();

            if(!FTBUConfigWorld.CHUNK_CLAIMING.getBoolean())
            {
                FTBLibIntegration.API.sendNotification(player, FTBUNotifications.CLAIMING_NOT_ENABLED);
                return;
            }

            if(!PermissionAPI.hasPermission(player.getGameProfile(), FTBUPermissions.CLAIMS_CLAIM_CHUNKS, null))
            {
                FTBLibIntegration.API.sendNotification(player, FTBUNotifications.CLAIMING_NOT_ALLOWED);
                return;
            }

            if(!p.equalsPlayer(ClaimedChunkStorage.INSTANCE.getChunkOwner(pos)) && !PermissionAPI.hasPermission(player.getGameProfile(), FTBUPermissions.CLAIMS_MODIFY_OTHER_CHUNKS, new BlockPosContext(player, pos.getChunkPos())))
            {
                throw new CommandException("commands.generic.permission");
            }

            if(FTBUUniverseData.unclaimChunk(p, pos))
            {
                FTBLibIntegration.API.sendNotification(player, FTBUNotifications.CHUNK_UNCLAIMED);
                updateChunk(player, pos);
            }
            else
            {
                FTBLibIntegration.API.sendNotification(player, FTBUNotifications.CANT_MODIFY_CHUNK);
            }
        }
    }

    public class CmdLoad extends CommandLM
    {
        @Override
        public String getCommandName()
        {
            return "load";
        }

        @Override
        public int getRequiredPermissionLevel()
        {
            return 0;
        }

        @Override
        public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
        {
            EntityPlayerMP player = getCommandSenderAsPlayer(sender);
            IForgePlayer p = FTBLibIntegration.API.getForgePlayer(player);
            ChunkDimPos pos = new EntityDimPos(player).toBlockDimPos().toChunkPos();

            if(FTBUUniverseData.setLoaded(p, pos, true))
            {
                FTBLibIntegration.API.sendNotification(player, FTBUNotifications.CHUNK_LOADED);
                updateChunk(player, pos);
            }
            else
            {
                FTBLibIntegration.API.sendNotification(player, FTBUNotifications.CANT_MODIFY_CHUNK);
            }
        }
    }

    public class CmdUnload extends CommandLM
    {
        @Override
        public String getCommandName()
        {
            return "unload";
        }

        @Override
        public int getRequiredPermissionLevel()
        {
            return 0;
        }

        @Override
        public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
        {
            EntityPlayerMP player = getCommandSenderAsPlayer(sender);
            IForgePlayer p = FTBLibIntegration.API.getForgePlayer(player);
            ChunkDimPos pos = new EntityDimPos(player).toBlockDimPos().toChunkPos();

            if(FTBUUniverseData.setLoaded(p, pos, false))
            {
                FTBLibIntegration.API.sendNotification(player, FTBUNotifications.CHUNK_UNLOADED);
                updateChunk(player, pos);
            }
            else
            {
                FTBLibIntegration.API.sendNotification(player, FTBUNotifications.CANT_MODIFY_CHUNK);
            }
        }
    }

    public class CmdUnclaimAll extends CommandLM
    {
        @Override
        public String getCommandName()
        {
            return "unclaim_all";
        }

        @Override
        public int getRequiredPermissionLevel()
        {
            return 0;
        }

        @Override
        public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
        {
            EntityPlayerMP ep = getCommandSenderAsPlayer(sender);

            checkArgs(args, 1, "<all_dimensions> [player]");

            IForgePlayer p;

            if(args.length >= 2)
            {
                if(!PermissionAPI.hasPermission(ep, FTBUPermissions.CLAIMS_MODIFY_OTHER_CHUNKS))
                {
                    throw new CommandException("commands.generic.permission");
                }

                p = FTBLibIntegration.API.getForgePlayer(args[1]);
            }
            else
            {
                p = FTBLibIntegration.API.getForgePlayer(ep);
            }

            FTBUUniverseData.unclaimAllChunks(p, parseBoolean(args[0]) ? null : ep.dimension);
            FTBLibIntegration.API.sendNotification(ep, FTBUNotifications.UNCLAIMED_ALL);
        }
    }

    public class CmdUnloadAll extends CommandLM
    {
        @Override
        public String getCommandName()
        {
            return "admin_unload_all";
        }

        @Override
        public boolean isUsernameIndex(String[] args, int i)
        {
            return i == 0;
        }

        @Override
        public void execute(MinecraftServer server, ICommandSender ics, String[] args) throws CommandException
        {
            checkArgs(args, 1, "<player>");
            IForgePlayer p = FTBLibIntegration.API.getForgePlayer(args[0]);

            for(IClaimedChunk chunk : ClaimedChunkStorage.INSTANCE.getChunks(p))
            {
                chunk.setLoaded(false);
            }

            LoadedChunkStorage.INSTANCE.checkAll();
            ics.addChatMessage(new TextComponentString("Unloaded all " + p.getProfile().getName() + "'s chunks")); //TODO: Lang
        }
    }

    public class CmdAdminUnclaimAll extends CommandLM
    {
        @Override
        public String getCommandName()
        {
            return "admin_unclaim_all";
        }

        @Override
        public String getCommandUsage(ICommandSender ics)
        {
            return '/' + getCommandName() + " <player | @a>";
        }

        @Override
        public boolean isUsernameIndex(String[] args, int i)
        {
            return i == 0;
        }

        @Override
        public void execute(MinecraftServer server, ICommandSender ics, String[] args) throws CommandException
        {
            checkArgs(args, 1, "<player>");
            IForgePlayer p = FTBLibIntegration.API.getForgePlayer(args[0]);
            FTBUUniverseData.unclaimAllChunks(p, null);
            ics.addChatMessage(new TextComponentString("Unclaimed all " + p.getProfile().getName() + "'s chunks")); //TODO: Lang
        }
    }

    public class CmdClaimFor extends CommandLM
    {
        @Override
        public String getCommandName()
        {
            return "claim_for";
        }

        @Override
        public String getCommandUsage(ICommandSender ics)
        {
            return '/' + getCommandName() + " <player> <chunkX> <chunkZ> [<dimension>]";
        }

        @Override
        public boolean isUsernameIndex(String[] args, int i)
        {
            return i == 0;
        }

        @Override
        public void execute(MinecraftServer server, ICommandSender ics, String[] args) throws CommandException
        {

            checkArgs(args, 3, "<player> <chunkX> <chunkZ> <dimension>");
            String playerName = args[0];
            IForgePlayer claim_for = FTBLibIntegration.API.getForgePlayer(playerName);
            int chunkXPos = parseInt(args[1]);
            int chunkZPos = parseInt(args[2]);

            TIntList dimensions = args.length > 3 ? TIntArrayList.wrap(new int[] {parseInt(args[3])}) : FTBUConfigWorld.LOCKED_IN_DIMENSIONS.getIntList();

            EntityPlayerMP player = claim_for.getPlayer();
            if(player == null)
            {
                ics.addChatMessage(new TextComponentString("Can't find player " + playerName));
                return;
            }

            TIntIterator it = dimensions.iterator();
            while(it.hasNext())
            {
                int dimension = it.next();
                ChunkDimPos pos = new ChunkDimPos(chunkXPos, chunkZPos, dimension);
                if(FTBUUniverseData.claimChunk(claim_for, pos))
                {
                    String msg = String.format("Claimed %d, %d in %d for %s", chunkXPos, chunkZPos, dimension, playerName);
                    ics.addChatMessage(new TextComponentString(msg));
                    updateChunk(player, pos);
                }
                else
                {
                    String msg = String.format("ERROR: Can't claim %d, %d in %d for %s", chunkXPos, chunkZPos, dimension, playerName);
                    ics.addChatMessage(new TextComponentString(msg));
                }
            }

        }
    }

    public CmdChunks()
    {
        addSubcommand(new CmdClaim());
        addSubcommand(new CmdUnclaim());
        addSubcommand(new CmdLoad());
        addSubcommand(new CmdUnload());

        addSubcommand(new CmdUnclaimAll());
        addSubcommand(new CmdUnloadAll());
        addSubcommand(new CmdAdminUnclaimAll());
        addSubcommand(new CmdClaimFor());
    }

    @Override
    public String getCommandName()
    {
        return "chunks";
    }

    @Override
    public int getRequiredPermissionLevel()
    {
        return 0;
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender)
    {
        return true;
    }

    @Override
    public String getCommandUsage(ICommandSender sender)
    {
        return "command.ftb.chunks.usage";
    }
}
