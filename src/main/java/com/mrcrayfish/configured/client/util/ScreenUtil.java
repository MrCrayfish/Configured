package com.mrcrayfish.configured.client.util;

/**
 * Author: MrCrayfish
 */
public class ScreenUtil
{
    public static boolean isMouseWithin(int x, int y, int width, int height, int mouseX, int mouseY)
    {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
