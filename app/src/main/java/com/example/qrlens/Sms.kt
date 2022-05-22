package com.example.qrlens

class Sms {
    fun breakString(s: String): Array<String> {
        return s.split(":").toTypedArray()
    }
}