package com.fviret.podometre.data.weather

import android.content.Context
import android.util.Log
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.IOException
import java.time.LocalDate

/**
 * Teste [WeatherRepository] : classification RAIN_NOW/RAIN_SOON/NO_RAIN, parsing des
 * prévisions journalières Open-Meteo, cache 30 min, et tolérance aux réponses invalides.
 * [OkHttpClient] est mocké — aucun appel réseau réel.
 */
class WeatherRepositoryTest {

    private val context = mockk<Context>(relaxed = true)
    private val okHttpClient = mockk<OkHttpClient>()
    private val call = mockk<Call>()
    private lateinit var repository: WeatherRepository

    @BeforeEach
    fun setup() {
        mockkStatic(Log::class)
        every { Log.w(any<String>(), any<String>(), any<Throwable>()) } returns 0
        every { okHttpClient.newCall(any()) } returns call
        // Le cache disque utilise context.filesDir — on renvoie un répertoire temp valide.
        every { context.filesDir } returns kotlin.io.path.createTempDirectory("weather_test").toFile()
        repository = WeatherRepository(context, okHttpClient)
    }

    @AfterEach
    fun teardown() {
        unmockkStatic(Log::class)
    }

    private fun mockResponse(json: String) {
        every { call.execute() } returns Response.Builder()
            .request(Request.Builder().url("https://api.open-meteo.com/v1/forecast").build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(json.toResponseBody("application/json".toMediaType()))
            .build()
    }

    private fun openMeteoJson(
        currentTime: String = "2026-01-01T10:00",
        currentWeatherCode: Int = 1,
        currentPrecipitation: Double = 0.0,
        hourlyTime: List<String> = listOf("2026-01-01T10:00", "2026-01-01T11:00"),
        hourlyWeatherCode: List<Int> = listOf(1, 1),
        dailyTime: List<String> = List(7) { "2026-01-0${it + 1}" },
        dailyWeatherCode: List<Int> = List(7) { 0 },
        dailyTempMax: List<Double> = List(7) { 20.0 },
        dailyTempMin: List<Double> = List(7) { 10.0 },
        dailyPrecipitationSum: List<Double> = List(7) { 0.0 },
    ): String {
        fun strList(l: List<String>) = l.joinToString(",") { "\"$it\"" }
        fun intList(l: List<Int>) = l.joinToString(",")
        fun doubleList(l: List<Double>) = l.joinToString(",")
        return """
            {
              "current": {"time": "$currentTime", "weather_code": $currentWeatherCode, "precipitation": $currentPrecipitation},
              "hourly": {"time": [${strList(hourlyTime)}], "weather_code": [${intList(hourlyWeatherCode)}]},
              "daily": {
                "time": [${strList(dailyTime)}],
                "weather_code": [${intList(dailyWeatherCode)}],
                "temperature_2m_max": [${doubleList(dailyTempMax)}],
                "temperature_2m_min": [${doubleList(dailyTempMin)}],
                "precipitation_sum": [${doubleList(dailyPrecipitationSum)}]
              }
            }
        """.trimIndent()
    }

    @Test
    fun `getWeatherState retourne RAIN_NOW quand il pleut actuellement (precipitation supérieure à 0)`() = runTest {
        mockResponse(openMeteoJson(currentWeatherCode = 1, currentPrecipitation = 0.5))

        assertEquals(WeatherState.RAIN_NOW, repository.getWeatherState(48.85, 2.35))
    }

    @Test
    fun `getWeatherState retourne RAIN_NOW quand le code courant est un code de pluie`() = runTest {
        mockResponse(openMeteoJson(currentWeatherCode = 61, currentPrecipitation = 0.0))

        assertEquals(WeatherState.RAIN_NOW, repository.getWeatherState(48.85, 2.35))
    }

    @Test
    fun `getWeatherState retourne RAIN_SOON quand la pluie est prevue a l'heure suivante`() = runTest {
        mockResponse(
            openMeteoJson(
                currentTime = "2026-01-01T10:00",
                currentWeatherCode = 1,
                currentPrecipitation = 0.0,
                hourlyTime = listOf("2026-01-01T10:00", "2026-01-01T11:00"),
                hourlyWeatherCode = listOf(1, 61),
            )
        )

        assertEquals(WeatherState.RAIN_SOON, repository.getWeatherState(48.85, 2.35))
    }

    @Test
    fun `getWeatherState retourne NO_RAIN quand aucune pluie n'est prevue`() = runTest {
        mockResponse(
            openMeteoJson(
                hourlyTime = listOf("2026-01-01T10:00", "2026-01-01T11:00"),
                hourlyWeatherCode = listOf(1, 2),
            )
        )

        assertEquals(WeatherState.NO_RAIN, repository.getWeatherState(48.85, 2.35))
    }

    @Test
    fun `getDailyForecasts parse les 7 jours de prevision`() = runTest {
        mockResponse(
            openMeteoJson(
                dailyTime = listOf(
                    "2026-01-01", "2026-01-02", "2026-01-03", "2026-01-04",
                    "2026-01-05", "2026-01-06", "2026-01-07",
                ),
                dailyWeatherCode = listOf(0, 1, 2, 3, 61, 80, 95),
                dailyTempMax = listOf(24.0, 22.0, 19.0, 17.0, 21.0, 25.0, 23.0),
                dailyTempMin = listOf(14.0, 13.0, 11.0, 10.0, 12.0, 15.0, 14.0),
                dailyPrecipitationSum = listOf(0.0, 0.0, 0.0, 5.2, 1.8, 0.0, 3.0),
            )
        )

        val forecasts = repository.getDailyForecasts(48.85, 2.35)

        assertEquals(7, forecasts.size)
        assertEquals(
            DailyForecast(
                date = LocalDate.of(2026, 1, 4),
                weatherCode = 3,
                tempMaxCelsius = 17.0,
                tempMinCelsius = 10.0,
                precipitationMm = 5.2,
            ),
            forecasts[3]
        )
    }

    @Test
    fun `getDailyForecasts ignore une date invalide sans faire echouer les autres jours`() = runTest {
        mockResponse(
            openMeteoJson(
                dailyTime = listOf("2026-01-01", "date-invalide", "2026-01-03"),
                dailyWeatherCode = listOf(0, 1, 2),
                dailyTempMax = listOf(20.0, 21.0, 22.0),
                dailyTempMin = listOf(10.0, 11.0, 12.0),
                dailyPrecipitationSum = listOf(0.0, 0.0, 0.0),
            )
        )

        val forecasts = repository.getDailyForecasts(48.85, 2.35)

        assertEquals(2, forecasts.size)
        assertTrue(forecasts.none { it.date == LocalDate.of(2026, 1, 1) && it.weatherCode == 1 })
    }

    @Test
    fun `getWeatherState met en cache et n'appelle le reseau qu'une fois pour deux appels rapproches`() = runTest {
        mockResponse(openMeteoJson())

        repository.getWeatherState(48.85, 2.35)
        repository.getWeatherState(48.85, 2.35)

        verify(exactly = 1) { okHttpClient.newCall(any()) }
    }

    @Test
    fun `getDailyForecasts reutilise le cache partage avec getWeatherState`() = runTest {
        mockResponse(openMeteoJson())

        repository.getWeatherState(48.85, 2.35)
        repository.getDailyForecasts(48.85, 2.35)

        verify(exactly = 1) { okHttpClient.newCall(any()) }
    }

    @Test
    fun `getWeatherState retourne null si l'appel reseau echoue`() = runTest {
        every { call.execute() } throws IOException("network down")

        assertNull(repository.getWeatherState(48.85, 2.35))
    }

    @Test
    fun `getDailyForecasts retourne une liste vide si la reponse est illisible`() = runTest {
        mockResponse("{ceci n'est pas du JSON valide}")

        assertEquals(emptyList<DailyForecast>(), repository.getDailyForecasts(48.85, 2.35))
    }
}
