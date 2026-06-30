package com.fviret.podometre.data.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Accès aux données de santé via Health Connect.
 * Ne stocke jamais les données localement — toujours lu depuis la source.
 * Équivalent iOS : StepCountViewModel / HealthKit queries.
 */
@Singleton
class HealthConnectRepository @Inject constructor(
    private val client: HealthConnectClient,
    @ApplicationContext private val context: Context
) {

    /**
     * Lit le nombre total de pas entre [from] et [to].
     * Requête idempotente : recalcule depuis [from], ne jamais incrémenter.
     */
    suspend fun readSteps(from: Instant, to: Instant = Instant.now()): Long {
        val request = ReadRecordsRequest(
            recordType = StepsRecord::class,
            timeRangeFilter = TimeRangeFilter.between(from, to)
        )
        return runCatching {
            client.readRecords(request).records.sumOf { it.count }
        }.getOrDefault(0L)
    }

    /**
     * Lit la distance totale parcourue (en km) entre [from] et [to].
     * Requête idempotente : recalcule depuis [from], ne jamais incrémenter.
     */
    suspend fun readDistance(from: Instant, to: Instant = Instant.now()): Double {
        val request = ReadRecordsRequest(
            recordType = DistanceRecord::class,
            timeRangeFilter = TimeRangeFilter.between(from, to)
        )
        return runCatching {
            client.readRecords(request).records.sumOf { it.distance.inKilometers }
        }.getOrDefault(0.0)
    }

    /** Retourne true si Health Connect est installé et disponible sur cet appareil. */
    fun isAvailable(): Boolean =
        HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    /**
     * Vérifie si toutes les permissions de lecture requises (pas, distance) sont accordées.
     */
    suspend fun hasAllPermissions(): Boolean =
        runCatching {
            client.permissionController.getGrantedPermissions().containsAll(PERMISSIONS)
        }.getOrDefault(false)

    companion object {
        /** Permissions de lecture demandées à l'onboarding (KAN-18). */
        val PERMISSIONS: Set<String> = setOf(
            HealthPermission.getReadPermission(StepsRecord::class),
            HealthPermission.getReadPermission(DistanceRecord::class),
        )

        /** Contrat système pour la demande de permissions Health Connect. */
        fun requestPermissionsContract() = PermissionController.createRequestPermissionResultContract()
    }
}
