/**
 * 视频帧数据
 * @author guangren.gu  2020.6.2
 * @version 1.0
 */
package com.yqunity.ksymediaplayer4unity;

public class VideoFrameData {
    public byte[] buffer;
    public int bufferSize;
    public int videoWidth;
    public int videoHeight;
    public VideoFrameData(byte[] buffer, int size, int width, int height)
    {
        this.buffer = buffer;
        bufferSize = size;
        videoWidth = width;
        videoHeight = height;
    }

    public VideoFrameData(){}
}
