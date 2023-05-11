package ch.heigvd.iict.dma.labo4.ble

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.content.Context
import android.util.Log
import no.nordicsemi.android.ble.BleManager
import java.util.*

class DMABleManager(applicationContext: Context, private val dmaServiceListener: DMAServiceListener? = null) : BleManager(applicationContext) {

    //Services and Characteristics of the SYM Pixl
    private var timeService: BluetoothGattService? = null
    private var symService: BluetoothGattService? = null
    private var currentTimeChar: BluetoothGattCharacteristic? = null
    private var integerChar: BluetoothGattCharacteristic? = null
    private var temperatureChar: BluetoothGattCharacteristic? = null
    private var buttonClickChar: BluetoothGattCharacteristic? = null

    fun requestDisconnection() {
        this.disconnect().enqueue()
    }


    private var servicesMap = mutableMapOf(
        timeServiceUUID to timeService,
        symServiceUUID to symService
    )
    private var charMap = mutableMapOf(
        currentTimeCharUUID to currentTimeChar,
        integerCharUUID to integerChar,
        temperatureCharUUID to temperatureChar,
        buttonClickCharUUID to buttonClickChar
    )
    override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {

        Log.d(TAG, "isRequiredServiceSupported - discovered services:")
        for (service in gatt.services) {
            Log.d(TAG, service.uuid.toString())
            val bluetoothGattService = BluetoothGattService(service.uuid, service.type)

            servicesMap[service.uuid] = bluetoothGattService


            for (characteristic in service.characteristics) {
                Log.d(TAG, characteristic.uuid.toString())
                val bluetoothGattCharacteristic = BluetoothGattCharacteristic(characteristic.uuid, characteristic.properties, characteristic.permissions)

                charMap[characteristic.uuid] = bluetoothGattCharacteristic


            }
        }



        /* TODO
            - Nous devons vérifier ici que le périphérique auquel on vient de se connecter possède
              bien tous les services et les caractéristiques attendus, on vérifiera aussi que les
              caractéristiques présentent bien les opérations attendues
            - On en profitera aussi pour garder les références vers les différents services et
              caractéristiques (déclarés en lignes 14 à 19)
        */

        return false //FIXME si tout est OK, on doit retourner true
        // sinon la librairie appelera la méthode onDeviceDisconnected() avec le flag REASON_NOT_SUPPORTED
    }

    override fun initialize() {
        super.initialize()
        /* TODO
            Ici nous somme sûr que le périphérique possède bien tous les services et caractéristiques
            attendus et que nous y sommes connectés. Nous pouvous effectuer les premiers échanges BLE.
            Dans notre cas il s'agit de s'enregistrer pour recevoir les notifications proposées par certaines
            caractéristiques, on en profitera aussi pour mettre en place les callbacks correspondants.
            CF. méthodes setNotificationCallback().with{} et enableNotifications().enqueue()
         */
    }

    override fun onServicesInvalidated() {
        super.onServicesInvalidated()
        //we reset services and characteristics
        timeService = null
        currentTimeChar = null
        symService = null
        integerChar = null
        temperatureChar = null
        buttonClickChar = null
    }

    fun readTemperature(): Boolean {
        /* TODO
            on peut effectuer ici la lecture de la caractéristique température
            la valeur récupérée sera envoyée à au ViewModel en utilisant le mécanisme
            du DMAServiceListener: Cf. temperatureUpdate()
                Cf. méthode readCharacteristic().with{}.enqueue()
            On placera des méthodes similaires pour les autres opérations
                Cf. méthode writeCharacteristic().enqueue()
        */

        return false //FIXME
    }

    companion object {
        private val TAG = DMABleManager::class.java.simpleName
        private val timeServiceUUID = UUID.fromString("00001805-0000-1000-8000-00805f9b34fb")
        private val symServiceUUID =  UUID.fromString("3c0a1000-281d-4b48-b2a7-f15579a1c38f")
        private val currentTimeCharUUID = UUID.fromString("00002A2B-0000-1000-8000-00805f9b34fb")
        private val integerCharUUID = UUID.fromString("3c0a1001-281d-4b48-b2a7-f15579a1c38f")
        private val temperatureCharUUID = UUID.fromString("3c0a1002-281d-4b48-b2a7-f15579a1c38f")
        private val buttonClickCharUUID = UUID.fromString("3c0a1003-281d-4b48-b2a7-f15579a1c38f")
    }

}

interface DMAServiceListener {
    fun dateUpdate(date : Calendar)
    fun temperatureUpdate(temperature : Float)
    fun clickCountUpdate(clickCount : Int)
}
