package com.mrcrayfish.configured.util.fabric;

import com.mrcrayfish.configured.util.CustomValueUtil;
import net.fabricmc.loader.api.metadata.CustomValue;

public class CustomValueUtilCustomValueImpl {
    public static String getAsStringInternal(CustomValueUtil.CustomValue customValue) {
        CustomValue value = CustomValueUtil.getInternalValue(customValue);
        return value.getAsString();
    }

    public static CustomValueUtil.CvArray getAsArrayInternal(CustomValueUtil.CustomValue customValue) {
        CustomValue value = CustomValueUtil.getInternalValue(customValue);
        return new CustomValueUtil.CvArray(value.getAsArray());
    }

    public static CustomValueUtil.CvType getTypeInternal(CustomValueUtil.CustomValue customValue) {
        CustomValue value = CustomValueUtil.getInternalValue(customValue);
        if (value == null) return null;
        return CustomValueUtil.CvType.values()[value.getType().ordinal()];
    }

    public static CustomValueUtil.CvObject getAsObjectInternal(CustomValueUtil.CustomValue customValue) {
        CustomValue value = CustomValueUtil.getInternalValue(customValue);
        return new CustomValueUtil.CvObject(value.getAsObject());
    }
}
