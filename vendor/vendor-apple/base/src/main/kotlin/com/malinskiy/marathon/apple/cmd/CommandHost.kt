package com.malinskiy.marathon.apple.cmd

import java.nio.charset.Charset

interface CommandHost {
    val id: String
    val charset: Charset
}
