package net.minecraft.src;

import java.io.*;
import java.util.*;
import net.minecraft.server.MinecraftServer;

public class EntityPlayerMP extends EntityPlayer implements ICrafting
{
    private StringTranslate translator;

    /**
     * The NetServerHandler assigned to this player by the ServerConfigurationManager.
     */
    public NetServerHandler playerNetServerHandler;

    /** Reference to the MinecraftServer object. */
    public MinecraftServer mcServer;

    /** The ItemInWorldManager belonging to this player */
    public ItemInWorldManager theItemInWorldManager;

    /** player X position as seen by PlayerManager */
    public double managedPosX;

    /** player Z position as seen by PlayerManager */
    public double managedPosZ;

    /** LinkedList that holds the loaded chunks. */
    public final List loadedChunks = new LinkedList();

    /** entities added to this list will  be packet29'd to the player */
    public final List destroyedItemsNetCache = new LinkedList();

    /** set to getHealth */
    private int lastHealth;

    /** set to foodStats.GetFoodLevel */
    private int lastFoodLevel;

    /** set to foodStats.getSaturationLevel() == 0.0F each tick */
    private boolean wasHungry;

    /** Amount of experience the client was last set to */
    private int lastExperience;

    /** de-increments onUpdate, attackEntityFrom is ignored if this >0 */
    private int initialInvulnerability;

    /** must be between 3>x>15 (strictly between) */
    private int renderDistance;
    private int chatVisibility;
    private boolean chatColours;

    /**
     * The currently in use window ID. Incremented every time a window is opened.
     */
    private int currentWindowId;

    /**
     * poor mans concurency flag, lets hope the jvm doesn't re-order the setting of this flag wrt the inventory change
     * on the next line
     */
    public boolean playerInventoryBeingManipulated;
    public int ping;

    /**
     * Set when a player beats the ender dragon, used to respawn the player at the spawn point while retaining inventory
     * and XP
     */
    public boolean playerConqueredTheEnd;

    public EntityPlayerMP(MinecraftServer par1MinecraftServer, World par2World, String par3Str, ItemInWorldManager par4ItemInWorldManager)
    {
        super(par2World);
        translator = new StringTranslate("en_US");
        lastHealth = 0xfa0a1f01;
        lastFoodLevel = 0xfa0a1f01;
        wasHungry = true;
        lastExperience = 0xfa0a1f01;
        initialInvulnerability = 60;
        renderDistance = 0;
        chatVisibility = 0;
        chatColours = true;
        currentWindowId = 0;
        playerConqueredTheEnd = false;
        par4ItemInWorldManager.thisPlayerMP = this;
        theItemInWorldManager = par4ItemInWorldManager;
        renderDistance = par1MinecraftServer.getConfigurationManager().getViewDistance();
        ChunkCoordinates chunkcoordinates = par2World.getSpawnPoint();
        int i = chunkcoordinates.posX;
        int j = chunkcoordinates.posZ;
        int k = chunkcoordinates.posY;

        if (!par2World.provider.hasNoSky && par2World.getWorldInfo().getGameType() != EnumGameType.ADVENTURE)
        {
            int l = Math.max(5, par1MinecraftServer.func_82357_ak() - 6);
            i += rand.nextInt(l * 2) - l;
            j += rand.nextInt(l * 2) - l;
            k = par2World.getTopSolidOrLiquidBlock(i, j);
        }

        setLocationAndAngles((double)i + 0.5D, k, (double)j + 0.5D, 0.0F, 0.0F);
        mcServer = par1MinecraftServer;
        stepHeight = 0.0F;
        username = par3Str;
        yOffset = 0.0F;
    }

    public EntityPlayerMP(MinecraftServer par1MinecraftServer, String par3Str)
    {
        super(net.minecraft.client.Minecraft.getMinecraft().theWorld);
        this.mcServer = par1MinecraftServer;
        this.username = par3Str;
    }

    /**
     * (abstract) Protected helper method to read subclass entity data from NBT.
     */
    public void readEntityFromNBT(NBTTagCompound par1NBTTagCompound)
    {
        super.readEntityFromNBT(par1NBTTagCompound);

        if (par1NBTTagCompound.hasKey("playerGameType"))
        {
            theItemInWorldManager.setGameType(EnumGameType.getByID(par1NBTTagCompound.getInteger("playerGameType")));
        }
    }

