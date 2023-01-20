package com.mrcrayfish.configured.util.fabric;

import com.mrcrayfish.configured.util.CustomValueUtil;
import net.fabricmc.loader.api.metadata.CustomValue;

public class CustomValueUtilCvObjectImpl {
    public static int getSizeInternal(CustomValueUtil.CvObject cvObject) {
        CustomValue.CvObject object = CustomValueUtil.getInternalValue(cvObject);
        return object.size();
    }

    public static boolean containsKeyInternal(String key, CustomValueUtil.CvObject cvObject) {
        CustomValue.CvObject object = CustomValueUtil.getInternalValue(cvObject);
        return object.containsKey(key);
    }

    public static CustomValueUtil.CustomValue getInternal(String key, CustomValueUtil.CvObject cvObject) {
        CustomValue.CvObject object = CustomValueUtil.getInternalValue(cvObject);
        return new CustomValueUtil.CustomValue(object.get(key));
    }
}
