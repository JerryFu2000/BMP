package com.FjUtils;

import java.text.SimpleDateFormat;

public class FjTimeUtils {

    public static String format(long ms){
        SimpleDateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
        String dateStr = dateformat.format(ms);
        return dateStr;
    }
}
