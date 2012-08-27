package net.minecraft.src;

import java.util.List;
import net.minecraft.client.Minecraft;

public class CommandClientEmote extends CommandServerEmote
{
    public CommandClientEmote()
    {
    }

    public void processCommand(ICommandSender par1ICommandSender, String par2ArrayOfStr[])
    {
        if (par2ArrayOfStr.length > 0)
        {
            String s = joinString(par2ArrayOfStr, 0);
            Minecraft.getMinecraft().thePlayer.sendChatToPlayer("* "+par1ICommandSender.getCommandSenderName()+" "+s);
            return;
        }
        else
        {
            throw new WrongUsageException("commands.me.usage", new Object[0]);
        }
    }

    /**
     * Adds the strings available in this command to the given list of tab completion options.
     */
    public List addTabCompletionOptions(ICommandSender par1ICommandSender, String par2ArrayOfStr[])
    {
        return getListOfStringsMatchingLastWord(par2ArrayOfStr, Minecraft.getMinecraft().getAllUsernames());
    }
}