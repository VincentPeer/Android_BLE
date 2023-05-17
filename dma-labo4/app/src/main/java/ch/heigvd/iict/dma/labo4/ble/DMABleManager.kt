package ch.heigvd.iict.dma.labo4.ble

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattCharacteristic.*
import android.bluetooth.BluetoothGattService
import android.content.Context
import android.util.Log
import no.nordicsemi.android.ble.BleManager
import no.nordicsemi.android.ble.data.Data
import java.util.*

class DMABleManager(applicationContext: Context, private val dmaServiceListener: DMAServiceListener? = null) : BleManager(applicationContext) {
    private var servicesMap:  MutableMap<UUID, BluetoothGattService?> = mutableMapOf(
        timeServiceUUID to null,
        symServiceUUID to null
    )
    private var characteristicsMap: MutableMap<UUID, BluetoothGattCharacteristic?> = mutableMapOf(
        currentTimeCharUUID to null,
        integerCharUUID to null,
        temperatureCharUUID to null,
        buttonClickCharUUID to null
    )

    var temp: Int = 0
    fun requestDisconnection() {
        this.disconnect().enqueue()
    }

    override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {
        Log.d(TAG, "isRequiredServiceSupported - discovered services:")
        for (service in gatt.services) {
            if(!servicesMap.containsKey(service.uuid)) // Treats only specific services
                continue
            Log.d("$TAG : service", service.uuid.toString())

            // Add the service to the services map
            servicesMap[service.uuid] = service

            for (characteristic in service.characteristics) {
                if(!characteristicsMap.containsKey(characteristic.uuid)) // Treats only specific characteristics
                    continue
                Log.d("$TAG : characteristic", characteristic.uuid.toString())

                // Add the characteristic to the characteristics map
                characteristicsMap[characteristic.uuid] = characteristic
            }
        }

        // Check that all services and characteristics are present
        if(servicesMap.containsValue(null) or characteristicsMap.containsValue(null)) {
            Log.e(TAG, "This device doesn't have our use requirement")
            return false
        }


        // Check that each characteristic has the required functionalities
        return (
                // Current time requirements
                hasProperties(characteristicsMap[currentTimeCharUUID]!!, (PROPERTY_NOTIFY or PROPERTY_WRITE)) and
                        // Temperature requirements
                        hasProperties(characteristicsMap[temperatureCharUUID]!!, PROPERTY_READ) and
                        // Integer requirements
                        hasProperties(characteristicsMap[integerCharUUID]!!, PROPERTY_WRITE) and
                        // Button click requirements
                        hasProperties(characteristicsMap[buttonClickCharUUID]!!, PROPERTY_NOTIFY)
                )
    }

    override fun initialize() {
        super.initialize()
        /* TODO
            Dans notre cas il s'agit de s'enregistrer pour recevoir les notifications proposées par certaines
            caractéristiques, on en profitera aussi pour mettre en place les callbacks correspondants.
            CF. méthodes setNotificationCallback().with{} et enableNotifications().enqueue()
         */


        setNotificationCallback(characteristicsMap[buttonClickCharUUID]).with {_, data ->
            Log.d(TAG, " : button click notification : $data")
            dmaServiceListener?.clickCountUpdate(data.getIntValue(Data.FORMAT_UINT8,0)!!)
        }
        enableNotifications(characteristicsMap[buttonClickCharUUID]).enqueue()

        setNotificationCallback(characteristicsMap[currentTimeCharUUID]).with { _, data ->
            Log.d(TAG, " : current time notification : $data")
            val year = data.getIntValue(Data.FORMAT_UINT16_LE, 0)
            val month = data.getIntValue(Data.FORMAT_UINT8, 2)
            val day = data.getIntValue(Data.FORMAT_UINT8, 3)
            val hour = data.getIntValue(Data.FORMAT_UINT8, 4)
            val minute = data.getIntValue(Data.FORMAT_UINT8, 5)
            val second = data.getIntValue(Data.FORMAT_UINT8, 6)

            val calendar = Calendar.getInstance()
            calendar.set(year!!, month!!, day!!, hour!!, minute!!, second!!)

            dmaServiceListener?.dateUpdate(calendar)
        }
        enableNotifications(characteristicsMap[currentTimeCharUUID]).enqueue()
    }

    /**
     *
     */
    private fun hasProperties(characteristic: BluetoothGattCharacteristic, requiredProperties: Int) : Boolean {
        return (characteristic.properties and requiredProperties == requiredProperties)
    }

    override fun onServicesInvalidated() {
        super.onServicesInvalidated()

        // Reset services and characteristics
        for(service in servicesMap)
            servicesMap[service.key] = null

        for(characteristic in characteristicsMap)
            characteristicsMap[characteristic.key] = null
    }

    fun readTemperature(): Boolean {
        /* TODO
            on peut effectuer ici la lecture de la caractéristique température
            la valeur récupérée sera envoyée au ViewModel en utilisant le mécanisme
            du DMAServiceListener: Cf. temperatureUpdate()
                Cf. méthode readCharacteristic().with{}.enqueue()
            On placera des méthodes similaires pour les autres opérations
                Cf. méthode writeCharacteristic().enqueue()
        */

        readCharacteristic(characteristicsMap[temperatureCharUUID]).with { _, data ->
            temp = data.getIntValue(Data.FORMAT_UINT16_LE, 0)!! // TODO
            Log.d(TAG, " : temperature read : $temp")
            dmaServiceListener?.temperatureUpdate(temp / 10f)
        }.enqueue()


        //return false //FIXME
        return true
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
