package com.mrcrayfish.configured.util.fabric;

import com.mrcrayfish.configured.util.CustomValueUtil;
import net.fabricmc.loader.api.metadata.CustomValue;

import java.util.Iterator;

public class CustomValueUtilCvArrayImpl {
    public static int getSizeInternal(CustomValueUtil.CvArray array) {
        CustomValue.CvArray cvArray = CustomValueUtil.getInternalValue(array);
        return cvArray.size();
    }

    public static Iterator getIteratorInternal(CustomValueUtil.CvArray array) {
        CustomValue.CvArray cvArray = CustomValueUtil.getInternalValue(array);
        return cvArray.iterator();
    }
}
