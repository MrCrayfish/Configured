package com.mrcrayfish.configured.util.forge;

import com.mrcrayfish.configured.util.CustomValueUtil;

import java.util.List;

public class CustomValueUtilCustomValueImpl {
    public static String getAsStringInternal(CustomValueUtil.CustomValue customValue) {
        return CustomValueUtil.getInternalValue(customValue);
    }

    public static CustomValueUtil.CvArray getAsArrayInternal(CustomValueUtil.CustomValue customValue) {
        return new CustomValueUtil.CvArray(CustomValueUtil.getInternalValue(customValue));
    }

    public static CustomValueUtil.CvType getTypeInternal(CustomValueUtil.CustomValue customValue) {
        if (CustomValueUtil.getInternalValue(customValue) instanceof String) return CustomValueUtil.CvType.STRING;
        if (CustomValueUtil.getInternalValue(customValue) instanceof List<?>) return CustomValueUtil.CvType.ARRAY;
        return CustomValueUtil.CvType.NULL;
    }

    public static CustomValueUtil.CvObject getAsObjectInternal(CustomValueUtil.CustomValue customValue) {
        return null;
    }
}
