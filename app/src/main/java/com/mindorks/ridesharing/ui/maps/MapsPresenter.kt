package com.mindorks.ridesharing.ui.maps


import com.google.android.gms.maps.model.LatLng
import com.mindorks.ridesharing.data.network.NetworkService
import com.mindorks.ridesharing.simulator.WebSocket
import com.mindorks.ridesharing.simulator.WebSocketListener
import com.mindorks.ridesharing.utils.Constants
import org.json.JSONArray
import org.json.JSONObject

class MapsPresenter(private val networkService: NetworkService) : WebSocketListener {

    private var view: MapsView? = null
    lateinit var webSocket: WebSocket

    fun onAttach(view: MapsView) {
        this.view = view
        webSocket = networkService.createWebSocket(this)
        webSocket.connect()
    }

    fun onDetach() {
        webSocket.disconnect()
    }

    fun requestNearByCabs(latLng: LatLng) {
        val jsonObject = JSONObject()
        jsonObject.put(Constants.TYPE, Constants.NEAR_BY_CABS)
        jsonObject.put(Constants.LAT, latLng.latitude)
        jsonObject.put(Constants.LNG, latLng.longitude)
        webSocket.sendMessage(jsonObject.toString())
    }

    override fun onConnect() {

    }

    override fun onMessage(data: String) {
        val jsonObject = JSONObject(data)
        when (jsonObject.getString(Constants.TYPE)) {
            Constants.NEAR_BY_CABS -> {
                handleOnMessageNearByCabs(jsonObject)
            }
        }
    }

    private fun handleOnMessageNearByCabs(jsonObject: JSONObject) {
        val nearByCabLocations = arrayListOf<LatLng>()
        val jsonArray = jsonObject.getJSONArray(Constants.LOCATIONS)
        for (i in 0 until jsonArray.length()) {
            val lat = (jsonArray.get(i) as JSONObject).getDouble(Constants.LAT)
            val lng = (jsonArray.get(i) as JSONObject).getDouble(Constants.LNG)
            val latLng = LatLng(lat, lng)
            nearByCabLocations.add(latLng)
        }
        view?.run {
            showNearByCabs(nearByCabLocations)
        }
    }

    override fun onDisconnect() {

    }

    override fun onError(error: String) {

    }

}