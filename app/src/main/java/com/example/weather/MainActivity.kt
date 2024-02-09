package com.example.weather

import android.Manifest
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.SearchView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.weather.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {
    private val binding:ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        fusedLocationProviderClient=LocationServices.getFusedLocationProviderClient(this)
        getCurrentLocation()

        fetchWeatherData("jaipur")
        searchCity()
    }
    private fun getCurrentLocation(){
        if(checkPermissions()){
            if(isLocationEnabled()){
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ){
                    requestPermission()
                    return
                }
                fusedLocationProviderClient.lastLocation.addOnCompleteListener { task->
                    val location:Location?=task.result
                    if(location==null){
                        Toast.makeText(this,"Null Received",Toast.LENGTH_SHORT).show()
                    }
                    else{
                        Toast.makeText(this,"Get Success",Toast.LENGTH_SHORT).show()
                        Log.e(TAG, "getCurrentLocation: "+location.longitude+location.latitude)
                        val geocoder = Geocoder(this,Locale.getDefault())
                        val addresses: List<Address>? = geocoder.getFromLocation(location.longitude,location.latitude,1,)
                        val cityName: String = addresses!![0].getAddressLine(0)
                    }
                }
            }
            else{
                Toast.makeText(this,"Turn On Location",Toast.LENGTH_SHORT).show()
                val intent= Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
        }
        else{
            requestPermission()
        }
    }
    companion object{
        private const val PERMISSION_REQUEST_ACCESS_LOACATION=100
    }
    private fun checkPermissions():Boolean{
        if(ActivityCompat.checkSelfPermission(this,android.Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,android.Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED){
            return true
        }
        return false
    }
    private fun requestPermission(){
        ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION,android.Manifest.permission.ACCESS_FINE_LOCATION),
            PERMISSION_REQUEST_ACCESS_LOACATION)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode== PERMISSION_REQUEST_ACCESS_LOACATION){
            if(grantResults.isNotEmpty() &&grantResults[0]==PackageManager.PERMISSION_GRANTED ){
                Toast.makeText(applicationContext,"GRANTED",Toast.LENGTH_SHORT).show()
            }
            else{
                Toast.makeText(applicationContext,"DENIED",Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun isLocationEnabled(): Boolean {
        val locationManager:LocationManager=getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)||locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }
    private fun searchCity() {
        val searchView=binding.searchView
        searchView.setOnQueryTextListener(object :SearchView.OnQueryTextListener,
            androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (query != null) {
                    fetchWeatherData(query)
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return true
            }

        })
    }

    private fun fetchWeatherData(cityname:String) {
        val retrofit=Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl("https://api.openweathermap.org/data/2.5/")
            .build().create(APIInterface::class.java)
        val response=retrofit.getWeatherData("jaipur","dfa980f994c057754df421108d096eb6","metric")
        response.enqueue(object :Callback<WeatherApp>{
            override fun onResponse(call: Call<WeatherApp>, response: Response<WeatherApp>) {
                val respnonseBody=response.body()
                val temp= respnonseBody?.main?.temp
                val humidity=respnonseBody?.main?.humidity
                val windSpeed=respnonseBody?.wind?.speed
                val sunRise=respnonseBody?.sys?.sunrise?.toLong()
                val sunSet=respnonseBody?.sys?.sunset?.toLong()
                val seaLevel=respnonseBody?.main?.pressure
                val condtion=respnonseBody?.weather?.firstOrNull()?.main?:"unknown"
                val maxTemp=respnonseBody?.main?.temp_max
                val minTemp=respnonseBody?.main?.temp_min
                binding.temperature.text=temp.toString()+"°C"
                binding.Conditions.text=condtion
                binding.Humidity.text=humidity.toString()+" %"
                binding.Sunrise.text=time(sunRise)
                binding.Sunset.text=time(sunSet)
                binding.Sea.text=seaLevel.toString()+" hPA"
                binding.Windspeed.text=windSpeed.toString()+" m/s"
                binding.maxTemp.text=maxTemp.toString()+"°C"
                binding.minTemp.text=minTemp.toString()+"°C"
                binding.weather.text=condtion
                binding.cityName.text=cityname.capitalize()
                binding.day.text=dayName(System.currentTimeMillis())
                binding.date.text=date()
                Log.e(TAG, "onResponse:"+temp)
                changeImage(condtion)
            }

            override fun onFailure(call: Call<WeatherApp>, t: Throwable) {
                TODO("Not yet implemented")
            }

        })

    }

    private fun changeImage(conditions:String) {
        when(conditions){
            "Partly Clouds","Overcast","Clouds","Mist","Foggy"->{
                binding.root.setBackgroundResource(R.drawable.colud_background)
                binding.lottieAnimationView.setAnimation(R.raw.cloud)
            }
            "Clear Sky","Sunny","Clear"->{
                binding.root.setBackgroundResource(R.drawable.sunny_background)
                binding.lottieAnimationView.setAnimation(R.raw.sun)
            }
            "Light Rain","Drizzle","Showers","Moderate Rain","Heavy Rain"->{
                binding.root.setBackgroundResource(R.drawable.rain_background)
                binding.lottieAnimationView.setAnimation(R.raw.rain)
            }
            "Light Snow","Moderate Snow","Heavy Snow","Blizzard"->{
                binding.root.setBackgroundResource(R.drawable.snow_background)
                binding.lottieAnimationView.setAnimation(R.raw.snow)
            }
            else->{
                binding.root.setBackgroundResource(R.drawable.sunny_background)
                binding.lottieAnimationView.setAnimation(R.raw.sun)


            }

        }
        binding.lottieAnimationView.playAnimation()
    }

    fun date(): String {
        val sdf=SimpleDateFormat("dd MMMM yyyy",Locale.getDefault())
        return sdf.format((Date()))
    }
    fun time(timeStamp: Long?): String {
        val sdf=SimpleDateFormat("HH:mm",Locale.getDefault())
        if (timeStamp != null) {
            return sdf.format((Date(timeStamp*1000)))
        }
        return "";
    }

    fun dayName(timeStamp:Long):String{
        val sdf=SimpleDateFormat("EEEE", Locale.getDefault())
        return sdf.format((Date()))
    }

}