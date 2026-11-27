package com.example.prefixblocker

import android.telecom.Call
import android.telecom.CallScreeningService

class PrefixBlockerService : CallScreeningService() {
    override fun onScreenCall(callDetails: Call.Details) {
        val number = callDetails.handle?.schemeSpecificPart.orEmpty()
        val isIncoming = callDetails.callDirection == Call.Details.DIRECTION_INCOMING
        val block = isIncoming &&
            PrefixStore.isEnabled(this) &&
            PrefixStore.matches(this, number)

        val response = CallResponse.Builder()
            .setDisallowCall(block)
            .setRejectCall(block)
            .setSkipNotification(block)
            .setSkipCallLog(false)
            .build()

        respondToCall(callDetails, response)

        if (block) PrefixStore.addToLog(this, number)
    }
}
