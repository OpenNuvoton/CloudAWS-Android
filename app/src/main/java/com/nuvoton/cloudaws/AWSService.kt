package com.nuvoton.cloudaws

import android.content.Context
import android.os.Handler
import android.os.Looper

import android.util.Log
import com.amazonaws.auth.CognitoCachingCredentialsProvider
import com.amazonaws.regions.Regions
import com.amazonaws.services.iotdata.AWSIotDataClient
import com.amazonaws.services.iotdata.model.GetThingShadowRequest
import kotlin.concurrent.thread

class AWSService  {

    private val _handler = Handler(Looper.getMainLooper())
    private var _resultListener: ((String)->Unit)? = null

    private var _AWSIoTdataClient: AWSIotDataClient? = null
    private var _CognitoIdentityPoolId = ""
    private var _CredentialProvider: CognitoCachingCredentialsProvider? = null
    private var _IoTEndpoint = "a1fljoeglhtf61-ats.iot.us-east-1.amazonaws.com"
    private var _IoTThingName = ""
    private var _Region = Regions.US_EAST_1

    fun init(context:Context,poolId :String , region :Regions):Boolean{

        if(poolId == "") return false

        _CognitoIdentityPoolId = poolId
        _Region = region

        _CredentialProvider = CognitoCachingCredentialsProvider(context,_CognitoIdentityPoolId,_Region)
        _AWSIoTdataClient = AWSIotDataClient(_CredentialProvider)

        if(_AWSIoTdataClient == null) return false

        return true
    }

    fun setIotThing(endPoint:String,thingName:String) {
        if(endPoint == "") return
        if(thingName == "") return
        if(_AWSIoTdataClient == null) return

        _IoTThingName = thingName
        _IoTEndpoint = endPoint

        _AWSIoTdataClient!!.endpoint = _IoTEndpoint
        val shadowRequest = GetThingShadowRequest().withThingName(_IoTThingName)
    }

    fun StartGetShadow(loopTime:Int){

        _handler.post(object : Runnable {
            override fun run() {
                thread {
                    try {
                        val shadowRequest = GetThingShadowRequest().withThingName(_IoTThingName)
                        var result = _AWSIoTdataClient?.getThingShadow(shadowRequest)

                        if (result != null) {
                            val bytes = ByteArray(result.payload.remaining())
                            result.payload.get(bytes)
                            val json = String(bytes)
                            if(_resultListener != null){
                                _resultListener?.let { it(json) }
                            }

                            _handler.postDelayed(Runnable { Thread(this).start() }, 2000)
                        }
                    }
                    catch (e: Exception) {
                        Log.e("AWSService", "error:", e);
                        if(_resultListener != null) {
                            _resultListener?.let { it(e.toString()) }
                        }
                        return@thread
                    }
                }
//                _handler.postDelayed(Runnable { Thread(this).start() }, 2000)
            }
        })

    }

    fun addShadowRequestListener(callbacks: (String)->Unit){
        _resultListener = callbacks
    }
}