    /**
     * (abstract) Protected helper method to write subclass entity data to NBT.
     */
    public void writeEntityToNBT(NBTTagCompound par1NBTTagCompound)
    {
        super.writeEntityToNBT(par1NBTTagCompound);
        par1NBTTagCompound.setInteger("playerGameType", theItemInWorldManager.getGameType().getID());
    }

    public void func_82242_a(int par1)
    {
        super.func_82242_a(par1);
        lastExperience = -1;
    }

    public void addSelfToInternalCraftingInventory()
    {
        craftingInventory.addCraftingToCrafters(this);
    }

    /**
     * sets the players height back to normal after doing things like sleeping and dieing
     */
    protected void resetHeight()
    {
        yOffset = 0.0F;
    }

    public float getEyeHeight()
    {
        return 1.62F;
    }

    /**
     * Called to update the entity's position/logic.
     */
    public void onUpdate()
    {
        theItemInWorldManager.updateBlockRemoving();
        initialInvulnerability--;
        craftingInventory.updateCraftingResults();

        if (!loadedChunks.isEmpty())
        {
            ArrayList arraylist = new ArrayList();
            Iterator iterator = loadedChunks.iterator();
            ArrayList arraylist1 = new ArrayList();

            do
            {
                if (!iterator.hasNext() || arraylist.size() >= 5)
                {
                    break;
                }

                ChunkCoordIntPair chunkcoordintpair = (ChunkCoordIntPair)iterator.next();
                iterator.remove();

                if (chunkcoordintpair != null && worldObj.blockExists(chunkcoordintpair.chunkXPos << 4, 0, chunkcoordintpair.chunkZPos << 4))
                {
                    arraylist.add(worldObj.getChunkFromChunkCoords(chunkcoordintpair.chunkXPos, chunkcoordintpair.chunkZPos));
                    arraylist1.addAll(((WorldServer)worldObj).getAllTileEntityInBox(chunkcoordintpair.chunkXPos * 16, 0, chunkcoordintpair.chunkZPos * 16, chunkcoordintpair.chunkXPos * 16 + 16, 256, chunkcoordintpair.chunkZPos * 16 + 16));
                }
            }
            while (true);

            if (!arraylist.isEmpty())
            {
                playerNetServerHandler.sendPacketToPlayer(new Packet56MapChunks(arraylist));
                TileEntity tileentity;

                for (Iterator iterator2 = arraylist1.iterator(); iterator2.hasNext(); sendTileEntityToPlayer(tileentity))
                {
                    tileentity = (TileEntity)iterator2.next();
                }
            }
        }

        if (!destroyedItemsNetCache.isEmpty())
        {
            int i = Math.min(destroyedItemsNetCache.size(), 127);
            int ai[] = new int[i];
            Iterator iterator1 = destroyedItemsNetCache.iterator();

            for (int j = 0; iterator1.hasNext() && j < i; iterator1.remove())
            {
                ai[j++] = ((Integer)iterator1.next()).intValue();
            }

            playerNetServerHandler.sendPacketToPlayer(new Packet29DestroyEntity(ai));
        }
    }

    public void onUpdateEntity()
    {
        super.onUpdate();

        for (int i = 0; i < inventory.getSizeInventory(); i++)
        {
            ItemStack itemstack = inventory.getStackInSlot(i);

            if (itemstack == null || !Item.itemsList[itemstack.itemID].isMap() || playerNetServerHandler.packetSize() > 5)
            {
                continue;
            }

            Packet packet = ((ItemMapBase)Item.itemsList[itemstack.itemID]).createMapDataPacket(itemstack, worldObj, this);

            if (packet != null)
            {
                playerNetServerHandler.sendPacketToPlayer(packet);
            }
        }

        if (getHealth() != lastHealth || lastFoodLevel != foodStats.getFoodLevel() || (foodStats.getSaturationLevel() == 0.0F) != wasHungry)
        {
            playerNetServerHandler.sendPacketToPlayer(new Packet8UpdateHealth(getHealth(), foodStats.getFoodLevel(), foodStats.getSaturationLevel()));
            lastHealth = getHealth();
            lastFoodLevel = foodStats.getFoodLevel();
            wasHungry = foodStats.getSaturationLevel() == 0.0F;
        }

        if (experienceTotal != lastExperience)
        {
            lastExperience = experienceTotal;
            playerNetServerHandler.sendPacketToPlayer(new Packet43Experience(experience, experienceTotal, experienceLevel));
        }
    }

