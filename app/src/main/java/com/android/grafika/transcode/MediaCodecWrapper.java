package com.android.grafika.transcode;

import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaCodec.Callback;
import android.media.MediaFormat;
import android.os.Bundle;
import android.view.Surface;

import java.nio.ByteBuffer;

/**
 * This interface exposes the minimum set of {@link MediaCodec} APIs tested in {@link EncodeDecodeTest}
 * and {@link VpxEncoderTest}.
 */
public interface MediaCodecWrapper {

    void release();

    void configure(MediaFormat format, int flags);

    void configure(MediaFormat format, int flags, Surface surface);

    void setInputSurface(InputSurfaceInterface inputSurface);

    InputSurfaceInterface createInputSurface();

    void start();

    void stop();

    int dequeueOutputBuffer(BufferInfo info, long timeoutUs);

    void releaseOutputBuffer(int index, boolean render);

    void signalEndOfInputStream();

    String getOutputFormatString();

    ByteBuffer getOutputBuffer(int index);

    ByteBuffer[] getOutputBuffers();

    ByteBuffer getInputBuffer(int index);

    ByteBuffer[] getInputBuffers();

    void queueInputBuffer(
            int index,
            int offset,
            int size,
            long presentationTimeUs,
            int flags);

    int dequeueInputBuffer(long timeoutUs);

    void setParameters(Bundle params);

    void setCallback(Callback mCallback);

}