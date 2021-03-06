package com.feed_the_beast.ftbu.badges;

import com.feed_the_beast.ftbl.api.client.FTBLibClient;
import com.feed_the_beast.ftbl.lib.FinalIDObject;
import com.feed_the_beast.ftbu.FTBUFinals;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.VertexBuffer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public class Badge extends FinalIDObject
{
    public static final ResourceLocation defTex = new ResourceLocation(FTBUFinals.MOD_ID, "textures/failed_badge.png");

    // -- //

    public final String imageURL;
    private ResourceLocation textureURL = null;

    public Badge(String id, String url)
    {
        super(id);
        imageURL = url;
    }

    @Override
    public String toString()
    {
        return getName() + '=' + imageURL;
    }

    public ResourceLocation getTexture()
    {
        if(textureURL == null)
        {
            textureURL = new ResourceLocation(FTBUFinals.MOD_ID, "badges/" + getName() + ".png");
            FTBLibClient.getDownloadImage(textureURL, imageURL, defTex, null);
        }

        return textureURL;
    }

    public void onPlayerRender(EntityPlayer ep)
    {
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        FTBLibClient.setTexture(getTexture());
        FTBLibClient.pushMaxBrightness();
        GlStateManager.pushMatrix();

        if(ep.isSneaking())
        {
            GlStateManager.rotate(25F, 1F, 0F, 0F);
        }

        GlStateManager.translate(0.04F, 0.01F, 0.86F);

        ItemStack armor = ep.getItemStackFromSlot(EntityEquipmentSlot.CHEST);
        if(armor != null && armor.getItem().isValidArmor(armor, EntityEquipmentSlot.CHEST, ep))
        {
            GlStateManager.translate(0F, 0F, -0.0625F);
        }

        GlStateManager.translate(0F, 0F, -1F);
        GlStateManager.color(1F, 1F, 1F, 1F);
        Tessellator tessellator = Tessellator.getInstance();
        VertexBuffer buffer = tessellator.getBuffer();
        buffer.begin(7, DefaultVertexFormats.POSITION_TEX);
        buffer.pos(0D, 0.2D, 0D).tex(0D, 1D).endVertex();
        buffer.pos(0.2D, 0.2D, 0D).tex(1D, 1D).endVertex();
        buffer.pos(0.2D, 0D, 0D).tex(1D, 0D).endVertex();
        buffer.pos(0D, 0D, 0D).tex(0D, 0D).endVertex();
        tessellator.draw();

        FTBLibClient.popMaxBrightness();
        GlStateManager.popMatrix();
    }
}