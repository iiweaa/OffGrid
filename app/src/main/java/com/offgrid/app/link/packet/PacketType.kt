package com.offgrid.app.link.packet

/**
 * Mesh packet payload type.
 *
 * Values are stable and must not be reordered without bumping the packet
 * [version][Packet.CURRENT_VERSION].
 */
enum class PacketType(val code: Int) {
    HELLO(0),
    VOICE(1),
    LOCATION(2),
    SIGNAL(3);

    companion object {
        private val BY_CODE = entries.associateBy { it.code }

        fun fromCode(code: Int): PacketType? = BY_CODE[code]
    }
}
