package com.android.grafika.media3.transcode

import android.content.Context
import android.os.Looper
import androidx.media3.common.util.Clock
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.AssetLoader
import androidx.media3.transformer.DefaultDecoderFactory
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExoPlayerAssetLoader
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.google.common.collect.ImmutableMap
import kotlin.getValue

//@UnstableApi
//class MyCustomAssetLoader: AssetLoader {
//
//    class Factory(val context: Context): AssetLoader.Factory {
//
//        private val exoPlayerAssetLoaderFactory by lazy {
//            ExoPlayerAssetLoader.Factory(
//                context,
//                DefaultDecoderFactory.Builder(context).build(),
//                Clock.DEFAULT,
//                null,
//                null
//            )
//        }
//
//        override fun createAssetLoader(
//            editedMediaItem: EditedMediaItem,
//            looper: Looper,
//            listener: AssetLoader.Listener,
//            compositionSettings: AssetLoader.CompositionSettings
//        ): AssetLoader {
//            return exoPlayerAssetLoaderFactory.createAssetLoader(editedMediaItem, looper, listener, compositionSettings)
//        }
//
//    }
//
//    override fun start() {
//
//    }
//
//    override fun getProgress(progressHolder: ProgressHolder): @Transformer.ProgressState Int {
//        return Transformer.PROGRESS_STATE_UNAVAILABLE
//    }
//
//    override fun getDecoderNames(): ImmutableMap<Int, String> {
//        return ImmutableMap.Builder<Int, String>().build()
//    }
//
//    override fun release() {
//
//    }
//}