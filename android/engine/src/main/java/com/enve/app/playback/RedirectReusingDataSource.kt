package com.enve.app.playback

import android.net.Uri
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

@UnstableApi
internal class RedirectReusingDataSource(
    private val upstream: DataSource,
    private val redirects: ConcurrentHashMap<Uri, Uri>,
) : DataSource {

    override fun addTransferListener(transferListener: TransferListener) {
        upstream.addTransferListener(transferListener)
    }

    override fun open(dataSpec: DataSpec): Long {
        val requested = dataSpec.uri
        redirects[requested]?.let { resolved ->
            try {
                return upstream.open(dataSpec.withUri(resolved))
            } catch (_: IOException) {
                upstream.close()
                redirects.remove(requested, resolved)
            }
        }
        val length = upstream.open(dataSpec)
        upstream.uri?.takeIf { it != requested }?.let { redirects[requested] = it }
        return length
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
        upstream.read(buffer, offset, length)

    override fun getUri(): Uri? = upstream.uri

    override fun getResponseHeaders(): Map<String, List<String>> = upstream.responseHeaders

    override fun close() {
        upstream.close()
    }
}
