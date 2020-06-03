
/**
 * 插件主逻辑处理类。负责调用KSYMediaPlayer SDK来为Unity提供调用接口
 * @author guangren.gu  2020.6.2
 * @version 1.0
 */
package com.yqunity.ksymediaplayer4unity;

import android.content.Context;
import com.ksyun.media.player.IMediaPlayer;
import com.ksyun.media.player.KSYMediaPlayer;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class KSMedia4Unity {
    private Context mContext;
    private KSYMediaPlayer mMediaPlayer;
    private INative2UnityNotifier mNotifier;
    private int mVideoWidth;
    private int mVideoHeight;
    private int mvideoWidthAfterAlign;
    private int mVideoBufferSize;
    private int mVideoDataOutputFormat = 1;
    private ByteBuffer mRawBuffer[];
    private String mVideoUrl;
    private List<VideoFrameData> mFrameDataPool;

    private IMediaPlayer.OnPreparedListener mOnPrepared;
    private IMediaPlayer.OnCompletionListener mOnComplete;
    private IMediaPlayer.OnErrorListener mOnError;
    private IMediaPlayer.OnInfoListener mOnInfo;
    private IMediaPlayer.OnVideoSizeChangedListener mOnVideoSizeChanged;
    private KSYMediaPlayer.OnVideoRawDataListener mOnVideoRawData;

    public KSMedia4Unity(Context context)
    {
        init();
        mMediaPlayer = new KSYMediaPlayer.Builder(context).build();
        mMediaPlayer.setVideoScalingMode(0);//不渲染视频内容
        mMediaPlayer.setLogEnabled(false);
        mMediaPlayer.setOnCompletionListener(mOnComplete);
        mMediaPlayer.setOnPreparedListener(mOnPrepared);
        mMediaPlayer.setOnInfoListener(mOnInfo);
        mMediaPlayer.setOnVideoSizeChangedListener(mOnVideoSizeChanged);
        mMediaPlayer.setOnErrorListener(mOnError);
        mContext = context;
    }

    /**
     * 设置Unity侧的通知器
     * 注意：：该方法由unity侧调用
     * @param notifier
     */
    public void setUnityNotifier(INative2UnityNotifier notifier)
    {
        mNotifier = notifier;
    }

    public KSYMediaPlayer getRawMediaPlayerInstance()
    {
        return mMediaPlayer;
    }

    /**
     * 设置输出的数据格式
     * @param format 0：YUV420格式； 1：ABGR8888格式
     */
    public void setOutputDataFormat(int format)
    {
        mVideoDataOutputFormat = format;
    }

    public void play(String videoUrl)
    {
        if(mMediaPlayer != null){
            try {
                if(mVideoDataOutputFormat == 0){
                    // 输出视频格式为 YUV(一般情况是YUV420)
                    mMediaPlayer.setOption(KSYMediaPlayer.OPT_CATEGORY_PLAYER, "overlay-format", KSYMediaPlayer.SDL_FCC_YV12);
                }
                else{
                    // 输出视频格式为 ABGR8888
                    mMediaPlayer.setOption(KSYMediaPlayer.OPT_CATEGORY_PLAYER, "overlay-format", KSYMediaPlayer.SDL_FCC_RV32);
                }
                mVideoUrl = videoUrl;
                mMediaPlayer.setDataSource(videoUrl);
                mMediaPlayer.prepareAsync();
            } catch (IOException e) {
                e.printStackTrace();
                msgFormatter("exception", e.getMessage());
            }
        }
    }

    public void stop()
    {
        if(mMediaPlayer != null){
            mMediaPlayer.stop();
            mMediaPlayer.release();
        }
    }

    public void pause()
    {

    }

    /**
     * 重连
     */
    public void softReset(String videoUrl)
    {
        if(mMediaPlayer != null){
            String url = videoUrl == null || videoUrl.isEmpty() ? mVideoUrl : videoUrl;
            mMediaPlayer.softReset();
            try
            {
                mMediaPlayer.setDataSource(url);
                mMediaPlayer.prepareAsync();
            }
            catch (IOException e){
                e.printStackTrace();
                msgFormatter("exception", e.getMessage());
            }
        }
    }

    public void softReset()
    {
        softReset(null);
    }

    public void reload(String videoUrl)
    {
        if(mMediaPlayer != null){
            String url = videoUrl == null || videoUrl.isEmpty() ? mVideoUrl : videoUrl;
            mMediaPlayer.reload(url, true, KSYMediaPlayer.KSYReloadMode.KSY_RELOAD_MODE_ACCURATE);
        }

    }

    public void reload()
    {
        reload(null);
    }

    public void setBufferTimeMax(float time)
    {
        if(mMediaPlayer != null)
        {
            mMediaPlayer.setBufferTimeMax(time);
        }
    }

    /**
     * 回收帧数据.
     * 此方法由Unity侧调用
     * @param data
     */
    public void RecycleFrameData(VideoFrameData data)
    {
        if(data != null){
            mFrameDataPool.add(data);
           // msgFormatter("normal", String.valueOf(mFrameDataPool.size()));
        }
    }

    /**
     * 释放所有引用类型的引用
     */
    public void Destroy()
    {
        mContext = null;
        mMediaPlayer.release();
        mMediaPlayer = null;
        mNotifier = null;
        mRawBuffer = null;
        mVideoUrl = null;
        mFrameDataPool.clear();
        mFrameDataPool = null;
    }

    /**
     * @param width 视频原始宽度
     * @param align 地址对齐的基值，输出视频格式不同时，此值亦不同
     * @return 地址对齐后的值
     */
    private int alignWidth(int width, int align) {
        return (width + align - 1) / align * align;
    }

    private void init()
    {
        mRawBuffer = new ByteBuffer[5];
        mFrameDataPool = new ArrayList<VideoFrameData>();

        mOnPrepared = new IMediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(IMediaPlayer mp) {
                mVideoWidth = mMediaPlayer.getVideoWidth();
                mVideoHeight = mMediaPlayer.getVideoHeight();
                setRawDataBuffer();
                if(mNotifier != null){
                    mNotifier.onVideoPrepared(mVideoWidth, mVideoHeight);
                }
                // 设置视频伸缩模式，此模式为裁剪模式
                mMediaPlayer.setVideoScalingMode(KSYMediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
                // 开始播放视频
                mMediaPlayer.start();
            }
        };

        mOnVideoRawData = new KSYMediaPlayer.OnVideoRawDataListener(){
            @Override
            public void onVideoRawDataAvailable(IMediaPlayer mp, byte[] buf, int size, int width, int height, int format, long pts) {
                if(mNotifier != null){
                    VideoFrameData data = getFrameDataFromPool();
                    data.buffer = buf;
                    data.bufferSize = size;
                    data.videoWidth = width;
                    data.videoHeight = height;
                    mNotifier.onVideoFrameDataArrive(data);
                }
                mMediaPlayer.addVideoRawBuffer(buf);
            }
        };

        mOnInfo = new KSYMediaPlayer.OnInfoListener(){
            @Override
            public boolean onInfo(IMediaPlayer mp, int i, int extra) {
                    mNotifier.onMediaPlayerInfo(i);
                return false;
            }
        };

        mOnError = new IMediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(IMediaPlayer mp, int i, int extra) {
                mNotifier.onMediaPlayerInfo(i);
                return false;
            }
        };
    }

    private VideoFrameData getFrameDataFromPool()
    {
        if(mFrameDataPool.size() == 0){
            return new VideoFrameData();
        }
        return mFrameDataPool.remove(0);
    }

    private void setRawDataBuffer()
    {
        switch (mVideoDataOutputFormat){
            case 0:
                mvideoWidthAfterAlign = alignWidth(mVideoWidth, 16);
                mVideoBufferSize = mvideoWidthAfterAlign * mVideoHeight * 3 / 2;
                break;
            case 1:
                mvideoWidthAfterAlign = alignWidth(mVideoWidth, 4);
                mVideoBufferSize = mvideoWidthAfterAlign * mVideoHeight * 4;
                break;
        }

        mMediaPlayer.setVideoRawDataListener(mOnVideoRawData);

        for(int index=0; index<mRawBuffer.length; index++) {
            mRawBuffer[index] = ByteBuffer.allocate(mVideoBufferSize);
            mMediaPlayer.addVideoRawBuffer(mRawBuffer[index].array());
        }
    }

    private void msgFormatter(String type, String msg)
    {
        if(mNotifier != null){
            mNotifier.onMsg(String.format("[%s] %s", type, msg));
        }
    }
}
