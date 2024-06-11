package com.emm.justchill.experiences.readassets.data

import android.content.Context
import com.emm.justchill.core.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStream

private const val JSON_PATH = "experiences.json"

class ExperiencesLocalDataSource(
    dispatchers: Dispatchers,
    private val context: Context,
) : ExperiencesDataSource, Dispatchers by dispatchers {

    override fun readExperiences(): Flow<List<ExperiencesLocalModel>> {
        return flow {
            val inputStream: InputStream = context.assets.open(JSON_PATH)
            val stringReader: String = inputStream.bufferedReader().use(BufferedReader::readText)
            emit(Json.decodeFromString<List<ExperiencesLocalModel>>(stringReader))
        }.flowOn(ioDispatcher)
    }
}