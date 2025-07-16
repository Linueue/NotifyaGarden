package com.strling.notifyagarden

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.google.protobuf.InvalidProtocolBufferException
import com.strling.notifyagarden.proto.GameItemsOuterClass.GameItems
import java.io.InputStream
import java.io.OutputStream

object GameItemsSerializer: Serializer<GameItems> {
    override val defaultValue: GameItems
        get() = GameItems.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): GameItems {
        try {
            return GameItems.parseFrom(input)
        } catch(e: InvalidProtocolBufferException)
        {
            return defaultValue
        }
    }

    override suspend fun writeTo(
        t: GameItems,
        output: OutputStream
    ) {
        t.writeTo(output)
    }
}

val Context.gameItemsDataStore: DataStore<GameItems> by dataStore(
    fileName = "gameitems.pb",
    serializer = GameItemsSerializer
)