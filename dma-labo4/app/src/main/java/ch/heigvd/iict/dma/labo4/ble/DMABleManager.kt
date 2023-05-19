package ch.heigvd.iict.dma.labo4.ble

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattCharacteristic.*
import android.bluetooth.BluetoothGattService
import android.content.Context
import android.util.Log
import no.nordicsemi.android.ble.BleManager
import no.nordicsemi.android.ble.data.Data
import java.nio.ByteBuffer
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

        setNotificationCallback(characteristicsMap[buttonClickCharUUID]).with { _, data ->
            Log.d(TAG, " : button click notification : $data")
            dmaServiceListener?.clickCountUpdate(data.getIntValue(Data.FORMAT_UINT8,0)!!)
        }
        enableNotifications(characteristicsMap[buttonClickCharUUID]).enqueue()

        setNotificationCallback(characteristicsMap[currentTimeCharUUID]).with { _, data ->
            val year = data.getIntValue(Data.FORMAT_UINT16_LE, 0)
            val month = data.getIntValue(Data.FORMAT_UINT8, 2)?.minus(1)
            val day = data.getIntValue(Data.FORMAT_UINT8, 3)
            val hour = data.getIntValue(Data.FORMAT_UINT8, 4)
            val minute = data.getIntValue(Data.FORMAT_UINT8, 5)
            val second = data.getIntValue(Data.FORMAT_UINT8, 6)

            val calendar = Calendar.getInstance()
            calendar.set(year!!, month!!, day!!, hour!!, minute!!, second!!)
            Log.d(TAG, " : current time notification : ${calendar.time}")

            dmaServiceListener?.dateUpdate(calendar)
        }
        enableNotifications(characteristicsMap[currentTimeCharUUID]).enqueue()
    }

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
        readCharacteristic(characteristicsMap[temperatureCharUUID]).with { _, data ->
            val temp = data.getIntValue(Data.FORMAT_UINT16_LE, 0)?.div(10f)
            Log.d(TAG, " : temperature read : $temp")
            dmaServiceListener?.temperatureUpdate(temp!!)
        }.enqueue()

        return true
    }

    fun sendCurrentTime() : Boolean {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val second = calendar.get(Calendar.SECOND)

        val data = ByteArray(7)
        data[0] = (year and 0xFF).toByte()
        data[1] = ((year shr 8) and 0xFF).toByte()
        data[2] = (month and 0xFF).toByte()
        data[3] = (day and 0xFF).toByte()
        data[4] = (hour and 0xFF).toByte()
        data[5] = (minute and 0xFF).toByte()
        data[6] = (second and 0xFF).toByte()

        writeCharacteristic(characteristicsMap[currentTimeCharUUID], data).enqueue()
        // writeCharacteristic(characteristicsMap[currentTimeCharUUID], data, WRITE_TYPE_DEFAULT).enqueue()
        return true
    }

    fun sendValue(value: Int) :Boolean {
        val buffer = ByteBuffer.allocate(4)
        buffer.putInt(value)

        writeCharacteristic(characteristicsMap[integerCharUUID], Data(buffer.array()))
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
