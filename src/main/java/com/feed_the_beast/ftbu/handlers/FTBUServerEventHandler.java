package com.feed_the_beast.ftbu.handlers;

import com.feed_the_beast.ftbl.api.IForgePlayer;
import com.feed_the_beast.ftbl.api.events.RegisterFTBCommandsEvent;
import com.feed_the_beast.ftbl.api.events.ReloadEvent;
import com.feed_the_beast.ftbl.lib.util.LMUtils;
import com.feed_the_beast.ftbu.FTBLibIntegration;
import com.feed_the_beast.ftbu.FTBU;
import com.feed_the_beast.ftbu.api_impl.LoadedChunkStorage;
import com.feed_the_beast.ftbu.cmd.CmdAdminHome;
import com.feed_the_beast.ftbu.cmd.CmdBack;
import com.feed_the_beast.ftbu.cmd.CmdBackup;
import com.feed_the_beast.ftbu.cmd.CmdChunks;
import com.feed_the_beast.ftbu.cmd.CmdDelHome;
import com.feed_the_beast.ftbu.cmd.CmdDelWarp;
import com.feed_the_beast.ftbu.cmd.CmdEditRanks;
import com.feed_the_beast.ftbu.cmd.CmdGetRank;
import com.feed_the_beast.ftbu.cmd.CmdHome;
import com.feed_the_beast.ftbu.cmd.CmdInv;
import com.feed_the_beast.ftbu.cmd.CmdRestart;
import com.feed_the_beast.ftbu.cmd.CmdServerInfo;
import com.feed_the_beast.ftbu.cmd.CmdSetHome;
import com.feed_the_beast.ftbu.cmd.CmdSetRank;
import com.feed_the_beast.ftbu.cmd.CmdSetWarp;
import com.feed_the_beast.ftbu.cmd.CmdSpawn;
import com.feed_the_beast.ftbu.cmd.CmdTplast;
import com.feed_the_beast.ftbu.cmd.CmdTrashCan;
import com.feed_the_beast.ftbu.cmd.CmdWarp;
import com.feed_the_beast.ftbu.config.FTBUConfigBackups;
import com.feed_the_beast.ftbu.config.FTBUConfigCommands;
import com.feed_the_beast.ftbu.config.FTBUConfigGeneral;
import com.feed_the_beast.ftbu.ranks.Ranks;
import com.feed_the_beast.ftbu.world.FTBUPlayerData;
import com.feed_the_beast.ftbu.world.FTBUUniverseData;
import com.feed_the_beast.ftbu.world.ServerInfoFile;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

public class FTBUServerEventHandler
{
    private static final String[] LINK_PREFIXES = {"http://", "https://"};

    private static int getFirstLinkIndex(String s)
    {
        for(String s1 : LINK_PREFIXES)
        {
            int idx = s.indexOf(s1);
            if(idx != -1)
            {
                return idx;
            }
        }

        return -1;
    }

    @SubscribeEvent
    public void onReloadEvent(ReloadEvent event)
    {
        if(event.getSide().isServer())
        {
            ServerInfoFile.CachedInfo.reload();
            Ranks.INSTANCE.reload();

            FTBUUniverseData.reloadServerBadges();
            LoadedChunkStorage.INSTANCE.checkAll();
        }
        else
        {
            FTBU.PROXY.onReloadedClient();
        }
    }

    @SubscribeEvent
    public void onRegisterFTBCommandsEvent(RegisterFTBCommandsEvent event)
    {
        if(event.isDedicatedServer())
        {
            event.add(new CmdRestart());
        }

        if(FTBUConfigCommands.INV.getBoolean())
        {
            event.add(new CmdInv());
        }

        if(FTBUConfigCommands.WARP.getBoolean())
        {
            event.add(new CmdWarp());
            event.add(new CmdSetWarp());
            event.add(new CmdDelWarp());
        }

        if(FTBUConfigBackups.ENABLED.getBoolean())
        {
            event.add(new CmdBackup());
        }

        if(FTBUConfigCommands.HOME.getBoolean())
        {
            event.add(new CmdAdminHome());
            event.add(new CmdHome());
            event.add(new CmdSetHome());
            event.add(new CmdDelHome());
        }

        if(FTBUConfigCommands.SERVER_INFO.getBoolean())
        {
            event.add(new CmdServerInfo());
        }

        if(FTBUConfigCommands.TPL.getBoolean())
        {
            event.add(new CmdTplast());
        }

        if(FTBUConfigCommands.TRASH_CAN.getBoolean())
        {
            event.add(new CmdTrashCan());
        }

        if(FTBUConfigCommands.BACK.getBoolean())
        {
            event.add(new CmdBack());
        }

        if(FTBUConfigCommands.SPAWN.getBoolean())
        {
            event.add(new CmdSpawn());
        }

        if(FTBUConfigCommands.CHUNKS.getBoolean())
        {
            event.add(new CmdChunks());
        }

        if(FTBUConfigGeneral.RANKS_ENABLED.getBoolean())
        {
            event.add(new CmdGetRank());
            event.add(new CmdSetRank());
            event.add(new CmdEditRanks());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onServerChatEvent(ServerChatEvent e)
    {
        String msg = e.getMessage().trim();

        if(msg.startsWith(FTBUConfigGeneral.CHAT_SUBSTITUTE_PREFIX.getString()))
        {
            ITextComponent replacement = FTBUConfigGeneral.CHAT_SUBSTITUTES.value.get(msg.substring(FTBUConfigGeneral.CHAT_SUBSTITUTE_PREFIX.getString().length()));

            if(replacement != null)
            {
                e.setComponent(replacement.createCopy());
                return;
            }
        }

        if(!FTBUConfigGeneral.ENABLE_LINKS.getBoolean())
        {
            return;
        }

        String[] splitMsg = LMUtils.removeFormatting(msg).split(" "); // https://github.com/LatvianModder

        List<String> links = new ArrayList<>();

        for(String s : splitMsg)
        {
            int index = getFirstLinkIndex(s);
            if(index != -1)
            {
                links.add(s.substring(index).trim());
            }
        }

        if(!links.isEmpty())
        {
            final ITextComponent line = new TextComponentString("");
            boolean oneLink = links.size() == 1;

            for(int i = 0; i < links.size(); i++)
            {
                String link = links.get(i);
                ITextComponent c = new TextComponentString(oneLink ? "[ Link ]" : ("[ Link #" + (i + 1) + " ]"));
                c.getStyle().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString(link)));
                c.getStyle().setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, link));
                line.appendSibling(c);
                if(!oneLink)
                {
                    line.appendSibling(new TextComponentString(" "));
                }
            }

            line.getStyle().setColor(TextFormatting.GOLD);

            FTBLibIntegration.API.addServerCallback(1, () ->
            {
                for(IForgePlayer p : FTBLibIntegration.API.getUniverse().getPlayers())
                {
                    if(p.isOnline() && FTBUPlayerData.get(p).chatLinks())
                    {
                        p.getPlayer().addChatMessage(line);
                    }
                }
            });
        }
    }
}