    /**
     * Called when the mob's health reaches 0.
     */
    public void onDeath(DamageSource par1DamageSource)
    {
        mcServer.getConfigurationManager().sendPacketToAllPlayers(new Packet3Chat(par1DamageSource.getDeathMessage(this)));

        if (!worldObj.func_82736_K().func_82766_b("keepInventory"))
        {
            inventory.dropAllItems();
        }
    }

    /**
     * Called when the entity is attacked.
     */
    public boolean attackEntityFrom(DamageSource par1DamageSource, int par2)
    {
        if (initialInvulnerability > 0)
        {
            return false;
        }

        if (!mcServer.isPVPEnabled() && (par1DamageSource instanceof EntityDamageSource))
        {
            Entity entity = par1DamageSource.getEntity();

            if (entity instanceof EntityPlayer)
            {
                return false;
            }

            if (entity instanceof EntityArrow)
            {
                EntityArrow entityarrow = (EntityArrow)entity;

                if (entityarrow.shootingEntity instanceof EntityPlayer)
                {
                    return false;
                }
            }
        }

        return super.attackEntityFrom(par1DamageSource, par2);
    }

    /**
     * returns if pvp is enabled or not
     */
    protected boolean isPVPEnabled()
    {
        return mcServer.isPVPEnabled();
    }

    public void travelToTheEnd(int par1)
    {
        if (dimension == 1 && par1 == 1)
        {
            triggerAchievement(AchievementList.theEnd2);
            worldObj.setEntityDead(this);
            playerConqueredTheEnd = true;
            playerNetServerHandler.sendPacketToPlayer(new Packet70GameEvent(4, 0));
        }
        else
        {
            if (dimension == 1 && par1 == 0)
            {
                triggerAchievement(AchievementList.theEnd);
                ChunkCoordinates chunkcoordinates = mcServer.worldServerForDimension(par1).getEntrancePortalLocation();

                if (chunkcoordinates != null)
                {
                    playerNetServerHandler.setPlayerLocation(chunkcoordinates.posX, chunkcoordinates.posY, chunkcoordinates.posZ, 0.0F, 0.0F);
                }

                par1 = 1;
            }
            else
            {
                triggerAchievement(AchievementList.portal);
            }

            mcServer.getConfigurationManager().transferPlayerToDimension(this, par1);
            lastExperience = -1;
            lastHealth = -1;
            lastFoodLevel = -1;
        }
    }

    /**
     * called from onUpdate for all tileEntity in specific chunks
     */
    private void sendTileEntityToPlayer(TileEntity par1TileEntity)
    {
        if (par1TileEntity != null)
        {
            Packet packet = par1TileEntity.getDescriptionPacket();

            if (packet != null)
            {
                playerNetServerHandler.sendPacketToPlayer(packet);
            }
        }
    }

    /**
     * Called whenever an item is picked up from walking over it. Args: pickedUpEntity, stackSize
     */
    public void onItemPickup(Entity par1Entity, int par2)
    {
        super.onItemPickup(par1Entity, par2);
        craftingInventory.updateCraftingResults();
    }

    /**
     * Attempts to have the player sleep in a bed at the specified location.
     */
    public EnumStatus sleepInBedAt(int par1, int par2, int par3)
    {
        EnumStatus enumstatus = super.sleepInBedAt(par1, par2, par3);

        if (enumstatus == EnumStatus.OK)
        {
            Packet17Sleep packet17sleep = new Packet17Sleep(this, 0, par1, par2, par3);
            getServerForPlayer().getEntityTracker().sendPacketToAllPlayersTrackingEntity(this, packet17sleep);
            playerNetServerHandler.setPlayerLocation(posX, posY, posZ, rotationYaw, rotationPitch);
            playerNetServerHandler.sendPacketToPlayer(packet17sleep);
        }

        return enumstatus;
    }

