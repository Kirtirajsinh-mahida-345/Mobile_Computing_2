package com.example.a2

import android.os.Bundle
import android.widget.DatePicker
import android.widget.Toast
import java.util.*
import android.os.Build
import okhttp3.Request
import org.json.JSONObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import android.widget.Button
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.room.Room
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import androidx.appcompat.app.AppCompatActivity
import java.time.LocalDate


class MainActivity : AppCompatActivity()
{
    private lateinit var room: AppDatabase
    private lateinit var minTemperatureTextView: TextView
    private lateinit var maxTemperatureTextView: TextView

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        room = Room.databaseBuilder(this, AppDatabase::class.java, "TemperatureData").build()

        val datePicker = findViewById<DatePicker>(R.id.datePicker)
        val findButton = findViewById<Button>(R.id.findButton)
        minTemperatureTextView = findViewById<TextView>(R.id.minTemperatureTextView)
        maxTemperatureTextView = findViewById<TextView>(R.id.maxTemperatureTextView)

        val calendar = Calendar.getInstance()
        datePicker.init(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH), null)

        findButton.setOnClickListener {
            val selectedDay = datePicker.dayOfMonth
            val selectedMonth = datePicker.month
            val selectedYear = datePicker.year

            val message = "Selected Date: $selectedDay/${selectedMonth + 1}/$selectedYear" // Adding 1 to month as it starts from 0
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

            findTemp(selectedDay,selectedMonth,selectedYear);
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun findTemp(date: Int, month: Int, year: Int)
    {
        val curr = Calendar.getInstance().get(Calendar.YEAR)

        if (year <= curr)
        {
            GlobalScope.launch(Dispatchers.IO) {
                val webData = room.TemperatureDataDao().getTemperatureDataByDate("$year-$month-$date")
                if (webData != null)
                {
                    withContext(Dispatchers.Main) {
                        minTemperatureTextView.visibility = TextView.VISIBLE
                        maxTemperatureTextView.visibility = TextView.VISIBLE
                        minTemperatureTextView.text = "Minimum Temperature: ${"%.2f".format(webData.minTemperature)} °C"
                        maxTemperatureTextView.text = "Maximum Temperature: ${"%.2f".format(webData.maxTemperature)} °C"
                    }
                }
                else
                {
                    val cityData = databaseTemp(date,month, year)
                    if (cityData != null)
                    {
                        withContext(Dispatchers.Main) {
                            minTemperatureTextView.visibility = TextView.VISIBLE
                            maxTemperatureTextView.visibility = TextView.VISIBLE
                            minTemperatureTextView.text = "Minimum Temperature: ${"%.2f".format(cityData.first)} °C"
                            maxTemperatureTextView.text = "Maximum Temperature: ${"%.2f".format(cityData.second)} °C"
                        }
                    }
                }
            }
        }
        else
        {
            val tYear = LocalDate.now().year
            val previous10 = (tYear - 10 + 1)..tYear
            var mx = 0.0
            var mn = 0.0
            GlobalScope.launch(Dispatchers.IO) {
                for (py in previous10)
                {
                    val dataValue = databaseTemp(date, month, py)
                    if (dataValue != null)
                    {
                        mx +=dataValue.first
                        mn +=dataValue.second
                    }
                }
                val fY = String.format("%04d", year)
                mn /= 10
                mx /= 10
                val temperatureData = TemperatureData(date = "$fY-$month-$date", maxTemperature = mx, minTemperature = mn)
                room.TemperatureDataDao().insertTemperatureData(temperatureData)

                withContext(Dispatchers.Main) {
                    minTemperatureTextView.visibility = TextView.VISIBLE
                    maxTemperatureTextView.visibility = TextView.VISIBLE
                    minTemperatureTextView.text = "Minimum Temperature: ${"%.2f".format(mn)} °C"
                    maxTemperatureTextView.text = "Maximum Temperature: ${"%.2f".format(mx)} °C"
                }
            }
        }
    }

    private fun databaseTemp(date: Int, month: Int, year: Int): Pair<Double, Double>? {

        val fDate = "%02d".format(date)
        val fMonth = "%02d".format(month)
        val fYear = "%04d".format(year)

        val api1="https://archive-api.open-meteo.com/v1/archive?latitude=22&longitude=79"
        val api2="&hourly=temperature_2m&daily=temperature_2m_max,temperature_2m_min"
        val que="&start_date=${fYear}-${fMonth}-${fDate}&end_date=${fYear}-${fMonth}-${fDate}"
        val query=api1+que+api2
        val req = Request.Builder().url(query).build()

        return try
        {
            val app = OkHttpClient()
            val sol = app.newCall(req).execute()

            if (!sol.isSuccessful)
            {
                throw RuntimeException("Encountered HTTP issue with code: ${sol.code}")
            }

            val result = sol.body?.string()

            if (result != null)
            {
                val jsonVal = JSONObject(result)
                val regular = jsonVal.getJSONObject("daily")
                val minR =regular.getJSONArray("temperature_2m_min")
                val maxR = regular.getJSONArray("temperature_2m_max")

                var maxi: Double
                var mini: Double

                maxi = if (maxR.length() > 0) maxR.getDouble(0) else 0.0
                mini = if (minR.length() > 0) minR.getDouble(0) else 0.0

                val answer = TemperatureData(date = "$fYear-$fMonth-$fDate", maxTemperature = maxi, minTemperature = mini)
                CoroutineScope(Dispatchers.IO).launch { room.TemperatureDataDao().insertTemperatureData(answer) }
                maxi to mini
            }
            else
            {
                return null
            }
        }
        catch (e: Exception)
        {
            null
        }
    }
}