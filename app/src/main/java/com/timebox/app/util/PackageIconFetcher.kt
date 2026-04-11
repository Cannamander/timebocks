package com.timebox.app.util

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.ColorDrawable
import android.graphics.Color
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.Fetcher
import coil.request.Options
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Coil model type for loading launcher icons by package name (avoids URL ambiguity). */
@JvmInline
value class PackageIcon(val packageName: String)

class PackageIconFetcher(
    private val context: Context,
    private val packageName: String,
) : Fetcher {
    override suspend fun fetch(): FetchResult {
        val drawable = try {
            context.packageManager.getApplicationIcon(packageName)
        } catch (_: PackageManager.NameNotFoundException) {
            return DrawableResult(
                drawable = ColorDrawable(Color.TRANSPARENT),
                isSampled = false,
                dataSource = DataSource.MEMORY
            )
        }
        return DrawableResult(
            drawable = drawable,
            isSampled = false,
            dataSource = DataSource.DISK
        )
    }

    class Factory(
        private val context: Context
    ) : Fetcher.Factory<PackageIcon> {
        override fun create(
            data: PackageIcon,
            options: Options,
            imageLoader: ImageLoader
        ): Fetcher = PackageIconFetcher(context, data.packageName)
    }
}

@Singleton
class TimeboxImageLoader @Inject constructor(
    @ApplicationContext context: Context
) {
    val imageLoader: ImageLoader = ImageLoader.Builder(context)
        .components {
            add(PackageIconFetcher.Factory(context))
        }
        .build()
}