    /**
     * Wake up the player if they're sleeping.
     */
    public void wakeUpPlayer(boolean par1, boolean par2, boolean par3)
    {
        if (isPlayerSleeping())
        {
            getServerForPlayer().getEntityTracker().sendPacketToAllAssociatedPlayers(this, new Packet18Animation(this, 3));
        }

        super.wakeUpPlayer(par1, par2, par3);

        if (playerNetServerHandler != null)
        {
            playerNetServerHandler.setPlayerLocation(posX, posY, posZ, rotationYaw, rotationPitch);
        }
    }

    /**
     * Called when a player mounts an entity. e.g. mounts a pig, mounts a boat.
     */
    public void mountEntity(Entity par1Entity)
    {
        super.mountEntity(par1Entity);
        playerNetServerHandler.sendPacketToPlayer(new Packet39AttachEntity(this, ridingEntity));
        playerNetServerHandler.setPlayerLocation(posX, posY, posZ, rotationYaw, rotationPitch);
    }

    /**
     * Takes in the distance the entity has fallen this tick and whether its on the ground to update the fall distance
     * and deal fall damage if landing on the ground.  Args: distanceFallenThisTick, onGround
     */
    protected void updateFallState(double d, boolean flag)
    {
    }

    /**
     * likeUpdateFallState, but called from updateFlyingState, rather than moveEntity
     */
    public void updateFlyingState(double par1, boolean par3)
    {
        super.updateFallState(par1, par3);
    }

    private void incrementWindowID()
    {
        currentWindowId = currentWindowId % 100 + 1;
    }

    /**
     * Displays the crafting GUI for a workbench.
     */
    public void displayGUIWorkbench(int par1, int par2, int par3)
    {
        incrementWindowID();
        playerNetServerHandler.sendPacketToPlayer(new Packet100OpenWindow(currentWindowId, 1, "Crafting", 9));
        craftingInventory = new ContainerWorkbench(inventory, worldObj, par1, par2, par3);
        craftingInventory.windowId = currentWindowId;
        craftingInventory.addCraftingToCrafters(this);
    }

    public void displayGUIEnchantment(int par1, int par2, int par3)
    {
        incrementWindowID();
        playerNetServerHandler.sendPacketToPlayer(new Packet100OpenWindow(currentWindowId, 4, "Enchanting", 9));
        craftingInventory = new ContainerEnchantment(inventory, worldObj, par1, par2, par3);
        craftingInventory.windowId = currentWindowId;
        craftingInventory.addCraftingToCrafters(this);
    }

    public void func_82244_d(int par1, int par2, int par3)
    {
        incrementWindowID();
        playerNetServerHandler.sendPacketToPlayer(new Packet100OpenWindow(currentWindowId, 8, "Repairing", 9));
        craftingInventory = new ContainerRepair(inventory, worldObj, par1, par2, par3, this);
        craftingInventory.windowId = currentWindowId;
        craftingInventory.addCraftingToCrafters(this);
    }

    /**
     * Displays the GUI for interacting with a chest inventory. Args: chestInventory
     */
    public void displayGUIChest(IInventory par1IInventory)
    {
        if (craftingInventory != inventorySlots)
        {
            closeScreen();
        }

        incrementWindowID();
        playerNetServerHandler.sendPacketToPlayer(new Packet100OpenWindow(currentWindowId, 0, par1IInventory.getInvName(), par1IInventory.getSizeInventory()));
        craftingInventory = new ContainerChest(inventory, par1IInventory);
        craftingInventory.windowId = currentWindowId;
        craftingInventory.addCraftingToCrafters(this);
    }

    /**
     * Displays the furnace GUI for the passed in furnace entity. Args: tileEntityFurnace
     */
    public void displayGUIFurnace(TileEntityFurnace par1TileEntityFurnace)
    {
        incrementWindowID();
        playerNetServerHandler.sendPacketToPlayer(new Packet100OpenWindow(currentWindowId, 2, par1TileEntityFurnace.getInvName(), par1TileEntityFurnace.getSizeInventory()));
        craftingInventory = new ContainerFurnace(inventory, par1TileEntityFurnace);
        craftingInventory.windowId = currentWindowId;
        craftingInventory.addCraftingToCrafters(this);
    }

