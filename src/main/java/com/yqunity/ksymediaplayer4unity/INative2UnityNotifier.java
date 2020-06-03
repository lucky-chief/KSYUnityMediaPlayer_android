/**
 * Unity侧通知器，该接口文件由unity侧实现
 * @author guangren.gu  2020.6.2
 * @version 1.0
 */
package com.yqunity.ksymediaplayer4unity;

public interface INative2UnityNotifier {

    /**
     * 视频流已经准备好，主要用于unity侧获取视频宽高来创建相应尺寸的纹理进行视频渲染。
     * @param width
     * @param height
     */
    void onVideoPrepared(int width, int height);

    /**
     * 视频帧数据到达
     * @param buf 视频数据
     * @param size 数据长度
     * @param width 视频宽
     * @param height 视频高
     */
    void onVideoFrameDataArrive(VideoFrameData data);

    /**
     * 转发setOnInfoListener 和 setOnErrorListener 两个监听器的信息给unity
     * @param info
     */
    void onMediaPlayerInfo(int info);

    /**
     * 发送消息给unity，可以是各种信息。
     * 一般msg格式: [normal] msgmsgmsg
     * 错误msg格式：[Error] msgmsgmsg
     * 异常msg格式：[exception] msgmsgmsg
     * @param msg 消息内容
     */
    void onMsg(String msg);
}
