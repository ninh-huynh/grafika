package com.android.grafika.transcode;

import android.media.MediaCodec;

/**
 * This interface exposes the minimum set of {@link InputSurface} APIs used in {@link EncodeDecodeTest}.
 */
public interface InputSurfaceInterface {

    void makeCurrent();

    boolean swapBuffers();

    void setPresentationTime(long nsecs);

    void configure(MediaCodec codec);

    void configure(NdkMediaCodec codec);

    void updateSize(int width, int height);

    void release();

}