    /**
     * Displays the dipsenser GUI for the passed in dispenser entity. Args: TileEntityDispenser
     */
    public void displayGUIDispenser(TileEntityDispenser par1TileEntityDispenser)
    {
        incrementWindowID();
        playerNetServerHandler.sendPacketToPlayer(new Packet100OpenWindow(currentWindowId, 3, par1TileEntityDispenser.getInvName(), par1TileEntityDispenser.getSizeInventory()));
        craftingInventory = new ContainerDispenser(inventory, par1TileEntityDispenser);
        craftingInventory.windowId = currentWindowId;
        craftingInventory.addCraftingToCrafters(this);
    }

    /**
     * Displays the GUI for interacting with a brewing stand.
     */
    public void displayGUIBrewingStand(TileEntityBrewingStand par1TileEntityBrewingStand)
    {
        incrementWindowID();
        playerNetServerHandler.sendPacketToPlayer(new Packet100OpenWindow(currentWindowId, 5, par1TileEntityBrewingStand.getInvName(), par1TileEntityBrewingStand.getSizeInventory()));
        craftingInventory = new ContainerBrewingStand(inventory, par1TileEntityBrewingStand);
        craftingInventory.windowId = currentWindowId;
        craftingInventory.addCraftingToCrafters(this);
    }

    public void func_82240_a(TileEntityBeacon par1TileEntityBeacon)
    {
        incrementWindowID();
        playerNetServerHandler.sendPacketToPlayer(new Packet100OpenWindow(currentWindowId, 7, par1TileEntityBeacon.getInvName(), par1TileEntityBeacon.getSizeInventory()));
        craftingInventory = new ContainerBeacon(inventory, par1TileEntityBeacon);
        craftingInventory.windowId = currentWindowId;
        craftingInventory.addCraftingToCrafters(this);
    }

    public void displayGUIMerchant(IMerchant par1IMerchant)
    {
        incrementWindowID();
        craftingInventory = new ContainerMerchant(inventory, par1IMerchant, worldObj);
        craftingInventory.windowId = currentWindowId;
        craftingInventory.addCraftingToCrafters(this);
        InventoryMerchant inventorymerchant = ((ContainerMerchant)craftingInventory).getMerchantInventory();
        playerNetServerHandler.sendPacketToPlayer(new Packet100OpenWindow(currentWindowId, 6, inventorymerchant.getInvName(), inventorymerchant.getSizeInventory()));
        MerchantRecipeList merchantrecipelist = par1IMerchant.getRecipes(this);

        if (merchantrecipelist != null)
        {
            try
            {
                ByteArrayOutputStream bytearrayoutputstream = new ByteArrayOutputStream();
                DataOutputStream dataoutputstream = new DataOutputStream(bytearrayoutputstream);
                dataoutputstream.writeInt(currentWindowId);
                merchantrecipelist.writeRecipiesToStream(dataoutputstream);
                playerNetServerHandler.sendPacketToPlayer(new Packet250CustomPayload("MC|TrList", bytearrayoutputstream.toByteArray()));
            }
            catch (IOException ioexception)
            {
                ioexception.printStackTrace();
            }
        }
    }

    /**
     * inform the player of a change in a single slot
     */
    public void updateCraftingInventorySlot(Container par1Container, int par2, ItemStack par3ItemStack)
    {
        if (par1Container.getSlot(par2) instanceof SlotCrafting)
        {
            return;
        }

        if (playerInventoryBeingManipulated)
        {
            return;
        }
        else
        {
            playerNetServerHandler.sendPacketToPlayer(new Packet103SetSlot(par1Container.windowId, par2, par3ItemStack));
            return;
        }
    }

    public void sendContainerToPlayer(Container par1Container)
    {
        sendContainerAndContentsToPlayer(par1Container, par1Container.getInventory());
    }

    public void sendContainerAndContentsToPlayer(Container par1Container, List par2List)
    {
        playerNetServerHandler.sendPacketToPlayer(new Packet104WindowItems(par1Container.windowId, par2List));
        playerNetServerHandler.sendPacketToPlayer(new Packet103SetSlot(-1, -1, inventory.getItemStack()));
    }

