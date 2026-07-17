package com.fviret.podometre.fakes

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.aggregate.AggregationResult
import androidx.health.connect.client.aggregate.AggregationResultGroupedByDuration
import androidx.health.connect.client.aggregate.AggregationResultGroupedByPeriod
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.request.AggregateGroupByDurationRequest
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ChangesTokenRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.response.ChangesResponse
import androidx.health.connect.client.response.InsertRecordsResponse
import androidx.health.connect.client.response.ReadRecordResponse
import androidx.health.connect.client.response.ReadRecordsResponse
import androidx.health.connect.client.time.TimeRangeFilter
import kotlin.reflect.KClass

/** Stub de [PermissionController] — jamais sollicité par les écrans testés en mode émulateur. */
private class FakePermissionController : PermissionController {
    override suspend fun getGrantedPermissions(): Set<String> = emptySet()
    override suspend fun revokeAllPermissions() = Unit
}

/**
 * Stub de [HealthConnectClient] pour les tests d'intégration UI.
 * Sur l'émulateur, [com.fviret.podometre.util.isEmulator] court-circuite tous les écrans
 * testés vers des données mock avant qu'un appel à ce client ne soit nécessaire —
 * ce stub n'existe que pour satisfaire la construction de [HealthConnectRepository].
 */
class FakeHealthConnectClient : HealthConnectClient {

    override val permissionController: PermissionController = FakePermissionController()

    override suspend fun insertRecords(records: List<Record>): InsertRecordsResponse =
        error("FakeHealthConnectClient: non utilisé en mode émulateur")

    override suspend fun updateRecords(records: List<Record>) =
        error("FakeHealthConnectClient: non utilisé en mode émulateur")

    override suspend fun deleteRecords(
        recordType: KClass<out Record>,
        recordIdsList: List<String>,
        clientRecordIdsList: List<String>,
    ) = error("FakeHealthConnectClient: non utilisé en mode émulateur")

    override suspend fun deleteRecords(
        recordType: KClass<out Record>,
        timeRangeFilter: TimeRangeFilter,
    ) = error("FakeHealthConnectClient: non utilisé en mode émulateur")

    override suspend fun <T : Record> readRecord(
        recordType: KClass<T>,
        recordId: String,
    ): ReadRecordResponse<T> = error("FakeHealthConnectClient: non utilisé en mode émulateur")

    override suspend fun <T : Record> readRecords(
        request: ReadRecordsRequest<T>,
    ): ReadRecordsResponse<T> = ReadRecordsResponse(emptyList(), null)

    override suspend fun aggregate(request: AggregateRequest): AggregationResult =
        error("FakeHealthConnectClient: non utilisé en mode émulateur")

    override suspend fun aggregateGroupByDuration(
        request: AggregateGroupByDurationRequest,
    ): List<AggregationResultGroupedByDuration> =
        error("FakeHealthConnectClient: non utilisé en mode émulateur")

    override suspend fun aggregateGroupByPeriod(
        request: AggregateGroupByPeriodRequest,
    ): List<AggregationResultGroupedByPeriod> =
        error("FakeHealthConnectClient: non utilisé en mode émulateur")

    override suspend fun getChangesToken(request: ChangesTokenRequest): String =
        error("FakeHealthConnectClient: non utilisé en mode émulateur")

    override suspend fun getChanges(changesToken: String): ChangesResponse =
        error("FakeHealthConnectClient: non utilisé en mode émulateur")
}
