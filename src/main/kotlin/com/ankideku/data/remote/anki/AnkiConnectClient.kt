package com.ankideku.data.remote.anki

import com.ankideku.util.json
import com.ankideku.util.serializeToJsonElement
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.*
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException

class AnkiConnectClient(
    private val httpClient: HttpClient,
    private val baseUrl: String = "http://localhost:8765",
    private val version: Int = 6,
) {
    /**
     * Make a request to AnkiConnect
     */
    suspend inline fun <reified T> request(
        action: String,
        params: Map<String, Any?> = emptyMap(),
        timeoutMillis: Long = 30_000,
    ): T {
        val result = requestRaw(action, params, timeoutMillis)
        return json.decodeFromJsonElement(result)
    }

    @PublishedApi
    internal suspend fun requestRaw(
        action: String,
        params: Map<String, Any?>,
        timeoutMillis: Long,
    ): JsonElement {
        try {
            val requestBody = buildJsonObject {
                put("action", action)
                put("version", version)
                if (params.isNotEmpty()) {
                    put("params", params.serializeToJsonElement())
                }
            }

            val response: HttpResponse = httpClient.post(baseUrl) {
                contentType(ContentType.Application.Json)
                timeout {
                    requestTimeoutMillis = timeoutMillis
                }
                setBody(requestBody.toString())
            }

            val responseText = response.bodyAsText()
            val jsonResponse = json.parseToJsonElement(responseText).jsonObject

            val error = jsonResponse["error"]
            if (error != null && error !is JsonNull) {
                throw AnkiConnectException.ApiError(error.jsonPrimitive.content)
            }

            return jsonResponse["result"] ?: JsonNull

        } catch (e: ConnectException) {
            throw AnkiConnectException.NotRunning()
        } catch (e: SocketException) {
            if (e.message?.contains("reset", ignoreCase = true) == true) {
                throw AnkiConnectException.ConnectionReset()
            }
            throw AnkiConnectException.NetworkError(e)
        } catch (e: SocketTimeoutException) {
            throw AnkiConnectException.Timeout()
        } catch (e: HttpRequestTimeoutException) {
            throw AnkiConnectException.Timeout()
        } catch (e: AnkiConnectException) {
            throw e
        } catch (e: Exception) {
            throw AnkiConnectException.NetworkError(e)
        }
    }

    /**
     * Check if AnkiConnect is accessible
     */
    suspend fun ping(): Boolean {
        return try {
            val v: Int = request("version")
            v == version
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get all deck names
     */
    suspend fun getDeckNames(): List<String> {
        return request("deckNames")
    }

    /**
     * Get deck names with IDs
     */
    suspend fun getDeckNamesAndIds(): Map<String, Long> {
        return request("deckNamesAndIds")
    }

    /**
     * Find notes matching a query
     */
    suspend fun findNotes(query: String): List<Long> {
        return request("findNotes", mapOf("query" to query))
    }

    /**
     * Get detailed information about notes
     */
    suspend fun notesInfo(noteIds: List<Long>): List<AnkiNoteInfo> {
        return request("notesInfo", mapOf("notes" to noteIds))
    }

    /**
     * Get detailed information about cards (includes deck names)
     */
    suspend fun cardsInfo(cardIds: List<Long>): List<AnkiCardInfo> {
        return request("cardsInfo", mapOf("cards" to cardIds))
    }

    /**
     * Update fields of a note
     */
    suspend fun updateNoteFields(noteId: Long, fields: Map<String, String>) {
        request<JsonElement>("updateNoteFields", mapOf(
            "note" to mapOf(
                "id" to noteId,
                "fields" to fields
            )
        ))
    }

    /**
     * Batch update multiple notes using multi action
     */
    suspend fun batchUpdateNotes(updates: List<Pair<Long, Map<String, String>>>): List<JsonElement?> {
        val actions = updates.map { (noteId, fields) ->
            mapOf(
                "action" to "updateNoteFields",
                "params" to mapOf(
                    "note" to mapOf(
                        "id" to noteId,
                        "fields" to fields
                    )
                )
            )
        }
        return request("multi", mapOf("actions" to actions))
    }
}