    /**
     * send information about the crafting inventory to the client(currently only for furnace times)
     */
    public void updateCraftingInventoryInfo(Container par1Container, int par2, int par3)
    {
        playerNetServerHandler.sendPacketToPlayer(new Packet105UpdateProgressbar(par1Container.windowId, par2, par3));
    }

    /**
     * sets current screen to null (used on escape buttons of GUIs)
     */
    public void closeScreen()
    {
        playerNetServerHandler.sendPacketToPlayer(new Packet101CloseWindow(craftingInventory.windowId));
        closeInventory();
    }

    public void sendInventoryToPlayer()
    {
        if (playerInventoryBeingManipulated)
        {
            return;
        }
        else
        {
            playerNetServerHandler.sendPacketToPlayer(new Packet103SetSlot(-1, -1, inventory.getItemStack()));
            return;
        }
    }

    public void closeInventory()
    {
        craftingInventory.onCraftGuiClosed(this);
        craftingInventory = inventorySlots;
    }

    /**
     * Adds a value to a statistic field.
     */
    public void addStat(StatBase par1StatBase, int par2)
    {
        if (par1StatBase == null)
        {
            return;
        }

        if (!par1StatBase.isIndependent)
        {
            for (; par2 > 100; par2 -= 100)
            {
                playerNetServerHandler.sendPacketToPlayer(new Packet200Statistic(par1StatBase.statId, 100));
            }

            playerNetServerHandler.sendPacketToPlayer(new Packet200Statistic(par1StatBase.statId, par2));
        }
    }

    public void mountEntityAndWakeUp()
    {
        if (ridingEntity != null)
        {
            mountEntity(ridingEntity);
        }

        if (riddenByEntity != null)
        {
            riddenByEntity.mountEntity(this);
        }

        if (sleeping)
        {
            wakeUpPlayer(true, false, false);
        }
    }

    /**
     * this function is called when a players inventory is sent to him, lastHealth is updated on any dimension
     * transitions, then reset.
     */
    public void setPlayerHealthUpdated()
    {
        lastHealth = 0xfa0a1f01;
    }

    /**
     * Add a chat message to the player
     */
    public void addChatMessage(String par1Str)
    {
        StringTranslate stringtranslate = StringTranslate.getInstance();
        String s = stringtranslate.translateKey(par1Str);
        playerNetServerHandler.sendPacketToPlayer(new Packet3Chat(s));
    }

    /**
     * Used for when item use count runs out, ie: eating completed
     */
    protected void onItemUseFinish()
    {
        playerNetServerHandler.sendPacketToPlayer(new Packet38EntityStatus(entityId, (byte)9));
        super.onItemUseFinish();
    }

    /**
     * sets the itemInUse when the use item button is clicked. Args: itemstack, int maxItemUseDuration
     */
    public void setItemInUse(ItemStack par1ItemStack, int par2)
    {
        super.setItemInUse(par1ItemStack, par2);

        if (par1ItemStack != null && par1ItemStack.getItem() != null && par1ItemStack.getItem().getItemUseAction(par1ItemStack) == EnumAction.eat)
        {
            getServerForPlayer().getEntityTracker().sendPacketToAllAssociatedPlayers(this, new Packet18Animation(this, 5));
        }
    }

    /**
     * Copies the values from the given player into this player if boolean par2 is true. Always clones Ender Chest
     * Inventory.
     */
    public void clonePlayer(EntityPlayer par1EntityPlayer, boolean par2)
    {
        super.clonePlayer(par1EntityPlayer, par2);
        lastExperience = -1;
        lastHealth = -1;
        lastFoodLevel = -1;
        destroyedItemsNetCache.addAll(((EntityPlayerMP)par1EntityPlayer).destroyedItemsNetCache);
    }

    protected void onNewPotionEffect(PotionEffect par1PotionEffect)
    {
        super.onNewPotionEffect(par1PotionEffect);
        playerNetServerHandler.sendPacketToPlayer(new Packet41EntityEffect(entityId, par1PotionEffect));
    }

    protected void onChangedPotionEffect(PotionEffect par1PotionEffect)
    {
        super.onChangedPotionEffect(par1PotionEffect);
        playerNetServerHandler.sendPacketToPlayer(new Packet41EntityEffect(entityId, par1PotionEffect));
    }

