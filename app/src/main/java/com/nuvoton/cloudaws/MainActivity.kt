package com.nuvoton.cloudaws

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.amazonaws.regions.Regions
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.preference.PreferenceManager

import com.github.mikephil.charting.charts.LineChart

/**
 * Created by nuvoton WPHU on 2021/10
 */

class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"

    private var _AWS_GetTimes = 0
    private var _AWS_Cognito_Identity_Pool_ID = "us-east-2:f7c9d0c0-2d71-4395-902d-6e0679af3d09"//"us-east-1:9e41d4ca-03a7-4af0-a6ec-0bc5b5814781"
    private var _AWS_IoT_Endpoint = "https://a1fljoeglhtf61-ats.iot.us-east-2.amazonaws.com"//"a1fljoeglhtf61-ats.iot.us-east-1.amazonaws.com"
    private var _AWS_IoT_Thing_Name = "Nuvoton-Mbed-D001"//"Nuvoton-RTOS-D002"
    private var _AWS_IoT_Region = Regions.US_EAST_2
    private val _AWS_Service = AWSService()

    private lateinit var _MPM : MPChartManager

    private lateinit var _SelectButton: Button
    private lateinit var _SettingButton: Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val LineChart = findViewById<View>(R.id.MPChart) as LineChart
        _MPM =  MPChartManager(LineChart,this)

        _SelectButton = findViewById<View>(R.id.Select_Button) as Button
        _SelectButton!!.setOnClickListener(onClickSelectButton)

        _SettingButton = findViewById<View>(R.id.Setting_Button) as Button
        _SettingButton!!.setOnClickListener(onClickSettingButton)


    }

    private val onClickSelectButton = View.OnClickListener {

        val displayNameArray = ArrayList<String>()
        displayNameArray.add("ALL")
        for(ldsName in _MPM.getLineLabelNameArray()){
            displayNameArray.add(ldsName)
        }

        var singleChoiceIndex = 0
        AlertDialog.Builder(this@MainActivity)
            .setSingleChoiceItems(displayNameArray.toTypedArray(), singleChoiceIndex
            ) { _, which -> singleChoiceIndex = which }
            .setPositiveButton("ok") { dialog, _ ->
//                Toast.makeText(this@MainActivity, "你選擇的是" + singleChoiceIndex, Toast.LENGTH_SHORT).show()
                _MPM.setDisplayLine(displayNameArray.get(singleChoiceIndex))
                dialog.dismiss()
            }
            .show()
    }

    private val onClickSettingButton = View.OnClickListener {

        val intent = Intent(this, SettingActivity::class.java)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()

        GetAWS_Setting()//取得最新設定組

        _AWS_Service.init(this,_AWS_Cognito_Identity_Pool_ID,_AWS_IoT_Region)
        _AWS_Service.setIotThing(_AWS_IoT_Endpoint,_AWS_IoT_Thing_Name)
        _AWS_Service.StartGetShadow(1)
        _AWS_Service.addShadowRequestListener {
            Log.i(TAG,"rtsp:"+it)

            if(it.indexOf("state") < 0 ){
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "connection failed. Please check setting.", Toast.LENGTH_SHORT).show()
                }
                return@addShadowRequestListener
            }

            val map: Map<String, Any?> = Gson().fromJson(it, object: TypeToken<Any>() {}.type)
            val state = map["state"] as Map<String, Any?>
            val reported = state["reported"] as Map<String, Any?>
            val hashmap = HashMap<String, Any?>()
            hashmap.putAll(reported)
            val temperature = hashmap["temperature"].toString()
            val clientName = hashmap["clientName"].toString()
            Log.i(TAG,"state:"+state+ "   reported:"+reported)
            Log.i(TAG,"temperature:" + temperature)
            Log.i(TAG,"clientName:" + clientName)

            if (temperature != null) {

                val lastPoint = _MPM.getLastValue()
                if(lastPoint.name == clientName && lastPoint.value == temperature.toFloat()){
                    if(_AWS_GetTimes <= 5){
                        _AWS_GetTimes = _AWS_GetTimes + 1
                        return@addShadowRequestListener
                    }
                }

                _AWS_GetTimes = 0

                runOnUiThread {

                    _MPM.addChartDataSet(clientName)
                    _MPM.addEntry(clientName,temperature.toFloat())

                }
            }
        }

    }

    fun GetAWS_Setting() {

        val defaultPref = PreferenceManager.getDefaultSharedPreferences(this)

        val regionString = defaultPref.getString("pref_aws_region",null)
        if (regionString == null) {
            Toast.makeText(this@MainActivity, "Setting null. Please set.", Toast.LENGTH_SHORT).show()
            return
        }else{
            _AWS_IoT_Region = Regions.fromName(regionString.toString())
        }

        val cognitoPoolId = defaultPref.getString("pref_aws_cognito_pool_id",null)
        if (cognitoPoolId == null) {
            Toast.makeText(this@MainActivity, "Setting null. Please set.", Toast.LENGTH_SHORT).show()
            return
        }else{
            _AWS_Cognito_Identity_Pool_ID = cognitoPoolId.toString()
        }

        val iotEndpoint = defaultPref.getString("pref_aws_iot_endpoint",null)
        if (iotEndpoint == null) {
            Toast.makeText(this@MainActivity, "Setting null. Please set.", Toast.LENGTH_SHORT).show()
            return
        }else{
            _AWS_IoT_Endpoint = iotEndpoint.toString()
        }

        val iotThingName = defaultPref.getString("pref_aws_iot_thing_name",null)
        if (iotThingName == null) {
            Toast.makeText(this@MainActivity, "Setting null. Please set.", Toast.LENGTH_SHORT).show()
            return
        }else{
            _AWS_IoT_Thing_Name = iotThingName.toString()
        }
    }
}