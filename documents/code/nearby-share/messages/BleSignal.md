<!-- Source: https://developers.google.com/android/reference/com/google/android/gms/nearby/messages/BleSignal -->

# BleSignal

![Spark icon](/_static/images/icons/spark.svg)

## AI-generated Key Takeaways

outlined\_flag

* BleSignal represents properties of the BLE signal associated with a Message.
* It provides methods to get the received signal strength indicator (RSSI) and transmission power level (TxPower).
* UNKNOWN\_TX\_POWER is a constant indicating an unknown transmission power level.



public interface
**BleSignal**

Represents properties of the BLE signal associated with a
`Message`
.

### Constant Summary

|  |  |  |
| --- | --- | --- |
| int | [UNKNOWN\_TX\_POWER](/android/reference/com/google/android/gms/nearby/messages/BleSignal#UNKNOWN_TX_POWER) | Unknown transmission power level. |

### Public Method Summary

|  |  |
| --- | --- |
| abstract int | [getRssi](/android/reference/com/google/android/gms/nearby/messages/BleSignal#getRssi()) () Returns the received signal strength indicator (RSSI) in dBm. |
| abstract int | [getTxPower](/android/reference/com/google/android/gms/nearby/messages/BleSignal#getTxPower()) () Returns the transmission power level at 1 meter, in dBm. |







## Constants

#### public static final int **UNKNOWN\_TX\_POWER**

Unknown transmission power level. See
`getTxPower()`
.

Constant Value:

-2147483648







## Public Methods

#### public abstract int **getRssi** ()

Returns the received signal strength indicator (RSSI) in dBm. The valid range is
[-127, 127], inclusive.

This is a weighted average of sightings, with later sightings having more
weight.

#### public abstract int **getTxPower** ()

Returns the transmission power level at 1 meter, in dBm. Returns
`UNKNOWN_TX_POWER`
if the advertiser did not report its transmission
power.