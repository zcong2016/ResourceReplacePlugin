package com.zcong.plugin.resourcereplace;

public class Config {

    public static final String OPTIMIZE_WEBP_CONVERT = "ConvertWebp"; //webp化
    public static final String OPTIMIZE_COMPRESS_PICTURE = "Compress"; //压缩图片

    public boolean enableWhenDebug = true;
    public String[] whiteList = new String[]{}; //优化图片白名单
    public int[] oColor = new int[]{}; //优化图片白名单
    public int[] nColor = new int[]{}; //优化图片白名单
    public String[] picList = new String[]{}; //优化图片白名单

    public boolean multiThread = true;
    public String[] bigImageWhiteList = new String[]{}; //大图检测白名单


    public void enableWhenDebug(boolean enableWhenDebug) {
        this.enableWhenDebug = enableWhenDebug;
    }


    public void whiteList(String[] whiteList) {
        this.whiteList = whiteList;
    }

    public void multiThread(boolean multiThread) {
        this.multiThread = multiThread;
    }

    public void bigImageWhiteList(String[] bigImageWhiteList) {
        this.bigImageWhiteList = bigImageWhiteList;
    }
}