    protected void onFinishedPotionEffect(PotionEffect par1PotionEffect)
    {
        super.onFinishedPotionEffect(par1PotionEffect);
        playerNetServerHandler.sendPacketToPlayer(new Packet42RemoveEntityEffect(entityId, par1PotionEffect));
    }

    /**
     * Move the entity to the coordinates informed, but keep yaw/pitch values.
     */
    public void setPositionAndUpdate(double par1, double par3, double par5)
    {
        playerNetServerHandler.setPlayerLocation(par1, par3, par5, rotationYaw, rotationPitch);
    }

    /**
     * Called when the player performs a critical hit on the Entity. Args: entity that was hit critically
     */
    public void onCriticalHit(Entity par1Entity)
    {
        getServerForPlayer().getEntityTracker().sendPacketToAllAssociatedPlayers(this, new Packet18Animation(par1Entity, 6));
    }

    public void onEnchantmentCritical(Entity par1Entity)
    {
        getServerForPlayer().getEntityTracker().sendPacketToAllAssociatedPlayers(this, new Packet18Animation(par1Entity, 7));
    }

    /**
     * Sends the player's abilities to the server (if there is one).
     */
    public void sendPlayerAbilities()
    {
        if (playerNetServerHandler == null)
        {
            return;
        }
        else
        {
            playerNetServerHandler.sendPacketToPlayer(new Packet202PlayerAbilities(capabilities));
            return;
        }
    }

    public WorldServer getServerForPlayer()
    {
        return (WorldServer)worldObj;
    }

    public void sendGameTypeToPlayer(EnumGameType par1EnumGameType)
    {
        theItemInWorldManager.setGameType(par1EnumGameType);
        playerNetServerHandler.sendPacketToPlayer(new Packet70GameEvent(3, par1EnumGameType.getID()));
    }

    public void sendChatToPlayer(String par1Str)
    {
        playerNetServerHandler.sendPacketToPlayer(new Packet3Chat(par1Str));
    }

    /**
     * Returns true if the command sender is allowed to use the given command.
     */
    public boolean canCommandSenderUseCommand(int par1, String par2Str)
    {
        if ("seed".equals(par2Str) && !mcServer.isDedicatedServer())
        {
            return true;
        }

        if ("tell".equals(par2Str) || "help".equals(par2Str) || "me".equals(par2Str))
        {
            return true;
        }
        else
        {
            return mcServer.getConfigurationManager().areCommandsAllowed(username);
        }
    }

    public String func_71114_r()
    {
        String s = playerNetServerHandler.netManager.getSocketAddress().toString();
        s = s.substring(s.indexOf("/") + 1);
        s = s.substring(0, s.indexOf(":"));
        return s;
    }

    public void updateClientInfo(Packet204ClientInfo par1Packet204ClientInfo)
    {
        if (translator.getLanguageList().containsKey(par1Packet204ClientInfo.getLanguage()))
        {
            translator.setLanguage(par1Packet204ClientInfo.getLanguage());
        }

        int i = 256 >> par1Packet204ClientInfo.getRenderDistance();

        if (i > 3 && i < 15)
        {
            renderDistance = i;
        }

        chatVisibility = par1Packet204ClientInfo.getChatVisibility();
        chatColours = par1Packet204ClientInfo.getChatColours();

        if (mcServer.isSinglePlayer() && mcServer.getServerOwner().equals(username))
        {
            mcServer.setDifficultyForAllWorlds(par1Packet204ClientInfo.getDifficulty());
        }

        func_82239_b(1, !par1Packet204ClientInfo.func_82563_j());
    }

    public StringTranslate getTranslator()
    {
        return translator;
    }

    public int getChatVisibility()
    {
        return chatVisibility;
    }

    /**
     * on recieving this message the client (if permission is given) will download the requested textures
     */
    public void requestTexturePackLoad(String par1Str, int par2)
    {
        String s = (new StringBuilder()).append(par1Str).append("\0").append(par2).toString();
        playerNetServerHandler.sendPacketToPlayer(new Packet250CustomPayload("MC|TPack", s.getBytes()));
    }

    public ChunkCoordinates func_82114_b()
    {
        return new ChunkCoordinates(MathHelper.floor_double(posX), MathHelper.floor_double(posY + 0.5D), MathHelper.floor_double(posZ));
    }
}